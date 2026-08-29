package io.github.jhundeniel.ArithmeticHeroes.skills.division;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.*;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

/**
 * Inverted Group Burden → Group Reflection.
 * AOE: adds reflection to all living allies. Percentage depends on ally count:
 * - 2 allies: 25% each
 * - 3 allies: 15% each
 * One-hit consume per ally.
 */
public class GroupReflectionSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);

    private final Engine          engine;
    private final ActionLogSystem actionLog;

    public GroupReflectionSkill(Engine engine, ActionLogSystem actionLog) {
        this.engine    = engine;
        this.actionLog = actionLog;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);

        if (!skm.has(user)) { actionLog.addMessage("Error: No Component Detected"); return; }

        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.GROUP_REFLECTION);
        if (data == null) { actionLog.addMessage("Error: No GROUP_REFLECTION data!"); return; }

        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);
        if (uStats.mana < finalCost) { actionLog.addMessage("Not enough Mana!"); return; }
        uStats.mana -= finalCost;

        // Count living allies
        ImmutableArray<Entity> allEntities = engine.getEntitiesFor(
            Family.all(StatsComponent.class, PartyComponent.class).get());

        java.util.List<Entity> livingAllies = new java.util.ArrayList<>();
        for (Entity ally : allEntities) {
            PartyComponent party = ally.getComponent(PartyComponent.class);
            StatsComponent s     = sm.get(ally);
            if (party != null && party.isPlayer && s != null && s.hp > 0) {
                livingAllies.add(ally);
            }
        }

        // Determine reflection percentage based on ally count
        float reflectPct;
        if (livingAllies.size() <= 2) {
            reflectPct = data.value;           // 0.25 (25%)
        } else {
            reflectPct = data.secondaryValue;  // 0.15 (15%)
        }

        // Apply to all living allies
        for (Entity ally : livingAllies) {
            StatusEffects.add(ally, StatusEffect.reflection(reflectPct));
            CombatMechanics.notifyBuff(ally, "REFLECT " + (int)(reflectPct * 100) + "%");
        }

        actionLog.addMessage(String.format(
            "GROUP REFLECTION: %s granted %d allies %d%% damage reflection!",
            uStats.name.trim(), livingAllies.size(), (int)(reflectPct * 100)));
    }
}
