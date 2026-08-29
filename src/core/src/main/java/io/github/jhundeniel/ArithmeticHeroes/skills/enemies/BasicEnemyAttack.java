package io.github.jhundeniel.ArithmeticHeroes.skills.enemies;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class BasicEnemyAttack implements SkillStrategy {

    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ComponentMapper<VisualComponent> vm  = ComponentMapper.getFor(VisualComponent.class);

    private final ActionLogSystem  actionLog;
    private final BattleAnimations animations;
    private final Texture          genericAttackSheet;
    private final ArithmeticAssetManager assets;

    private static final float SPRITE_SCALE = 2.0f;

    public BasicEnemyAttack(ActionLogSystem actionLog,
                            BattleAnimations animations, Texture genericAttackSheet,
                            ArithmeticAssetManager assets) {
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
        SkillData data = skills.get(ActionRequestComponent.ActionType.ENEMY_ATTACK);

        if (data == null) {
            actionLog.addMessage("Error: no ENEMY_ATTACK skill data found!");
            return;
        }

        double multiplier  = CombatMechanics.BuffConsumption(user);
        int    finalDamage = (int) (data.value * multiplier);

        StatsComponent tStats     = sm.get(target);
        String         targetName = (tStats != null) ? tStats.name.trim() : "???";
        actionLog.addMessage(targetName + " took " + finalDamage + " damage!");

        // ── Play boss-specific or generic attack animation ───────────
        if (animations != null) {
            String name = (uStats != null) ? uStats.name.trim() : "";
            Texture bossSheet = getBossAttackSheet(name);

            if (bossSheet != null) {
                // Boss: freeze idle sprite & play full boss attack overlay
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
            } else if (genericAttackSheet != null) {
                // Regular mob: play generic hit effect on target
                animations.playSkillAnim(genericAttackSheet, 20, 0.04f, target);
            }
        }

        CombatMechanics.applyDamage(user, target, finalDamage);
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
