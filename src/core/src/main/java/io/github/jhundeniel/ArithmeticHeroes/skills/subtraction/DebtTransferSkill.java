package io.github.jhundeniel.ArithmeticHeroes.skills.subtraction;

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
 * Inverted Life Steal → Debt Transfer.
 * 15 base damage to an enemy, 50% of damage heals an ally (secondaryTarget).
 * Uses ENEMY_THEN_ALLY targeting: first pick enemy, then pick ally.
 */
public class DebtTransferSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>         sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent>        skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ComponentMapper<ActionRequestComponent> am  = ComponentMapper.getFor(ActionRequestComponent.class);

    private final ActionLogSystem actionLog;

    public DebtTransferSkill(ActionLogSystem actionLog) {
        this.actionLog = actionLog;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);
        StatsComponent tStats = sm.get(target);

        if (!skm.has(user)) { actionLog.addMessage("Error: No Component Detected"); return; }

        // Get the second target (ally to heal)
        Entity allyTarget = am.has(user) ? am.get(user).secondaryTarget : null;
        if (allyTarget == null) {
            actionLog.addMessage("Error: Debt Transfer requires an ally target!");
            return;
        }

        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.DEBT_TRANSFER);
        if (data == null) { actionLog.addMessage("Error: No DEBT_TRANSFER data!"); return; }

        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);
        if (uStats.mana < finalCost) { actionLog.addMessage("Not enough Mana!"); return; }
        uStats.mana -= finalCost;

        double multiplier   = CombatMechanics.BuffConsumption(user);
        int    flatBonus    = CombatMechanics.AdditiveBonusConsumption(user);
        float  effectiveMult = CombatMechanics.getEffectiveness(user);
        int    finalDamage  = (int) ((data.value * multiplier * effectiveMult) + flatBonus);

        // Deal damage to enemy
        CombatMechanics.applyDamage(user, target, finalDamage);

        // Heal ally for 50% of damage dealt
        int healAmount = finalDamage / 2;
        StatsComponent allyStats = sm.get(allyTarget);
        CombatMechanics.applyHeal(allyTarget, healAmount);

        actionLog.addMessage(String.format(
            "DEBT TRANSFER: %s dealt %d dmg to %s, healed %s for %d!",
            uStats.name.trim(), finalDamage, tStats.name.trim(),
            allyStats.name.trim(), healAmount));
    }
}
