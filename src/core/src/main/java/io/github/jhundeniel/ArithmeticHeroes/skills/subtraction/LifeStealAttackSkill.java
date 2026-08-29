package io.github.jhundeniel.ArithmeticHeroes.skills.subtraction;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class LifeStealAttackSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ActionLogSystem actionLog;
    private final BattleAnimations animations;
    private final com.badlogic.gdx.graphics.Texture slashSheet;
    private final com.badlogic.gdx.graphics.Texture healSheet;

    public LifeStealAttackSkill(ActionLogSystem actionLog,
                                BattleAnimations animations,
                                com.badlogic.gdx.graphics.Texture slashSheet,
                                com.badlogic.gdx.graphics.Texture healSheet) {
        this.actionLog  = actionLog;
        this.animations = animations;
        this.slashSheet = slashSheet;
        this.healSheet  = healSheet;
    }
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);

        //1. CHECKS IF USER HAS COMPONENT
        if (!skm.has(user)) {
            actionLog.addMessage("Error: No Component Detected");
            return;
        }

        //2. GET SPECIFIC DATA
        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.LIFESTEAL_ATTACK);

        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);


        if (uStats.mana >= finalCost) {
            uStats.mana -= finalCost;

            //1. GET BUFF MULTIPLIER & FLAT BONUS
            double multiplier = CombatMechanics.BuffConsumption(user);
            int flatBonus = CombatMechanics.AdditiveBonusConsumption(user);
            float effectiveMult = CombatMechanics.getEffectiveness(user);

            //2. CALCULATE FINAL DAMAGE
            int finalDamage = (int) ((data.value * multiplier * effectiveMult) + flatBonus);

            //3. CALCULATE LIFESTEAL AMOUNT
            int siphonedHP = (finalDamage / 2);

            //4. APPLY DAMAGE
            CombatMechanics.applyDamage(user, target, finalDamage);
            if (animations != null && slashSheet != null) {
                animations.playSkillAnim(slashSheet,
                    io.github.jhundeniel.ArithmeticHeroes.battle.SkillAnimConfig.LIFE_STEAL_FRAMES,
                    io.github.jhundeniel.ArithmeticHeroes.battle.SkillAnimConfig.LIFE_STEAL_DUR,
                    target);
            }

            //5. APPLY LIFESTEAL
            CombatMechanics.applyHeal(user, siphonedHP);
            if (animations != null && healSheet != null) {
                animations.playSkillAnim(healSheet,
                    io.github.jhundeniel.ArithmeticHeroes.battle.SkillAnimConfig.LIFE_STEAL_HEAL_FRAMES,
                    io.github.jhundeniel.ArithmeticHeroes.battle.SkillAnimConfig.LIFE_STEAL_HEAL_DUR,
                    user);
            }
        } else {
            actionLog.addMessage("Not enough Mana!");
        }
    }
}
