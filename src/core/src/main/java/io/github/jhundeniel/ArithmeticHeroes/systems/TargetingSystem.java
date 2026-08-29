package io.github.jhundeniel.ArithmeticHeroes.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;

import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent.ActionType;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent.TargetGroup;
import io.github.jhundeniel.ArithmeticHeroes.components.Operator;
import io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;

import java.util.ArrayList;
import java.util.List;

public class TargetingSystem {

    private boolean isTargeting = false;
    private boolean needsTwoTargets = false;
    private Entity sourceEntity;
    private ActionType pendingAction;
    private TargetGroup requiredGroup;

    // Two-target state (Mana Transfer, Battle Equalizer)
    private Entity firstTarget = null;

    // ── Group Burden multi-target state ───────────────────────────────────
    private boolean isGroupBurdenMode = false;
    private int maxTargets = 0;
    private final List<Entity> lockedTargets = new ArrayList<>();
    private boolean pendingGroupBurdenChoice = false;
    private Entity groupBurdenCaster = null;

    private final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<TypeComponent> tm = ComponentMapper.getFor(TypeComponent.class);
    private final ActionLogSystem actionLog;
    private final io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager assets;

    /** Held target when waiting for a value chooser (e.g., Additional Buff). */
    private Entity pendingBuffTarget = null;

    // ── Unified Selection System ──────────────────────────────────────────
    private List<Entity> allEntities = new ArrayList<>();
    public List<Entity> validTargets = new ArrayList<>();
    public int currentTargetIndex = -1;

    public TargetingSystem(ActionLogSystem actionLog,
            io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager assets) {
        this.actionLog = actionLog;
        this.assets = assets;
    }

    public void setAllEntities(List<Entity> allEntities) {
        this.allEntities = allEntities;
    }

    // ──────────────────────────────────────────────────────────────
    // Start single-target selection
    // ──────────────────────────────────────────────────────────────
    public boolean startTargeting(Entity source, ActionType action) {
        this.sourceEntity = source;
        this.pendingAction = action;
        this.requiredGroup = ActionRequestComponent.getTargetGroup(action);
        this.needsTwoTargets = false;
        this.firstTarget = null;
        this.isTargeting = true;

        populateValidTargets();

        if (validTargets.isEmpty()) {
            actionLog.addMessage("No valid targets available for " + label(action) + "!");
            reset();
            return false;
        }

        String name = name(source);
        actionLog.addMessage(name + ": select a target for " + label(action));
        System.out.println(">> Targeting: " + action + " (group: " + requiredGroup + ")");
        return true;
    }

    // ────────────────────────────────────────────────────────────────
    // Start Group Burden multi-target selection
    // ────────────────────────────────────────────────────────────────
    public boolean startGroupBurdenTargeting(Entity source, int maxTgts) {
        this.sourceEntity = source;
        this.pendingAction = ActionType.GROUP_BURDEN;
        this.requiredGroup = TargetGroup.ALLY;
        this.needsTwoTargets = false;
        this.firstTarget = null;
        this.isTargeting = true;
        this.isGroupBurdenMode = true;
        this.maxTargets = maxTgts;
        this.lockedTargets.clear();
        this.pendingGroupBurdenChoice = false;

        populateValidTargets();

        // Exclude self from valid targets
        validTargets.remove(source);
        currentTargetIndex = validTargets.isEmpty() ? -1 : 0;

        if (validTargets.isEmpty()) {
            actionLog.addMessage("No valid allies to target for Group Burden!");
            reset();
            return false;
        }

        actionLog.addMessage(name(source) + ": select FIRST ally for Group Burden");
        System.out.println(">> Group Burden targeting: pick " + maxTgts + " allies");
        return true;
    }

    // ────────────────────────────────────────────────────────────────
    // Start two-target selection
    // Supports: ALLY_TWO (both allies), ENEMY_TWO (both enemies),
    // ENEMY_THEN_ALLY (1st pick enemy, 2nd pick ally)
    // ────────────────────────────────────────────────────────────────
    public boolean startTargetingTwo(Entity source, ActionType action) {
        this.sourceEntity = source;
        this.pendingAction = action;
        this.needsTwoTargets = true;
        this.firstTarget = null;
        this.isTargeting = true;

        // Decide what kind of entity the FIRST click needs
        TargetGroup fullGroup = ActionRequestComponent.getTargetGroup(action);
        if (fullGroup == TargetGroup.ENEMY_TWO || fullGroup == TargetGroup.ENEMY_THEN_ALLY) {
            this.requiredGroup = TargetGroup.ENEMY; // first pick is an enemy
        } else {
            this.requiredGroup = TargetGroup.ALLY; // ALLY_TWO: both picks are allies
        }

        populateValidTargets();

        if (validTargets.isEmpty()) {
            actionLog.addMessage("No valid targets available for " + label(action) + "!");
            reset();
            return false;
        }

        String name = name(source);
        if (action == ActionType.MANA_TRANSFER) {
            actionLog.addMessage(name + ": select who RECEIVES mana");
        } else if (action == ActionType.BATTLE_EQUALIZER) {
            actionLog.addMessage(name + ": select FIRST ally to equalize HP");
        } else {
            actionLog.addMessage(name + ": select FIRST target for " + label(action));
        }
        System.out.println(">> Two-target mode: " + action);
        return true;
    }

    private void populateValidTargets() {
        validTargets.clear();
        for (Entity candidate : allEntities) {
            StatsComponent stats = sm.get(candidate);
            if (stats != null && stats.hp > 0 && isValidTarget(candidate)) {
                validTargets.add(candidate);
            }
        }

        // Sort: Addition → Subtraction → Multiplication → Division → Mobs
        validTargets.sort((e1, e2) -> {
            TypeComponent t1 = tm.get(e1);
            TypeComponent t2 = tm.get(e2);
            return Integer.compare(
                    getOperatorSortRank(t1 != null ? t1.type : null),
                    getOperatorSortRank(t2 != null ? t2.type : null));
        });

        currentTargetIndex = validTargets.isEmpty() ? -1 : 0;
    }

    private int getOperatorSortRank(Operator op) {
        if (op == null)
            return 99;
        switch (op) {
            case ADDITION:
                return 1;
            case SUBTRACTION:
                return 2;
            case MULTIPLICATION:
                return 3;
            case DIVISION:
                return 4;
            default:
                return 99; // MOB and any others
        }
    }

    // ── Target Navigation ──────────────────────────────────────────────────

    public void cycleNext() {
        if (validTargets.isEmpty())
            return;
        currentTargetIndex = (currentTargetIndex + 1) % validTargets.size();
        if (assets != null)
            assets.playSound(io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager.SFX_BUTTON_HOVER);
    }

    public void cyclePrev() {
        if (validTargets.isEmpty())
            return;
        currentTargetIndex = (currentTargetIndex - 1 + validTargets.size()) % validTargets.size();
        if (assets != null)
            assets.playSound(io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager.SFX_BUTTON_HOVER);
    }

    public void setTargetIndex(Entity entity) {
        int index = validTargets.indexOf(entity);
        if (index != -1 && currentTargetIndex != index) {
            currentTargetIndex = index;
            if (assets != null)
                assets.playSound(
                        io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager.SFX_BUTTON_HOVER);
        }
    }

    public Entity getCurrentTarget() {
        if (currentTargetIndex >= 0 && currentTargetIndex < validTargets.size()) {
            return validTargets.get(currentTargetIndex);
        }
        return null;
    }

    // ──────────────────────────────────────────────────────────────
    // Validate whether an entity is a legal target right now
    // ──────────────────────────────────────────────────────────────
    public boolean isValidTarget(Entity candidate) {
        if (!isTargeting || candidate == null)
            return false;

        // Can't re-select the already chosen first target
        if (needsTwoTargets && candidate == firstTarget)
            return false;

        // AOE has no click target
        if (requiredGroup == TargetGroup.AOE_ALLY
                || requiredGroup == TargetGroup.AOE_ENEMY
                || requiredGroup == TargetGroup.AOE_ALL
                || requiredGroup == TargetGroup.NONE)
            return false;

        TypeComponent type = tm.get(candidate);
        if (type == null)
            return false;

        // ── SELF-TARGETING PREVENTION ────────────────────────────────────────
        if (sourceEntity != null && candidate == sourceEntity) {
            TypeComponent sourceType = tm.get(sourceEntity);
            if (sourceType != null) {

                // 1. Subtraction can NEVER target themselves with anything
                if (sourceType.type == Operator.SUBTRACTION) {
                    return false;
                }

                // 2. Multiplication cannot target themselves with AMPLIFY
                if (sourceType.type == Operator.MULTIPLICATION && pendingAction == ActionType.AMPLIFY) {
                    return false;
                }

                // 3. Division cannot target themselves with BURDEN (but Reflect is okay!)
                if (sourceType.type == Operator.DIVISION && pendingAction == ActionType.BURDEN) {
                    return false;
                }
            }
        }
        // ─────────────────────────────────────────────────────────────────────

        boolean isMob = (type.type == Operator.MOB);

        // SQUARED_POWER: only Addition and Subtraction can be targeted
        if (pendingAction == ActionType.SQUARED_POWER) {
            return type.type == Operator.ADDITION || type.type == Operator.SUBTRACTION;
        }

        // INVERSION: Multiplication cannot be targeted
        if (pendingAction == ActionType.INVERSION) {
            return type.type == Operator.ADDITION || type.type == Operator.SUBTRACTION ||
                    type.type == Operator.DIVISION;
        }

        switch (requiredGroup) {
            case ALLY:
                return !isMob;
            case ALLY_TWO:
                return !isMob;
            case ENEMY:
                return isMob;
            case ENEMY_TWO:
                return isMob;
            case ANY_SINGLE:
                return true; // can target any living entity
            default:
                return true;
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Confirm a clicked target
    // ──────────────────────────────────────────────────────────────
    public void confirmTarget(Entity target) {
        if (!isTargeting)
            return;

        if (!isValidTarget(target)) {
            actionLog.addMessage("Can't target " + name(target) + " with this skill!");
            return; // stay in targeting mode
        }

        // ── Group Burden multi-target accumulation ─────────────────────
        if (isGroupBurdenMode) {
            // Don't allow re-selecting an already locked target
            if (lockedTargets.contains(target)) {
                actionLog.addMessage(name(target) + " is already selected!");
                return;
            }

            lockedTargets.add(target);
            // SFX: play select sound for first target
            if (assets != null)
                assets.playSound(
                        io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager.SFX_BUTTON_CLICK);

            if (lockedTargets.size() < maxTargets) {
                // Still need more targets
                actionLog.addMessage(name(target) + " locked in! Select next ally (" + lockedTargets.size() + "/"
                        + maxTargets + ").");
                // Refresh valid targets to exclude already-locked ones
                validTargets.remove(target);
                currentTargetIndex = validTargets.isEmpty() ? -1 : 0;
                return; // stay in targeting mode
            } else {
                // All targets selected — queue the action
                String src = name(sourceEntity);
                StringBuilder names = new StringBuilder();
                for (int i = 0; i < lockedTargets.size(); i++) {
                    if (i > 0)
                        names.append(" & ");
                    names.append(name(lockedTargets.get(i)));
                }
                actionLog.addMessage(src + " targets " + names + "!");
                sourceEntity
                        .add(ActionRequestComponent.withMultiTargets(pendingAction, new ArrayList<>(lockedTargets)));
                reset();
                return;
            }
        }

        if (needsTwoTargets) {
            if (firstTarget == null) {
                // First pick done — ask for second with skill-specific prompt
                firstTarget = target;

                // For ENEMY_THEN_ALLY: switch required group to ALLY for second pick
                TargetGroup fullGroup = ActionRequestComponent.getTargetGroup(pendingAction);
                if (fullGroup == TargetGroup.ENEMY_THEN_ALLY) {
                    this.requiredGroup = TargetGroup.ALLY;
                    actionLog.addMessage(name(target) + " targeted! Now select an ALLY to heal.");
                } else if (pendingAction == ActionType.MANA_TRANSFER) {
                    actionLog.addMessage(name(target) + " will RECEIVE mana. Now select who GIVES mana.");
                } else if (pendingAction == ActionType.BATTLE_EQUALIZER) {
                    actionLog.addMessage("First: " + name(target) + ". Now select SECOND ally to equalize HP.");
                } else if (pendingAction == ActionType.UNFAIR_BATTLE) {
                    actionLog.addMessage("First: " + name(target) + ". Now select SECOND enemy.");
                } else {
                    actionLog.addMessage("First target: " + name(target) + ". Now select SECOND target.");
                }
                populateValidTargets(); // Refresh list to exclude the first target
                return; // stay in targeting mode for second pick
            } else {
                // Both targets chosen — submit
                String src = name(sourceEntity);
                actionLog.addMessage(src + " targets " + name(firstTarget) + " & " + name(target) + "!");
                sourceEntity.add(new ActionRequestComponent(pendingAction, firstTarget, target));
                reset();
                return;
            }
        }

        // Single target
        // Special case: ADDITIONAL_BUFF needs a value chooser before submitting
        if (pendingAction == ActionType.ADDITIONAL_BUFF) {
            pendingBuffTarget = target;
            actionLog.addMessage(name(target) + " targeted! Choose buff amount (3-5).");
            System.out.println(">> ADDITIONAL_BUFF: waiting for value chooser");
            // Don't reset — SkillButtonsUI will call submitWithValue()
            return;
        }

        actionLog.addMessage(name(sourceEntity) + " targets " + name(target) + "!");
        System.out.println(">> Target confirmed: " + name(target));
        sourceEntity.add(new ActionRequestComponent(pendingAction, target));
        reset();
    }

    // ──────────────────────────────────────────────────────────────
    // AOE skills — no click target needed
    // ──────────────────────────────────────────────────────────────
    public void confirmAOE() {
        if (!isTargeting)
            return;

        actionLog.addMessage(name(sourceEntity) + " uses " + label(pendingAction) + "!");
        sourceEntity.add(new ActionRequestComponent(pendingAction, null));
        reset();
    }

    public void cancel() {
        actionLog.addMessage("Action cancelled.");
        reset();
    }

    private void reset() {
        isTargeting = false;
        needsTwoTargets = false;
        sourceEntity = null;
        pendingAction = null;
        requiredGroup = null;
        firstTarget = null;
        pendingBuffTarget = null;
        // Group Burden state
        isGroupBurdenMode = false;
        maxTargets = 0;
        lockedTargets.clear();
        pendingGroupBurdenChoice = false;
        groupBurdenCaster = null;
        // Lists
        validTargets.clear();
        currentTargetIndex = -1;
    }

    // ──────────────────────────────────────────────────────────────
    // Value chooser support (Additional Buff)
    // ──────────────────────────────────────────────────────────────

    /** Called by SkillButtonsUI when the player picks a buff value (3, 4, or 5). */
    public void submitWithValue(int chosenValue) {
        if (sourceEntity == null || pendingBuffTarget == null)
            return;
        actionLog.addMessage(name(sourceEntity) + " targets " + name(pendingBuffTarget)
                + " with +" + chosenValue + " buff!");
        sourceEntity.add(new ActionRequestComponent(pendingAction, pendingBuffTarget, chosenValue));
        reset();
    }

    /** @return true if waiting for a value choice (e.g., Additional Buff). */
    public boolean isWaitingForValue() {
        return pendingBuffTarget != null;
    }

    // ──────────────────────────────────────────────────────────────
    // Getters
    // ──────────────────────────────────────────────────────────────
    public boolean isTargeting() {
        return isTargeting;
    }

    public boolean isWaitingForSecond() {
        return needsTwoTargets && firstTarget != null;
    }

    public boolean isGroupBurdenMode() {
        return isGroupBurdenMode;
    }

    public TargetGroup getRequiredGroup() {
        return requiredGroup;
    }

    public ActionType getPendingAction() {
        return pendingAction;
    }

    public Entity getSourceEntity() {
        return sourceEntity;
    }

    public Entity getFirstTarget() {
        return firstTarget;
    }

    public List<Entity> getLockedTargets() {
        return lockedTargets;
    }

    // ── Group Burden choice state ─────────────────────────────────────
    public boolean isPendingGroupBurdenChoice() {
        return pendingGroupBurdenChoice;
    }

    public Entity getGroupBurdenCaster() {
        return groupBurdenCaster;
    }

    public void setPendingGroupBurdenChoice(Entity caster) {
        this.pendingGroupBurdenChoice = true;
        this.groupBurdenCaster = caster;
    }

    public void clearPendingGroupBurdenChoice() {
        this.pendingGroupBurdenChoice = false;
        this.groupBurdenCaster = null;
    }

    // ── Alive ally count for Group Burden dialog ──────────────────────
    public int countLivingAllies(Entity excludingSelf) {
        int count = 0;
        for (Entity e : allEntities) {
            StatsComponent s = sm.get(e);
            if (s != null && s.hp > 0 && e != excludingSelf) {
                // Only count player entities (non-mobs)
                TypeComponent t = ComponentMapper.getFor(TypeComponent.class).get(e);
                if (t != null && t.type != io.github.jhundeniel.ArithmeticHeroes.components.Operator.MOB) {
                    count++;
                }
            }
        }
        return count;
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────
    private String name(Entity e) {
        return (e != null && sm.has(e)) ? sm.get(e).name.trim() : "?";
    }

    private String label(ActionType action) {
        switch (action) {
            case HEAL:
                return "Heal";
            case GROUP_HEAL:
                return "Group Heal";
            case ADDITIONAL_BUFF:
                return "Add Buff";
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
            default:
                return action.toString();
        }
    }
}
