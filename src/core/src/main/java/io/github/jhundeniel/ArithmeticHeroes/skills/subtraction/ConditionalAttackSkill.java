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

public class ConditionalAttackSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ActionLogSystem actionLog;
    private final BattleAnimations animations;
    private final com.badlogic.gdx.graphics.Texture animSheet;

    public ConditionalAttackSkill(ActionLogSystem actionLog,
                                  BattleAnimations animations,
                                  com.badlogic.gdx.graphics.Texture animSheet) {
        this.actionLog  = actionLog;
        this.animations = animations;
        this.animSheet  = animSheet;
    }

    @Override
    public void execute(Entity user, Entity target){
        StatsComponent uStats = sm.get(user);
        StatsComponent tStats = sm.get(target);

        //1. CHECKS IF USER HAS COMPONENT
        if (!skm.has(user)) {
            actionLog.addMessage("Error: No Component Detected");
            return;
        }

        //CHECK IF THE USER IS BELOW HALF HEALTH
        int HalfHealth = (int) (uStats.maxHp *0.5f);

        if (!(uStats.hp <= HalfHealth)) {
            String msg = String.format("%s is not below half HP! \n", uStats.name);
            actionLog.addMessage(msg);
            return;
        }

        //2. GET SPECIFIC DATA
        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.CONDITIONAL_ATTACK);

        //3. MANA CHECK
        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);


        if (uStats.mana >= finalCost) {
            uStats.mana -= finalCost;

            //1. GET BUFF MULTIPLIER & FLAT BONUS
            double multiplier = CombatMechanics.BuffConsumption(user);
            int flatBonus = CombatMechanics.AdditiveBonusConsumption(user);
            float effectiveMult = CombatMechanics.getEffectiveness(user);

            //2. CALCULATE FINAL DAMAGE
            int finalDamage = (int) ((data.value * multiplier * effectiveMult) + flatBonus);
            CombatMechanics.applyDamage(user, target, finalDamage);
            if (animations != null && animSheet != null) {
                animations.playSkillAnim(animSheet,
                    io.github.jhundeniel.ArithmeticHeroes.battle.SkillAnimConfig.CONDITIONAL_FRAMES,
                    io.github.jhundeniel.ArithmeticHeroes.battle.SkillAnimConfig.CONDITIONAL_DUR,
                    target);
            }
        } else {
            actionLog.addMessage("Not enough Mana!");
        }
    }
}
