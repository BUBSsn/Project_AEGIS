package io.github.jhundeniel.ArithmeticHeroes.screens;

import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

/**
 * Full-screen Game Over overlay displayed when all heroes are defeated.
 *
 * KEY DESIGN NOTE:
 *   BattleRenderSystem uses ScreenViewport, so its camera world-coordinates
 *   equal actual screen pixels (e.g. 1512×757), NOT a fixed 1280×720.
 *   All layout is therefore derived from camera.viewportWidth / viewportHeight
 *   at render time, so it stays perfectly centred at any resolution.
 */
public class GameOverOverlay {

    public enum GameOverAction { NONE, RETRY, MAIN_MENU }

    // ── Proportional layout constants (fraction of screen) ───────────────
    private static final float PANEL_W_FRAC = 0.46f;  // panel width  = 46% of screen
    private static final float PANEL_H_FRAC = 0.62f;  // panel height = 62% of screen

    private static final float BTN_W_FRAC  = 0.165f;  // each button width
    private static final float BTN_H_FRAC  = 0.082f;  // button height
    private static final float BTN_GAP_FRAC= 0.018f;  // gap between the two buttons
    private static final float BTN_Y_FRAC  = 0.135f;  // button bottom edge from panel bottom

    private final ShapeRenderer shapes;
    private final BitmapFont    font;
    private final SpriteBatch   textBatch;
    private final Texture       white;

    private final int stagesCleared;

    // Recomputed each frame from the camera — always centred
    private float vw, vh;
    private float panelX, panelY, panelW, panelH;
    private final Rectangle retryBtn = new Rectangle();
    private final Rectangle menuBtn  = new Rectangle();
    private final Vector3   mouseVec = new Vector3(); 

    private float animTime  = 0f;
    private float fadeAlpha = 0f;
    private static final float FADE_SPEED = 1.8f;

    // Ember particles
    private static final int PC = 28;
    private final float[] px      = new float[PC];
    private final float[] py      = new float[PC];
    private final float[] pvx     = new float[PC];
    private final float[] pvy     = new float[PC];
    private final float[] plife   = new float[PC];
    private final float[] pmaxL   = new float[PC];
    private final float[] psize   = new float[PC];
    private boolean particlesSeeded = false;

    public GameOverOverlay(int stagesCleared, ArithmeticAssetManager assets) {
        this.stagesCleared = stagesCleared;
        shapes    = new ShapeRenderer();
        font      = new BitmapFont();
        textBatch = new SpriteBatch();
        white     = makePixel(Color.WHITE);
    }

    // ── Layout ────────────────────────────────────────────────────────────

    /** Recompute all layout rectangles from current camera viewport size. */
    private void updateLayout(OrthographicCamera camera) {
        vw = camera.viewportWidth;
        vh = camera.viewportHeight;

        panelW = vw * PANEL_W_FRAC;
        panelH = vh * PANEL_H_FRAC;
        panelX = vw / 2f - panelW / 2f;
        panelY = vh / 2f - panelH / 2f;

        float btnW   = vw * BTN_W_FRAC;
        float btnH   = vh * BTN_H_FRAC;
        float btnGap = vw * BTN_GAP_FRAC;
        float btnY   = panelY + panelH * BTN_Y_FRAC;
        float leftX  = vw / 2f - (btnW * 2 + btnGap) / 2f;

        retryBtn.set(leftX,              btnY, btnW, btnH);
        menuBtn .set(leftX + btnW + btnGap, btnY, btnW, btnH);

        // Seed particles once we know the panel bounds
        if (!particlesSeeded) {
            for (int i = 0; i < PC; i++) respawnParticle(i, true);
            particlesSeeded = true;
        }
    }

    // ── Particles ─────────────────────────────────────────────────────────

    private void respawnParticle(int i, boolean randomY) {
        px[i]    = MathUtils.random(panelX + 10f, panelX + panelW - 10f);
        py[i]    = randomY
            ? MathUtils.random(panelY, panelY + panelH)
            : panelY + panelH + MathUtils.random(10f, 40f);
        pvx[i]   = MathUtils.random(-18f, 18f);
        pvy[i]   = MathUtils.random(-55f, -25f);
        pmaxL[i] = MathUtils.random(2.0f, 4.5f);
        plife[i] = randomY ? MathUtils.random(0f, pmaxL[i]) : pmaxL[i];
        psize[i] = MathUtils.random(2.5f, 5.5f);
    }

    private void updateParticles(float dt) {
        for (int i = 0; i < PC; i++) {
            px[i]    += pvx[i] * dt;
            py[i]    += pvy[i] * dt;
            plife[i] -= dt;
            if (plife[i] <= 0 || py[i] < panelY - 10f) respawnParticle(i, false);
        }
    }

    // ── Render ────────────────────────────────────────────────────────────

    public void render(OrthographicCamera camera) {
        float dt  = Gdx.graphics.getDeltaTime();
        animTime  += dt;
        fadeAlpha  = Math.min(1f, fadeAlpha + dt * FADE_SPEED);

        updateLayout(camera);   // ← recalculate every frame from real camera size
        updateParticles(dt);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);

        // 1. Full-screen dark vignette
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f * fadeAlpha);
        shapes.rect(0, 0, vw, vh);
        shapes.end();

        // 2. Panel drop-shadow
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.55f * fadeAlpha);
        shapes.rect(panelX + 8f, panelY - 8f, panelW, panelH);
        shapes.end();

        // 3. Panel body (deep crimson)
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.10f, 0.03f, 0.03f, 0.97f * fadeAlpha);
        shapes.rect(panelX, panelY, panelW, panelH);
        shapes.end();

        // 4. Top title band (darker)
        float titleBandH = panelH * 0.27f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.05f, 0.01f, 0.01f, 0.60f * fadeAlpha);
        shapes.rect(panelX, panelY + panelH - titleBandH, panelW, titleBandH);
        shapes.end();

        // 5. Outer pulsing red border (double-line)
        float borderPulse = 0.60f + 0.30f * MathUtils.sin(animTime * 2.2f);
        drawBorderRect(panelX,      panelY,      panelW,      panelH,      3f,
            0.75f, 0.12f, 0.12f, borderPulse * fadeAlpha);
        drawBorderRect(panelX + 6f, panelY + 6f, panelW-12f, panelH-12f, 1.5f,
            0.45f, 0.07f, 0.07f, 0.45f * fadeAlpha);

        // 6. Divider line under title band
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.55f, 0.10f, 0.10f, 0.60f * fadeAlpha);
        shapes.rect(panelX + 20f, panelY + panelH - titleBandH - 1.5f, panelW - 40f, 1.5f);
        shapes.end();

        // 7. Ember particles (additive blend)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < PC; i++) {
            float a = MathUtils.clamp(plife[i] / pmaxL[i], 0f, 1f) * 0.65f * fadeAlpha;
            shapes.setColor(0.90f, 0.20f, 0.10f, a);
            float s = psize[i];
            shapes.rect(px[i] - s/2f,      py[i] - s*0.15f, s,       s*0.30f);
            shapes.rect(px[i] - s*0.15f,   py[i] - s/2f,    s*0.30f, s);
        }
        shapes.end();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // 8. RETRY button
        drawButton(retryBtn,
            0.10f,0.38f,0.12f, 0.16f,0.60f,0.18f,
            0.35f,0.85f,0.40f, true, fadeAlpha);

        // 9. MAIN MENU button
        drawButton(menuBtn,
            0.10f,0.12f,0.35f, 0.14f,0.18f,0.55f,
            0.38f,0.45f,0.85f, false, fadeAlpha);

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 10. Text — scale font proportionally to panel size
        textBatch.setProjectionMatrix(camera.combined);
        textBatch.begin();

        // ── "GAME OVER" title ─────────────────────────────────────────────
        float flicker   = 0.82f + 0.18f * MathUtils.sin(animTime * 8f);
        float titleScale = panelH * 0.0068f;   // scales with panel height
        font.getData().setScale(titleScale);
        GlyphLayout titleLay = new GlyphLayout(font, "GAME OVER");
        float titleX = vw / 2f - titleLay.width / 2f;
        float titleY = panelY + panelH - titleBandH / 2f + titleLay.height / 2f;
        // shadow
        font.setColor(0.25f*flicker, 0f, 0f, 0.80f*fadeAlpha);
        font.draw(textBatch, "GAME OVER", titleX + 3f, titleY - 3f);
        // main
        font.setColor(0.95f*flicker, 0.12f, 0.08f, fadeAlpha);
        font.draw(textBatch, "GAME OVER", titleX, titleY);

        // ── Subtitle ──────────────────────────────────────────────────────
        float bodyScale = panelH * 0.0030f;
        font.getData().setScale(bodyScale);
        font.setColor(0.82f, 0.58f, 0.58f, 0.90f*fadeAlpha);
        String sub = stagesCleared == 0
            ? "Your heroes fell before clearing any stage."
            : "Your heroes fell after clearing "
            + stagesCleared + (stagesCleared == 1 ? " stage." : " stages.");
        GlyphLayout subLay = new GlyphLayout(font, sub);
        float contentTop = panelY + panelH - titleBandH - 14f;
        font.draw(textBatch, sub, vw/2f - subLay.width/2f, contentTop);

        // ── Flavour quote ─────────────────────────────────────────────────
        float quoteScale = panelH * 0.0026f;
        font.getData().setScale(quoteScale);
        font.setColor(0.58f, 0.40f, 0.40f, 0.72f*fadeAlpha);
        String flavour = "\"The math was too powerful this time...\"";
        GlyphLayout flLay = new GlyphLayout(font, flavour);
        font.draw(textBatch, flavour, vw/2f - flLay.width/2f, contentTop - subLay.height - 14f);

        // ── Ornament ──────────────────────────────────────────────────────
        font.getData().setScale(bodyScale * 0.85f);
        font.setColor(0.50f, 0.15f, 0.15f, 0.55f*fadeAlpha);
        String orn = "- * -";
        GlyphLayout ornLay = new GlyphLayout(font, orn);
        float ornY = contentTop - subLay.height - flLay.height - 28f;
        font.draw(textBatch, orn, vw/2f - ornLay.width/2f, ornY);

        // ── "What will you do?" prompt ────────────────────────────────────
        font.getData().setScale(bodyScale);
        float promptPulse = 0.65f + 0.25f * MathUtils.sin(animTime * 2.5f);
        font.setColor(0.78f, 0.65f, 0.55f, promptPulse*fadeAlpha);
        String prompt = "What will you do?";
        GlyphLayout prLay = new GlyphLayout(font, prompt);
        font.draw(textBatch, prompt,
            vw/2f - prLay.width/2f,
            retryBtn.y + retryBtn.height + panelH * 0.09f);

        // ── Button labels ─────────────────────────────────────────────────
        float btnLabelScale = panelH * 0.0032f;
        font.getData().setScale(btnLabelScale);
        font.setColor(0.88f, 1.00f, 0.86f, fadeAlpha);
        GlyphLayout retLay = new GlyphLayout(font, "RETRY");
        font.draw(textBatch, "RETRY",
            retryBtn.x + retryBtn.width/2f - retLay.width/2f,
            retryBtn.y + retryBtn.height/2f + retLay.height/2f);

        font.getData().setScale(btnLabelScale * 0.88f);
        font.setColor(0.82f, 0.86f, 1.00f, fadeAlpha);
        GlyphLayout menuLay = new GlyphLayout(font, "MAIN MENU");
        font.draw(textBatch, "MAIN MENU",
            menuBtn.x + menuBtn.width/2f - menuLay.width/2f,
            menuBtn.y + menuBtn.height/2f + menuLay.height/2f);

        // ── Keyboard hint ─────────────────────────────────────────────────
        font.getData().setScale(panelH * 0.0020f);
        font.setColor(0.45f, 0.38f, 0.38f, 0.65f*fadeAlpha);
        String hint = "[R] Retry    [ESC] Main Menu";
        GlyphLayout hintLay = new GlyphLayout(font, hint);
        font.draw(textBatch, hint,
            vw/2f - hintLay.width/2f,
            panelY + panelH * 0.07f);

        textBatch.end();
    }

    // ── Draw helpers ──────────────────────────────────────────────────────

    private void drawBorderRect(float x, float y, float w, float h,
                                float t, float r, float g, float b, float a) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(r, g, b, a);
        shapes.rect(x,         y + h - t, w,  t);
        shapes.rect(x,         y,         w,  t);
        shapes.rect(x,         y,         t,  h);
        shapes.rect(x + w - t, y,         t,  h);
        shapes.end();
    }

    private void drawButton(Rectangle r,
                            float br, float bg, float bb,
                            float pr, float pg, float pb,
                            float borderR, float borderG, float borderB,
                            boolean pulse, float alpha) {
        // Shadow
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.45f * alpha);
        shapes.rect(r.x + 4f, r.y - 4f, r.width, r.height);
        shapes.end();

        // Fill
        float p = pulse ? 0.45f + 0.12f * MathUtils.sin(animTime * 3.2f) : 0f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(br + p*(pr-br), bg + p*(pg-bg), bb + p*(pb-bb), 0.93f*alpha);
        shapes.rect(r.x, r.y, r.width, r.height);
        shapes.end();

        // Top bevel highlight
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 1f, 1f, 0.10f * alpha);
        shapes.rect(r.x + 1f, r.y + r.height - 4f, r.width - 2f, 4f);
        shapes.end();

        // Border
        drawBorderRect(r.x, r.y, r.width, r.height, 2f,
            borderR, borderG, borderB, 0.80f * alpha);
    }

    private static Texture makePixel(Color c) {
        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(c); px.fill();
        Texture t = new Texture(px); px.dispose(); return t;
    }

    // ── Input ─────────────────────────────────────────────────────────────

    public GameOverAction handleInput(OrthographicCamera camera, ArithmeticAssetManager assets) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R))      { if (assets != null) assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK); return GameOverAction.RETRY; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || Gdx.input.isKeyJustPressed(Input.Keys.M))   { if (assets != null) assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK); return GameOverAction.MAIN_MENU; }

        mouseVec.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouseVec);
        float mx = mouseVec.x;
        float my = mouseVec.y;

        if (Gdx.input.justTouched()) {
            if (retryBtn.contains(mx, my)) { if (assets != null) assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK); return GameOverAction.RETRY; }
            if (menuBtn .contains(mx, my)) { if (assets != null) assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK); return GameOverAction.MAIN_MENU; }
        }
        return GameOverAction.NONE;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    public void dispose() {
        shapes.dispose();
        font.dispose();
        textBatch.dispose();
        white.dispose();
    }
}
