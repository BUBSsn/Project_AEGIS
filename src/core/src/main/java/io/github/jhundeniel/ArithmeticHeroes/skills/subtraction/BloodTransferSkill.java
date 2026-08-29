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
 * Inverted Poke → Blood Transfer.
 * Costs 10% own HP to heal an ally target for 10 base.
 */
public class BloodTransferSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);

    private final ActionLogSystem actionLog;

    public BloodTransferSkill(ActionLogSystem actionLog) {
        this.actionLog = actionLog;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);
        StatsComponent tStats = sm.get(target);

        if (!skm.has(user)) { actionLog.addMessage("Error: No Component Detected"); return; }

        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.BLOOD_TRANSFER);
        if (data == null) { actionLog.addMessage("Error: No BLOOD_TRANSFER data!"); return; }

        // HP cost
        int selfCost  = (int) (uStats.maxHp * data.hpCostPct);
        int finalCost = CombatMechanics.getFinalHpCost(user, selfCost);

        if (uStats.hp <= finalCost) {
            actionLog.addMessage("Not enough HP for Blood Transfer!");
            return;
        }
        uStats.hp -= finalCost;

        // Calculate heal
        double multiplier   = CombatMechanics.BuffConsumption(user);
        int    flatBonus    = CombatMechanics.AdditiveBonusConsumption(user);
        float  effectiveMult = CombatMechanics.getEffectiveness(user);
        int    finalHeal    = (int) ((data.value * multiplier * effectiveMult) + flatBonus);

        CombatMechanics.applyHeal(target, finalHeal);

        actionLog.addMessage(String.format("BLOOD TRANSFER: %s sacrificed %d HP to heal %s for %d!",
            uStats.name.trim(), finalCost, tStats.name.trim(), finalHeal));
    }
}
