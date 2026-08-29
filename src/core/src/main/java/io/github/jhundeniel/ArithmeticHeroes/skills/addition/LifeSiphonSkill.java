package io.github.jhundeniel.ArithmeticHeroes.skills.addition;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.*;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

/**
 * Inverted Group Heal → Life Siphon.
 * Removes HP from ALL entities on the field. 3 base damage, 5 mana.
 */
public class LifeSiphonSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);

    private final Engine          engine;
    private final ActionLogSystem actionLog;

    public LifeSiphonSkill(Engine engine, ActionLogSystem actionLog) {
        this.engine    = engine;
        this.actionLog = actionLog;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);

        if (!skm.has(user)) { actionLog.addMessage("Error: No Component Detected"); return; }

        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.LIFE_SIPHON);
        if (data == null) { actionLog.addMessage("Error: No LIFE_SIPHON data!"); return; }

        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);
        if (uStats.mana < finalCost) { actionLog.addMessage("Not enough Mana!"); return; }
        uStats.mana -= finalCost;

        double multiplier   = CombatMechanics.BuffConsumption(user);
        int    flatBonus    = CombatMechanics.AdditiveBonusConsumption(user);
        float  effectiveMult = CombatMechanics.getEffectiveness(user);
        int    finalDamage  = (int) ((data.value * multiplier * effectiveMult) + flatBonus);

        ImmutableArray<Entity> all = engine.getEntitiesFor(
            Family.all(StatsComponent.class).get());

        int hitCount = 0;
        for (Entity entity : all) {
            StatsComponent s = sm.get(entity);
            if (s != null && s.hp > 0) {
                CombatMechanics.applyDamage(user, entity, finalDamage);
                hitCount++;
            }
        }

        actionLog.addMessage(uStats.name.trim() + " uses Life Siphon! "
            + finalDamage + " dmg to ALL " + hitCount + " entities.");
    }
}
