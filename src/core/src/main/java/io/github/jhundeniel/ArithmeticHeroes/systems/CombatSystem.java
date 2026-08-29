package io.github.jhundeniel.ArithmeticHeroes.systems;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;

import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleState;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.config.GameConfig;
import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.TurnManager;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.skills.addition.AdditionBuffSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.addition.GroupHealSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.addition.HealSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.addition.ManaTransferSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.division.BattleEqualizerSKill;
import io.github.jhundeniel.ArithmeticHeroes.skills.division.BurdenSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.division.CostReductionSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.division.GroupBurdenSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.enemies.BasicEnemyAOEAttack;
import io.github.jhundeniel.ArithmeticHeroes.skills.enemies.BasicEnemyAttack;
import io.github.jhundeniel.ArithmeticHeroes.skills.enemies.BasicEnemyBuff;
import io.github.jhundeniel.ArithmeticHeroes.skills.enemies.BasicEnemyHeal;
import io.github.jhundeniel.ArithmeticHeroes.skills.multiplication.AmplifySkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.multiplication.GroupAmplifySkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.multiplication.InversionSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.multiplication.SquaredPowerSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.subtraction.ConditionalAttackSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.subtraction.LifeStealAttackSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.subtraction.PokeSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.subtraction.SlamSkill;

import io.github.jhundeniel.ArithmeticHeroes.skills.addition.SingleDrainSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.addition.LifeSiphonSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.addition.ManaStealSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.subtraction.BloodTransferSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.subtraction.SacrificeSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.subtraction.ManaNukeSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.subtraction.DebtTransferSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.division.SingleReflectionSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.division.GroupReflectionSkill;
import io.github.jhundeniel.ArithmeticHeroes.skills.division.UnfairBattleSkill;

public class CombatSystem extends EntitySystem {

    private final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<ActionRequestComponent> am = ComponentMapper.getFor(ActionRequestComponent.class);
    private final ComponentMapper<TypeComponent> tm = ComponentMapper.getFor(TypeComponent.class);
    private final ComponentMapper<VisualComponent> vm = ComponentMapper.getFor(VisualComponent.class);

    private ImmutableArray<Entity> entities;
    private final Map<ActionRequestComponent.ActionType, SkillStrategy> skillMap;
    private final TurnManager turnManager;
    private final ActionLogSystem actionLog;
    private final TurnOrderDisplay turnOrderDisplay;
    private final BattleAnimations animations;
    private final ArithmeticAssetManager assets;

    // ── Chat bubbles (optional, set from BattleScreen) ────────────────────
    private io.github.jhundeniel.ArithmeticHeroes.battle.ChatBubbleSystem chatBubbles;
    private io.github.jhundeniel.ArithmeticHeroes.managers.DialogueManager dialogueManager;

    public void setChatBubbles(io.github.jhundeniel.ArithmeticHeroes.battle.ChatBubbleSystem cb,
            io.github.jhundeniel.ArithmeticHeroes.managers.DialogueManager dm) {
        this.chatBubbles = cb;
        this.dialogueManager = dm;
    }

    public CombatSystem(Engine engine, TurnManager turnManager, ActionLogSystem actionLog,
            TurnOrderDisplay turnOrderDisplay,
            BattleAnimations animations,
            ArithmeticAssetManager assets) {
        this.turnManager = turnManager;
        this.actionLog = actionLog;
        this.turnOrderDisplay = turnOrderDisplay;
        this.animations = animations;
        this.assets = assets;

        skillMap = new HashMap<>();

        // ADDITION
        skillMap.put(ActionRequestComponent.ActionType.HEAL,
                new HealSkill(actionLog, animations, assets.getTexture(ArithmeticAssetManager.ANIM_HEAL)));
        skillMap.put(ActionRequestComponent.ActionType.MANA_TRANSFER,
                new ManaTransferSkill(actionLog, animations,
                        assets.getTexture(ArithmeticAssetManager.ANIM_MANA_TRANSFER)));
        skillMap.put(ActionRequestComponent.ActionType.ADDITIONAL_BUFF,
                new AdditionBuffSkill(actionLog, animations,
                        assets.getTexture(ArithmeticAssetManager.ANIM_ADDITIONAL_BUFF)));
        skillMap.put(ActionRequestComponent.ActionType.GROUP_HEAL,
                new GroupHealSkill(engine, actionLog, animations, assets.getTexture(ArithmeticAssetManager.ANIM_HEAL)));

        // SUBTRACTION
        skillMap.put(ActionRequestComponent.ActionType.POKE,
                new PokeSkill(actionLog, animations, assets.getTexture(ArithmeticAssetManager.ANIM_POKE)));
        skillMap.put(ActionRequestComponent.ActionType.SLAM,
                new SlamSkill(engine, actionLog, animations, assets.getTexture(ArithmeticAssetManager.ANIM_SLAM)));
        skillMap.put(ActionRequestComponent.ActionType.CONDITIONAL_ATTACK,
                new ConditionalAttackSkill(actionLog, animations,
                        assets.getTexture(ArithmeticAssetManager.ANIM_CONDITIONAL)));
        skillMap.put(ActionRequestComponent.ActionType.LIFESTEAL_ATTACK,
                new LifeStealAttackSkill(actionLog, animations,
                        assets.getTexture(ArithmeticAssetManager.ANIM_LIFE_STEAL),
                        assets.getTexture(ArithmeticAssetManager.ANIM_LIFE_STEAL_HEAL)));
        // MULTIPLICATION
        skillMap.put(ActionRequestComponent.ActionType.AMPLIFY,
                new AmplifySkill(actionLog, animations, assets.getTexture(ArithmeticAssetManager.ANIM_AMPLIFY)));
        skillMap.put(ActionRequestComponent.ActionType.GROUP_AMPLIFY,
                new GroupAmplifySkill(engine, actionLog, animations,
                        assets.getTexture(ArithmeticAssetManager.ANIM_AMPLIFY)));
        skillMap.put(ActionRequestComponent.ActionType.INVERSION,
                new InversionSkill(actionLog, animations, assets.getTexture(ArithmeticAssetManager.ANIM_INVERSE)));
        skillMap.put(ActionRequestComponent.ActionType.SQUARED_POWER,
                new SquaredPowerSkill(actionLog, animations, assets.getTexture(ArithmeticAssetManager.ANIM_SQUARED)));

        // DIVISION
        skillMap.put(ActionRequestComponent.ActionType.BURDEN,
                new BurdenSkill(actionLog, animations, assets.getTexture(ArithmeticAssetManager.ANIM_SHIELD)));
        skillMap.put(ActionRequestComponent.ActionType.BATTLE_EQUALIZER,
                new BattleEqualizerSKill(actionLog, animations,
                        assets.getTexture(ArithmeticAssetManager.ANIM_EQUALIZER)));
        skillMap.put(ActionRequestComponent.ActionType.COST_REDUCTION,
                new CostReductionSkill(actionLog));
        skillMap.put(ActionRequestComponent.ActionType.GROUP_BURDEN,
                new GroupBurdenSkill(engine, actionLog, animations,
                        assets.getTexture(ArithmeticAssetManager.ANIM_SHIELD)));

        // ENEMY — bosses use their own attack sheets, mobs use generic Poke/Slam
        skillMap.put(ActionRequestComponent.ActionType.ENEMY_ATTACK,
                new BasicEnemyAttack(actionLog, animations,
                        assets.getTexture(ArithmeticAssetManager.ANIM_POKE), assets));
        skillMap.put(ActionRequestComponent.ActionType.ENEMY_AOE_ATTACK,
                new BasicEnemyAOEAttack(engine, actionLog, animations,
                        assets.getTexture(ArithmeticAssetManager.ANIM_SLAM), assets));
        skillMap.put(ActionRequestComponent.ActionType.ENEMY_SUPPORT_HEAL,
                new BasicEnemyHeal(actionLog));
        skillMap.put(ActionRequestComponent.ActionType.ENEMY_SUPPORT_BUFF,
                new BasicEnemyBuff(actionLog));

        // ── INVERTED SKILLS ──────────────────────────────────────────
        // Addition inverted
        skillMap.put(ActionRequestComponent.ActionType.SINGLE_DRAIN,
                new SingleDrainSkill(actionLog));
        skillMap.put(ActionRequestComponent.ActionType.LIFE_SIPHON,
                new LifeSiphonSkill(engine, actionLog));
        skillMap.put(ActionRequestComponent.ActionType.MANA_STEAL,
                new ManaStealSkill(actionLog));

        // Subtraction inverted
        skillMap.put(ActionRequestComponent.ActionType.BLOOD_TRANSFER,
                new BloodTransferSkill(actionLog));
        skillMap.put(ActionRequestComponent.ActionType.SACRIFICE,
                new SacrificeSkill(engine, actionLog));
        skillMap.put(ActionRequestComponent.ActionType.MANA_NUKE,
                new ManaNukeSkill(actionLog));
        skillMap.put(ActionRequestComponent.ActionType.DEBT_TRANSFER,
                new DebtTransferSkill(actionLog));

        // Division inverted
        skillMap.put(ActionRequestComponent.ActionType.SINGLE_REFLECTION,
                new SingleReflectionSkill(actionLog));
        skillMap.put(ActionRequestComponent.ActionType.GROUP_REFLECTION,
                new GroupReflectionSkill(engine, actionLog));
        skillMap.put(ActionRequestComponent.ActionType.UNFAIR_BATTLE,
                new UnfairBattleSkill(actionLog));

    }

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(
                Family.all(ActionRequestComponent.class, StatsComponent.class).get());
    }

    @Override
    public void update(float deltaTime) {
        if (turnManager.getState() != BattleState.ACTION_QUEUED)
            return;

        if (entities.size() > 0) {
            Entity user = entities.first();
            ActionRequestComponent request = am.get(user);

            announceAction(user, request);
            executeAction(user, request);

            user.remove(ActionRequestComponent.class);
            turnManager.setState(BattleState.ANIMATING);
        }
    }

    private void announceAction(Entity user, ActionRequestComponent request) {
        StatsComponent stats = sm.get(user);
        String userName = (stats != null) ? stats.name.trim() : "Unknown";
        String skillName = getSkillName(request.actionType);
        actionLog.addMessage(userName + " uses " + skillName + "!");

        // ── ENEMY & HERO CHAT BUBBLES ───────────────────────────────────────
        if (chatBubbles != null && dialogueManager != null) {
            String line = null;

            // 1. Check for LOW HP (Heroes only)
            TypeComponent tc = tm.get(user);
            if (tc != null && tc.type != io.github.jhundeniel.ArithmeticHeroes.components.Operator.MOB) {
                // If HP is 30% or lower...
                if (stats != null && stats.hp <= (stats.maxHp * 0.3f)) {
                    // 50% chance to say a hurt line instead of a skill line
                    if (com.badlogic.gdx.math.MathUtils.randomBoolean(0.5f)) {
                        line = dialogueManager.getCombatLine("onLowHP", userName);
                    }
                }
            }

            // 2. If no Low HP line was chosen, fetch the normal skill/attack line
            if (line == null) {
                if (request.actionType.name().startsWith("ENEMY_")) {
                    // Enemies pull from the "onEnemyAttack" category!
                    line = dialogueManager.getCombatLine("onEnemyAttack", request.actionType.name());
                } else {
                    // Heroes try for a specific line first (e.g., Addition_HEAL)
                    String specificKey = userName.replace(" ", "_") + "_" + request.actionType.name();
                    line = dialogueManager.getCombatLine("onSkillUse", specificKey);

                    // Fallback to generic skill line
                    if (line == null) {
                        line = dialogueManager.getCombatLine("onSkillUse", request.actionType.name());
                    }
                }
            }

            // 3. Show the bubble!
            if (line != null) {
                chatBubbles.showBubble(user, line);
            }
        }
    }

    private void executeAction(Entity user, ActionRequestComponent request) {
        SkillStrategy strategy = skillMap.get(request.actionType);

        if (strategy != null) {
            // ── Multi-target path (Group Burden 2-ally selection) ────────
            if (request.multiTargets != null && strategy instanceof GroupBurdenSkill) {
                ((GroupBurdenSkill) strategy).executeMulti(user, request.multiTargets);
                playSkillSound(request.actionType);
            } else {
                // Standard single/AOE execution
                strategy.execute(user, request.target);
                playSkillSound(request.actionType);

                if (StatusEffects.has(user, StatusEffect.Type.SQUARED)) {
                    StatsComponent stats = sm.get(user);
                    String name = (stats != null) ? stats.name.trim() : "???";

                    actionLog.addMessage("SQUARED POWER: " + name + " casts again!");

                    // Set delay so echo animations play AFTER the first cast's animations
                    animations.setEchoDelay(GameConfig.ECHO_ANIM_DELAY);

                    StatusEffects.add(user, StatusEffect.echoCast());
                    strategy.execute(user, request.target);
                    StatusEffects.remove(user, StatusEffect.Type.ECHO_CAST);
                    StatusEffects.remove(user, StatusEffect.Type.SQUARED);

                    // Schedule the echo cast sound to play when the delayed animation starts
                    final ActionRequestComponent.ActionType echoType = request.actionType;
                    animations.scheduleSound(() -> playSkillSound(echoType), 0.8f);

                    // Schedule the hit reaction sound for the echo (damage skills only)
                    if (request.target != null) {
                        io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent tc = request.target
                                .getComponent(io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent.class);
                        final String hitSfx = (tc != null
                                && tc.type == io.github.jhundeniel.ArithmeticHeroes.components.Operator.MOB)
                                        ? ArithmeticAssetManager.SFX_ENEMY_HIT
                                        : ArithmeticAssetManager.SFX_HERO_HURT;
                        animations.scheduleSound(() -> assets.playSound(hitSfx), 0.8f);
                    }
                    animations.clearEchoDelay();
                }
            }

            // ── INVERSION EXPIRY: consume after using an inverted skill ──
            if (INVERTED_ACTIONS.contains(request.actionType)
                    && StatusEffects.has(user, StatusEffect.Type.INVERSION)) {
                StatusEffects.remove(user, StatusEffect.Type.INVERSION);
                StatsComponent stats = sm.get(user);
                String name = (stats != null) ? stats.name.trim() : "???";
                actionLog.addMessage(name + "'s Inversion wore off. Skills reverted to normal.");
                System.out.println(">> INVERSION CONSUMED: " + name + " used inverted skill, buff removed.");
            }
        } else {
            actionLog.addMessage("Unknown action: " + request.actionType);
        }
    }

    /** All ActionTypes that count as "inverted" skills. */
    private static final java.util.EnumSet<ActionRequestComponent.ActionType> INVERTED_ACTIONS = java.util.EnumSet.of(
            ActionRequestComponent.ActionType.SINGLE_DRAIN,
            ActionRequestComponent.ActionType.LIFE_SIPHON,
            ActionRequestComponent.ActionType.MANA_STEAL,
            ActionRequestComponent.ActionType.BLOOD_TRANSFER,
            ActionRequestComponent.ActionType.SACRIFICE,
            ActionRequestComponent.ActionType.MANA_NUKE,
            ActionRequestComponent.ActionType.DEBT_TRANSFER,
            ActionRequestComponent.ActionType.SINGLE_REFLECTION,
            ActionRequestComponent.ActionType.GROUP_REFLECTION,
            ActionRequestComponent.ActionType.UNFAIR_BATTLE);

    private String getSkillName(ActionRequestComponent.ActionType type) {
        switch (type) {
            case HEAL:
                return "Heal";
            case GROUP_HEAL:
                return "Group Heal";
            case ADDITIONAL_BUFF:
                return "Addition Buff";
            case MANA_TRANSFER:
                return "Mana Transfer";

            case POKE:
                return "Poke";
            case SLAM:
                return "Slam";
            case CONDITIONAL_ATTACK:
                return "Conditional Attack";
            case LIFESTEAL_ATTACK:
                return "Life Steal";

            case AMPLIFY:
                return "Amplify";
            case GROUP_AMPLIFY:
                return "Group Amplify";
            case INVERSION:
                return "Inversion";
            case SQUARED_POWER:
                return "Squared Power";

            case BURDEN:
                return "Burden";
            case GROUP_BURDEN:
                return "Group Burden";
            case COST_REDUCTION:
                return "Cost Reduction";
            case BATTLE_EQUALIZER:
                return "Battle Equalizer";

            // Inverted skills
            case SINGLE_DRAIN:
                return "Single Drain";
            case LIFE_SIPHON:
                return "Life Siphon";
            case MANA_STEAL:
                return "Mana Steal";
            case BLOOD_TRANSFER:
                return "Blood Transfer";
            case SACRIFICE:
                return "Sacrifice";
            case MANA_NUKE:
                return "Mana Nuke";
            case DEBT_TRANSFER:
                return "Debt Transfer";
            case SINGLE_REFLECTION:
                return "Single Reflection";
            case GROUP_REFLECTION:
                return "Group Reflection";
            case UNFAIR_BATTLE:
                return "Unfair Battle";

            case ENEMY_ATTACK:
                return "Attack";
            case ENEMY_AOE_ATTACK:
                return "AOE Attack";
            case ENEMY_SUPPORT_HEAL:
                return "Heal";
            case ENEMY_SUPPORT_BUFF:
                return "Buff";

            default:
                return "Unknown Skill";
        }
    }

    private void playSkillSound(ActionRequestComponent.ActionType type) {
        String sfx = null;
        switch (type) {
            case HEAL:
            case GROUP_HEAL:
                sfx = ArithmeticAssetManager.SFX_HEAL;
                break;
            case ADDITIONAL_BUFF:
                sfx = ArithmeticAssetManager.SFX_ADDITIONAL_BUFF;
                break;
            case MANA_TRANSFER:
                sfx = ArithmeticAssetManager.SFX_MANA_TRANSFER;
                break;
            case LIFE_SIPHON:
                sfx = ArithmeticAssetManager.SFX_LIFE_SIPHON;
                break;
            case MANA_STEAL:
                sfx = ArithmeticAssetManager.SFX_MANA_STEAL;
                break;
            case SINGLE_DRAIN:
                sfx = ArithmeticAssetManager.SFX_SINGLE_DRAIN;
                break;

            case BATTLE_EQUALIZER:
            case UNFAIR_BATTLE:
                sfx = ArithmeticAssetManager.SFX_BATTLE_EQUALIZER_UNFAIR_BATTLE;
                break;
            case BURDEN:
            case GROUP_BURDEN:
                sfx = ArithmeticAssetManager.SFX_BURDEN;
                break;
            case SINGLE_REFLECTION:
            case GROUP_REFLECTION:
                sfx = ArithmeticAssetManager.SFX_REFLECT;
                break;
            case COST_REDUCTION:
                sfx = ArithmeticAssetManager.SFX_SKILL_COST_REDUCTION;
                break;

            case AMPLIFY:
            case GROUP_AMPLIFY:
                sfx = ArithmeticAssetManager.SFX_AMPLIFY;
                break;
            case INVERSION:
                sfx = ArithmeticAssetManager.SFX_INVERSION;
                break;
            case SQUARED_POWER:
                sfx = ArithmeticAssetManager.SFX_SQUARED_POWER;
                break;

            case BLOOD_TRANSFER:
                sfx = ArithmeticAssetManager.SFX_BLOOD_TRANSFER;
                break;
            case CONDITIONAL_ATTACK:
                sfx = ArithmeticAssetManager.SFX_CONDITIONAL_ATTACK;
                break;
            case DEBT_TRANSFER:
                sfx = ArithmeticAssetManager.SFX_DEBT_TRANSFER;
                break;
            case LIFESTEAL_ATTACK:
                sfx = ArithmeticAssetManager.SFX_LIFE_STEAL;
                break;
            case MANA_NUKE:
                sfx = ArithmeticAssetManager.SFX_MANA_NUKE;
                break;
            case SACRIFICE:
                sfx = ArithmeticAssetManager.SFX_SACRIFICE;
                break;
            case POKE:
                sfx = ArithmeticAssetManager.SFX_POKE;
                break;
            case SLAM:
                sfx = ArithmeticAssetManager.SFX_SLAM;
                break;

            case ENEMY_AOE_ATTACK:
                sfx = ArithmeticAssetManager.SFX_ENEMY_AOE;
                break;
            case ENEMY_ATTACK:
                sfx = ArithmeticAssetManager.SFX_ENEMY_BASIC;
                break;
            case ENEMY_SUPPORT_HEAL:
                sfx = ArithmeticAssetManager.SFX_ENEMY_HEAL;
                break;
            case ENEMY_SUPPORT_BUFF:
                sfx = ArithmeticAssetManager.SFX_ENEMY_SUPPORT_BUFF;
                break;

            default:
                break;
        }
        if (sfx != null) {
            assets.playSound(sfx);
        }
    }
}
