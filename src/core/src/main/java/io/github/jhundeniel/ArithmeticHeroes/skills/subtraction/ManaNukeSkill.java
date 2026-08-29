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
 * Inverted Conditional Attack → Mana Nuke.
 * 20 base damage, 7 mana. If caster HP > 75%, deals extra damage based on missing mana.
 */
public class ManaNukeSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);

    private final ActionLogSystem actionLog;

    public ManaNukeSkill(ActionLogSystem actionLog) {
        this.actionLog = actionLog;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);
        StatsComponent tStats = sm.get(target);

        if (!skm.has(user)) { actionLog.addMessage("Error: No Component Detected"); return; }

        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.MANA_NUKE);
        if (data == null) { actionLog.addMessage("Error: No MANA_NUKE data!"); return; }

        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);
        if (uStats.mana < finalCost) { actionLog.addMessage("Not enough Mana!"); return; }
        uStats.mana -= finalCost;

        double multiplier   = CombatMechanics.BuffConsumption(user);
        int    flatBonus    = CombatMechanics.AdditiveBonusConsumption(user);
        float  effectiveMult = CombatMechanics.getEffectiveness(user);
        int    baseDamage   = (int) ((data.value * multiplier * effectiveMult) + flatBonus);

        // Bonus: if HP > 75%, add missing mana as extra damage
        int bonusDamage = 0;
        if (uStats.hp > (int)(uStats.maxHp * 0.75f)) {
            int missingMana = uStats.maxMana - uStats.mana;
            bonusDamage = missingMana;
        }

        int finalDamage = baseDamage + bonusDamage;
        CombatMechanics.applyDamage(user, target, finalDamage);

        String msg = uStats.name.trim() + " uses Mana Nuke on " + tStats.name.trim()
            + " for " + finalDamage + " dmg!";
        if (bonusDamage > 0) msg += " (+" + bonusDamage + " from missing mana!)";
        actionLog.addMessage(msg);
    }
}
