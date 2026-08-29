package io.github.jhundeniel.ArithmeticHeroes.skills.division;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.battle.SkillAnimConfig;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.PartyComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

import java.util.ArrayList;
import java.util.List;

public class GroupBurdenSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm      = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm     = ComponentMapper.getFor(SkillsComponent.class);
    private final ComponentMapper<PartyComponent>  pm      = ComponentMapper.getFor(PartyComponent.class);

    private final Engine           engine;
    private final ActionLogSystem  actionLog;
    private final BattleAnimations animations;
    private final Texture          shieldSheet;   // Shield.png (16 frames)

    public GroupBurdenSkill(Engine engine, ActionLogSystem actionLog,
                            BattleAnimations animations, Texture shieldSheet) {
        this.engine      = engine;
        this.actionLog   = actionLog;
        this.animations  = animations;
        this.shieldSheet = shieldSheet;
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
        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.GROUP_BURDEN);

        // 3. MANA CHECK
        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);
        if (uStats.mana < finalCost) {
            actionLog.addMessage("Not enough mana!");
            return;
        }
        uStats.mana -= finalCost;

        // 4. GET BUFF MULTIPLIER
        double multiplier = CombatMechanics.BuffConsumption(user);

        // 5. FIND VALID ALLIES (same pattern as GroupAmplifySkill)
        List<Entity> validTargets = new ArrayList<>();
        ImmutableArray<Entity> allEntities = engine.getEntitiesFor(
            Family.all(StatsComponent.class, PartyComponent.class).get());

        for (Entity entity : allEntities) {
            if (pm.get(entity).isPlayer && entity != user && sm.get(entity).hp > 0) {
                validTargets.add(entity);
            }
        }

        // 6. DETERMINE SHARE PERCENTAGE BASED ON ALLY COUNT
        int   allyCount   = validTargets.size();
        float basePercent = (allyCount <= 2) ? data.value : data.secondaryValue;
        float finalPercent = Math.min(1.0f, basePercent * (float) multiplier);

        String msg = String.format("%s casts Group Burden! Protecting %d allies at %.0f%% each.",
            uStats.name.trim(), allyCount, finalPercent * 100);
        actionLog.addMessage(msg);

        // 7. APPLY BURDEN + ANIMATION to each valid ally
        for (Entity ally : validTargets) {
            StatusEffect existing = StatusEffects.get(ally, StatusEffect.Type.BURDEN);
            if (existing != null) {
                if (finalPercent > existing.shareRatio) {
                    existing.shareRatio = finalPercent;
                    existing.protector  = user;
                    actionLog.addMessage("Burden upgraded for: " + sm.get(ally).name.trim());
                }
            } else {
                StatusEffects.add(ally, StatusEffect.burden(user, finalPercent));
                actionLog.addMessage("Sharing Burden with: " + sm.get(ally).name.trim());
            }

            // ── Shield.png: 16 frames × 125 px, 2000 px wide ────────────
            if (animations != null && shieldSheet != null) {
                animations.playSkillAnim(shieldSheet,
                    SkillAnimConfig.SHIELD_FRAMES,
                    SkillAnimConfig.SHIELD_DUR,
                    ally);
            }
        }
    }

    /**
     * Multi-target execution path (2-ally branch).
     * Applies burden with data.value (25%) to exactly the specified targets.
     */
    public void executeMulti(Entity user, List<Entity> targets) {
        StatsComponent uStats = sm.get(user);

        // 1. CHECK COMPONENT
        if (!skm.has(user)) {
            actionLog.addMessage("Error: No Component Detected");
            return;
        }

        // 2. GET SKILL DATA
        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.GROUP_BURDEN);

        // 3. MANA CHECK
        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);
        if (uStats.mana < finalCost) {
            actionLog.addMessage("Not enough mana!");
            return;
        }
        uStats.mana -= finalCost;

        // 4. GET BUFF MULTIPLIER
        double multiplier = CombatMechanics.BuffConsumption(user);

        // 5. USE data.value (25% for 2 targets) with multiplier
        float basePercent = data.value;
        float finalPercent = Math.min(1.0f, basePercent * (float) multiplier);

        String msg = String.format("%s casts Group Burden! Protecting %d allies at %.0f%% each.",
            uStats.name.trim(), targets.size(), finalPercent * 100);
        actionLog.addMessage(msg);

        // 6. APPLY BURDEN + ANIMATION to each specified target
        for (Entity ally : targets) {
            StatusEffect existing = StatusEffects.get(ally, StatusEffect.Type.BURDEN);
            if (existing != null) {
                if (finalPercent > existing.shareRatio) {
                    existing.shareRatio = finalPercent;
                    existing.protector  = user;
                    actionLog.addMessage("Burden upgraded for: " + sm.get(ally).name.trim());
                }
            } else {
                StatusEffects.add(ally, StatusEffect.burden(user, finalPercent));
                actionLog.addMessage("Sharing Burden with: " + sm.get(ally).name.trim());
            }

            // ── Shield.png: 16 frames × 125 px, 2000 px wide ────────────
            if (animations != null && shieldSheet != null) {
                animations.playSkillAnim(shieldSheet,
                    SkillAnimConfig.SHIELD_FRAMES,
                    SkillAnimConfig.SHIELD_DUR,
                    ally);
            }
        }
    }
}
