package io.github.jhundeniel.ArithmeticHeroes.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.WaveAnnouncer;
import io.github.jhundeniel.ArithmeticHeroes.components.Operator;
import io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.managers.TurnManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;
import io.github.jhundeniel.ArithmeticHeroes.components.PassiveComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.passives.MultiplicationPassive;
import io.github.jhundeniel.ArithmeticHeroes.passives.SubtractionPassive;

public class BattleRenderSystem extends IteratingSystem {

    private final SpriteBatch      batch;
    private final BitmapFont             font;
    private final BitmapFont             barFont;
    private final StageSystem      stageSystem;
    private TurnManager            turnManager;
    private final OrthographicCamera     camera;
    private final Viewport               viewport;
    private BattleAnimations       animations;
    private WaveAnnouncer          waveAnnouncer;
    private TargetingSystem        targetingSystem;
    private final ArithmeticAssetManager assets;

    private static final float SPRITE_SCALE = 2.0f;

    // Arrow
    private float arrowTimer             = 0f;
    private static final float BOB_SPEED = 3.5f;
    private static final float BOB_RANGE = 5f;
    private static final float ARROW_W   = 16f;
    private static final float ARROW_H   = 20f;

    // Bar dimensions
    private static final float BAR_W    = 120f;
    private static final float BAR_H    = 16f;
    private static final float BORDER   = 2f;
    private static final float BAR_GAP  = 5f;
    private static final float LABEL_W  = 22f;

    // Hero bars: how far BELOW sprite bottom
    private static final float HERO_BAR_OFFSET_Y = 22f;

    // Colours
    private static final Color NAME_COL   = new Color(1f,    1f,    1f,    1f);
    private static final Color NAME_ACT   = new Color(1f,    0.95f, 0.22f, 1f);
    private static final Color HP_FILL    = new Color(0.85f, 0.08f, 0.08f, 1f);
    private static final Color MP_FILL    = new Color(0.15f, 0.40f, 0.95f, 1f);
    private static final Color BAR_BG     = new Color(0.08f, 0.08f, 0.10f, 1f);
    private static final Color BORDER_COL = new Color(0.55f, 0.55f, 0.60f, 1f);
    private static final Color TEXT_COL   = new Color(1f,    1f,    1f,    1f);
    private static final Color HP_LBL_COL = new Color(1f,    0.35f, 0.35f, 1f);
    private static final Color MP_LBL_COL = new Color(0.45f, 0.70f, 1f,    1f);
    private static final Color ARROW_COL  = new Color(1f,    0.92f, 0.15f, 1f);

    private final Texture whiteTex;
    private final Texture arrowTex;
    private Texture handCursorTex;

    private final ComponentMapper<VisualComponent> vm = ComponentMapper.getFor(VisualComponent.class);
    private final ComponentMapper<StatsComponent>  sm = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<TypeComponent>   tm = ComponentMapper.getFor(TypeComponent.class);

    private final ComponentMapper<PassiveComponent> pm = ComponentMapper.getFor(PassiveComponent.class);

    public BattleRenderSystem(SpriteBatch batch, StageSystem stageSystem, ArithmeticAssetManager assets) {
        super(Family.all(VisualComponent.class, StatsComponent.class).get());
        this.batch       = batch;
        this.stageSystem = stageSystem;
        this.assets      = assets;

        camera   = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        font = new BitmapFont();
        font.getData().setScale(1.1f);

        barFont = new BitmapFont();
        barFont.getData().setScale(0.85f);

        barFont.getData().setScale(0.85f);

        whiteTex = pixel(Color.WHITE);
        arrowTex = buildArrow();

        try {
            handCursorTex = new Texture(Gdx.files.internal("sprites/hand_cursor.png"));
        } catch (Exception e) {}
    }

    // ── Setters ───────────────────────────────────────────────────
    public void setAnimations(BattleAnimations a)   { this.animations    = a; }
    public void setTurnManager(TurnManager t)        { this.turnManager   = t; }
    public void setWaveAnnouncer(WaveAnnouncer wa)   { this.waveAnnouncer = wa; }
    public void setTargetingSystem(TargetingSystem ts) { this.targetingSystem = ts; }
    public void resize(int w, int h)                 { viewport.update(w, h, true); }
    public OrthographicCamera getCamera()            { return camera; }

    // ── Helpers ───────────────────────────────────────────────────
    private Texture pixel(Color c) {
        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(c); px.fill();
        Texture t = new Texture(px); px.dispose(); return t;
    }

    private Texture buildArrow() {
        int w = (int) ARROW_W, h = (int) ARROW_H;
        Pixmap px = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        px.setColor(0, 0, 0, 0); px.fill();
        px.setColor(ARROW_COL);
        for (int r = 0; r < h; r++) {
            float p    = (float) r / (h - 1);
            int   half = (int) ((1 - p) * (w / 2f));
            for (int c2 = w/2 - half; c2 <= w/2 + half; c2++) px.drawPixel(c2, r);
        }
        Texture t = new Texture(px); px.dispose(); return t;
    }

    // ── Main render loop ──────────────────────────────────────────
    @Override
    public void update(float dt) {
        arrowTimer += dt;
        if (waveAnnouncer != null) waveAnnouncer.update(dt);

        // ── Unfreeze boss idle once its attack overlay finishes ───────────
        if (animations != null
            && animations.getAttackingBoss() != null
            && animations.isBossAttackDone()) {
            Entity boss = animations.getAttackingBoss();
            if (vm.has(boss)) vm.get(boss).frozen = false;
            animations.clearBossAttack();
        }

        ScreenUtils.clear(0, 0, 0, 1);
        viewport.apply();

        float cx = camera.position.x, cy = camera.position.y;
        if (animations != null) {
            Vector2 s = animations.getShakeOffset();
            if (s.x != 0 || s.y != 0) {
                camera.position.x += s.x;
                camera.position.y += s.y;
                camera.update();
            }
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Background
        Texture bg = stageSystem.getCurrentBackground();
        if (bg != null) {
            batch.setColor(Color.WHITE);
            batch.draw(bg, 0, 0, camera.viewportWidth, camera.viewportHeight);
        }

        // Entities (sprites + HUD)
        super.update(dt);

        // Overlay layers
        if (animations != null) {
            animations.renderGravestones(batch);
            animations.renderFlashes(batch);
            animations.renderSkillAnims(batch);
            animations.renderText(batch, font);
        }

        batch.end();

        // Restore camera after shake
        if (camera.position.x != cx || camera.position.y != cy) {
            camera.position.x = cx; camera.position.y = cy; camera.update();
        }
        if (animations != null) animations.renderParticles(camera);
    }

    @Override
    protected void processEntity(Entity entity, float dt) {
        VisualComponent v     = vm.get(entity);
        StatsComponent  stats = sm.get(entity);

        float drawW = v.width  * SPRITE_SCALE;
        float drawH = v.height * SPRITE_SCALE;
        float drawX = v.x + (v.width  - drawW) / 2f;
        float drawY = v.y;

        TypeComponent typeComponent = tm.get(entity);
        boolean isEnemy = (typeComponent != null && typeComponent.type == Operator.MOB);
        if (!isEnemy && stats.hp <= 0) return;

        // Suppress frozen boss sprite while its attack overlay is rendering
        boolean suppressSprite = v.frozen
            && animations != null
            && animations.getAttackingBoss() == entity;

        batch.setColor(Color.WHITE);
        if (!suppressSprite) {
            batch.draw(v.getCurrentFrame(), drawX, drawY, drawW, drawH);
        }

        float   cx     = drawX + drawW / 2f;
        boolean active = turnManager != null && turnManager.getCurrentEntityTurn() == entity;

        // Bobbing turn arrow
        if (active) {
            float bob = MathUtils.sin(arrowTimer * BOB_SPEED) * BOB_RANGE;
            batch.setColor(ARROW_COL);
            batch.draw(arrowTex, cx - ARROW_W/2f, drawY + drawH + 56f + bob, ARROW_W, ARROW_H);
            batch.setColor(Color.WHITE);
        }

        // Hand Cursor for targeting
        if (turnManager != null && turnManager.getState() == io.github.jhundeniel.ArithmeticHeroes.battle.BattleState.SELECT_TARGET) {
            boolean isHovered = (targetingSystem != null && targetingSystem.getCurrentTarget() == entity);
            if (isHovered && handCursorTex != null) {
                float bob = MathUtils.sin(arrowTimer * BOB_SPEED * 1.5f) * (BOB_RANGE * 1.2f);
                float cursorW = 48f;
                float cursorH = 48f;
                // Draw hand cursor pointing to the entity
                batch.draw(handCursorTex, cx - cursorW/2f, drawY + drawH + 75f + bob, cursorW, cursorH);
            }
        }

        // Name label
        String name   = stats.name.trim();
        float  nScale = 1.6f;
        font.getData().setScale(nScale);
        font.setColor(active ? NAME_ACT : NAME_COL);
        float nameW = name.length() * 9f * nScale;
        float nameX = cx - nameW / 2f;
        float nameY = isEnemy ? drawY + drawH + 8f : drawY + drawH + 50f;

        float padX = 8f, padY = 5f;
        batch.setColor(0.04f, 0.04f, 0.06f, 0.85f);
        batch.draw(whiteTex,
            nameX - padX,
            nameY - font.getCapHeight() - padY,
            nameW + padX * 2f,
            font.getCapHeight() + padY * 2f);
        batch.setColor(Color.WHITE);
        font.draw(batch, name, nameX, nameY);
        font.getData().setScale(1.1f);

        // ── Bars ──────────────────────────────────────────────────────
        // Enemy bars sit just below sprite feet (small offset), heroes use larger offset
        float barOffset = isEnemy ? 10f : HERO_BAR_OFFSET_Y;
        float hpBarY    = drawY - barOffset;
        float mpBarY    = hpBarY - BAR_H - BAR_GAP;
        drawStatBar(cx, hpBarY, stats.hp,   stats.maxHp,   HP_FILL, HP_LBL_COL, "HP");

        // Only draw the MP Bar if the entity is NOT a mob
        if (!isEnemy) {
            drawStatBar(cx, mpBarY, stats.mana, stats.maxMana, MP_FILL, MP_LBL_COL, "MP");
        }

        // ── Buff & Passive Icons (below the name label) ────────────────
        float iconSize = 24f;
        float iconGap  = 4f;

        java.util.List<Texture> activeIcons = new java.util.ArrayList<>();

        // Passives conditional logic
        PassiveComponent passiveOpt = pm.get(entity);
        if (passiveOpt != null && passiveOpt.passive != null) {
            if (passiveOpt.passive instanceof MultiplicationPassive) {
                // Only show Multiplication Passive icon if it's currently buffed (from Pass)
                if (StatusEffects.has(entity, StatusEffect.Type.BUFF)) {
                    activeIcons.add(assets.getTexture(ArithmeticAssetManager.ICON_MULT_PASSIVE));
                }
            } else if (passiveOpt.passive instanceof SubtractionPassive) {
                // Subtraction Passive (Berserk) only shows when HP < 50%
                if (stats.hp < stats.maxHp * io.github.jhundeniel.ArithmeticHeroes.config.GameConfig.BERSERKER_HP_THRESHOLD) {
                    activeIcons.add(assets.getTexture(ArithmeticAssetManager.ICON_SUB_PASSIVE));
                }
            }
        }

        // General Buffs
        if (StatusEffects.has(entity, StatusEffect.Type.ADDITIVE_BONUS)) {
            activeIcons.add(assets.getTexture(ArithmeticAssetManager.ICON_ADDITIONAL_BUFF));
        }
        // Exclude general BuffComponent icon for Multiplication hero since we use the passive icon
        boolean isMult = passiveOpt != null && passiveOpt.passive instanceof MultiplicationPassive;
        boolean hasAmplify = StatusEffects.has(entity, StatusEffect.Type.BUFF) && !isMult;

        if (hasAmplify) {
            if (!isEnemy) {
                activeIcons.add(assets.getTexture(ArithmeticAssetManager.ICON_AMPLIFY));
            }
        }

        if (StatusEffects.has(entity, StatusEffect.Type.BURDEN)) {
            activeIcons.add(assets.getTexture(ArithmeticAssetManager.ICON_BURDEN));
        }
        if (StatusEffects.has(entity, StatusEffect.Type.INVERSION)) {
            activeIcons.add(assets.getTexture(ArithmeticAssetManager.ICON_INVERSION));
        }
        if (StatusEffects.has(entity, StatusEffect.Type.COST_REDUCTION)) {
            activeIcons.add(assets.getTexture(ArithmeticAssetManager.ICON_COST_RED));
        }
        if (StatusEffects.has(entity, StatusEffect.Type.SQUARED)) {
            activeIcons.add(assets.getTexture(ArithmeticAssetManager.ICON_SQUARED));
        }
        if (StatusEffects.has(entity, StatusEffect.Type.REFLECTION)) {
            activeIcons.add(assets.getTexture(ArithmeticAssetManager.ICON_REFLECT));
        }

        if (!activeIcons.isEmpty()) {
            float totalIconW = activeIcons.size() * iconSize + (activeIcons.size() - 1) * iconGap;
            float currentIconX = cx - totalIconW / 2f;
            // Place icons directly BELOW the name label
            // nameY is the top of the name text, so icons go below the name badge
            float nameBadgeBottom = nameY - font.getCapHeight() - padY;
            float iconY = nameBadgeBottom - iconSize - 2f;

            batch.setColor(Color.WHITE);
            for (Texture tex : activeIcons) {
                if (tex != null) {
                    batch.draw(tex, currentIconX, iconY, iconSize, iconSize);
                }
                currentIconX += iconSize + iconGap;
            }
        }

        // Draw amplify icon separately for enemies so it appears below and on the side
        if (isEnemy && hasAmplify) {
            Texture amplifyTex = assets.getTexture(ArithmeticAssetManager.ICON_AMPLIFY);
            if (amplifyTex != null) {
                float ampX = cx + (LABEL_W + BAR_W) / 2f + 5f; // On the right side of the HP bar
                float ampY = drawY - 14f; // Below the mob/boss, roughly level with HP bar
                batch.setColor(Color.WHITE);
                batch.draw(amplifyTex, ampX, ampY, iconSize, iconSize);
            }
        }
    }

    private void drawStatBar(float centreX, float topY,
                             int cur, int max,
                             Color fillColor, Color labelColor, String label) {
        float totalW = LABEL_W + BAR_W;
        float startX = centreX - totalW / 2f;

        // Label
        barFont.getData().setScale(0.9f);
        barFont.setColor(labelColor);
        GlyphLayout lgl = new GlyphLayout(barFont, label);
        barFont.draw(batch, label, startX, topY + BAR_H / 2f + lgl.height / 2f);

        float bx = startX + LABEL_W;

        // Border
        batch.setColor(BORDER_COL);
        batch.draw(whiteTex, bx,                  topY + BAR_H - BORDER, BAR_W,  BORDER); // top
        batch.draw(whiteTex, bx,                  topY,                  BAR_W,  BORDER); // bottom
        batch.draw(whiteTex, bx,                  topY,                  BORDER, BAR_H);  // left
        batch.draw(whiteTex, bx + BAR_W - BORDER, topY,                  BORDER, BAR_H);  // right

        // Dark background
        float innerX = bx + BORDER;
        float innerY = topY + BORDER;
        float innerW = BAR_W - BORDER * 2f;
        float innerH = BAR_H - BORDER * 2f;
        batch.setColor(BAR_BG);
        batch.draw(whiteTex, innerX, innerY, innerW, innerH);

        // Fill
        float ratio = max > 0 ? MathUtils.clamp((float) cur / max, 0f, 1f) : 0f;
        float fillW = innerW * ratio;
        if (fillW > 0f) {
            batch.setColor(fillColor);
            batch.draw(whiteTex, innerX, innerY, fillW, innerH);
            // Highlight strip
            batch.setColor(
                Math.min(1f, fillColor.r + 0.25f),
                Math.min(1f, fillColor.g + 0.15f),
                Math.min(1f, fillColor.b + 0.15f), 0.55f);
            batch.draw(whiteTex, innerX, innerY + innerH - 2f, fillW, 2f);
        }

        // "cur/max" text
        barFont.getData().setScale(0.82f);
        barFont.setColor(TEXT_COL);
        String txt = cur + "/" + max;
        GlyphLayout tgl = new GlyphLayout(barFont, txt);
        barFont.draw(batch, txt,
            innerX + innerW / 2f - tgl.width  / 2f,
            innerY + innerH / 2f + tgl.height / 2f);
    }

    public void dispose() {
        font.dispose();
        barFont.dispose();
        whiteTex.dispose();
        arrowTex.dispose();
        if (handCursorTex != null) handCursorTex.dispose();
    }
}
