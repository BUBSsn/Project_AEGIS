package io.github.jhundeniel.ArithmeticHeroes.screens;

import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

public class VictoryOverlay {

    public enum VictoryAction {
        NONE, SUBMIT
    }

    private static final float PANEL_W_FRAC = 0.42f;
    private static final float PANEL_H_FRAC = 0.85f;
    private static final int STAR_COUNT = 60;
    private static final int RAY_COUNT = 12;

    private final ShapeRenderer shapes;
    private final BitmapFont font;
    private final SpriteBatch batch;

    private final int finalScore;
    private final int heroesAlive;
    private final int stagesCleared;

    private final StringBuilder nameBuilder = new StringBuilder();
    private static final int MAX_NAME_LENGTH = 12;

    private float vw, vh;
    private float panelX, panelY, panelW, panelH;
    private final Rectangle submitBtn = new Rectangle();
    private final Vector3 mouseVec = new Vector3();

    private float animTime = 0f;
    private float fadeAlpha = 0f;
    private static final float FADE_SPEED = 1.8f;
    private boolean submitted = false;

    private final float[] sx, sy, ssize, sphase;
    private boolean starsSeeded = false;

    // Cached layout values computed once per frame
    private float titleBandH;
    private float scoreSectionTop, scoreSectionBot;
    private float cardY, cardH, cardW, cardGap;
    private float card1X, card2X, card3X;
    private float inputX, inputY, inputW, inputH;

    public VictoryOverlay(int finalScore, int heroesAlive, int stagesCleared, ArithmeticAssetManager assets) {
        this.finalScore = finalScore;
        this.heroesAlive = heroesAlive;
        this.stagesCleared = stagesCleared;

        shapes = new ShapeRenderer();
        font = new BitmapFont();
        batch = new SpriteBatch();

        sx = new float[STAR_COUNT];
        sy = new float[STAR_COUNT];
        ssize = new float[STAR_COUNT];
        sphase = new float[STAR_COUNT];
    }

    // ── Layout ─────────────────────────────────────────────────────────────
    // Everything is derived top-to-bottom from panelY + panelH downward.
    // Each section is a fixed fraction of panelH so nothing overlaps.
    //
    // panelTop (panelY + panelH)
    // ├─ [4px] gold bar
    // ├─ [26%] title band → badge, ornament, VICTORY, subtitle
    // ├─ [1px] divider
    // ├─ [18%] score section (FINAL SCORE label + number)
    // ├─ [1px] divider
    // ├─ [16%] stat cards row
    // ├─ [3%] gap
    // ├─ [3%] "ENTER YOUR NAME" label
    // ├─ [10%] name input box
    // ├─ [4%] gap
    // ├─ [10%] submit button
    // ├─ [4%] hint text
    // panelBot (panelY)

    private void updateLayout(OrthographicCamera camera) {
        vw = camera.viewportWidth;
        vh = camera.viewportHeight;

        panelW = vw * PANEL_W_FRAC;
        panelH = vh * PANEL_H_FRAC;
        panelX = vw / 2f - panelW / 2f;
        panelY = vh / 2f - panelH / 2f;

        // Work top-down as fractions of panelH
        titleBandH = panelH * 0.30f;

        float divider1Y = panelY + panelH - titleBandH; // below title band
        scoreSectionTop = divider1Y;
        scoreSectionBot = scoreSectionTop - panelH * 0.20f; // score section height = 20%

        float divider2Y = scoreSectionBot;

        // Stat cards — 16% tall, sitting just below score section
        cardH = panelH * 0.16f;
        cardGap = panelW * 0.03f;
        cardW = (panelW - 28f - cardGap * 2f) / 3f;
        cardY = divider2Y - panelH * 0.02f - cardH; // 2% gap then cards
        card1X = panelX + 14f;
        card2X = card1X + cardW + cardGap;
        card3X = card2X + cardW + cardGap;

        // Name input — below cards
        inputW = panelW * 0.76f;
        inputH = panelH * 0.09f;
        inputX = vw / 2f - inputW / 2f;
        inputY = cardY - panelH * 0.11f; // 11% gap below cards

        // Submit button — below input
        float btnW = panelW * 0.72f;
        float btnH = panelH * 0.09f;
        float btnY = inputY - panelH * 0.04f - btnH; // 4% gap below input
        submitBtn.set(vw / 2f - btnW / 2f, btnY, btnW, btnH);

        if (!starsSeeded) {
            for (int i = 0; i < STAR_COUNT; i++) {
                sx[i] = MathUtils.random(0f, vw);
                sy[i] = MathUtils.random(0f, vh);
                ssize[i] = MathUtils.random(0.8f, 2.5f);
                sphase[i] = MathUtils.random(0f, MathUtils.PI2);
            }
            starsSeeded = true;
        }
    }

    // ── Render ─────────────────────────────────────────────────────────────

    public void render(OrthographicCamera camera) {
        float dt = Gdx.graphics.getDeltaTime();
        animTime += dt;
        fadeAlpha = Math.min(1f, fadeAlpha + dt * FADE_SPEED);

        updateLayout(camera);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);

        // 1. Full-screen dark background
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.04f, 0.03f, 0.10f, 0.92f * fadeAlpha);
        shapes.rect(0, 0, vw, vh);
        shapes.end();

        // 2. Twinkling stars
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < STAR_COUNT; i++) {
            float twinkle = 0.3f + 0.6f * (0.5f + 0.5f * MathUtils.sin(animTime * 1.8f + sphase[i]));
            shapes.setColor(1f, 1f, 1f, twinkle * fadeAlpha * 0.7f);
            shapes.circle(sx[i], sy[i], ssize[i], 4);
        }
        shapes.end();

        // 3. Rotating light rays (additive blend)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        float cx = vw / 2f, cy = vh / 2f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < RAY_COUNT; i++) {
            float angle = animTime * 0.055f + (i * MathUtils.PI2 / RAY_COUNT);
            float angleW = 0.022f;
            float len = Math.max(vw, vh) * 0.72f;
            float ax1 = cx + MathUtils.cos(angle - angleW) * 20f;
            float ay1 = cy + MathUtils.sin(angle - angleW) * 20f;
            float ax2 = cx + MathUtils.cos(angle + angleW) * 20f;
            float ay2 = cy + MathUtils.sin(angle + angleW) * 20f;
            float bx1 = cx + MathUtils.cos(angle - angleW) * len;
            float by1 = cy + MathUtils.sin(angle - angleW) * len;
            float bx2 = cx + MathUtils.cos(angle + angleW) * len;
            float by2 = cy + MathUtils.sin(angle + angleW) * len;
            shapes.setColor(1f, 0.85f, 0.25f, 0.028f * fadeAlpha);
            shapes.triangle(ax1, ay1, ax2, ay2, bx1, by1);
            shapes.triangle(ax2, ay2, bx1, by1, bx2, by2);
        }
        shapes.end();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // 4. Panel drop-shadow
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.55f * fadeAlpha);
        shapes.rect(panelX + 8f, panelY - 8f, panelW, panelH);
        shapes.end();

        // 5. Panel body
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.07f, 0.05f, 0.17f, 0.97f * fadeAlpha);
        shapes.rect(panelX, panelY, panelW, panelH);
        shapes.end();

        // 6. Title band
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.04f, 0.02f, 0.12f, 0.75f * fadeAlpha);
        shapes.rect(panelX, panelY + panelH - titleBandH, panelW, titleBandH);
        shapes.end();

        // 7. Gold top accent bar
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 0.84f, 0.2f, fadeAlpha);
        shapes.rect(panelX, panelY + panelH - 4f, panelW, 4f);
        shapes.end();

        // 8. Pulsing gold border
        float bp = 0.55f + 0.30f * MathUtils.sin(animTime * 2.2f);
        drawBorderRect(panelX, panelY, panelW, panelH, 2f,
                1f, 0.84f, 0.2f, bp * fadeAlpha);

        // 9. Inner border (subtle purple)
        drawBorderRect(panelX + 6f, panelY + 6f, panelW - 12f, panelH - 12f, 1f,
                0.45f, 0.30f, 0.80f, 0.20f * fadeAlpha);

        // 10. Divider below title band
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 0.84f, 0.2f, 0.20f * fadeAlpha);
        shapes.rect(panelX + 24f, scoreSectionTop - 1f, panelW - 48f, 1f);
        shapes.end();

        // 11. Divider below score section
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 1f, 1f, 0.06f * fadeAlpha);
        shapes.rect(panelX + 14f, scoreSectionBot - 1f, panelW - 28f, 1f);
        shapes.end();

        // 12. Three stat cards
        drawStatCard(card1X, cardY, cardW, cardH, fadeAlpha);
        drawStatCard(card2X, cardY, cardW, cardH, fadeAlpha);
        drawStatCard(card3X, cardY, cardW, cardH, fadeAlpha);

        // 13. Name input box
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.10f, 0.07f, 0.22f, 0.95f * fadeAlpha);
        shapes.rect(inputX, inputY, inputW, inputH);
        shapes.end();
        float cursorAlpha = 0.45f + 0.35f * MathUtils.sin(animTime * 4f);
        drawBorderRect(inputX, inputY, inputW, inputH, 1.5f,
                0.50f, 0.55f, 1.00f, cursorAlpha * fadeAlpha);

        // 14. Submit button shadow + fill + bevel + border
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.40f * fadeAlpha);
        shapes.rect(submitBtn.x + 4f, submitBtn.y - 4f, submitBtn.width, submitBtn.height);
        shapes.end();

        boolean canSubmit = nameBuilder.length() > 0;
        float pulse = canSubmit ? 0.48f + 0.10f * MathUtils.sin(animTime * 3f) : 0f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.15f + pulse * 0.08f, 0.10f + pulse * 0.05f, 0.38f + pulse * 0.08f, 0.95f * fadeAlpha);
        shapes.rect(submitBtn.x, submitBtn.y, submitBtn.width, submitBtn.height);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 1f, 1f, 0.07f * fadeAlpha);
        shapes.rect(submitBtn.x + 1f, submitBtn.y + submitBtn.height - 4f, submitBtn.width - 2f, 4f);
        shapes.end();

        drawBorderRect(submitBtn.x, submitBtn.y, submitBtn.width, submitBtn.height, 2f,
                canSubmit ? 1f : 0.35f,
                canSubmit ? 0.84f : 0.35f,
                canSubmit ? 0.20f : 0.35f,
                (canSubmit ? 0.75f : 0.30f) * fadeAlpha);

        // 15. Corner bracket ornaments
        float cSize = 14f, cThick = 2f;
        drawCornerBracket(panelX, panelY + panelH, cSize, cThick, true, true, fadeAlpha);
        drawCornerBracket(panelX + panelW, panelY + panelH, cSize, cThick, false, true, fadeAlpha);
        drawCornerBracket(panelX, panelY, cSize, cThick, true, false, fadeAlpha);
        drawCornerBracket(panelX + panelW, panelY, cSize, cThick, false, false, fadeAlpha);

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── Text ───────────────────────────────────────────────────────────
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float panelTop = panelY + panelH;

        // Badge — top of title band
        float badgeScale = panelH * 0.0020f;
        font.getData().setScale(badgeScale);
        font.setColor(1f, 0.84f, 0.20f, 0.65f * fadeAlpha);
        String badge = "- ALL STAGES CLEARED -";
        GlyphLayout badgeLay = new GlyphLayout(font, badge);
        font.draw(batch, badge,
                vw / 2f - badgeLay.width / 2f,
                panelTop - 10f);

        // Ornament row
        font.getData().setScale(panelH * 0.0022f);
        font.setColor(1f, 0.84f, 0.20f, 0.55f * fadeAlpha);
        String ornRow = "* * *";
        GlyphLayout ornLay = new GlyphLayout(font, ornRow);
        font.draw(batch, ornRow,
                vw / 2f - ornLay.width / 2f,
                panelTop - titleBandH * 0.30f);

        // VICTORY title
        float flicker = 0.88f + 0.12f * MathUtils.sin(animTime * 7f);
        float titleScale = panelH * 0.0080f;
        font.getData().setScale(titleScale);
        String titleStr = "VICTORY";
        GlyphLayout titleLay = new GlyphLayout(font, titleStr);
        float titleX = vw / 2f - titleLay.width / 2f;
        float titleY = panelTop - titleBandH * 0.52f + titleLay.height / 2f;
        font.setColor(0.20f, 0.08f, 0.04f, 0.70f * fadeAlpha);
        font.draw(batch, titleStr, titleX + 3f, titleY - 3f);
        font.setColor(flicker, 0.84f * flicker, 0.20f, fadeAlpha);
        font.draw(batch, titleStr, titleX, titleY);

        // Subtitle
        float subScale = panelH * 0.0022f;
        font.getData().setScale(subScale);
        font.setColor(1f, 0.84f, 0.20f, 0.38f * fadeAlpha);
        String sub = "~ the math has been conquered ~";
        GlyphLayout subLay = new GlyphLayout(font, sub);
        font.draw(batch, sub,
                vw / 2f - subLay.width / 2f,
                panelTop - titleBandH + panelH * 0.014f);

        // FINAL SCORE label
        float labelScale = panelH * 0.0021f;
        float scoreCenterY = (scoreSectionTop + scoreSectionBot) / 2f;
        font.getData().setScale(labelScale);
        font.setColor(0.65f, 0.58f, 0.90f, 0.55f * fadeAlpha);
        String scoreLbl = "FINAL SCORE";
        GlyphLayout scoreLblLay = new GlyphLayout(font, scoreLbl);
        font.draw(batch, scoreLbl,
                vw / 2f - scoreLblLay.width / 2f,
                scoreCenterY + scoreLblLay.height * 1.6f);

        // Score number
        float scoreNumScale = panelH * 0.0058f;
        font.getData().setScale(scoreNumScale);
        font.setColor(1f, 1f, 1f, fadeAlpha);
        String scoreStr = String.format("%,d", finalScore);
        GlyphLayout scoreNumLay = new GlyphLayout(font, scoreStr);
        font.draw(batch, scoreStr,
                vw / 2f - scoreNumLay.width / 2f,
                scoreCenterY + scoreNumLay.height / 2f - panelH * 0.005f);

        // Stat card values + labels
        String[] statValues = { String.valueOf(stagesCleared), String.valueOf(heroesAlive), getRank() };
        String[] statLabels = { "STAGES", "HEROES ALIVE", "RANK" };
        float statValScale = panelH * 0.0042f;
        float statLblScale = panelH * 0.0018f;
        float statCenterY = cardY + cardH / 2f;

        for (int i = 0; i < 3; i++) {
            float cardCX = (i == 0 ? card1X : i == 1 ? card2X : card3X) + cardW / 2f;

            font.getData().setScale(statValScale);
            font.setColor(0.90f, 0.85f, 1.00f, fadeAlpha);
            GlyphLayout vLay = new GlyphLayout(font, statValues[i]);
            font.draw(batch, statValues[i],
                    cardCX - vLay.width / 2f,
                    statCenterY + vLay.height / 2f + panelH * 0.012f);

            font.getData().setScale(statLblScale);
            font.setColor(0.55f, 0.50f, 0.75f, 0.65f * fadeAlpha);
            GlyphLayout lLay = new GlyphLayout(font, statLabels[i]);
            font.draw(batch, statLabels[i],
                    cardCX - lLay.width / 2f,
                    statCenterY - panelH * 0.016f);
        }

        // ENTER YOUR NAME label — sits above input box
        font.getData().setScale(labelScale);
        font.setColor(0.65f, 0.58f, 0.90f, 0.55f * fadeAlpha);
        String nameLbl = "ENTER YOUR NAME";
        GlyphLayout nameLblLay = new GlyphLayout(font, nameLbl);
        font.draw(batch, nameLbl,
                vw / 2f - nameLblLay.width / 2f,
                inputY + inputH + nameLblLay.height + 6f);

        // Typed name / placeholder
        float nameScale = panelH * 0.0034f;
        font.getData().setScale(nameScale);
        String displayName = nameBuilder.length() == 0 ? "_ _ _ _ _ _ _ _" : nameBuilder.toString();
        if (nameBuilder.length() > 0 && (int) (animTime * 2f) % 2 == 0)
            displayName += "_";
        GlyphLayout dispLay = new GlyphLayout(font, displayName);
        font.setColor(nameBuilder.length() == 0
                ? new Color(1f, 1f, 1f, 0.22f * fadeAlpha)
                : new Color(1f, 1f, 1f, fadeAlpha));
        font.draw(batch, displayName,
                vw / 2f - dispLay.width / 2f,
                inputY + inputH / 2f + dispLay.height / 2f + 3f);

        // Submit button label
        float btnScale = panelH * 0.0030f;
        font.getData().setScale(btnScale);
        font.setColor(canSubmit
                ? new Color(1f, 0.90f, 0.50f, fadeAlpha)
                : new Color(0.45f, 0.40f, 0.55f, 0.55f * fadeAlpha));
        String btnStr = "SUBMIT SCORE";
        GlyphLayout btnLay = new GlyphLayout(font, btnStr);
        font.draw(batch, btnStr,
                submitBtn.x + submitBtn.width / 2f - btnLay.width / 2f,
                submitBtn.y + submitBtn.height / 2f + btnLay.height / 2f + 2f);

        // Hint
        font.getData().setScale(panelH * 0.0018f);
        font.setColor(1f, 1f, 1f, 0.22f * fadeAlpha);
        String hint = "[ENTER] to confirm";
        GlyphLayout hintLay = new GlyphLayout(font, hint);
        font.draw(batch, hint,
                vw / 2f - hintLay.width / 2f,
                submitBtn.y - panelH * 0.015f);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // ── Draw helpers ───────────────────────────────────────────────────────

    private void drawBorderRect(float x, float y, float w, float h,
            float t, float r, float g, float b, float a) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(r, g, b, a);
        shapes.rect(x, y + h - t, w, t);
        shapes.rect(x, y, w, t);
        shapes.rect(x, y, t, h);
        shapes.rect(x + w - t, y, t, h);
        shapes.end();
    }

    private void drawStatCard(float x, float y, float w, float h, float alpha) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 1f, 1f, 0.035f * alpha);
        shapes.rect(x, y, w, h);
        shapes.end();
        drawBorderRect(x, y, w, h, 1f, 1f, 1f, 1f, 0.08f * alpha);
    }

    private void drawCornerBracket(float x, float y, float size, float thick,
            boolean left, boolean top, float alpha) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 0.84f, 0.2f, 0.70f * alpha);
        float dx = left ? 0f : -size;
        float dy = top ? -size : 0f;
        shapes.rect(x + dx, top ? y - thick : y, size, thick);
        shapes.rect(left ? x : x - thick, y + dy, thick, size);
        shapes.end();
    }

    private String getRank() {
        if (heroesAlive == 4 && stagesCleared >= 2)
            return "S";
        if (heroesAlive >= 3)
            return "A";
        if (heroesAlive >= 2)
            return "B";
        return "C";
    }

    // ── Input ──────────────────────────────────────────────────────────────

    public VictoryAction handleInput(OrthographicCamera camera, ArithmeticAssetManager assets) {
        if (submitted)
            return VictoryAction.NONE;

        for (int i = 0; i < 256; i++) {
            if (Gdx.input.isKeyJustPressed(i)) {
                if (i == Input.Keys.BACKSPACE) {
                    if (nameBuilder.length() > 0)
                        nameBuilder.deleteCharAt(nameBuilder.length() - 1);
                    continue;
                }
                if (i == Input.Keys.ENTER) {
                    if (nameBuilder.length() > 0) {
                        submitted = true;
                        if (assets != null)
                            assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                        return VictoryAction.SUBMIT;
                    }
                    continue;
                }
                if (nameBuilder.length() < MAX_NAME_LENGTH) {
                    String keyName = Input.Keys.toString(i);
                    if (keyName != null && keyName.length() == 1) {
                        char c = keyName.charAt(0);
                        if (Character.isLetterOrDigit(c) || c == ' ')
                            nameBuilder.append(c);
                    }
                }
            }
        }

        if (Gdx.input.justTouched() && nameBuilder.length() > 0) {
            mouseVec.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            Vector3 mouse = mouseVec;
            camera.unproject(mouse);
            if (submitBtn.contains(mouse.x, mouse.y)) {
                submitted = true;
                if (assets != null)
                    assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                return VictoryAction.SUBMIT;
            }
        }

        return VictoryAction.NONE;
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public String getPlayerName() {
        return nameBuilder.toString().trim();
    }

    public int getFinalScore() {
        return finalScore;
    }

    public int getHeroesAlive() {
        return heroesAlive;
    }

    public int getStagesCleared() {
        return stagesCleared;
    }

    public void dispose() {
        shapes.dispose();
        font.dispose();
        batch.dispose();
    }
}
