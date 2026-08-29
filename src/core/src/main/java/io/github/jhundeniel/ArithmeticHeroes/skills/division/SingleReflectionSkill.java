package io.github.jhundeniel.ArithmeticHeroes.skills.division;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

/**
 * Inverted Single Burden → Single Reflection.
 * Grants a buff to an ally: 50% of the next incoming damage is reflected back to the attacker.
 * One-hit consume — the ReflectionComponent is removed after the first reflected hit.
 */
public class SingleReflectionSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);

    private final ActionLogSystem actionLog;

    public SingleReflectionSkill(ActionLogSystem actionLog) {
        this.actionLog = actionLog;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);
        StatsComponent tStats = sm.get(target);

        if (!skm.has(user)) { actionLog.addMessage("Error: No Component Detected"); return; }

        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.SINGLE_REFLECTION);
        if (data == null) { actionLog.addMessage("Error: No SINGLE_REFLECTION data!"); return; }

        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);
        if (uStats.mana < finalCost) { actionLog.addMessage("Not enough Mana!"); return; }
        uStats.mana -= finalCost;

        // Apply 50% reflect buff
        StatusEffects.add(target, StatusEffect.reflection(0.50f));

        CombatMechanics.notifyBuff(target, "REFLECT 50%");

        actionLog.addMessage(String.format(
            "SINGLE REFLECTION: %s granted %s a 50%% damage reflection shield!",
            uStats.name.trim(), tStats.name.trim()));
    }
}
