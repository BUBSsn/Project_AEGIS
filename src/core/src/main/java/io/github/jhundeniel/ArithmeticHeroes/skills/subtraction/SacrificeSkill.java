package io.github.jhundeniel.ArithmeticHeroes.skills.subtraction;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.*;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

/**
 * Inverted Slam → Sacrifice.
 * Costs 5% own HP to heal ALL allies for 5 base.
 */
public class SacrificeSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);

    private final Engine          engine;
    private final ActionLogSystem actionLog;

    public SacrificeSkill(Engine engine, ActionLogSystem actionLog) {
        this.engine    = engine;
        this.actionLog = actionLog;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);

        if (!skm.has(user)) { actionLog.addMessage("Error: No Component Detected"); return; }

        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.SACRIFICE);
        if (data == null) { actionLog.addMessage("Error: No SACRIFICE data!"); return; }

        // HP cost
        int selfCost  = (int) (uStats.maxHp * data.hpCostPct);
        int finalCost = CombatMechanics.getFinalHpCost(user, selfCost);

        if (uStats.hp <= finalCost) {
            actionLog.addMessage("Not enough HP for Sacrifice!");
            return;
        }
        uStats.hp -= finalCost;

        // Calculate heal
        double multiplier   = CombatMechanics.BuffConsumption(user);
        int    flatBonus    = CombatMechanics.AdditiveBonusConsumption(user);
        float  effectiveMult = CombatMechanics.getEffectiveness(user);
        int    finalHeal    = (int) ((data.value * multiplier * effectiveMult) + flatBonus);

        // Heal all living allies
        ImmutableArray<Entity> allEntities = engine.getEntitiesFor(
            Family.all(StatsComponent.class, PartyComponent.class).get());

        int healed = 0;
        for (Entity ally : allEntities) {
            PartyComponent party = ally.getComponent(PartyComponent.class);
            StatsComponent s     = sm.get(ally);
            // Exclude the caster — Sacrifice costs HP, the caster should NOT heal themselves
            if (ally == user) continue;
            if (party != null && party.isPlayer && s != null && s.hp > 0) {
                CombatMechanics.applyHeal(ally, finalHeal);
                healed++;
            }
        }

        actionLog.addMessage(String.format("SACRIFICE: %s sacrificed %d HP to heal %d allies for %d each!",
            uStats.name.trim(), finalCost, healed, finalHeal));
    }
}
