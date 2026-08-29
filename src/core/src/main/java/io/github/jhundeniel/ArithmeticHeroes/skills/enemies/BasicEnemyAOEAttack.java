package io.github.jhundeniel.ArithmeticHeroes.skills.enemies;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.PartyComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

import java.util.ArrayList;
import java.util.List;

public class BasicEnemyAOEAttack implements SkillStrategy {

    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ComponentMapper<PartyComponent>  pm  = ComponentMapper.getFor(PartyComponent.class);
    private final ComponentMapper<VisualComponent> vm  = ComponentMapper.getFor(VisualComponent.class);

    private final Engine           engine;
    private final ActionLogSystem  actionLog;
    private final BattleAnimations animations;
    private final Texture          genericAttackSheet;
    private final ArithmeticAssetManager assets;

    private static final float SPRITE_SCALE = 2.0f;

    public BasicEnemyAOEAttack(Engine engine, ActionLogSystem actionLog,
                               BattleAnimations animations, Texture genericAttackSheet,
                               ArithmeticAssetManager assets) {
        this.engine             = engine;
        this.actionLog          = actionLog;
        this.animations         = animations;
        this.genericAttackSheet = genericAttackSheet;
        this.assets             = assets;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);

        if (!skm.has(user)) {
            actionLog.addMessage("Error: No SkillsComponent on " +
                (uStats != null ? uStats.name.trim() : "enemy"));
            return;
        }

        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.ENEMY_AOE_ATTACK);

        if (data == null) {
            actionLog.addMessage("Error: no ENEMY_AOE_ATTACK skill data found!");
            return;
        }

        ImmutableArray<Entity> allEntities = engine.getEntitiesFor(
            Family.all(StatsComponent.class, PartyComponent.class).get());

        List<Entity> validTargets = new ArrayList<>();
        for (Entity e : allEntities) {
            PartyComponent party = pm.get(e);
            StatsComponent stats = sm.get(e);
            if (party != null && party.isPlayer && stats != null && stats.hp > 0)
                validTargets.add(e);
        }

        if (validTargets.isEmpty()) {
            actionLog.addMessage("No valid targets!");
            return;
        }

        double multiplier  = CombatMechanics.BuffConsumption(user);
        int    finalDamage = (int) (data.value * multiplier);

        actionLog.addMessage("All heroes took " + finalDamage + " damage!");

        // ── Play boss-specific attack animation overlay ───────────
        if (animations != null) {
            String name = (uStats != null) ? uStats.name.trim() : "";
            Texture bossSheet = getBossAttackSheet(name);

            if (bossSheet != null) {
                VisualComponent v = vm.get(user);
                if (v != null) {
                    v.frozen = true;
                    int frames = getBossAttackFrames(name);
                    float frameDur = getBossFrameDuration(name);
                    float drawW = v.width  * SPRITE_SCALE;
                    float drawH = v.height * SPRITE_SCALE;
                    float drawX = v.x + (v.width - drawW) / 2f;
                    float drawY = v.y;
                    animations.playBossAttackAnim(bossSheet, frames,
                        frameDur,
                        drawX, drawY, drawW, drawH, user);
                }
            }
        }

        for (Entity e : validTargets) {
            // ── Play generic hit effect on each hero (for mobs only) ───
            if (animations != null && genericAttackSheet != null) {
                String name = (uStats != null) ? uStats.name.trim() : "";
                if (getBossAttackSheet(name) == null) {
                    animations.playSkillAnim(genericAttackSheet, 20, 0.04f, e);
                }
            }

            CombatMechanics.applyDamage(user, e, finalDamage);
        }
    }

    private Texture getBossAttackSheet(String name) {
        if (assets == null) return null;
        switch (name) {
            case "Prof. Minus":   return assets.getTexture(ArithmeticAssetManager.ANIM_BOSS1_ATTACK);
            case "Lady Sigma":    return assets.getTexture(ArithmeticAssetManager.ANIM_BOSS2_ATTACK);
            case "Dr. Infinitum": return assets.getTexture(ArithmeticAssetManager.ANIM_BOSS3_ATTACK);
            default:              return null;
        }
    }

    private int getBossAttackFrames(String name) {
        switch (name) {
            case "Prof. Minus":   return ArithmeticAssetManager.BOSS1_ATTACK_FRAMES;
            case "Lady Sigma":    return ArithmeticAssetManager.BOSS2_ATTACK_FRAMES;
            case "Dr. Infinitum": return ArithmeticAssetManager.BOSS3_ATTACK_FRAMES;
            default:              return 20;
        }
    }

    private float getBossFrameDuration(String name) {
        switch (name) {
            case "Prof. Minus":   return ArithmeticAssetManager.BOSS_ATTACK_FRAME_DUR;
            case "Lady Sigma":    return ArithmeticAssetManager.BOSS2_ATTACK_FRAME_DUR;
            case "Dr. Infinitum": return ArithmeticAssetManager.BOSS3_ATTACK_FRAME_DUR;
            default:              return 0.04f;
        }
    }
}
