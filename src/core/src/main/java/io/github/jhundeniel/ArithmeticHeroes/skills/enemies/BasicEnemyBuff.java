package io.github.jhundeniel.ArithmeticHeroes.skills.enemies;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class BasicEnemyBuff implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ActionLogSystem actionLog;

    public BasicEnemyBuff(ActionLogSystem actionLog) {
        this.actionLog = actionLog;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);

        // 1. CHECK COMPONENT
        if (!skm.has(user)) {
            actionLog.addMessage("Error: No Component Detected");
            return;
        }

        // 2. GET SKILL DATA
        // FIX 1: was ENEMY_SUPPORT_HEAL (wrong key) → now correctly uses ENEMY_SUPPORT_BUFF
        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.ENEMY_SUPPORT_BUFF);

        if (data == null) {
            actionLog.addMessage("Error: " + uStats.name.trim() + " has no buff data!");
            return;
        }

        // FIX 2: enemy buffs ITSELF, not the target parameter (which can be null)
        // Using 'user' directly instead of 'target' avoids a NullPointerException
        StatusEffects.add(user, StatusEffect.buff(data.value));

        String msg = String.format("%s buffs himself! (x%.2f next action)\n",
            uStats.name.trim(), data.value);
        actionLog.addMessage(msg);
    }
}
