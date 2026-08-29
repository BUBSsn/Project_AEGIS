package io.github.jhundeniel.ArithmeticHeroes.skills.enemies;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class BasicEnemyHeal implements SkillStrategy {

    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ActionLogSystem actionLog;

    public BasicEnemyHeal(ActionLogSystem actionLog) {
        this.actionLog = actionLog;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);

        if (!skm.has(user)) {
            actionLog.addMessage("Error: No SkillsComponent on " +
                (uStats != null ? uStats.name.trim() : "enemy"));
            return;
        }

        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.ENEMY_SUPPORT_HEAL);

        if (data == null) {
            actionLog.addMessage("Error: no ENEMY_SUPPORT_HEAL skill data found!");
            return;
        }

        double multiplier = CombatMechanics.BuffConsumption(user);
        int    finalHeal  = (int) (data.value * multiplier);

        // CombatSystem already logged "X uses Bandage!" — just show the result
        String name = uStats != null ? uStats.name.trim() : "Enemy";
        actionLog.addMessage(name + " restored " + finalHeal + " HP!");

        CombatMechanics.applyHeal(user, finalHeal);
    }
}
