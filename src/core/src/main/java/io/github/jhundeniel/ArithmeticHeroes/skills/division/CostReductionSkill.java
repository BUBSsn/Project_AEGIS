package io.github.jhundeniel.ArithmeticHeroes.skills.division;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class CostReductionSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ActionLogSystem actionLog;

    public CostReductionSkill(ActionLogSystem actionLog) {
        this.actionLog = actionLog;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);
        StatsComponent tStats = sm.get(target);

        //1. CHECKS IF USER HAS COMPONENT
        if (!skm.has(user)) {
            actionLog.addMessage("Error: No Component Detected");
            return;
        }

        //2. GET SPECIFIC DATA
        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.COST_REDUCTION);

        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);


        if (uStats.mana >= finalCost) {
            uStats.mana -= finalCost;

            //TODO: SQUARED BUFF STACKING
            //APPLY BUFF
            // 50% Reduction means the multiplier is 0.5
            float multiplier = 1.0f - data.value;

            StatusEffects.add(target, StatusEffect.costReduction(multiplier, data.duration));
            String msg = String.format("%s applied Skill Cost Reduction on %s! Costs halved for %d turns.\n",
                uStats.name, tStats.name, data.duration);
            actionLog.addMessage(msg);
        } else {
            actionLog.addMessage("Not enough Mana!");
        }
    }
}
