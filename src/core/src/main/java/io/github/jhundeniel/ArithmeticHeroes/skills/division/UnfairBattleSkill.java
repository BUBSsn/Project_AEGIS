package io.github.jhundeniel.ArithmeticHeroes.skills.division;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

/**
 * Inverted Battle Equalizer → Unfair Battle.
 * Target two ENEMIES. Sum their total HP. Redistribute:
 * - Boss + Mob: Boss gets 80%, Mob gets 20%
 * - Mob + Mob: 50% / 50% split
 */
public class UnfairBattleSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>         sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent>        skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ComponentMapper<ActionRequestComponent> am  = ComponentMapper.getFor(ActionRequestComponent.class);

    private final ActionLogSystem actionLog;

    public UnfairBattleSkill(ActionLogSystem actionLog) {
        this.actionLog = actionLog;
    }

    @Override
    public void execute(Entity user, Entity target1) {
        StatsComponent uStats = sm.get(user);

        if (!skm.has(user)) { actionLog.addMessage("Error: No Component Detected"); return; }

        // Get second enemy target
        if (!am.has(user)) {
            actionLog.addMessage("Error: Unfair Battle requires an action request!");
            return;
        }
        Entity target2 = am.get(user).secondaryTarget;
        if (target2 == null) {
            actionLog.addMessage("Error: Unfair Battle requires TWO enemy targets!");
            return;
        }

        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.UNFAIR_BATTLE);
        if (data == null) { actionLog.addMessage("Error: No UNFAIR_BATTLE data!"); return; }

        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);
        if (uStats.mana < finalCost) { actionLog.addMessage("Not enough Mana!"); return; }
        uStats.mana -= finalCost;

        StatsComponent t1Stats = sm.get(target1);
        StatsComponent t2Stats = sm.get(target2);

        int totalHP = t1Stats.hp + t2Stats.hp;
        int oldHp1  = t1Stats.hp;
        int oldHp2  = t2Stats.hp;

        // Determine type: is either a Boss (non-MOB, non-player)?
        // In this game's context, all enemies are MOB type.
        // We check if either has a "boss" flag in their name or higher maxHp as heuristic.
        // Actually, the user spec says Boss vs Mob. Let's check maxHp > some threshold,
        // or simply check if it's a boss by maxHp comparison.
        boolean t1IsBoss = isBoss(target1);
        boolean t2IsBoss = isBoss(target2);

        if (t1IsBoss && !t2IsBoss) {
            // Boss gets 80%, Mob gets 20%
            t1Stats.hp = Math.min(t1Stats.maxHp, (int)(totalHP * 0.80f));
            t2Stats.hp = Math.min(t2Stats.maxHp, totalHP - t1Stats.hp);
        } else if (!t1IsBoss && t2IsBoss) {
            // Mob gets 20%, Boss gets 80%
            t2Stats.hp = Math.min(t2Stats.maxHp, (int)(totalHP * 0.80f));
            t1Stats.hp = Math.min(t1Stats.maxHp, totalHP - t2Stats.hp);
        } else {
            // Mob + Mob: 50/50
            int half      = totalHP / 2;
            int remainder = totalHP % 2;
            t1Stats.hp = Math.min(t1Stats.maxHp, half + remainder);
            t2Stats.hp = Math.min(t2Stats.maxHp, half);
        }

        actionLog.addMessage(String.format(
            "UNFAIR BATTLE: Combined %d HP. %s: %d→%d, %s: %d→%d",
            totalHP, t1Stats.name.trim(), oldHp1, t1Stats.hp,
            t2Stats.name.trim(), oldHp2, t2Stats.hp));
    }

    /**
     * Heuristic: a boss has significantly higher maxHp (>= 50).
     * This can be refined with a proper BossComponent flag.
     */
    private boolean isBoss(Entity entity) {
        StatsComponent s = sm.get(entity);
        return s != null && s.maxHp >= 50;
    }
}
