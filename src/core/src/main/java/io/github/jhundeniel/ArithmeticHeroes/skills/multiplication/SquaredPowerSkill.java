package io.github.jhundeniel.ArithmeticHeroes.skills.multiplication;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.PartyComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class SquaredPowerSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ComponentMapper<PartyComponent>  pm  = ComponentMapper.getFor(PartyComponent.class);

    private final ActionLogSystem  actionLog;
    private final BattleAnimations animations;
    private final Texture          squaredSheet;

    public SquaredPowerSkill(ActionLogSystem actionLog,
                             BattleAnimations animations, Texture squaredSheet) {
        this.actionLog    = actionLog;
        this.animations   = animations;
        this.squaredSheet = squaredSheet;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);
        StatsComponent tStats = sm.get(target);

        // 1. CHECKS IF USER HAS COMPONENT
        if (!skm.has(user)) {
            actionLog.addMessage("Error: No Component Detected");
            return;
        }

        // 2. GET SPECIFIC DATA
        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.SQUARED_POWER);

        // 3. MANA CHECK
        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);

        if (uStats.mana >= finalCost) {
            uStats.mana -= finalCost;

            StatusEffects.add(target, StatusEffect.squared(data.value));

            String msg = String.format("SQUARED POWER! %s's next action will cast TWICE at %.0f%% power!\n",
                tStats.name, data.value * 100);
            actionLog.addMessage(msg);

            // ── Play Squared animation on the target ────────────
            if (animations != null && squaredSheet != null)
                animations.playSkillAnim(squaredSheet, 8, 0.10f, target);

        } else {
            actionLog.addMessage("Not Enough Mana!");
        }
    }
}
