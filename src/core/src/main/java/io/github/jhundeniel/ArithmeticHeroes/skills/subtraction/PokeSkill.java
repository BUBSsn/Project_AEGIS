package io.github.jhundeniel.ArithmeticHeroes.skills.subtraction;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.battle.SkillAnimConfig;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class PokeSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);

    private final ActionLogSystem  actionLog;
    private final BattleAnimations animations;
    private final Texture          pokeSheet;

    public PokeSkill(ActionLogSystem actionLog, BattleAnimations animations, Texture pokeSheet) {
        this.actionLog  = actionLog;
        this.animations = animations;
        this.pokeSheet  = pokeSheet;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);

        if (!skm.has(user)) { actionLog.addMessage("Error: No Component Detected"); return; }

        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.POKE);

        int selfCost  = (int) (uStats.maxHp * data.hpCostPct);
        int finalCost = CombatMechanics.getFinalHpCost(user, selfCost);

        if (uStats.hp <= finalCost) {
            actionLog.addMessage("NOT ENOUGH HP TO USE POKE!");
            return;
        }

        uStats.hp -= finalCost;

        double buffMultiplier = CombatMechanics.BuffConsumption(user);
        int    flatBonus      = CombatMechanics.AdditiveBonusConsumption(user);
        float  effectiveMult  = CombatMechanics.getEffectiveness(user);
        int    finalDamage    = (int) ((data.value * buffMultiplier * effectiveMult) + flatBonus);

        CombatMechanics.applyDamage(user, target, finalDamage);

        // ── Poke.png: 7 frames × 128 px, 896 px wide ────────────────────
        // Show on the ENEMY — the poke lands on them.
        if (animations != null && pokeSheet != null) {
            animations.playSkillAnim(pokeSheet,
                SkillAnimConfig.POKE_FRAMES,
                SkillAnimConfig.POKE_DUR,
                target);
        }

        StatsComponent tStats = sm.get(target);
        actionLog.addMessage(uStats.name.trim() + " pokes " + tStats.name.trim()
            + " for " + finalDamage + " dmg! (-" + finalCost + " HP)");
    }
}
