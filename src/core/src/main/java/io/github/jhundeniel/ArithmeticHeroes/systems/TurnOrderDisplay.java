package io.github.jhundeniel.ArithmeticHeroes.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import java.util.List;

import io.github.jhundeniel.ArithmeticHeroes.components.PortraitComponent;
import io.github.jhundeniel.ArithmeticHeroes.managers.TurnManager;

public class TurnOrderDisplay {
    private final SpriteBatch batch;
    private final TurnManager turnManager;
    private final ComponentMapper<PortraitComponent> pm = ComponentMapper.getFor(PortraitComponent.class);

    private Texture bgStage1;
    private Texture bgStage2;
    private Texture bgStage3;
    private int currentStageIndex = 0;

    public void setCurrentStage(int idx) {
        this.currentStageIndex = idx;
    }

    // ── Turn number pop-up ────────────────────────────────────────
    private BitmapFont turnFont;
    private final Texture    whiteTex;
    private int        lastRound     = -1;
    private float      popupTimer    = 0f;
    private String     popupText     = "";
    private boolean    popupActive   = false;
    private Texture    activeTurnBg;
    private final Texture    rightArrowTex;
    private float      arrowTimer    = 0f;

    private static final float POPUP_FADE_IN  = 0.25f;
    private static final float POPUP_HOLD     = 1.00f;
    private static final float POPUP_FADE_OUT = 0.40f;
    private static final float POPUP_TOTAL    = POPUP_FADE_IN + POPUP_HOLD + POPUP_FADE_OUT;

    // Pop-up colours
    private static final Color POPUP_BG_COL    = new Color(0.06f, 0.04f, 0.12f, 1f);
    private static final Color POPUP_BORDER    = new Color(1f,    0.85f, 0.15f, 1f);  // gold border
    private static final Color POPUP_TEXT_COL  = new Color(1f,    0.92f, 0.20f, 1f);  // gold text

    public TurnOrderDisplay(SpriteBatch batch, TurnManager turnManager) {
        this.batch = batch;
        this.turnManager = turnManager;

        // Pixel font for the turn number pop-up
        try {
            turnFont = new BitmapFont(Gdx.files.internal("ui/font export.fnt"));
            turnFont.getData().setScale(1.2f);
        } catch (Exception e) {
            turnFont = new BitmapFont();
            turnFont.getData().setScale(2.5f);
        }

        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE); px.fill();
        whiteTex = new Texture(px); px.dispose();

        try { bgStage1 = new Texture(Gdx.files.internal("other_asset/turn_order_stage1.png")); } catch (Exception e) {}
        try { bgStage2 = new Texture(Gdx.files.internal("other_asset/turn_order_stage2.png")); } catch (Exception e) {}
        try { bgStage3 = new Texture(Gdx.files.internal("other_asset/turn_order_stage3.png")); } catch (Exception e) {}
        try { activeTurnBg = new Texture(Gdx.files.internal("other_asset/turn_order.png")); } catch (Exception e) {}

        Pixmap pxArrow = new Pixmap(24, 24, Pixmap.Format.RGBA8888);
        pxArrow.setColor(0, 0, 0, 0);
        pxArrow.fill();
        pxArrow.setColor(POPUP_TEXT_COL); // Gold
        for (int r = 0; r < 24; r++) {
            int dist = Math.abs(r - 12);
            int width = (12 - dist) * 2;
            // Draw left pointing arrow (widest on right, point at x=0)
            for (int c = 24 - width; c < 24; c++) {
                pxArrow.drawPixel(c, r);
            }
        }
        rightArrowTex = new Texture(pxArrow);
        pxArrow.dispose();
    }

    public void render() {
        List<Entity> currentRound = turnManager.getUpcomingTurnOrder();
        List<Entity> nextRound = turnManager.getNextRoundTurnOrder();

        float size = 65f;
        float verticalSpacing = 80f;
        float horizontalSpacing = 80f;

        // Left align the turn order to the edge
        float leftX = 25f;
        float startY = Gdx.graphics.getHeight() - 150f;
        arrowTimer += Gdx.graphics.getDeltaTime();

        // ── Check for new round → trigger pop-up ──────────────────
        int curRound = turnManager.getCurrentRound();
        if (curRound != lastRound && curRound > 0) {
            lastRound   = curRound;
            popupText   = "TURN " + curRound;
            popupTimer  = 0f;
            popupActive = true;
        }
        if (popupActive) {
            popupTimer += Gdx.graphics.getDeltaTime();
            if (popupTimer >= POPUP_TOTAL) popupActive = false;
        }

        batch.begin();

        // === 0. TOP LEFT PERSISTENT TURN INDICATOR ===
        if (curRound > 0) {
            String turnStr = "TURN " + curRound;
            turnFont.getData().setScale(1.1f);
            GlyphLayout tl = new GlyphLayout(turnFont, turnStr);

            float padX = 20f;
            float padY = 12f;
            float tW = tl.width + padX * 2f;
            float tH = tl.height + padY * 2f;

            float tX = 25f;
            float tY = Gdx.graphics.getHeight() - tH - 25f; // Top-left anchor

            // Background
            batch.setColor(POPUP_BG_COL.r, POPUP_BG_COL.g, POPUP_BG_COL.b, 0.9f);
            batch.draw(whiteTex, tX, tY, tW, tH);

            // Border
            float borderW = 3f;
            batch.setColor(POPUP_BORDER.r, POPUP_BORDER.g, POPUP_BORDER.b, 0.9f);
            batch.draw(whiteTex, tX, tY + tH - borderW, tW, borderW);
            batch.draw(whiteTex, tX, tY, tW, borderW);
            batch.draw(whiteTex, tX, tY, borderW, tH);
            batch.draw(whiteTex, tX + tW - borderW, tY, borderW, tH);

            // Text
            turnFont.setColor(POPUP_TEXT_COL.r, POPUP_TEXT_COL.g, POPUP_TEXT_COL.b, 1f);
            turnFont.draw(batch, turnStr, tX + padX, tY + tH - padY);
            turnFont.getData().setScale(1.2f); // restore scale
        }

        // === 1. LEFT SIDE: CURRENT ROUND (Vertical) ===
        if (currentRound != null && !currentRound.isEmpty()) {
            for (int i = 0; i < currentRound.size(); i++) {
                Entity entity = currentRound.get(i);
                PortraitComponent portrait = pm.get(entity);

                if (portrait != null && portrait.texture != null) {
                    float y = startY - (i * verticalSpacing);

                    if (i == 0) {
                        // Arrow pointing LEFT at active hero, placed on the RIGHT side
                        float bob = MathUtils.sin(arrowTimer * 6f) * 6f;
                        batch.setColor(Color.WHITE);
                        batch.draw(rightArrowTex, leftX + 80f + bob, y + size/2f - 12f, 24f, 24f);

                        // ACTIVE TURN: Use turn_order.png as background
                        if (activeTurnBg != null) {
                            batch.setColor(Color.WHITE);
                            batch.draw(activeTurnBg, leftX - 18f, y - 18f, size + 36f, size + 36f);
                        }

                        // Draw portrait
                        batch.setColor(Color.WHITE);
                        batch.draw(portrait.texture, leftX - 4f, y - 4f, size + 8f, size + 8f);
                    } else {
                        // WAITING: Normal size
                        batch.setColor(Color.WHITE);
                        batch.draw(portrait.texture, leftX, y, size, size);
                    }
                }
            }
        }

        // === 2. TOP MIDDLE: NEXT ROUND (Horizontal) ===
        if (nextRound != null && !nextRound.isEmpty()) {
            // Calculate total width of the next round bar
            float totalWidth = nextRound.size() * horizontalSpacing;
            float topStartX = (Gdx.graphics.getWidth() - totalWidth) / 2f + (horizontalSpacing - size)/2f; // center horizontally
            // Positioned slightly lower so it's vertically below the Wave Announcer HUD
            float topY = Gdx.graphics.getHeight() - size - 70f;

            // Render text
            turnFont.getData().setScale(1.0f);
            turnFont.setColor(POPUP_TEXT_COL); // Use the same gold color as the turn popups
            GlyphLayout layout = new GlyphLayout(turnFont, "NEXT TURN ORDER");
            float textX = (Gdx.graphics.getWidth() - layout.width) / 2f;
            turnFont.draw(batch, "NEXT TURN ORDER", textX, topY + size + layout.height + 15f);
            turnFont.getData().setScale(1.2f); // restore scale

            for (int i = 0; i < nextRound.size(); i++) {
                Entity entity = nextRound.get(i);
                PortraitComponent portrait = pm.get(entity);

                if (portrait != null && portrait.texture != null) {
                    float x = topStartX + (i * horizontalSpacing);

                    Texture stageBg = bgStage1;
                    if (currentStageIndex >= 5 && currentStageIndex <= 6) stageBg = bgStage2;
                    if (currentStageIndex >= 7) stageBg = bgStage3;

                    if (stageBg != null) {
                        batch.setColor(Color.WHITE);
                        batch.draw(stageBg, x - 15f, topY - 15f, size + 30f, size + 30f);
                    }

                    // NEXT ROUND: dim and square frame simulation via drawing logic
                    batch.setColor(Color.LIGHT_GRAY);
                    batch.draw(portrait.texture, x, topY, size, size);
                }
            }
        }

        // === 3. TURN NUMBER POP-UP ===
        if (popupActive) {
            renderTurnPopup();
        }

        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Renders a centred "TURN X" pop-up banner that fades in, holds, and fades out. */
    private void renderTurnPopup() {
        float alpha = computePopupAlpha();
        if (alpha <= 0f) return;

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Measure text
        GlyphLayout gl = new GlyphLayout(turnFont, popupText);
        float padX = 30f;
        float padY = 16f;
        float boxW = gl.width + padX * 2f;
        float boxH = gl.height + padY * 2f;
        float borderW = 3f;

        // Centre on screen
        float boxX = (screenW - boxW) / 2f;
        float boxY = (screenH - boxH) / 2f + screenH * 0.18f; // slightly above centre

        // Slide-in effect: starts 20px above final position
        float slideOffset = (1f - alpha) * 20f;
        boxY += slideOffset;

        // Dark background
        batch.setColor(POPUP_BG_COL.r, POPUP_BG_COL.g, POPUP_BG_COL.b, alpha * 0.92f);
        batch.draw(whiteTex, boxX, boxY, boxW, boxH);

        // Gold border
        float pulse = 0.85f + 0.15f * MathUtils.sin(popupTimer * 6f);
        batch.setColor(POPUP_BORDER.r, POPUP_BORDER.g, POPUP_BORDER.b, alpha * pulse);
        batch.draw(whiteTex, boxX,               boxY + boxH - borderW, boxW, borderW);  // top
        batch.draw(whiteTex, boxX,               boxY,                  boxW, borderW);  // bottom
        batch.draw(whiteTex, boxX,               boxY,                  borderW, boxH);  // left
        batch.draw(whiteTex, boxX + boxW - borderW, boxY,               borderW, boxH);  // right

        // Gold text
        turnFont.setColor(POPUP_TEXT_COL.r, POPUP_TEXT_COL.g, POPUP_TEXT_COL.b, alpha);
        turnFont.draw(batch, popupText,
            boxX + (boxW - gl.width) / 2f,
            boxY + (boxH + gl.height) / 2f);
    }

    private float computePopupAlpha() {
        if (popupTimer < POPUP_FADE_IN)
            return popupTimer / POPUP_FADE_IN;
        if (popupTimer < POPUP_FADE_IN + POPUP_HOLD)
            return 1f;
        return 1f - (popupTimer - POPUP_FADE_IN - POPUP_HOLD) / POPUP_FADE_OUT;
    }

    public void dispose() {
        if (turnFont != null) turnFont.dispose();
        if (whiteTex != null) whiteTex.dispose();
        if (bgStage1 != null) bgStage1.dispose();
        if (bgStage2 != null) bgStage2.dispose();
        if (bgStage3 != null) bgStage3.dispose();
        if (activeTurnBg != null) activeTurnBg.dispose();
        if (rightArrowTex != null) rightArrowTex.dispose();
    }
}
