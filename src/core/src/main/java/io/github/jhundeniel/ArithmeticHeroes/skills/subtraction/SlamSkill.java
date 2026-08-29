package io.github.jhundeniel.ArithmeticHeroes.skills.subtraction;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.*;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

import java.util.ArrayList;
import java.util.List;

public class SlamSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);

    private final Engine           engine;
    private final ActionLogSystem  actionLog;
    private final BattleAnimations animations;
    private final Texture          slamSheet;

    public SlamSkill(Engine engine, ActionLogSystem actionLog,
                     BattleAnimations animations, Texture slamSheet) {
        this.engine     = engine;
        this.actionLog  = actionLog;
        this.animations = animations;
        this.slamSheet  = slamSheet;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);

        // 1. CHECKS IF USER HAS COMPONENT
        if (!skm.has(user)) {
            actionLog.addMessage("Error: No Component Detected");
            return;
        }

        // 2. GET SPECIFIC DATA
        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.SLAM);

        // 3. CALCULATE HP COST
        int selfCost  = (int) (uStats.maxHp * data.hpCostPct);
        int finalCost = CombatMechanics.getFinalHpCost(user, selfCost);

        // 4. CHECK IF USER HP IS HIGH ENOUGH
        if (uStats.hp > finalCost) {
            uStats.hp -= finalCost;
            actionLog.addMessage(uStats.name.trim() + " uses Slam!");

            double multiplier    = CombatMechanics.BuffConsumption(user);
            int    flatBonus     = CombatMechanics.AdditiveBonusConsumption(user);
            float  effectiveMult = CombatMechanics.getEffectiveness(user);
            int    finalDamage   = (int) ((data.value * multiplier * effectiveMult) + flatBonus);

            // 5. COLLECT ALL LIVING ENEMIES
            List<Entity> validTargets = new ArrayList<>();
            ImmutableArray<Entity> allEntities = engine.getEntitiesFor(
                Family.all(StatsComponent.class, TypeComponent.class).get());

            for (Entity entity : allEntities) {
                TypeComponent  type = entity.getComponent(TypeComponent.class);
                StatsComponent s    = entity.getComponent(StatsComponent.class);
                if (type != null && type.type == Operator.MOB && s != null && s.hp > 0)
                    validTargets.add(entity);
            }

            // 6. DEAL DAMAGE AND PLAY ANIMATION ON EACH ENEMY
            for (Entity enemy : validTargets) {
                CombatMechanics.applyDamage(user, enemy, finalDamage);

                // ── Play Slam animation on each enemy hit ───────
                if (animations != null && slamSheet != null)
                    animations.playSkillAnim(slamSheet, 8, 0.1f, enemy);
            }

            // ── Also play on the user (caster) ──────────────────
            if (animations != null && slamSheet != null)
                animations.playSkillAnim(slamSheet, 9, 0.09f, user);

        } else {
            actionLog.addMessage("Not enough HP to use slam!");
        }
    }
}
