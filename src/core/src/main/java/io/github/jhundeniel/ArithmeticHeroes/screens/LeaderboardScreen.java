package io.github.jhundeniel.ArithmeticHeroes.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
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
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.jhundeniel.ArithmeticHeroes.Main;
import io.github.jhundeniel.ArithmeticHeroes.data.LeaderboardEntry;
import io.github.jhundeniel.ArithmeticHeroes.managers.LeaderboardManager;

import java.util.List;

/**
 * Full-screen leaderboard view displaying the top 5 scores.
 *
 * <p>
 * Accessible from the Main Menu or shown automatically after
 * submitting a score via the {@link VictoryOverlay}.
 * </p>
 *
 * <p>
 * The leaderboard entries are ranked by the <b>Insertion Sort</b>
 * algorithm implemented in {@link LeaderboardManager}.
 * </p>
 */
public class LeaderboardScreen implements Screen {

    private static final float VW = 1280f;
    private static final float VH = 720f;

    private final Main game;
    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final BitmapFont font;

    private final Rectangle backBtnRect;
    private final boolean backSelected = true;
    private float animTime = 0f;
    private boolean isTransitioning = false;

    private final List<LeaderboardEntry> cachedEntries;

    // Rank colors: Gold, Silver, Bronze, then muted tones
    private static final Color[] RANK_COLORS = {
            new Color(1.00f, 0.84f, 0.00f, 1f), // #1 Gold
            new Color(0.75f, 0.75f, 0.75f, 1f), // #2 Silver
            new Color(0.80f, 0.50f, 0.20f, 1f), // #3 Bronze
            new Color(0.55f, 0.65f, 0.75f, 1f), // #4
            new Color(0.45f, 0.55f, 0.65f, 1f), // #5
    };

    public LeaderboardScreen(Main game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new StretchViewport(VW, VH, camera);
        viewport.apply(true);

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        float btnW = 200f;
        float btnH = 50f;
        backBtnRect = new Rectangle(VW / 2f - btnW / 2f, 40f, btnW, btnH);

        cachedEntries = LeaderboardManager.getTopEntries();
    }

    @Override
    public void render(float delta) {
        animTime += delta;

        Gdx.gl.glClearColor(0.04f, 0.03f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        handleInput();
        drawBackground();
        drawLeaderboard();
        drawBackButton();
    }

    // ── Input ────────────────────────────────────────────────────────────

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (isTransitioning)
                return;
            isTransitioning = true;
            game.transitionToScreen(new MainMenuScreen(game));
            return;
        }

        if (Gdx.input.justTouched()) {
            Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mouse, viewport.getScreenX(), viewport.getScreenY(),
                    viewport.getScreenWidth(), viewport.getScreenHeight());
            if (backBtnRect.contains(mouse.x, mouse.y)) {
                if (isTransitioning)
                    return;
                isTransitioning = true;
                game.transitionToScreen(new MainMenuScreen(game));
            }
        }
    }

    // ── Drawing ──────────────────────────────────────────────────────────

    private void drawBackground() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Subtle animated gradient circles in the background
        for (int i = 0; i < 4; i++) {
            float cx = VW * (0.2f + 0.2f * i) + MathUtils.sin(animTime * 0.5f + i) * 40f;
            float cy = VH * 0.5f + MathUtils.cos(animTime * 0.3f + i * 2) * 60f;
            float alpha = 0.04f + 0.02f * MathUtils.sin(animTime + i);
            shapeRenderer.setColor(0.3f, 0.2f, 0.6f, alpha);
            shapeRenderer.circle(cx, cy, 200f);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawLeaderboard() {
        List<LeaderboardEntry> entries = LeaderboardManager.getTopEntries();

        batch.begin();

        // ── Title ────────────────────────────────────────────
        font.getData().setScale(2.8f);
        font.setColor(1f, 0.85f, 0.2f, 1f);
        GlyphLayout titleLayout = new GlyphLayout(font, "LEADERBOARD");
        float titleX = VW / 2f - titleLayout.width / 2f;
        float titleY = VH - 40f;
        font.draw(batch, "LEADERBOARD", titleX, titleY);

        batch.end();

        // ── Table header + rows ──────────────────────────────
        float tableTop = VH - 130f;
        float rowH = 55f;
        float tableW = 800f;
        float tableX = VW / 2f - tableW / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Header row background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.15f, 0.12f, 0.28f, 0.85f);
        shapeRenderer.rect(tableX, tableTop - rowH, tableW, rowH);
        shapeRenderer.end();

        // Header border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.5f, 0.4f, 0.8f, 0.6f);
        shapeRenderer.rect(tableX, tableTop - rowH, tableW, rowH);
        shapeRenderer.end();

        batch.begin();
        font.getData().setScale(1.3f);
        font.setColor(0.8f, 0.75f, 0.95f, 1f);

        // Column positions
        float colRank = tableX + 30f;
        float colName = tableX + 100f;
        float colScore = tableX + 400f;
        float colHeroes = tableX + 560f;
        float colStages = tableX + 700f;

        float headerY = tableTop - 15f;
        font.draw(batch, "#", colRank, headerY);
        font.draw(batch, "NAME", colName, headerY);
        font.draw(batch, "SCORE", colScore, headerY);
        font.draw(batch, "HEROES", colHeroes, headerY);
        font.draw(batch, "STAGES", colStages, headerY);
        batch.end();

        // ── Data rows ────────────────────────────────────────
        if (cachedEntries.isEmpty()) {
            batch.begin();
            font.getData().setScale(1.4f);
            font.setColor(0.5f, 0.5f, 0.6f, 0.7f);
            String emptyMsg = "No entries yet. Clear all stages to earn a spot!";
            GlyphLayout emptyLayout = new GlyphLayout(font, emptyMsg);
            font.draw(batch, emptyMsg,
                    VW / 2f - emptyLayout.width / 2f,
                    tableTop - rowH - 60f);
            batch.end();
        } else {
            for (int i = 0; i < cachedEntries.size(); i++) {
                LeaderboardEntry entry = cachedEntries.get(i);
                float rowY = tableTop - rowH * (i + 2);

                // Row background (alternating tint)
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                float bgAlpha = (i % 2 == 0) ? 0.15f : 0.10f;
                shapeRenderer.setColor(0.10f, 0.08f, 0.20f, bgAlpha);
                shapeRenderer.rect(tableX, rowY, tableW, rowH);

                // Rank indicator bar
                Color rankColor = (i < RANK_COLORS.length)
                        ? RANK_COLORS[i]
                        : new Color(0.4f, 0.4f, 0.5f, 1f);
                shapeRenderer.setColor(rankColor.r, rankColor.g, rankColor.b, 0.7f);
                shapeRenderer.rect(tableX, rowY, 6f, rowH);
                shapeRenderer.end();

                // Row border
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(0.25f, 0.2f, 0.4f, 0.3f);
                shapeRenderer.rect(tableX, rowY, tableW, rowH);
                shapeRenderer.end();

                // Text
                batch.begin();
                float textY = rowY + rowH / 2f + 8f;

                // Rank number (colored)
                font.getData().setScale(1.5f);
                font.setColor(rankColor);
                font.draw(batch, String.valueOf(i + 1), colRank, textY);

                // Name
                font.getData().setScale(1.3f);
                font.setColor(0.9f, 0.9f, 0.95f, 1f);
                font.draw(batch, entry.playerName != null ? entry.playerName : "???",
                        colName, textY);

                // Score (highlighted)
                font.setColor(1f, 0.92f, 0.4f, 1f);
                font.draw(batch, String.valueOf(entry.score), colScore, textY);

                // Heroes alive
                font.setColor(0.4f, 0.9f, 0.5f, 1f);
                font.draw(batch, String.valueOf(entry.heroesAlive), colHeroes + 25f, textY);

                // Stages cleared
                font.setColor(0.6f, 0.8f, 1.0f, 1f);
                font.draw(batch, String.valueOf(entry.stagesCleared), colStages + 20f, textY);

                batch.end();
            }
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawBackButton() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Button background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float pulse = 0.3f + 0.05f * MathUtils.sin(animTime * 3f);
        shapeRenderer.setColor(0.25f + pulse * 0.1f, 0.15f, 0.45f, 0.85f);
        shapeRenderer.rect(backBtnRect.x, backBtnRect.y, backBtnRect.width, backBtnRect.height);
        shapeRenderer.end();

        // Button border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.6f, 0.4f, 1.0f, 0.7f);
        shapeRenderer.rect(backBtnRect.x, backBtnRect.y, backBtnRect.width, backBtnRect.height);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Button text
        batch.begin();
        font.getData().setScale(1.5f);
        font.setColor(1f, 0.95f, 0.7f, 1f);
        GlyphLayout gl = new GlyphLayout(font, "BACK");
        font.draw(batch, "BACK",
                backBtnRect.x + backBtnRect.width / 2f - gl.width / 2f,
                backBtnRect.y + backBtnRect.height / 2f + gl.height / 2f);
        batch.end();
    }

    // ── Screen lifecycle ─────────────────────────────────────────────────

    @Override
    public void show() {
    }

    @Override
    public void resize(int w, int h) {
        viewport.update(w, h, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}
