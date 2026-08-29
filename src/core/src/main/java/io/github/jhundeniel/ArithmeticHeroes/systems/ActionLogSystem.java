package io.github.jhundeniel.ArithmeticHeroes.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Action Log — pixel-font style log panel with "Action Log" title header.
 * Dark purple background with bullet-pointed messages in uppercase pixel font.
 * Fixed-size box with text wrapping for long messages.
 * Positioned to the right of the turn order column with generous spacing.
 */
public class ActionLogSystem {

    private final SpriteBatch   batch;
    private BitmapFont    titleFont;
    private BitmapFont    messageFont;
    private final List<String>  messages   = new ArrayList<>();
    private Texture       whiteTex;

    private static final int   MAX_LINES     = 5;
    private static final float BOX_PAD_X     = 20f;
    private static final float BOX_PAD_Y     = 14f;
    private static final float LINE_SPACING  = 8f;
    private static final float MARGIN_TOP    = 15f; // Pushed back to top
    private static final float TITLE_HEIGHT  = 36f;

    // Fixed box width — large enough for most messages, text wraps within
    private static final float FIXED_BOX_W   = 420f;

    // Dark purple/navy background matching the reference image
    private static final Color BOX_BG       = new Color(0.15f, 0.08f, 0.22f, 0.95f);
    // Darker header background
    private static final Color TITLE_BG     = new Color(0.08f, 0.04f, 0.14f, 0.95f);
    // Border colour — subtle purple edge
    private static final Color BORDER_COL   = new Color(0.30f, 0.18f, 0.42f, 0.90f);

    // Title colour — blueish white (matches original)
    private static final Color TITLE_COL    = new Color(0.7f, 0.8f, 1f, 1f);
    // Most recent message — emphasized yellow (matches original)
    private static final Color RECENT_COL   = new Color(1f, 0.88f, 0.15f, 1f);
    // Older messages — light gray-blue (matches original)
    private static final Color TEXT_COL     = new Color(0.8f, 0.85f, 0.9f, 1f);

    private static final float BORDER_W = 2f;

    public ActionLogSystem(SpriteBatch batch) {
        this.batch = batch;
        loadFonts();
        buildWhiteTex();
    }

    private void loadFonts() {
        try {
            titleFont = new BitmapFont(Gdx.files.internal("ui/font export.fnt"));
            titleFont.getData().setScale(1.2f);

            messageFont = new BitmapFont(Gdx.files.internal("ui/font-small export.fnt"));
            messageFont.getData().setScale(1.1f);
        } catch (Exception e) {
            System.out.println("[ActionLog] Pixel font load failed, using fallback: " + e.getMessage());
            titleFont = new BitmapFont();
            titleFont.getData().setScale(1.6f);

            messageFont = new BitmapFont();
            messageFont.getData().setScale(1.3f);
        }
    }

    private void buildWhiteTex() {
        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        whiteTex = new Texture(px);
        px.dispose();
    }

    public void addMessage(String msg) {
        messages.add(0, msg.toUpperCase());
        if (messages.size() > 20) messages.remove(messages.size() - 1);
    }

    public void log(String msg) {
        addMessage(msg);
        System.out.println("[LOG] " + msg);
    }

    public void render() {
        if (messages.isEmpty()) return;

        int count = Math.min(messages.size(), MAX_LINES);

        // Maximum text width inside the box (for wrapping)
        float maxTextWidth = FIXED_BOX_W - BOX_PAD_X * 2f;

        // Measure content height with wrapping
        float totalTextH = 0;
        List<GlyphLayout> layouts = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String line = "\u00b7" + messages.get(i);
            Color col = (i == 0) ? RECENT_COL : TEXT_COL;
            // Wrap text within maxTextWidth
            GlyphLayout gl = new GlyphLayout(messageFont, line, col, maxTextWidth, com.badlogic.gdx.utils.Align.left, true);
            layouts.add(gl);
            totalTextH += gl.height;
            if (i < count - 1) totalTextH += LINE_SPACING;
        }

        float boxW = FIXED_BOX_W;
        float contentH = totalTextH + BOX_PAD_Y * 2f;
        float boxH = TITLE_HEIGHT + contentH;

        float screenH = Gdx.graphics.getHeight();

        // Position: more generous spacing from the turn order column, pushed to top
        float leftOffset = 210f;
        float boxX = leftOffset;
        float boxY = screenH - boxH - MARGIN_TOP;

        batch.begin();

        // ── Dark purple content background ──────────────────────
        batch.setColor(BOX_BG);
        batch.draw(whiteTex, boxX, boxY, boxW, contentH);

        // ── Darker title header background ──────────────────────
        batch.setColor(TITLE_BG);
        batch.draw(whiteTex, boxX, boxY + contentH, boxW, TITLE_HEIGHT);

        // ── Border around entire box ────────────────────────────
        batch.setColor(BORDER_COL);
        // Top
        batch.draw(whiteTex, boxX, boxY + boxH - BORDER_W, boxW, BORDER_W);
        // Bottom
        batch.draw(whiteTex, boxX, boxY, boxW, BORDER_W);
        // Left
        batch.draw(whiteTex, boxX, boxY, BORDER_W, boxH);
        // Right
        batch.draw(whiteTex, boxX + boxW - BORDER_W, boxY, BORDER_W, boxH);
        // Separator line between title and content
        batch.draw(whiteTex, boxX, boxY + contentH, boxW, BORDER_W);

        // ── Title: "Action Log" centred in header ───────────────
        String title = "Action Log";
        titleFont.setColor(TITLE_COL);
        GlyphLayout titleLayout = new GlyphLayout(titleFont, title);
        float titleX = boxX + (boxW - titleLayout.width) / 2f;
        float titleY = boxY + contentH + (TITLE_HEIGHT + titleLayout.height) / 2f;
        titleFont.draw(batch, title, titleX, titleY);

        // ── Message lines with bullet points (wrapped) ──────────
        float textX = boxX + BOX_PAD_X;
        float textY = boxY + contentH - BOX_PAD_Y;

        for (int i = 0; i < count; i++) {
            GlyphLayout gl = layouts.get(i);
            messageFont.setColor((i == 0) ? RECENT_COL : TEXT_COL);
            messageFont.draw(batch, gl, textX, textY);
            textY -= gl.height + LINE_SPACING;
        }

        batch.setColor(Color.WHITE);
        batch.end();
    }

    public void resize(int w, int h) {
        // Nothing needed — we use raw screen coords in render()
    }

    public void dispose() {
        if (titleFont != null) titleFont.dispose();
        if (messageFont != null) messageFont.dispose();
        if (whiteTex != null) whiteTex.dispose();
    }
}
