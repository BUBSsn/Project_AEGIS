package io.github.jhundeniel.ArithmeticHeroes.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

import java.util.List;

public class ActionRequestComponent implements Component {

    // ──────────────────────────────────────────────────────────────
    // Who can be targeted
    // ──────────────────────────────────────────────────────────────
    public enum TargetGroup {
        ALLY,           // single ally target
        ALLY_TWO,       // must pick exactly 2 allies (Mana Transfer, Battle Equalizer, Group Burden)
        ENEMY,          // single enemy target
        ENEMY_TWO,      // must pick exactly 2 enemies (Unfair Battle)
        ENEMY_THEN_ALLY,// pick 1 enemy, then 1 ally (Debt Transfer)
        ANY_SINGLE,     // single target — can be ally OR enemy (Single Drain)
        AOE_ALLY,       // hits ALL allies automatically  (Group Heal, Group Amplify)
        AOE_ENEMY,      // hits ALL enemies automatically (Slam)
        AOE_ALL,        // hits ALL entities on the field (Life Siphon)
        NONE            // no target needed
    }

    // ──────────────────────────────────────────────────────────────
    // Action types
    // ──────────────────────────────────────────────────────────────
    public enum ActionType {
        // ADDITION
        HEAL,               // 3 mana  — single ally
        GROUP_HEAL,         // 5 mana  — all allies (AOE)
        ADDITIONAL_BUFF,    // 4 mana  — single ally
        MANA_TRANSFER,      // 0 mana  — ally to ally (2 targets)

        // SUBTRACTION
        POKE,               // -10% HP cost — single enemy
        SLAM,               // -5%  HP cost — all enemies (AOE)
        CONDITIONAL_ATTACK, // 7 mana  — single enemy (only usable at half HP)
        LIFESTEAL_ATTACK,   // 5 mana  — single enemy

        // MULTIPLICATION
        AMPLIFY,            // 5 mana  — single ally
        GROUP_AMPLIFY,      // 7 mana  — all allies (AOE, allies take extra damage)
        INVERSION,          // 3 mana  — single ally ONLY (doc: CAN ONLY BE USED ON ALLIES)
        SQUARED_POWER,      // 8 mana  — single ally

        // DIVISION
        BURDEN,             // 3 mana  — single ally
        GROUP_BURDEN,       // 5 mana  — 2 or 3 ally targets
        COST_REDUCTION,     // 5 mana  — single ally
        BATTLE_EQUALIZER,   // 6 mana  — exactly 2 allies

        // ── INVERTED SKILLS (cast while InversionBuffComponent is active) ──
        // Addition inverted
        SINGLE_DRAIN,       // 3 mana  — single enemy (removes HP)
        LIFE_SIPHON,        // 5 mana  — AOE all entities (removes HP from all)
        MANA_STEAL,         // 0 mana  — single ally (steal 5 mana → give to caster)

        // Subtraction inverted
        BLOOD_TRANSFER,     // -10% HP — single ally (heals ally, costs own HP)
        SACRIFICE,          // -5% HP  — AOE all allies (heals all, costs own HP)
        MANA_NUKE,          // 7 mana  — single enemy (bonus dmg from missing mana if HP>75%)
        DEBT_TRANSFER,      // 5 mana  — enemy then ally (dmg enemy, 50% heals ally)

        // Division inverted
        SINGLE_REFLECTION,  // 3 mana  — single ally (50% damage reflect, one-hit)
        GROUP_REFLECTION,   // 5 mana  — AOE all allies (reflect based on ally count)
        UNFAIR_BATTLE,      // 6 mana  — exactly 2 enemies (redistribute HP)

        // ENEMY SKILLS
        ENEMY_ATTACK,       // single hero target
        ENEMY_AOE_ATTACK,   // all heroes
        ENEMY_SUPPORT_HEAL, // self-heal
        ENEMY_SUPPORT_BUFF  // self-buff
    }

    // ──────────────────────────────────────────────────────────────
    // Static lookup — single source of truth for targeting rules
    // ──────────────────────────────────────────────────────────────
    public static TargetGroup getTargetGroup(ActionType type) {
        switch (type) {

            // ── Single ally ───────────────────────────────────────
            case HEAL:
            case ADDITIONAL_BUFF:
            case AMPLIFY:
            case INVERSION:       // doc: CAN ONLY BE USED ON ALLIES
            case SQUARED_POWER:
            case BURDEN:
            case COST_REDUCTION:
            case MANA_STEAL:          // inverted: steal from 1 ally
            case BLOOD_TRANSFER:      // inverted: costs HP, heals 1 ally
            case SINGLE_REFLECTION:   // inverted: reflect buff on 1 ally
                return TargetGroup.ALLY;

            // ── Two ally targets ──────────────────────────────────
            case MANA_TRANSFER:     // FROM one ally TO another
            case BATTLE_EQUALIZER:  // redistribute HP between 2 allies
                return TargetGroup.ALLY_TWO;

            // ── Group Burden: handled by custom choice dialog ─────
            case GROUP_BURDEN:
                return TargetGroup.NONE;

            // ── Single enemy ──────────────────────────────────────
            case POKE:
            case CONDITIONAL_ATTACK:
            case LIFESTEAL_ATTACK:
            case SINGLE_DRAIN:        // inverted: damage any entity (ally or enemy)
                return TargetGroup.ANY_SINGLE;
            case MANA_NUKE:           // inverted: damage 1 enemy
                return TargetGroup.ENEMY;
            // ── Two enemy targets ─────────────────────────────────
            case UNFAIR_BATTLE:       // inverted: redistribute HP between 2 enemies
                return TargetGroup.ENEMY_TWO;

            // ── Enemy then Ally (sequential) ──────────────────────
            case DEBT_TRANSFER:       // inverted: damage enemy, heal ally
                return TargetGroup.ENEMY_THEN_ALLY;

            // ── AOE allies — fires immediately, no click ──────────
            case GROUP_HEAL:
            case GROUP_AMPLIFY:
            case SACRIFICE:           // inverted: heals all allies
            case GROUP_REFLECTION:    // inverted: reflect buff on all allies
                return TargetGroup.AOE_ALLY;

            // ── AOE enemies — fires immediately, no click ─────────
            case SLAM:
                return TargetGroup.AOE_ENEMY;

            // ── AOE all entities ───────────────────────────────────
            case LIFE_SIPHON:         // inverted: damage ALL entities
                return TargetGroup.AOE_ALL;

            // ── Enemy AI ──────────────────────────────────────────
            case ENEMY_ATTACK:
                return TargetGroup.ENEMY;
            case ENEMY_AOE_ATTACK:
                return TargetGroup.AOE_ENEMY;
            case ENEMY_SUPPORT_HEAL:
            case ENEMY_SUPPORT_BUFF:
                return TargetGroup.ALLY;

            default:
                return TargetGroup.NONE;
        }
    }

    /** True if skill fires on all targets with no click needed */
    public static boolean isAOE(ActionType type) {
        TargetGroup g = getTargetGroup(type);
        return g == TargetGroup.AOE_ALLY || g == TargetGroup.AOE_ENEMY || g == TargetGroup.AOE_ALL;
    }

    /** True if skill needs exactly 2 targets selected */
    public static boolean needsTwoTargets(ActionType type) {
        TargetGroup g = getTargetGroup(type);
        return g == TargetGroup.ALLY_TWO || g == TargetGroup.ENEMY_TWO || g == TargetGroup.ENEMY_THEN_ALLY;
    }

    // ──────────────────────────────────────────────────────────────
    // Instance fields
    // ──────────────────────────────────────────────────────────────
    public ActionType actionType;
    public Entity target;
    public Entity secondaryTarget = null; // Mana Transfer, Battle Equalizer
    public int chosenValue = -1;          // Player-chosen value (e.g., Additional Buff 3-5)
    public List<Entity> multiTargets = null; // Group Burden: list of chosen allies

    public ActionRequestComponent(ActionType type, Entity target) {
        this.actionType = type;
        this.target     = target;
    }

    public ActionRequestComponent(ActionType type, Entity target, Entity secondaryTarget) {
        this.actionType      = type;
        this.target          = target;
        this.secondaryTarget = secondaryTarget;
    }

    /** Constructor with a player-chosen value (e.g., Additional Buff). */
    public ActionRequestComponent(ActionType type, Entity target, int chosenValue) {
        this.actionType  = type;
        this.target      = target;
        this.chosenValue = chosenValue;
    }

    /** Factory for multi-target skills (Group Burden 2-ally path). */
    public static ActionRequestComponent withMultiTargets(ActionType type, List<Entity> multiTargets) {
        ActionRequestComponent c = new ActionRequestComponent(type, null);
        c.multiTargets = multiTargets;
        return c;
    }
}
