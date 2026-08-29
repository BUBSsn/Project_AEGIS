package io.github.jhundeniel.ArithmeticHeroes.skills.addition;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.battle.SkillAnimConfig;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.PassiveComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.passives.Passive;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;
import io.github.jhundeniel.ArithmeticHeroes.utils.ActionLogger;

public class HealSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>   sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent>  skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ComponentMapper<PassiveComponent> ps  = ComponentMapper.getFor(PassiveComponent.class);

    private final ActionLogSystem  actionLog;
    private final BattleAnimations animations;
    private final Texture          healSheet;

    public HealSkill(ActionLogSystem actionLog, BattleAnimations animations, Texture healSheet) {
        this.actionLog  = actionLog;
        this.animations = animations;
        this.healSheet  = healSheet;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);
        StatsComponent tStats = sm.get(target);

        // 1. CHECK COMPONENT
        if (!skm.has(user)) {
            actionLog.addMessage("Error: No Component Detected");
            return;
        }

        // 2. GET SKILL DATA
        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.HEAL);
        if (data == null) {
            actionLog.addMessage("Error: No HEAL skill data!");
            return;
        }

        // 3. MANA CHECK
        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);
        if (uStats.mana < finalCost) {
            actionLog.addMessage(uStats.name + " doesn't have enough Mana!");
            return;
        }
        uStats.mana -= finalCost;

        // 4. CALCULATE FINAL HEAL
        double multiplier    = CombatMechanics.BuffConsumption(user);
        int    flatBonus     = CombatMechanics.AdditiveBonusConsumption(user);
        float  effectiveMult = CombatMechanics.getEffectiveness(user);
        int    finalHeal     = (int) ((data.value * multiplier * effectiveMult) + flatBonus);

        // 5. APPLY HEAL
        CombatMechanics.applyHeal(target, finalHeal);
        actionLog.addMessage(ActionLogger.heal(user, target, finalHeal));

        // ── Play Heal animation on the target ──────────────────────
        // Heal.png: 11 frames × 128 px, 1408 px wide
        if (animations != null && healSheet != null) {
            animations.playSkillAnim(healSheet,
                SkillAnimConfig.HEAL_FRAMES,
                SkillAnimConfig.HEAL_DUR,
                target);
        }

        // 6. PASSIVE
        if (ps.has(user)) {
            Passive passive = ps.get(user).passive;
            passive.onHeal(user, target, finalHeal);
        }
    }
}
