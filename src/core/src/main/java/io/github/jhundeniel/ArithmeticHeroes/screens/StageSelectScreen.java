package io.github.jhundeniel.ArithmeticHeroes.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
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
import io.github.jhundeniel.ArithmeticHeroes.data.StageData;
import io.github.jhundeniel.ArithmeticHeroes.managers.SaveManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.StageRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage selection screen for PRACTICE MODE.
 *
 * <p>
 * Stages are unlocked only after the player has beaten them in a
 * normal (New Game) run. First-time players have nothing unlocked.
 * Heroes always spawn at full HP/mana, and saving is disabled.
 * </p>
 *
 * <p>
 * The card list is scrollable and clipped to a fixed visible area
 * between the title and the BACK button.
 * </p>
 */
public class StageSelectScreen implements Screen {

    private static final float VW = 1280f;
    private static final float VH = 720f;

    private final Main game;
    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final BitmapFont font;

    /** All stage data from the registry. */
    private final List<StageData> stages;

    /**
     * Highest stage index any save has reached.
     * A stage at index i is considered "cleared" (and thus unlocked for
     * practice) when highestClearedIndex > i — meaning the player has
     * advanced past it.
     *
     * -1 means no saves exist → nothing is unlocked.
     */
    private final int highestClearedIndex;

    /** Currently selected stage index (stages.size() = BACK button). */
    private int selectedIndex = 0;

    /** Hit-box for the BACK button. */
    private final Rectangle backBtnRect;

    private float animTime = 0f;
    private boolean isTransitioning = false;

    // ── Scrollable list layout ──────────────────────────────────────────
    private static final float CARD_W = 700f;
    private static final float CARD_H = 80f;
    private static final float CARD_GAP = 14f;
    private static final float CARD_X = VW / 2f - CARD_W / 2f;

    /** The visible region for scrollable cards (world coordinates). */
    private static final float LIST_TOP = VH - 120f; // below title + subtitle
    private static final float LIST_BOTTOM = 100f; // above BACK button
    private static final float LIST_HEIGHT = LIST_TOP - LIST_BOTTOM;

    /** Current scroll offset (0 = top, positive = scrolled down). */
    private float scrollOffset = 0f;
    /** Smooth scroll target for animated scrolling. */
    private float scrollTarget = 0f;

    // Card colors for unlocked stages
    private static final Color[] STAGE_COLORS = {
            new Color(0.20f, 0.55f, 0.35f, 1f), // Stage 0 — green
            new Color(0.20f, 0.35f, 0.60f, 1f), // Stage 1 — blue
            new Color(0.50f, 0.25f, 0.55f, 1f), // Stage 2 — purple
            new Color(0.55f, 0.30f, 0.20f, 1f), // Stage 3 — amber
            new Color(0.20f, 0.50f, 0.55f, 1f), // Stage 4 — teal
    };

    public StageSelectScreen(Main game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new StretchViewport(VW, VH, camera);
        viewport.apply(true);

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        stages = StageRegistry.getStageList();

        // Determine unlock state from ALL saves.
        highestClearedIndex = SaveManager.getHighestClearedStageIndex();

        // BACK button at the bottom
        float btnW = 200f;
        float btnH = 50f;
        backBtnRect = new Rectangle(VW / 2f - btnW / 2f, 30f, btnW, btnH);
    }

    /** A stage at 'index' is unlocked for practice if the player has beaten it. */
    private boolean isUnlocked(int index) {
        return highestClearedIndex > index;
    }

    /** Total content height of all cards. */
    private float totalContentHeight() {
        return stages.size() * (CARD_H + CARD_GAP) - CARD_GAP;
    }

    /** Maximum scroll offset (0 if all cards fit). */
    private float maxScroll() {
        return Math.max(0f, totalContentHeight() - LIST_HEIGHT);
    }

    /** Get the Y position of card 'i' in world coords, accounting for scroll. */
    private float cardY(int i) {
        // First card starts at LIST_TOP - CARD_H, each subsequent one below
        return LIST_TOP - CARD_H - (CARD_H + CARD_GAP) * i + scrollOffset;
    }

    /** Ensure the selected stage card is visible by adjusting scrollTarget. */
    private void ensureSelectedVisible() {
        if (selectedIndex >= stages.size())
            return; // BACK button, no scrolling needed

        // Card top and bottom in unscrolled space (relative to LIST_TOP)
        float cardTopOffset = (CARD_H + CARD_GAP) * selectedIndex;
        float cardBotOffset = cardTopOffset + CARD_H;

        // Visible window is [scrollTarget .. scrollTarget + LIST_HEIGHT]
        if (cardTopOffset < scrollTarget) {
            scrollTarget = cardTopOffset;
        }
        if (cardBotOffset > scrollTarget + LIST_HEIGHT) {
            scrollTarget = cardBotOffset - LIST_HEIGHT;
        }
        scrollTarget = MathUtils.clamp(scrollTarget, 0f, maxScroll());
    }

    @Override
    public void render(float delta) {
        animTime += delta;

        // Smooth scroll animation
        scrollOffset = MathUtils.lerp(scrollOffset, scrollTarget, Math.min(1f, delta * 12f));

        Gdx.gl.glClearColor(0.04f, 0.03f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        handleInput();
        drawBackground();
        drawTitle();
        drawStageCardsClipped();
        drawBackButton();
    }

    // ── Input ────────────────────────────────────────────────────────────

    private void handleInput() {
        int totalItems = stages.size() + 1;
        int backIndex = stages.size();

        // Keyboard navigation
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selectedIndex = (selectedIndex - 1 + totalItems) % totalItems;
            ensureSelectedVisible();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selectedIndex = (selectedIndex + 1) % totalItems;
            ensureSelectedVisible();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (isTransitioning) return;
            isTransitioning = true;
            game.transitionToScreen(new MainMenuScreen(game));
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            selectCurrent();
            return;
        }

        // Mouse hover + click
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse, viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight());
        float mx = mouse.x, my = mouse.y;

        // Only hover over cards that are within the visible list area
        if (my >= LIST_BOTTOM && my <= LIST_TOP) {
            for (int i = 0; i < stages.size(); i++) {
                float cy = cardY(i);
                if (mx >= CARD_X && mx <= CARD_X + CARD_W
                        && my >= cy && my <= cy + CARD_H) {
                    selectedIndex = i;
                }
            }
        }
        if (backBtnRect.contains(mx, my)) {
            selectedIndex = backIndex;
        }

        if (Gdx.input.justTouched()) {
            // Only click cards within the visible area
            if (my >= LIST_BOTTOM && my <= LIST_TOP) {
                for (int i = 0; i < stages.size(); i++) {
                    float cy = cardY(i);
                    if (mx >= CARD_X && mx <= CARD_X + CARD_W
                            && my >= cy && my <= cy + CARD_H) {
                        selectedIndex = i;
                        selectCurrent();
                        return;
                    }
                }
            }
            if (backBtnRect.contains(mx, my)) {
                if (isTransitioning) return;
                isTransitioning = true;
                game.transitionToScreen(new MainMenuScreen(game));
            }
        }
    }

    private void selectCurrent() {
        int backIndex = stages.size();

        if (selectedIndex == backIndex) {
            if (isTransitioning) return;
            isTransitioning = true;
            game.transitionToScreen(new MainMenuScreen(game));
            return;
        }

        if (!isUnlocked(selectedIndex)) {
            System.out.println("[StageSelect] Stage " + selectedIndex + " is locked.");
            return;
        }

        System.out.println("[StageSelect] Starting PRACTICE at stage " + selectedIndex);
        if (isTransitioning) return;
        isTransitioning = true;
        game.transitionToScreen(new BattleScreen(game, selectedIndex));
    }

    // ── Drawing ──────────────────────────────────────────────────────────

    private void drawBackground() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 3; i++) {
            float cx = VW * (0.25f + 0.25f * i) + MathUtils.sin(animTime * 0.4f + i) * 30f;
            float cy = VH * 0.5f + MathUtils.cos(animTime * 0.3f + i * 1.5f) * 50f;
            shapeRenderer.setColor(0.15f, 0.10f, 0.35f, 0.06f);
            shapeRenderer.circle(cx, cy, 250f);
        }
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawTitle() {
        batch.begin();
        font.getData().setScale(2.5f);
        font.setColor(1f, 0.85f, 0.2f, 1f);
        GlyphLayout title = new GlyphLayout(font, "PRACTICE MODE");
        font.draw(batch, "PRACTICE MODE", VW / 2f - title.width / 2f, VH - 30f);

        // Subtitle explaining the mode
        font.getData().setScale(1.0f);
        boolean hasAnythingUnlocked = highestClearedIndex > 0;
        if (hasAnythingUnlocked) {
            font.setColor(0.5f, 0.55f, 0.65f, 0.8f);
            String sub = "Select a cleared stage to practice. Heroes start at full HP/Mana. Saving disabled.";
            GlyphLayout subLayout = new GlyphLayout(font, sub);
            font.draw(batch, sub, VW / 2f - subLayout.width / 2f, VH - 80f);
        } else {
            font.setColor(0.7f, 0.45f, 0.3f, 0.9f);
            String sub = "Beat stages in New Game to unlock them for practice!";
            GlyphLayout subLayout = new GlyphLayout(font, sub);
            font.draw(batch, sub, VW / 2f - subLayout.width / 2f, VH - 80f);
        }
        batch.end();
    }

    /**
     * Draw stage cards inside a clipped (scissored) region so they
     * never overlap with the title or the BACK button.
     */
    private void drawStageCardsClipped() {
        // Convert list region from world coords to screen coords for glScissor
        // StretchViewport maps world(0..VW, 0..VH) → screen
        float scaleX = viewport.getScreenWidth() / VW;
        float scaleY = viewport.getScreenHeight() / VH;
        int sx = (int) (viewport.getScreenX() + CARD_X * scaleX) - 20; // slight padding
        int sy = (int) (viewport.getScreenY() + LIST_BOTTOM * scaleY);
        int sw = (int) ((CARD_W + 40) * scaleX); // slight padding
        int sh = (int) (LIST_HEIGHT * scaleY);

        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(sx, sy, sw, sh);

        for (int i = 0; i < stages.size(); i++) {
            float cy = cardY(i);

            // Skip cards that are fully outside the visible area (optimization)
            if (cy + CARD_H < LIST_BOTTOM - 10 || cy > LIST_TOP + 10)
                continue;

            StageData stage = stages.get(i);
            boolean unlocked = isUnlocked(i);
            boolean selected = (i == selectedIndex);

            drawStageCard(CARD_X, cy, i, stage, unlocked, selected);
        }

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    private void drawStageCard(float x, float y, int index, StageData stage,
            boolean unlocked, boolean selected) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Card background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (!unlocked) {
            shapeRenderer.setColor(0.10f, 0.08f, 0.15f, 0.6f);
        } else if (selected) {
            Color stageColor = STAGE_COLORS[index % STAGE_COLORS.length];
            float pulse = 0.85f + 0.15f * MathUtils.sin(animTime * 3f);
            shapeRenderer.setColor(stageColor.r * pulse, stageColor.g * pulse,
                    stageColor.b * pulse, 0.9f);
        } else {
            Color stageColor = STAGE_COLORS[index % STAGE_COLORS.length];
            shapeRenderer.setColor(stageColor.r * 0.5f, stageColor.g * 0.5f,
                    stageColor.b * 0.5f, 0.7f);
        }
        shapeRenderer.rect(x, y, CARD_W, CARD_H);
        shapeRenderer.end();

        // Selection highlight border
        if (selected && unlocked) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1f, 0.85f, 0.2f, 0.9f);
            shapeRenderer.rect(x - 2, y - 2, CARD_W + 4, CARD_H + 4);
            shapeRenderer.end();
        }

        // Card border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        if (unlocked) {
            shapeRenderer.setColor(0.4f, 0.35f, 0.6f, 0.5f);
        } else {
            shapeRenderer.setColor(0.2f, 0.18f, 0.3f, 0.4f);
        }
        shapeRenderer.rect(x, y, CARD_W, CARD_H);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Text
        batch.begin();
        float textY = y + CARD_H / 2f + 8f;

        // Stage number
        font.getData().setScale(1.5f);
        if (unlocked) {
            font.setColor(1f, 0.95f, 0.8f, 1f);
        } else {
            font.setColor(0.35f, 0.30f, 0.40f, 0.6f);
        }
        String stageNum = "Stage " + (index + 1);
        font.draw(batch, stageNum, x + 20f, textY);

        // Stage name
        font.getData().setScale(1.2f);
        if (unlocked) {
            font.setColor(0.85f, 0.85f, 0.9f, 0.9f);
        } else {
            font.setColor(0.3f, 0.28f, 0.35f, 0.5f);
        }
        String displayName = (stage.stageName != null && !stage.stageName.isEmpty())
                ? stage.stageName
                : stage.stageId;
        font.draw(batch, displayName, x + 160f, textY);

        // Enemy count
        font.getData().setScale(1.0f);
        if (unlocked) {
            font.setColor(0.7f, 0.75f, 0.8f, 0.8f);
        } else {
            font.setColor(0.25f, 0.25f, 0.3f, 0.4f);
        }
        int enemyCount = (stage.enemies != null) ? stage.enemies.length : 0;
        String enemyText = enemyCount + " enem" + (enemyCount == 1 ? "y" : "ies");
        font.draw(batch, enemyText, x + 480f, textY);

        // Status indicator (right side)
        font.getData().setScale(1.1f);
        if (!unlocked) {
            font.setColor(0.5f, 0.3f, 0.3f, 0.7f);
            String lockText = "LOCKED";
            GlyphLayout lockLayout = new GlyphLayout(font, lockText);
            font.draw(batch, lockText, x + CARD_W - lockLayout.width - 20f, textY);
        } else {
            font.setColor(0.3f, 0.85f, 0.9f, 0.9f);
            String practiceText = "PRACTICE";
            GlyphLayout practiceLayout = new GlyphLayout(font, practiceText);
            font.draw(batch, practiceText, x + CARD_W - practiceLayout.width - 20f, textY);
        }

        batch.end();
    }

    private void drawBackButton() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        boolean selected = (selectedIndex == stages.size());

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (selected) {
            float pulse = 0.35f + 0.08f * MathUtils.sin(animTime * 3f);
            shapeRenderer.setColor(0.25f + pulse * 0.1f, 0.15f, 0.45f, 0.85f);
        } else {
            shapeRenderer.setColor(0.15f, 0.10f, 0.28f, 0.6f);
        }
        shapeRenderer.rect(backBtnRect.x, backBtnRect.y, backBtnRect.width, backBtnRect.height);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.5f, 0.4f, 0.8f, 0.6f);
        shapeRenderer.rect(backBtnRect.x, backBtnRect.y, backBtnRect.width, backBtnRect.height);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.getData().setScale(1.4f);
        font.setColor(selected ? new Color(1f, 0.95f, 0.7f, 1f) : new Color(0.6f, 0.55f, 0.5f, 0.8f));
        GlyphLayout gl = new GlyphLayout(font, "BACK");
        font.draw(batch, "BACK",
                backBtnRect.x + backBtnRect.width / 2f - gl.width / 2f,
                backBtnRect.y + backBtnRect.height / 2f + gl.height / 2f);
        batch.end();
    }

    // ── Screen lifecycle ─────────────────────────────────────────────────

    @Override
    public void show() {
        // Set up an InputAdapter to capture mouse wheel scroll events
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                // amountY > 0 = scroll down, amountY < 0 = scroll up
                scrollTarget += amountY * (CARD_H + CARD_GAP);
                scrollTarget = MathUtils.clamp(scrollTarget, 0f, maxScroll());
                return true;
            }
        });
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
