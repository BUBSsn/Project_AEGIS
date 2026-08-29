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
import io.github.jhundeniel.ArithmeticHeroes.data.SaveData;
import io.github.jhundeniel.ArithmeticHeroes.managers.SaveManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.StageRegistry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Screen listing all available save slots. The player can select a
 * save to load, delete a save, or go back to the main menu.
 */
public class LoadGameScreen implements Screen {

    private static final float VW = 1280f;
    private static final float VH = 720f;

    private final Main game;
    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final BitmapFont font;

    private List<SaveData> saves;
    private int selectedIndex = 0; // 0..saves.size() = save slots, saves.size() = BACK button

    private final Rectangle backBtnRect;
    private float animTime = 0f;
    private boolean isTransitioning = false;

    // Row layout
    private static final float TABLE_X = 140f;
    private static final float TABLE_W = 1000f;
    private static final float ROW_H = 70f;
    private static final float TABLE_TOP = VH - 140f;

    // Delete confirmation
    private boolean confirmingDelete = false;
    private int deleteTarget = -1;

    public LoadGameScreen(Main game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new StretchViewport(VW, VH, camera);
        viewport.apply(true);

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        backBtnRect = new Rectangle(VW / 2f - 100f, 30f, 200f, 50f);

        refreshSaves();
    }

    private void refreshSaves() {
        saves = SaveManager.getAvailableSaves();
        // Clamp selection
        int maxIndex = saves.size(); // saves.size() == BACK button
        if (selectedIndex > maxIndex)
            selectedIndex = maxIndex;
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
        drawTitle();
        drawSaveSlots();
        drawBackButton();

        if (confirmingDelete) {
            drawDeleteConfirmation();
        }
    }

    // ── Input ────────────────────────────────────────────────────────────

    private void handleInput() {
        if (confirmingDelete) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) {
                if (deleteTarget >= 0 && deleteTarget < saves.size()) {
                    SaveManager.deleteSave(saves.get(deleteTarget).slotId);
                    refreshSaves();
                }
                confirmingDelete = false;
                deleteTarget = -1;
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.N)
                    || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                confirmingDelete = false;
                deleteTarget = -1;
            }
            return;
        }

        int maxIndex = saves.size(); // BACK is the last option

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selectedIndex = (selectedIndex - 1 + maxIndex + 1) % (maxIndex + 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selectedIndex = (selectedIndex + 1) % (maxIndex + 1);
        }

        // ENTER = load selected save (or BACK)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            confirmSelection();
        }

        // DELETE key = delete selected save
        if (Gdx.input.isKeyJustPressed(Input.Keys.DEL) || Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)) {
            if (selectedIndex < saves.size()) {
                deleteTarget = selectedIndex;
                confirmingDelete = true;
            }
        }

        // ESC = back to menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (isTransitioning) return;
            isTransitioning = true;
            game.transitionToScreen(new MainMenuScreen(game));
            return;
        }

        // Mouse hover + click
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse, viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight());
        float mx = mouse.x, my = mouse.y;

        // Check save rows
        for (int i = 0; i < saves.size(); i++) {
            float rowY = TABLE_TOP - ROW_H * (i + 1);
            Rectangle rowRect = new Rectangle(TABLE_X, rowY, TABLE_W, ROW_H);
            if (rowRect.contains(mx, my)) {
                selectedIndex = i;
            }
        }
        if (backBtnRect.contains(mx, my)) {
            selectedIndex = saves.size();
        }

        if (Gdx.input.justTouched()) {
            // Check delete buttons FIRST (they overlap with row rects)
            for (int i = 0; i < saves.size(); i++) {
                float rowY = TABLE_TOP - ROW_H * (i + 1);
                Rectangle delRect = new Rectangle(TABLE_X + TABLE_W - 100f, rowY + 10f, 80f, ROW_H - 20f);
                if (delRect.contains(mx, my)) {
                    deleteTarget = i;
                    confirmingDelete = true;
                    return;
                }
            }
            // Then check row clicks (load) — skip completed saves
            for (int i = 0; i < saves.size(); i++) {
                float rowY = TABLE_TOP - ROW_H * (i + 1);
                Rectangle rowRect = new Rectangle(TABLE_X, rowY, TABLE_W - 110f, ROW_H); // exclude DEL area
                if (rowRect.contains(mx, my)) {
                    selectedIndex = i;
                    if (!isCompleted(saves.get(i))) {
                        confirmSelection();
                    }
                    return;
                }
            }
            if (backBtnRect.contains(mx, my)) {
                if (isTransitioning) return;
                isTransitioning = true;
                game.transitionToScreen(new MainMenuScreen(game));
            }
        }
    }

    /**
     * Returns true if this save represents a finished game that can't be resumed.
     */
    private boolean isCompleted(SaveData save) {
        return save.currentStageIndex >= StageRegistry.getStageCount();
    }

    private void confirmSelection() {
        if (selectedIndex < saves.size()) {
            SaveData save = saves.get(selectedIndex);
            if (isCompleted(save)) {
                // Completed games cannot be loaded — only deleted
                return;
            }
            SaveData loaded = SaveManager.load(save.slotId);
            if (loaded != null) {
                if (isTransitioning) return;
                isTransitioning = true;
                game.transitionToScreen(new BattleScreen(game, loaded));
            } else {
                System.err.println("[LoadGameScreen] Failed to load slot: " + save.slotId);
            }
        } else {
            // BACK
            if (isTransitioning) return;
            isTransitioning = true;
            game.transitionToScreen(new MainMenuScreen(game));
        }
    }

    // ── Drawing ──────────────────────────────────────────────────────────

    private void drawBackground() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 4; i++) {
            float cx = VW * (0.2f + 0.2f * i) + MathUtils.sin(animTime * 0.5f + i) * 40f;
            float cy = VH * 0.5f + MathUtils.cos(animTime * 0.3f + i * 2) * 60f;
            float alpha = 0.04f + 0.02f * MathUtils.sin(animTime + i);
            shapeRenderer.setColor(0.2f, 0.15f, 0.5f, alpha);
            shapeRenderer.circle(cx, cy, 200f);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawTitle() {
        batch.begin();
        font.getData().setScale(2.8f);
        font.setColor(1f, 0.85f, 0.2f, 1f);
        GlyphLayout titleLayout = new GlyphLayout(font, "LOAD GAME");
        font.draw(batch, "LOAD GAME", VW / 2f - titleLayout.width / 2f, VH - 40f);

        font.getData().setScale(1.0f);
        font.setColor(0.5f, 0.55f, 0.65f, 0.8f);
        String subtitle = saves.isEmpty()
                ? "No saves found. Start a new game first!"
                : "Select a save to continue. Press DEL to delete.";
        GlyphLayout subLayout = new GlyphLayout(font, subtitle);
        font.draw(batch, subtitle, VW / 2f - subLayout.width / 2f, VH - 90f);
        batch.end();
    }

    private void drawSaveSlots() {
        if (saves.isEmpty())
            return;

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm");

        for (int i = 0; i < saves.size(); i++) {
            SaveData save = saves.get(i);
            float rowY = TABLE_TOP - ROW_H * (i + 1);
            boolean selected = (selectedIndex == i);
            boolean completed = isCompleted(save);

            // Row background
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            if (completed) {
                // Dimmed background for completed saves
                float bgAlpha = (i % 2 == 0) ? 0.10f : 0.07f;
                shapeRenderer.setColor(0.08f, 0.06f, 0.12f, bgAlpha);
            } else if (selected) {
                float pulse = 0.15f + 0.03f * MathUtils.sin(animTime * 4f);
                shapeRenderer.setColor(0.30f, 0.18f, 0.55f, 0.7f + pulse);
            } else {
                float bgAlpha = (i % 2 == 0) ? 0.15f : 0.10f;
                shapeRenderer.setColor(0.10f, 0.08f, 0.20f, bgAlpha);
            }
            shapeRenderer.rect(TABLE_X, rowY, TABLE_W, ROW_H);

            // Status indicator bar (left edge)
            if (completed) {
                shapeRenderer.setColor(0.4f, 0.4f, 0.45f, 0.5f); // grey = completed
            } else if (save.midBattle) {
                shapeRenderer.setColor(1.0f, 0.6f, 0.2f, 0.8f); // orange = mid-battle
            } else {
                shapeRenderer.setColor(0.3f, 0.9f, 0.4f, 0.8f); // green = stage clear
            }
            shapeRenderer.rect(TABLE_X, rowY, 6f, ROW_H);

            // Delete button background
            shapeRenderer.setColor(0.5f, 0.15f, 0.15f, 0.6f);
            shapeRenderer.rect(TABLE_X + TABLE_W - 100f, rowY + 10f, 80f, ROW_H - 20f);

            shapeRenderer.end();

            // Row border
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            if (completed) {
                shapeRenderer.setColor(0.2f, 0.2f, 0.25f, 0.3f);
            } else if (selected) {
                shapeRenderer.setColor(0.6f, 0.4f, 1.0f, 0.8f);
            } else {
                shapeRenderer.setColor(0.25f, 0.2f, 0.4f, 0.3f);
            }
            shapeRenderer.rect(TABLE_X, rowY, TABLE_W, ROW_H);
            shapeRenderer.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);

            // Text
            batch.begin();
            float textY = rowY + ROW_H / 2f + 8f;

            // Display name (dimmed for completed saves)
            font.getData().setScale(1.5f);
            if (completed) {
                font.setColor(0.45f, 0.45f, 0.50f, 0.6f);
            } else {
                font.setColor(selected ? new Color(1f, 0.95f, 0.6f, 1f) : new Color(0.9f, 0.9f, 0.95f, 1f));
            }
            String name = save.displayName != null ? save.displayName : save.slotId;
            font.draw(batch, name, TABLE_X + 20f, textY);

            // Stage info / Completed label
            font.getData().setScale(1.1f);
            int totalStages = StageRegistry.getStageCount();
            if (completed) {
                font.setColor(0.7f, 0.6f, 0.2f, 0.7f);
                font.draw(batch, "COMPLETED", TABLE_X + 300f, textY);
            } else {
                font.setColor(0.6f, 0.8f, 1.0f, 1f);
                String stageInfo = "Stage " + (save.currentStageIndex + 1) + "/" + totalStages;
                if (save.midBattle)
                    stageInfo += " (In Battle)";
                font.draw(batch, stageInfo, TABLE_X + 300f, textY);
            }

            // Timestamp
            font.getData().setScale(1.0f);
            font.setColor(completed ? new Color(0.35f, 0.38f, 0.42f, 0.5f)
                    : new Color(0.5f, 0.55f, 0.65f, 0.8f));
            String timeStr = save.timestamp > 0 ? sdf.format(new Date(save.timestamp)) : "---";
            font.draw(batch, timeStr, TABLE_X + 600f, textY);

            // Heroes alive count
            font.setColor(completed ? new Color(0.3f, 0.5f, 0.35f, 0.5f)
                    : new Color(0.4f, 0.9f, 0.5f, 1f));
            font.draw(batch, save.heroes.size() + " Heroes", TABLE_X + 780f, textY);

            // Delete label
            font.getData().setScale(0.9f);
            font.setColor(0.9f, 0.4f, 0.4f, 0.9f);
            GlyphLayout delLayout = new GlyphLayout(font, "DEL");
            font.draw(batch, "DEL",
                    TABLE_X + TABLE_W - 100f + 40f - delLayout.width / 2f,
                    rowY + ROW_H / 2f + delLayout.height / 2f);

            batch.end();
        }
    }

    private void drawBackButton() {
        boolean selected = (selectedIndex == saves.size());

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (selected) {
            float pulse = 0.3f + 0.05f * MathUtils.sin(animTime * 3f);
            shapeRenderer.setColor(0.35f + pulse * 0.1f, 0.18f, 0.55f, 0.85f);
        } else {
            shapeRenderer.setColor(0.25f, 0.15f, 0.45f, 0.7f);
        }
        shapeRenderer.rect(backBtnRect.x, backBtnRect.y, backBtnRect.width, backBtnRect.height);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.6f, 0.4f, 1.0f, 0.7f);
        shapeRenderer.rect(backBtnRect.x, backBtnRect.y, backBtnRect.width, backBtnRect.height);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.getData().setScale(1.5f);
        font.setColor(selected ? new Color(1f, 0.95f, 0.7f, 1f) : new Color(0.7f, 0.7f, 0.75f, 1f));
        GlyphLayout gl = new GlyphLayout(font, "BACK");
        font.draw(batch, "BACK",
                backBtnRect.x + backBtnRect.width / 2f - gl.width / 2f,
                backBtnRect.y + backBtnRect.height / 2f + gl.height / 2f);
        batch.end();
    }

    private void drawDeleteConfirmation() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
        shapeRenderer.rect(0, 0, VW, VH);
        shapeRenderer.end();

        // Confirmation panel
        float panelW = 500f, panelH = 180f;
        float panelX = VW / 2f - panelW / 2f;
        float panelY = VH / 2f - panelH / 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.12f, 0.08f, 0.22f, 0.95f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.8f, 0.3f, 0.3f, 0.8f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        String saveName = (deleteTarget >= 0 && deleteTarget < saves.size())
                ? saves.get(deleteTarget).displayName
                : "???";

        font.getData().setScale(1.6f);
        font.setColor(1f, 0.6f, 0.6f, 1f);
        GlyphLayout line1 = new GlyphLayout(font, "Delete \"" + saveName + "\"?");
        font.draw(batch, line1, panelX + panelW / 2f - line1.width / 2f, panelY + panelH - 40f);

        font.getData().setScale(1.2f);
        font.setColor(0.7f, 0.7f, 0.8f, 1f);
        GlyphLayout line2 = new GlyphLayout(font, "Press Y to confirm, N to cancel");
        font.draw(batch, line2, panelX + panelW / 2f - line2.width / 2f, panelY + 50f);
        batch.end();
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

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
