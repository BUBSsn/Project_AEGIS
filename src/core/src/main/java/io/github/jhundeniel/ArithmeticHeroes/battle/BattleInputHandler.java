package io.github.jhundeniel.ArithmeticHeroes.battle;

import java.util.List;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent.ActionType;
import io.github.jhundeniel.ArithmeticHeroes.components.Operator;
import io.github.jhundeniel.ArithmeticHeroes.components.PassiveComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.managers.TurnManager;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;
import io.github.jhundeniel.ArithmeticHeroes.systems.TargetingSystem;

public class BattleInputHandler extends InputAdapter {

    private final List<Entity>    heroes;
    private final List<Entity>    activeMobs;
    private final TurnManager     turnManager;
    private final TargetingSystem targetingSystem;
    private final ActionLogSystem actionLog;

    private final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<TypeComponent>  tm = ComponentMapper.getFor(TypeComponent.class);


    public BattleInputHandler(List<Entity> heroes, List<Entity> mobs,
                              TurnManager turnManager, TargetingSystem targetingSystem,
                              ActionLogSystem actionLog) {
        this.heroes          = heroes;
        this.activeMobs      = mobs;
        this.turnManager     = turnManager;
        this.targetingSystem = targetingSystem;
        this.actionLog       = actionLog;
    }

    @Override
    public boolean keyDown(int keycode) {
        BattleState currentState = turnManager.getState();

        // 1. INPUT LOCKOUT: Only allow inputs during these states
        if (currentState != BattleState.WAIT_FOR_INPUT
            && currentState != BattleState.SELECT_TARGET
            && currentState != BattleState.CHOOSE_VALUE) {
            return false;
        }

        // 2a. CHOOSE_VALUE: only allow BACKSPACE to cancel back
        if (currentState == BattleState.CHOOSE_VALUE) {
            if (keycode == Input.Keys.BACKSPACE) {
                targetingSystem.clearPendingGroupBurdenChoice();
                targetingSystem.cancel();
                turnManager.setState(BattleState.WAIT_FOR_INPUT);
                System.out.println(">> Choice cancelled. Back to skill selection.");
                return true;
            }
            return false; // Let Scene2D buttons handle other input
        }

        // 2b. TARGETING MODE NAVIGATION
        if (currentState == BattleState.SELECT_TARGET) {
            if (keycode == Input.Keys.BACKSPACE) {
                targetingSystem.cancel();
                turnManager.setState(BattleState.WAIT_FOR_INPUT);
                System.out.println(">> Targeting cancelled. Back to skill selection.");
                return true;
            } else if (keycode == Input.Keys.LEFT) {
                targetingSystem.cyclePrev();
                return true;
            } else if (keycode == Input.Keys.RIGHT) {
                targetingSystem.cycleNext();
                return true;
            } else if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                Entity target = targetingSystem.getCurrentTarget();
                if (target != null) {
                    targetingSystem.confirmTarget(target);
                    // TargetingSystem handling determines state change to CHOOSE_VALUE, ACTION_QUEUED, etc.
                    // But MouseTargetingSystem currently handles the BattleState shift! Wait, we need to handle it here.
                    // Actually, MouseTargetingSystem does it. I'll just rely on MouseTargetingSystem's logic, OR copy it here.
                    // Let's copy it here so Keyboard works fully independently from MouseTargetingSystem.
                    if (targetingSystem.isWaitingForValue()) {
                        turnManager.setState(BattleState.CHOOSE_VALUE);
                    } else if (!targetingSystem.isTargeting()) {
                        // Only transition if targeting is fully done
                        // (two-target skills stay in SELECT_TARGET after 1st pick)
                        turnManager.setState(BattleState.ACTION_QUEUED);
                    }
                    // else: still picking 2nd target, stay in SELECT_TARGET
                }
                return true;
            }
            return false; // Ignore all other keys while picking a target
        }

        // --- At this point, we are guaranteed to be in WAIT_FOR_INPUT ---

        Entity cur = turnManager.getCurrentEntityTurn();
        if (cur == null) return false;

        // Keyboard shortcuts are for heroes only
        if (activeMobs.contains(cur)) return false;

        StatsComponent stats = sm.get(cur);
        TypeComponent  type  = tm.get(cur);
        if (type == null || type.type == Operator.MOB) return false;

        Operator heroType = type.type;

        switch (keycode) {
            // ── Q — Single-target skill ───────────────────────────
            case Input.Keys.Q: fireSkillQ(cur, heroType); break;

            // ── W — Group / AOE skill ─────────────────────────────
            case Input.Keys.W: fireSkillW(cur, heroType); break;

            // ── E — Utility skill 1 ───────────────────────────────
            case Input.Keys.E: fireSkillE(cur, heroType); break;

            // ── R — Utility skill 2 ───────────────────────────────
            case Input.Keys.R: fireSkillR(cur, heroType); break;

            // ── PASS ──────────────────────────────────────────────
            case Input.Keys.P:
                actionLog.addMessage(stats.name.trim() + " passes.");

                PassiveComponent pc = cur.getComponent(PassiveComponent.class);
                if (pc != null && pc.passive != null) {
                    pc.passive.onPass(cur, heroes);
                }

                turnManager.setState(BattleState.TURN_END);
                break;

            default:
                return false;
        }
        return true;
    }

    // ── Q: Single-target skills ──────────────────────────────────

    private void fireSkillQ(Entity caster, Operator heroType) {
        boolean inv = StatusEffects.has(caster, StatusEffect.Type.INVERSION);
        switch (heroType) {
            case ADDITION:       singleTarget(caster, inv ? ActionType.SINGLE_DRAIN : ActionType.HEAL);    break;
            case SUBTRACTION:    singleTarget(caster, inv ? ActionType.BLOOD_TRANSFER : ActionType.POKE);  break;
            case MULTIPLICATION: singleTarget(caster, ActionType.AMPLIFY); break;
            case DIVISION:       singleTarget(caster, inv ? ActionType.SINGLE_REFLECTION : ActionType.BURDEN);  break;
            default: break;
        }
    }

    // ── W: Group / AOE skills ────────────────────────────────────

    private void fireSkillW(Entity caster, Operator heroType) {
        boolean inv = StatusEffects.has(caster, StatusEffect.Type.INVERSION);
        switch (heroType) {
            case ADDITION:       aoe(caster, inv ? ActionType.LIFE_SIPHON : ActionType.GROUP_HEAL);    break;
            case SUBTRACTION:    aoe(caster, inv ? ActionType.SACRIFICE : ActionType.SLAM);            break;
            case MULTIPLICATION: aoe(caster, ActionType.GROUP_AMPLIFY); break;
            case DIVISION:
                if (inv) aoe(caster, ActionType.GROUP_REFLECTION);
                else     groupBurdenChoice(caster);
                break;
            default: break;
        }
    }

    // ── E: Utility skill 1 ───────────────────────────────────────

    private void fireSkillE(Entity caster, Operator heroType) {
        boolean inv = StatusEffects.has(caster, StatusEffect.Type.INVERSION);
        switch (heroType) {
            case ADDITION:       singleTarget(caster, ActionType.ADDITIONAL_BUFF);    break;
            case SUBTRACTION:    singleTarget(caster, inv ? ActionType.MANA_NUKE : ActionType.CONDITIONAL_ATTACK); break;
            case MULTIPLICATION: singleTarget(caster, ActionType.INVERSION);          break;
            case DIVISION:       singleTarget(caster, ActionType.COST_REDUCTION);     break;
            default: break;
        }
    }

    // ── R: Utility skill 2 ───────────────────────────────────────

    private void fireSkillR(Entity caster, Operator heroType) {
        boolean inv = StatusEffects.has(caster, StatusEffect.Type.INVERSION);
        switch (heroType) {
            case ADDITION:
                if (inv) singleTarget(caster, ActionType.MANA_STEAL);
                else     twoTargets(caster, ActionType.MANA_TRANSFER);
                break;
            case SUBTRACTION:
                if (inv) twoTargets(caster, ActionType.DEBT_TRANSFER);
                else     singleTarget(caster, ActionType.LIFESTEAL_ATTACK);
                break;
            case MULTIPLICATION: singleTarget(caster, ActionType.SQUARED_POWER);    break;
            case DIVISION:
                if (inv) twoTargets(caster, ActionType.UNFAIR_BATTLE);
                else     twoTargets(caster, ActionType.BATTLE_EQUALIZER);
                break;
            default: break;
        }
    }

    // ── Targeting helpers ────────────────────────────────────────────

    private void singleTarget(Entity caster, ActionType action) {
        if (targetingSystem.startTargeting(caster, action)) {
            turnManager.setState(BattleState.SELECT_TARGET);
        }
    }

    private void aoe(Entity caster, ActionType action) {
        // AOE skills have no target selection — fire immediately
        caster.add(new ActionRequestComponent(action, null));
        turnManager.setState(BattleState.ACTION_QUEUED);
    }

    private void twoTargets(Entity caster, ActionType action) {
        if (targetingSystem.startTargetingTwo(caster, action)) {
            turnManager.setState(BattleState.SELECT_TARGET);
        }
    }

    private void groupBurdenChoice(Entity caster) {
        // Set the pending choice flag — SkillButtonsUI will detect this
        // and show the choice dialog on its next render/update cycle
        targetingSystem.setPendingGroupBurdenChoice(caster);
        turnManager.setState(BattleState.CHOOSE_VALUE);
    }
}
