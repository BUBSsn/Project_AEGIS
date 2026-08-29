package io.github.jhundeniel.ArithmeticHeroes.systems;

import io.github.jhundeniel.ArithmeticHeroes.battle.DamageTracker;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleState;
import io.github.jhundeniel.ArithmeticHeroes.battle.EnemyAI;
import io.github.jhundeniel.ArithmeticHeroes.components.*;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.factories.EntityFactory;
import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.TurnManager;

import java.util.List;

public class BattleStateSystem extends EntitySystem {
    private final TurnManager turnManager;
    private final EnemyAI enemyAI;
    private final ActionLogSystem actionLog;
    private final List<Entity> heroes;
    private final List<Entity> enemies;
    private final BattleAnimations animations; // ← NEW
    private final ArithmeticAssetManager assets;
    private final EntityFactory factory;

    // ── Chat bubbles ───────────────────────────────────────────────────
    private io.github.jhundeniel.ArithmeticHeroes.battle.ChatBubbleSystem chatBubbles;
    private io.github.jhundeniel.ArithmeticHeroes.managers.DialogueManager dialogueManager;

    public void setChatBubbles(io.github.jhundeniel.ArithmeticHeroes.battle.ChatBubbleSystem cb,
            io.github.jhundeniel.ArithmeticHeroes.managers.DialogueManager dm) {
        this.chatBubbles = cb;
        this.dialogueManager = dm;
    }

    private final ComponentMapper<TypeComponent> tm = ComponentMapper.getFor(TypeComponent.class);
    private final ComponentMapper<IntentComponent> im = ComponentMapper.getFor(IntentComponent.class);

    private final ComponentMapper<PassiveComponent> pm = ComponentMapper.getFor(PassiveComponent.class);

    private boolean hasTwinSpawned = false;

    public boolean getHasTwinSpawned() { return hasTwinSpawned; }
    public void setHasTwinSpawned(boolean value) { this.hasTwinSpawned = value; }

    // Timer for the loading of text
    private float stateTimer = 0f;
    private static final float TEXT_DELAY = io.github.jhundeniel.ArithmeticHeroes.config.GameConfig.TEXT_DELAY;

    public BattleStateSystem(TurnManager turnManager, EnemyAI enemyAI, ActionLogSystem actionLog,
            List<Entity> heroes, List<Entity> enemies,
            BattleAnimations animations, ArithmeticAssetManager assets, EntityFactory factory) {
        this.turnManager = turnManager;
        this.enemyAI = enemyAI;
        this.actionLog = actionLog;
        this.heroes = heroes;
        this.enemies = enemies;
        this.animations = animations;
        this.assets = assets;
        this.factory = factory;
    } // ← constructor closes HERE — this was the missing brace

    @Override
    public void update(float deltaTime) {
        // Do nothing once GAME_OVER is set — BattleScreen owns the overlay
        if (turnManager.getState() == BattleState.GAME_OVER)
            return;

        // GLOBAL DEATH CHECK
        // Catch instant-wipes from Passives before the game asks a dead hero for input!
        boolean heroesAlive = false;
        for (Entity h : heroes) {
            StatsComponent s = h.getComponent(StatsComponent.class);
            if (s != null && s.hp > 0)
                heroesAlive = true;
        }

        // If everyone is dead, hijack the state machine and force a Game Over check!
        if (!heroesAlive && turnManager.getState() != BattleState.CHECK_WIN_LOSS) {
            turnManager.setState(BattleState.CHECK_WIN_LOSS);
        }
        // ─────────────────────────────

        Entity currentEntity = turnManager.getCurrentEntityTurn();

        switch (turnManager.getState()) {
            case ROUND_START:
                tickRoundBuffs();
                turnManager.startNextRound();
                triggerRoundStartPassives();
                rollEnemyIntents();
                DamageTracker.reset();
                determineWhoseTurn(turnManager.getCurrentEntityTurn());
                announceTurn(turnManager.getCurrentEntityTurn());
                break;

            case WAIT_FOR_INPUT:
            case SELECT_TARGET:
                break;

            case ACTION_QUEUED:
                break;

            case CHOOSE_VALUE:
                break;

            case ENEMY_TURN:
                if (im.has(currentEntity)) {
                    IntentComponent intent = im.get(currentEntity);
                    currentEntity.add(new ActionRequestComponent(intent.actionType, intent.target));
                    currentEntity.remove(IntentComponent.class);
                    turnManager.setState(BattleState.ACTION_QUEUED);
                } else {
                    ActionRequestComponent action = enemyAI.decideAction(currentEntity, heroes, enemies, false);
                    if (action != null) {
                        currentEntity.add(action);
                        turnManager.setState(BattleState.ACTION_QUEUED);
                    } else {
                        turnManager.setState(BattleState.TURN_END);
                    }
                }
                break;

            case ANIMATING:
                // ← Wait for boss attack overlay to finish before counting text delay
                if (animations != null && !animations.isBossAttackDone())
                    break;
                stateTimer += deltaTime;
                if (stateTimer >= TEXT_DELAY) {
                    turnManager.setState(BattleState.CHECK_WIN_LOSS);
                    stateTimer = 0f;
                }
                break;

            case CHECK_WIN_LOSS:
                checkWinLossConditions();
                if (turnManager.getState() == BattleState.CHECK_WIN_LOSS) {
                    turnManager.setState(BattleState.TURN_END);
                }
                break;

            case TURN_END:
                turnManager.advanceTurn();
                if (turnManager.getState() != BattleState.ROUND_START) {
                    determineWhoseTurn(turnManager.getCurrentEntityTurn());
                }
                break;

            case GAME_OVER:
                // Handled by BattleScreen — do nothing here
                break;
        }
    }

    private void determineWhoseTurn(Entity entity) {
        if (entity == null)
            return;

        StatsComponent stats = entity.getComponent(StatsComponent.class);
        if (stats != null && stats.hp <= 0) {
            System.out.println("[BattleStateSystem] Skipping dead entity: " + stats.name.trim());
            turnManager.advanceTurn();
            if (turnManager.getState() != BattleState.ROUND_START) {
                determineWhoseTurn(turnManager.getCurrentEntityTurn());
            }
            return;
        }

        TypeComponent type = tm.get(entity);
        if (type != null && type.type == Operator.MOB) {
            turnManager.setState(BattleState.ENEMY_TURN);
        } else {
            turnManager.setState(BattleState.WAIT_FOR_INPUT);
        }
    }

    private void announceTurn(Entity entity) {
        if (entity == null)
            return;
        StatsComponent stats = entity.getComponent(StatsComponent.class);
        if (stats != null) {
            actionLog.addMessage(">>> " + stats.name.trim() + "'s turn!");
        }

        PassiveComponent pc = entity.getComponent(PassiveComponent.class);
        if (pc != null && pc.passive != null) {
            pc.passive.onTurnStart(entity);
        }
    }

    private void triggerRoundStartPassives() {
        int currentRound = turnManager.getCurrentRound();

        // 1. Trigger Enemy Passives (Boss Gimmicks!)
        for (Entity enemy : enemies) {
            if (pm.has(enemy) && pm.get(enemy).passive != null) {
                pm.get(enemy).passive.onRoundStart(enemy, heroes, enemies, currentRound);
            }
        }

        // 2. Trigger Hero Passives
        for (Entity hero : heroes) {
            if (pm.has(hero) && pm.get(hero).passive != null) {
                pm.get(hero).passive.onRoundStart(hero, heroes, enemies, currentRound);
            }
        }
    }

    private void tickRoundBuffs() {
        List<Entity> allCombatants = new java.util.ArrayList<>();
        allCombatants.addAll(heroes);
        allCombatants.addAll(enemies);

        for (Entity e : allCombatants) {
            StatusEffect costBuff = StatusEffects.get(e, StatusEffect.Type.COST_REDUCTION);
            if (costBuff != null) {
                costBuff.turnsRemaining--;
                if (costBuff.turnsRemaining <= 0) {
                    StatusEffects.remove(e, StatusEffect.Type.COST_REDUCTION);
                    StatsComponent stats = e.getComponent(StatsComponent.class);
                    if (stats != null) {
                        actionLog.addMessage(stats.name.trim() + "'s Cost Reduction expired.");
                    }
                }
            }
        }
    }

    private void checkWinLossConditions() {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Entity e = enemies.get(i);
            StatsComponent s = e.getComponent(StatsComponent.class);
            if (s != null && s.hp <= 0) {
                actionLog.addMessage(s.name.trim() + " was defeated!");
                assets.playSound(io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager.SFX_ENEMY_DEATH);
                enemies.remove(i);
                turnManager.removeEntity(e);
                getEngine().removeEntity(e);
            }
        }

        for (int i = heroes.size() - 1; i >= 0; i--) {
            Entity e = heroes.get(i);
            StatsComponent s = e.getComponent(StatsComponent.class);
            if (s != null && s.hp <= 0) {
                actionLog.addMessage(s.name.trim() + " fell in battle!");
                assets.playSound(io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager.SFX_HERO_DEAD);
                heroes.remove(i);
                turnManager.removeEntity(e);
                getEngine().removeEntity(e);
            }
        }

        for (int i = 0; i < enemies.size(); i++) {
            Entity enemy = enemies.get(i);
            TypeComponent type = tm.get(enemy);
            if (type != null && "ENEMY_BOSS3".equals(type.registryKey)) {
                StatsComponent stats = enemy.getComponent(StatsComponent.class);

                // Count how many Boss 3s are currently alive
                long twinCount = enemies.stream().filter(e -> "ENEMY_BOSS3".equals(tm.get(e).registryKey)).count();

                // --- ADD THE LOCK CHECK HERE! ---
                // If only 1 exists, they drop to 75% HP, AND we haven't spawned a twin yet...
                if (!hasTwinSpawned && twinCount == 1 && stats.hp <= (stats.maxHp * 0.75f) && stats.hp > 0) {

                    hasTwinSpawned = true; // LOCK IT DOWN! It can never split again.

                    actionLog.addMessage("The Equation split into Twins!");

                    io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent vm = enemy
                            .getComponent(io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent.class);

                    // Dynamically grab StageSystem to fetch the texture
                    StageSystem stageSys = getEngine().getSystem(StageSystem.class);

                    // Spawn the twin slightly to the left
                    Entity twin = factory.createEnemy("ENEMY_BOSS3", vm.x - 250f, vm.y,
                            stageSys.getEnemySheet("ENEMY_BOSS3"),
                            io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager.ENEMY_FRAMES);
                    twin.getComponent(PortraitComponent.class).texture = stageSys.getEnemyIcon("ENEMY_BOSS3");

                    // Sync their HP so they start perfectly balanced
                    twin.getComponent(StatsComponent.class).hp = stats.hp;

                    // Register the new twin into the current battle!
                    enemies.add(twin);
                    turnManager.addEntityMidBattle(twin);
                }
            }
        }

        if (heroes.isEmpty()) {
            actionLog.addMessage("GAME OVER... The math was too hard.");
            turnManager.setState(BattleState.GAME_OVER);
        } else if (enemies.isEmpty()) {
            actionLog.addMessage("VICTORY! Moving to the next stage...");
            // VICTORY BUBBLES
            if (chatBubbles != null && dialogueManager != null) {
                for (Entity hero : heroes) {
                    StatsComponent s = hero.getComponent(StatsComponent.class);
                    // If the hero is alive when the battle ends, they celebrate!
                    if (s != null && s.hp > 0) {
                        String line = dialogueManager.getCombatLine("onVictory", s.name.trim());
                        if (line != null)
                            chatBubbles.showBubble(hero, line);
                    }
                }
            }
            StageSystem stageSys = getEngine().getSystem(StageSystem.class);
            if (stageSys != null)
                stageSys.advanceStage();
        } else {
            turnManager.setState(BattleState.TURN_END);
        }
    }

    private void rollEnemyIntents() {
        for (Entity mob : enemies) {
            StatsComponent stats = mob.getComponent(StatsComponent.class);
            if (stats != null && stats.hp > 0 && !im.has(mob)) {
                ActionRequestComponent plannedAction = enemyAI.decideAction(mob, heroes, enemies, true);
                if (plannedAction != null) {
                    boolean isAttack = (plannedAction.actionType == ActionRequestComponent.ActionType.ENEMY_ATTACK ||
                            plannedAction.actionType == ActionRequestComponent.ActionType.ENEMY_AOE_ATTACK);
                    mob.add(new IntentComponent(plannedAction.actionType, plannedAction.target, isAttack));
                }
            }
        }
    }
}
