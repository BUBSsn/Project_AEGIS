package io.github.jhundeniel.ArithmeticHeroes.skills.division;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.battle.SkillAnimConfig;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class BurdenSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm      = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm     = ComponentMapper.getFor(SkillsComponent.class);

    private final ActionLogSystem  actionLog;
    private final BattleAnimations animations;
    private final Texture          shieldSheet;

    public BurdenSkill(ActionLogSystem actionLog,
                       BattleAnimations animations, Texture shieldSheet) {
        this.actionLog   = actionLog;
        this.animations  = animations;
        this.shieldSheet = shieldSheet;
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
        SkillData data = skills.get(ActionRequestComponent.ActionType.BURDEN);

        // 3. MANA CHECK
        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);

        if (uStats.mana >= finalCost) {
            uStats.mana -= finalCost;

            double multiplier    = CombatMechanics.BuffConsumption(user);
            float  newRatio      = (float) (data.value * multiplier);
            if (newRatio > 1.0f) newRatio = 1.0f;
            int    displayPercent = Math.round(newRatio * 100f);

            StatusEffect existing = StatusEffects.get(target, StatusEffect.Type.BURDEN);
            if (existing != null) {
                if (newRatio > existing.shareRatio) {
                    existing.shareRatio = newRatio;
                    existing.protector  = user;
                    actionLog.addMessage("Burden overwritten with a stronger shield!");
                } else {
                    actionLog.addMessage(tStats.name.trim() + " already has an equal or stronger Burden.");
                }
            } else {
                StatusEffects.add(target, StatusEffect.burden(user, newRatio));
                String msg = String.format("Burden applied! Sharing %d%% damage from %s.\n",
                    displayPercent, tStats.name.trim());
                actionLog.addMessage(msg);
            }

            // ── Play Shield animation on the target ─────────────
            if (animations != null && shieldSheet != null)
                animations.playSkillAnim(shieldSheet,
                    SkillAnimConfig.SHIELD_FRAMES,
                    SkillAnimConfig.SHIELD_DUR,
                    target);

        } else {
            actionLog.addMessage("Not enough Mana!");
        }
    }
}
