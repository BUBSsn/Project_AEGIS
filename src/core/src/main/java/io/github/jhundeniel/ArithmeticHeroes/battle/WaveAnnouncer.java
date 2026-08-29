package io.github.jhundeniel.ArithmeticHeroes.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/**
 * Compact full-width wave/stage announcement banner.
 * Deliberately narrow (≈12 % of screen height) so it doesn't cover sprites.
 * Sits vertically centred in the middle third of the screen.
 */
public class WaveAnnouncer {

    // ── Timing ────────────────────────────────────────────────────
    private static final float FADE_IN  = 0.30f;
    private static final float HOLD     = 1.20f;
    private static final float FADE_OUT = 0.35f;
    private static final float TOTAL    = FADE_IN + HOLD + FADE_OUT;

    // ── Banner geometry ───────────────────────────────────────────
    // Narrow strip: 12 % of screen height
    private static final float BANNER_H_RATIO = 0.12f;
    // Accent stripe height
    private static final float STRIPE_H_RATIO = 0.012f;

    // ── Colours ───────────────────────────────────────────────────
    private static final Color BG_COLOR    = new Color(0.04f, 0.04f, 0.08f, 1f);
    private static final Color STRIPE_COL  = new Color(1f,    0.85f, 0.10f, 1f);
    private static final Color LINE1_COLOR = new Color(1f,    0.92f, 0.20f, 1f); // gold
    private static final Color LINE2_COLOR = new Color(1f,    1f,    1f,    1f); // white

    // ── State ─────────────────────────────────────────────────────
    private float  timer = TOTAL;
    private String line1 = "";
    private String line2 = "";

    private final SpriteBatch batch;
    private final BitmapFont  fontBig;
    private final BitmapFont  fontSmall;
    private final Texture     whiteTex;

    public WaveAnnouncer(SpriteBatch batch) {
        this.batch = batch;

        // Smaller scales → compact banner that won't obscure sprites
        fontBig = new BitmapFont();
        fontBig.getData().setScale(2.4f);

        fontSmall = new BitmapFont();
        fontSmall.getData().setScale(1.6f);

        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE); px.fill();
        whiteTex = new Texture(px); px.dispose();
    }

    /** Show a two-line banner.  Examples:
     *    show("Stage 1", "Wave 1")
     *    show("Stage 1", "Complete!")
     *    show("Stage 3", "⚔ Final Boss!")
     */
    public void show(String topLine, String bottomLine) {
        this.line1 = topLine;
        this.line2 = bottomLine;
        this.timer = 0f;
    }

    public boolean isActive() { return timer < TOTAL; }

    public void update(float dt) {
        if (timer < TOTAL) timer = Math.min(timer + dt, TOTAL);
    }

    /** Call inside an active SpriteBatch.begin/end block. */
    public void render(OrthographicCamera camera) {
        if (!isActive()) return;

        float alpha   = computeAlpha();
        float vw      = camera.viewportWidth;
        float vh      = camera.viewportHeight;
        float bannerH = vh * BANNER_H_RATIO;
        float stripeH = vh * STRIPE_H_RATIO;

        // Centre the banner vertically (middle of screen)
        float bannerY = (vh - bannerH) / 2f;

        // ── Dark background bar ───────────────────────────────────
        batch.setColor(BG_COLOR.r, BG_COLOR.g, BG_COLOR.b, alpha * 0.90f);
        batch.draw(whiteTex, 0, bannerY, vw, bannerH);

        // ── Gold accent stripes (top & bottom edge) ───────────────
        float pulse = 0.80f + 0.20f * MathUtils.sin(timer * 7f);
        batch.setColor(STRIPE_COL.r, STRIPE_COL.g, STRIPE_COL.b, alpha * pulse);
        batch.draw(whiteTex, 0, bannerY + bannerH - stripeH, vw, stripeH); // top edge
        batch.draw(whiteTex, 0, bannerY,                     vw, stripeH); // bottom edge

        // ── Text ──────────────────────────────────────────────────
        float centreY = bannerY + bannerH / 2f;

        // Line 1 — stage name (gold)
        fontBig.setColor(LINE1_COLOR.r, LINE1_COLOR.g, LINE1_COLOR.b, alpha);
        GlyphLayout gl1 = new GlyphLayout(fontBig, line1);
        fontBig.draw(batch, line1,
            (vw - gl1.width) / 2f,
            centreY + gl1.height + 4f);

        // Line 2 — wave / complete (white, smaller)
        fontSmall.setColor(LINE2_COLOR.r, LINE2_COLOR.g, LINE2_COLOR.b, alpha);
        GlyphLayout gl2 = new GlyphLayout(fontSmall, line2);
        fontSmall.draw(batch, line2,
            (vw - gl2.width) / 2f,
            centreY - 4f);

        batch.setColor(Color.WHITE);
    }

    private float computeAlpha() {
        if (timer < FADE_IN)
            return timer / FADE_IN;
        if (timer < FADE_IN + HOLD)
            return 1f;
        return 1f - (timer - FADE_IN - HOLD) / FADE_OUT;
    }

    public void dispose() {
        fontBig.dispose();
        fontSmall.dispose();
        whiteTex.dispose();
    }
}
