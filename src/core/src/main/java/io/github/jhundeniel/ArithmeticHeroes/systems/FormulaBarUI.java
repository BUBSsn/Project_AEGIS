package io.github.jhundeniel.ArithmeticHeroes.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Floating "FINAL VALUE" bar driven by SkillButtonsUI hover.
 * Shows a formula breakdown like "10 x 125% + 3 = 15"
 * when the player hovers over a skill button.
 */
public class FormulaBarUI {

    // ── Layout ───────────────────────────────────────────────────────
    private static final float BAR_W   = 320f;
    private static final float BAR_H   = 56f;
    private static final float TITLE_H = 22f;

    // ── Colors ───────────────────────────────────────────────────────
    private static final Color BG_COLOR        = new Color(0.12f, 0.14f, 0.20f, 0.92f);
    private static final Color TITLE_BG        = new Color(0.08f, 0.10f, 0.14f, 0.95f);
    private static final Color TITLE_TEXT_COLOR = new Color(1f, 0.85f, 0.2f, 1f);
    private static final Color VALUE_COLOR     = new Color(0.4f, 1f, 0.5f, 1f);
    private static final Color BORDER_COLOR    = new Color(0.4f, 0.45f, 0.55f, 0.8f);

    // ── Rendering ────────────────────────────────────────────────────
    private final SpriteBatch   batch;
    private final ShapeRenderer shapes;
    private BitmapFont    fontTitle;
    private BitmapFont    fontValue;
    private final GlyphLayout   layout;
    private final Texture       whiteTex;

    // ── State (set by SkillButtonsUI on hover) ───────────────────────
    private String formulaText = null;   // e.g. "10 x 125% + 3 = 15"
    private boolean visible = false;

    public FormulaBarUI(SpriteBatch batch) {
        this.batch  = batch;
        this.shapes = new ShapeRenderer();
        this.layout = new GlyphLayout();

        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        whiteTex = new Texture(px);
        px.dispose();

        try {
            fontTitle = new BitmapFont(Gdx.files.internal("ui/font-small export.fnt"));
            fontTitle.getData().setScale(0.9f);
            fontValue = new BitmapFont(Gdx.files.internal("ui/font export.fnt"));
            fontValue.getData().setScale(1.0f);
        } catch (Exception e) {
            fontTitle = new BitmapFont();
            fontTitle.getData().setScale(1.0f);
            fontValue = new BitmapFont();
            fontValue.getData().setScale(1.3f);
        }
    }

    // ── API called by SkillButtonsUI ─────────────────────────────────

    /** Show the formula bar with the given breakdown text. */
    public void setFormula(String text) {
        this.formulaText = text;
        this.visible = (text != null && !text.isEmpty());
    }

    /** Hide the formula bar. */
    public void clearFormula() {
        this.formulaText = null;
        this.visible = false;
    }

    // ── Render ────────────────────────────────────────────────────────

    public void render() {
        if (!visible || formulaText == null) return;

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // Dynamically size the bar width to fit the formula text
        layout.setText(fontValue, formulaText);
        float contentW = Math.max(BAR_W, layout.width + 40f);

        float barX = (sw - contentW) / 2f;
        float barY = sh * 0.52f;

        batch.begin();

        // Main background
        batch.setColor(BG_COLOR);
        batch.draw(whiteTex, barX, barY, contentW, BAR_H);

        // Title bar
        batch.setColor(TITLE_BG);
        batch.draw(whiteTex, barX, barY + BAR_H - TITLE_H, contentW, TITLE_H);

        // Title text
        fontTitle.setColor(TITLE_TEXT_COLOR);
        GlyphLayout titleLayout = new GlyphLayout(fontTitle, "FINAL VALUE");
        float titleX = barX + (contentW - titleLayout.width) / 2f;
        float titleY = barY + BAR_H - (TITLE_H - titleLayout.height) / 2f;
        fontTitle.draw(batch, "FINAL VALUE", titleX, titleY);

        // Formula text
        fontValue.setColor(VALUE_COLOR);
        float valueX = barX + (contentW - layout.width) / 2f;
        float valueY = barY + BAR_H - TITLE_H - 4f;
        fontValue.draw(batch, formulaText, valueX, valueY);

        batch.setColor(Color.WHITE);
        batch.end();

        // Border
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(BORDER_COLOR);
        shapes.rect(barX, barY, contentW, BAR_H);
        shapes.end();
    }

    public void dispose() {
        shapes.dispose();
        fontTitle.dispose();
        fontValue.dispose();
        if (whiteTex != null) whiteTex.dispose();
    }
}
