package io.github.jhundeniel.ArithmeticHeroes.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent.ActionType;
import io.github.jhundeniel.ArithmeticHeroes.components.IntentComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.Operator;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent;

public class EnemyIntentSystem {
    private final SpriteBatch batch;
    private final Engine engine;
    private final Texture boxTexture;
    private final Texture whiteTexture;
    private BitmapFont font;

    private static final Color NAME_K = new Color(1f,    1f,    1f,    1f);   // white
    private static final Color MOVE_K = new Color(0.95f, 0.20f, 0.20f, 1f);  // red
    private static final Color TGT_K  = new Color(1f,    0.92f, 0.15f, 1f);  // yellow

    // Must match BattleRenderSystem.SPRITE_SCALE
    private static final float SPRITE_SCALE = 2.0f;

    private final ComponentMapper<IntentComponent> im = ComponentMapper.getFor(IntentComponent.class);
    private final ComponentMapper<VisualComponent> vm = ComponentMapper.getFor(VisualComponent.class);
    private final ComponentMapper<StatsComponent>  sm = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<TypeComponent>   tm = ComponentMapper.getFor(TypeComponent.class);
    private final com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();

    public EnemyIntentSystem(SpriteBatch batch, Engine engine) {
        this.batch  = batch;
        this.engine = engine;

        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(0.05f, 0.05f, 0.07f, 0.85f);
        px.fill();
        this.boxTexture = new Texture(px);
        px.dispose();

        Pixmap wp = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        wp.setColor(Color.WHITE);
        wp.fill();
        this.whiteTexture = new Texture(wp);
        wp.dispose();

        try {
            this.font = new BitmapFont(
                Gdx.files.internal("ui/font export.fnt"),
                Gdx.files.internal("ui/font export.png"), false);
            this.font.getData().setScale(1.8f);
        } catch (Exception e) {
            this.font = new BitmapFont();
            this.font.getData().setScale(1.5f);
        }
    }

    public void render(int sw, int sh) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(
            Family.all(IntentComponent.class, VisualComponent.class,
                StatsComponent.class, TypeComponent.class).get());

        batch.begin();

        for (Entity entity : entities) {
            TypeComponent  type  = tm.get(entity);
            StatsComponent stats = sm.get(entity);

            if (type == null || type.type != Operator.MOB || stats.hp <= 0) continue;

            IntentComponent intent = im.get(entity);
            VisualComponent visual = vm.get(entity);

            // Build all three label strings first
            String nameStr   = stats.name.trim().toUpperCase();
            String moveStr   = "MOVE: " + actionLabel(intent.actionType);
            String targetVal;
            if (intent.actionType == ActionType.ENEMY_AOE_ATTACK
                || intent.actionType == ActionType.SLAM) {
                targetVal = "ALL";
            } else if (intent.target != null && sm.has(intent.target)) {
                targetVal = sm.get(intent.target).name.trim().toUpperCase();
            } else {
                targetVal = "---";
            }
            String targetStr = "TARGET: " + targetVal;

            // Measure the widest line to size the box dynamically
            layout.setText(font, nameStr);   float nameW   = layout.width;
            layout.setText(font, moveStr);   float moveW   = layout.width;
            layout.setText(font, targetStr); float targetW = layout.width;
            float lineH = layout.height;
            if (lineH < 12f) lineH = 15f;

            // Boss Twist String Evaluation
            boolean isBoss = type.registryKey != null && type.registryKey.contains("BOSS");
            String bossTwistStr = null;
            float twistW = 0f;
            if (isBoss) {
                com.badlogic.ashley.core.ComponentMapper<io.github.jhundeniel.ArithmeticHeroes.components.PassiveComponent> pm =
                    com.badlogic.ashley.core.ComponentMapper.getFor(io.github.jhundeniel.ArithmeticHeroes.components.PassiveComponent.class);
                io.github.jhundeniel.ArithmeticHeroes.components.PassiveComponent passiveComp = pm.get(entity);
                if (passiveComp != null && passiveComp.passive != null) {
                    if (passiveComp.passive instanceof io.github.jhundeniel.ArithmeticHeroes.passives.bosses.ParityShieldPassive) bossTwistStr = "PARITY SHIELD";
                    else if (passiveComp.passive instanceof io.github.jhundeniel.ArithmeticHeroes.passives.bosses.InverseRealityPassive) bossTwistStr = "INVERSE REALITY";
                    else if (passiveComp.passive instanceof io.github.jhundeniel.ArithmeticHeroes.passives.bosses.FinalEquationPassive) bossTwistStr = "FINAL EQUATION";
                }
                if (bossTwistStr != null) {
                    bossTwistStr = "BOSS TWIST: " + bossTwistStr;
                    layout.setText(font, bossTwistStr);
                    twistW = layout.width;
                }
            }

            float padding = 20f;
            float rowGap  = lineH + 10f;
            float boxW    = Math.max(nameW, Math.max(moveW, targetW)) + padding * 2f;
            float boxH    = rowGap * 3f + padding;

            // Position box above the enemy sprite
            float scaledW = visual.width  * SPRITE_SCALE;
            float scaledH = visual.height * SPRITE_SCALE;
            float drawX   = visual.x + (visual.width - scaledW) / 2f;
            float boxX    = drawX + scaledW / 2f - boxW / 2f;
            float boxY    = visual.y + scaledH + 15f;

            // Clamp so the box doesn't go above the screen (factoring in the Twist box if active)
            float twistBoxH = 0f;
            float twistBoxW = 0f;
            if (bossTwistStr != null) {
                twistBoxH = rowGap + padding;
                twistBoxW = twistW + padding * 2f;
                float combinedH = boxH + twistBoxH + 5f;
                float maxY = sh - combinedH - 10f;
                if (boxY > maxY) boxY = maxY;
            } else {
                float maxY = sh - boxH - 10f;
                if (boxY > maxY) boxY = maxY;
            }

            // Background
            batch.setColor(Color.WHITE);
            batch.draw(boxTexture, boxX, boxY, boxW, boxH);

            // Draw Boss Twist Box on top
            if (bossTwistStr != null) {
                float twistBoxX = boxX + boxW/2f - twistBoxW/2f;
                float twistBoxY = boxY + boxH + 5f;

                // Purple background
                batch.setColor(0.20f, 0.05f, 0.28f, 0.95f);
                batch.draw(whiteTexture, twistBoxX, twistBoxY, twistBoxW, twistBoxH);

                // Gold border
                batch.setColor(TGT_K); // yellow/gold
                float borderW = 3f;
                batch.draw(whiteTexture, twistBoxX, twistBoxY + twistBoxH - borderW, twistBoxW, borderW);
                batch.draw(whiteTexture, twistBoxX, twistBoxY, twistBoxW, borderW);
                batch.draw(whiteTexture, twistBoxX, twistBoxY, borderW, twistBoxH);
                batch.draw(whiteTexture, twistBoxX + twistBoxW - borderW, twistBoxY, borderW, twistBoxH);
                batch.setColor(Color.WHITE);

                font.setColor(TGT_K);
                // Center text vertically in twist box
                font.draw(batch, bossTwistStr, twistBoxX + padding, twistBoxY + twistBoxH - padding/2f - (twistBoxH - lineH)/2f + lineH);
            }

            // Row 1 — Name (white)
            font.setColor(NAME_K);
            font.draw(batch, nameStr, boxX + padding, boxY + boxH - padding);

            // Row 2 — Move (red)
            font.setColor(MOVE_K);
            font.draw(batch, moveStr, boxX + padding, boxY + boxH - padding - rowGap);

            // Row 3 — Target (yellow)
            font.setColor(TGT_K);
            font.draw(batch, targetStr, boxX + padding, boxY + boxH - padding - rowGap * 2f);
        }

        batch.setColor(Color.WHITE);
        font.setColor(Color.WHITE);
        batch.end();
    }
    public void resize(int w, int h) {}

    public void dispose() {
        if (font       != null) font.dispose();
        if (boxTexture != null) boxTexture.dispose();
        if (whiteTexture != null) whiteTexture.dispose();
    }

    private String actionLabel(ActionType a) {
        if (a == null) return "NONE";
        switch (a) {
            case ENEMY_ATTACK:       return "ATTACK";
            case ENEMY_AOE_ATTACK:   return "AOE ATTACK";
            case ENEMY_SUPPORT_HEAL: return "HEAL";
            case ENEMY_SUPPORT_BUFF: return "BUFF";
            case POKE:               return "POKE";
            case SLAM:               return "SLAM";
            default:                 return a.name();
        }
    }
}
