package io.github.jhundeniel.ArithmeticHeroes.skills.addition;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

/**
 * Inverted Mana Transfer → Mana Steal.
 * Steals 5 mana from a single ally target and gives it to Addition (the caster).
 */
public class ManaStealSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);

    private final ActionLogSystem actionLog;

    public ManaStealSkill(ActionLogSystem actionLog) {
        this.actionLog = actionLog;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);
        StatsComponent tStats = sm.get(target);

        if (!skm.has(user)) { actionLog.addMessage("Error: No Component Detected"); return; }

        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.MANA_STEAL);
        if (data == null) { actionLog.addMessage("Error: No MANA_STEAL data!"); return; }

        int baseSteal = (int) data.value; // 5

        // Bonus mana proportional to the target ally's remaining HP
        float hpRatio = (tStats.maxHp > 0) ? (float) tStats.hp / tStats.maxHp : 0f;
        int bonusMana = (int) (hpRatio * baseSteal);
        int stealAmount = baseSteal + bonusMana;

        // Check if target has enough mana
        int actualSteal = Math.min(stealAmount, tStats.mana);
        if (actualSteal <= 0) {
            actionLog.addMessage(tStats.name.trim() + " has no mana to steal!");
            return;
        }

        tStats.mana -= actualSteal;
        uStats.mana = Math.min(uStats.maxMana, uStats.mana + actualSteal);

        actionLog.addMessage(String.format("MANA STEAL: %s stole %d mana from %s! (base %d + %d bonus)",
            uStats.name.trim(), actualSteal, tStats.name.trim(), baseSteal, bonusMana));
    }
}
