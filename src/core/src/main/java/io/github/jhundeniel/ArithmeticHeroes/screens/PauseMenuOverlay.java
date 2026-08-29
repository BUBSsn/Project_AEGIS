package io.github.jhundeniel.ArithmeticHeroes.screens;

import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.VolumeSettings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.OrthographicCamera;

/**
 * Semi-transparent pause overlay drawn on top of the battle.
 * Provides "Resume", "Save & Exit", and "Exit to Menu" buttons.
 *
 * <p>
 * Usage: call {@link #render(OrthographicCamera)} every frame when paused.
 * Call {@link #handleInput(OrthographicCamera)} to detect clicks.
 * </p>
 */
public class PauseMenuOverlay {

    public enum PauseAction {
        NONE, RESUME, SAVE, EXIT_TO_MENU
    }

    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;

    // Button layout — 3 buttons (positioned dynamically relative to camera)
    private final Rectangle resumeBtn = new Rectangle();
    private final Rectangle saveExitBtn = new Rectangle();
    private final Rectangle exitBtn = new Rectangle();
    private int selectedOption = 0; // 0 = Resume, 1 = Save, 2 = Exit
    private float animTime = 0f;

    // ── Volume sliders ────────────────────────────────────────────────
    private static final float SLIDER_W     = 180f;
    private static final float SLIDER_H     = 6f;
    private static final float SLIDER_KNOB  = 8f;
    private static final float SLIDER_GAP   = 28f;

    private final Rectangle musicSliderTrack = new Rectangle();
    private final Rectangle sfxSliderTrack   = new Rectangle();
    private boolean draggingMusic = false;
    private boolean draggingSfx   = false;

    // ── Save notification ──────────────────────────────────────────
    private float saveNotifyTimer = 0f;
    private static final float SAVE_NOTIFY_DURATION = 2.0f;

    /**
     * Whether the "SAVE & EXIT" button is enabled (only when state ==
     * WAIT_FOR_INPUT).
     */
    private boolean canSave;

    public PauseMenuOverlay(ArithmeticAssetManager assets) {
        this(true, assets); // default: can save
    }

    /**
     * @param canSave true if the game state allows saving (WAIT_FOR_INPUT)
     */
    public PauseMenuOverlay(boolean canSave, ArithmeticAssetManager assets) {
        this.canSave = canSave;
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
    }

    /** Recalculate button positions based on the camera's actual viewport size. */
    private void layoutButtons(OrthographicCamera camera) {
        float vw = camera.viewportWidth;
        float vh = camera.viewportHeight;

        float btnW = 280f;
        float btnH = 60f;
        float gap = 24f;
        float cx = vw / 2f - btnW / 2f;
        float totalH = btnH * 3 + gap * 2;
        float topY = vh / 2f + totalH / 2f - btnH;

        resumeBtn.set(cx, topY, btnW, btnH);
        saveExitBtn.set(cx, topY - btnH - gap, btnW, btnH);
        exitBtn.set(cx, topY - (btnH + gap) * 2, btnW, btnH);

        // Volume sliders — below the exit button
        float sliderX = vw / 2f - SLIDER_W / 2f;
        float sliderBaseY = exitBtn.y - 52f;
        musicSliderTrack.set(sliderX, sliderBaseY, SLIDER_W, SLIDER_H);
        sfxSliderTrack.set(sliderX, sliderBaseY + SLIDER_GAP, SLIDER_W, SLIDER_H);
    }

    /** Update whether "SAVE & EXIT" is available (call before rendering). */
    public void setCanSave(boolean canSave, ArithmeticAssetManager assets) {
        this.canSave = canSave;
        if (!canSave && selectedOption == 1) {
            selectedOption = 0;
            if (assets != null)
                assets.playSound(ArithmeticAssetManager.SFX_BUTTON_HOVER);
        }
    }

    /**
     * Draw the pause overlay (dark tint + buttons).
     * Call this AFTER the battle frame has been rendered.
     */
    public void render(OrthographicCamera camera, ArithmeticAssetManager assets) {
        animTime += Gdx.graphics.getDeltaTime();
        layoutButtons(camera);

        float vw = camera.viewportWidth;
        float vh = camera.viewportHeight;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        // ── Dark overlay (covers entire viewport) ───────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.65f);
        shapeRenderer.rect(0, 0, vw, vh);
        shapeRenderer.end();

        // ── Panel background ────────────────────────────────
        float panelW = 360f, panelH = 400f;
        float panelX = vw / 2f - panelW / 2f;
        float panelY = vh / 2f - panelH / 2f + 20f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.08f, 0.06f, 0.16f, 0.92f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.end();

        // ── Panel border ────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.6f, 0.4f, 1.0f, 0.8f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.end();

        // ── Buttons ─────────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawButtonBg(resumeBtn, selectedOption == 0, true);
        drawButtonBg(saveExitBtn, selectedOption == 1, canSave);
        drawButtonBg(exitBtn, selectedOption == 2, true);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── Volume sliders ──────────────────────────────────────
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        drawPauseSlider(musicSliderTrack, VolumeSettings.getInstance().getMusicVolume(),
                new Color(0.4f, 0.7f, 1.0f, 1f), draggingMusic);
        drawPauseSlider(sfxSliderTrack, VolumeSettings.getInstance().getSfxVolume(),
                new Color(1.0f, 0.7f, 0.3f, 1f), draggingSfx);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── Text ────────────────────────────────────────────
        com.badlogic.gdx.graphics.g2d.SpriteBatch textBatch = new com.badlogic.gdx.graphics.g2d.SpriteBatch();
        textBatch.setProjectionMatrix(camera.combined);
        textBatch.begin();

        // Title
        font.getData().setScale(2.2f);
        font.setColor(1f, 0.9f, 0.4f, 1f);
        GlyphLayout titleLayout = new GlyphLayout(font, "PAUSED");
        float titleX = vw / 2f - titleLayout.width / 2f;
        float titleY = panelY + panelH - 20f;
        font.draw(textBatch, "PAUSED", titleX, titleY);

        // Button labels
        drawButtonText(textBatch, resumeBtn, "RESUME", selectedOption == 0, true);
        drawButtonText(textBatch, saveExitBtn, "SAVE", selectedOption == 1, canSave);
        drawButtonText(textBatch, exitBtn, "EXIT TO MENU", selectedOption == 2, true);

        // Volume slider labels
        VolumeSettings vol = VolumeSettings.getInstance();
        font.getData().setScale(0.85f);

        font.setColor(0.65f, 0.75f, 1.0f, 0.9f);
        GlyphLayout ml = new GlyphLayout(font, "Music");
        font.draw(textBatch, "Music",
                musicSliderTrack.x - ml.width - 8f,
                musicSliderTrack.y + SLIDER_H / 2f + ml.height / 2f);
        font.setColor(0.5f, 0.6f, 0.85f, 0.7f);
        font.draw(textBatch, Math.round(vol.getMusicVolume() * 100) + "%",
                musicSliderTrack.x + SLIDER_W + 8f,
                musicSliderTrack.y + SLIDER_H / 2f + ml.height / 2f);

        font.setColor(1.0f, 0.8f, 0.45f, 0.9f);
        GlyphLayout sl = new GlyphLayout(font, "SFX");
        font.draw(textBatch, "SFX",
                sfxSliderTrack.x - sl.width - 8f,
                sfxSliderTrack.y + SLIDER_H / 2f + sl.height / 2f);
        font.setColor(0.85f, 0.65f, 0.3f, 0.7f);
        font.draw(textBatch, Math.round(vol.getSfxVolume() * 100) + "%",
                sfxSliderTrack.x + SLIDER_W + 8f,
                sfxSliderTrack.y + SLIDER_H / 2f + sl.height / 2f);

        textBatch.end();
        textBatch.dispose();

        // ── Save notification banner ────────────────────────
        if (saveNotifyTimer > 0f) {
            saveNotifyTimer -= Gdx.graphics.getDeltaTime();
            float alpha = Math.min(1f, saveNotifyTimer / 0.3f); // fade out in last 0.3s

            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.1f, 0.5f, 0.2f, 0.85f * alpha);
            float notifW = 240f, notifH = 40f;
            float notifX = vw / 2f - notifW / 2f;
            float notifY = panelY - notifH - 16f;
            shapeRenderer.rect(notifX, notifY, notifW, notifH);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            com.badlogic.gdx.graphics.g2d.SpriteBatch notifBatch = new com.badlogic.gdx.graphics.g2d.SpriteBatch();
            notifBatch.setProjectionMatrix(camera.combined);
            notifBatch.begin();
            font.getData().setScale(1.4f);
            font.setColor(0.6f, 1f, 0.6f, alpha);
            GlyphLayout gl = new GlyphLayout(font, "Game Saved!");
            font.draw(notifBatch, "Game Saved!",
                    notifX + notifW / 2f - gl.width / 2f,
                    notifY + notifH / 2f + gl.height / 2f);
            notifBatch.end();
            notifBatch.dispose();
        }
    }

    /**
     * Check for keyboard/mouse input and return the chosen action.
     */
    public PauseAction handleInput(OrthographicCamera camera, ArithmeticAssetManager assets) {
        int oldSelectedOption = selectedOption;

        // Keyboard navigation
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP)
                || Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.W)) {
            selectedOption = (selectedOption - 1 + 3) % 3;
            // Skip disabled SAVE & EXIT
            if (selectedOption == 1 && !canSave)
                selectedOption = 0;
        }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN)
                || Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.S)) {
            selectedOption = (selectedOption + 1) % 3;
            // Skip disabled SAVE & EXIT
            if (selectedOption == 1 && !canSave)
                selectedOption = 2;
        }

        if (oldSelectedOption != selectedOption) {
            if (assets != null)
                assets.playSound(ArithmeticAssetManager.SFX_BUTTON_HOVER);
        }

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)) {
            if (assets != null)
                assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
            switch (selectedOption) {
                case 0:
                    return PauseAction.RESUME;
                case 1:
                    return canSave ? PauseAction.SAVE : PauseAction.NONE;
                case 2:
                    return PauseAction.EXIT_TO_MENU;
            }
        }

        // Mouse hover + click
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);
        float mx = mouse.x, my = mouse.y;

        if (resumeBtn.contains(mx, my))
            selectedOption = 0;
        else if (saveExitBtn.contains(mx, my)) {
            if (canSave)
                selectedOption = 1;
        } else if (exitBtn.contains(mx, my))
            selectedOption = 2;

        if (oldSelectedOption != selectedOption && Gdx.input.getDeltaX() != 0) {
            if (assets != null)
                assets.playSound(ArithmeticAssetManager.SFX_BUTTON_HOVER);
        }

        if (Gdx.input.justTouched()) {
            if (resumeBtn.contains(mx, my)) {
                if (assets != null)
                    assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                return PauseAction.RESUME;
            }
            if (saveExitBtn.contains(mx, my) && canSave) {
                if (assets != null)
                    assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                return PauseAction.SAVE;
            }
            if (exitBtn.contains(mx, my)) {
                if (assets != null)
                    assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                return PauseAction.EXIT_TO_MENU;
            }
        }

        return PauseAction.NONE;
    }

    // ── Volume slider interaction ─────────────────────────────────────────

    /**
     * Must be called each frame when the pause overlay is active.
     * Handles slider dragging independently from button input.
     */
    public void updateSliders(OrthographicCamera camera, ArithmeticAssetManager assets) {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);
        float mx = mouse.x, my = mouse.y;

        if (Gdx.input.justTouched()) {
            if (isOnSlider(musicSliderTrack, mx, my)) {
                draggingMusic = true;
                applySlider(musicSliderTrack, mx, true, assets);
            } else if (isOnSlider(sfxSliderTrack, mx, my)) {
                draggingSfx = true;
                applySlider(sfxSliderTrack, mx, false, assets);
            }
        }

        if (draggingMusic || draggingSfx) {
            if (Gdx.input.isTouched()) {
                if (draggingMusic) applySlider(musicSliderTrack, mx, true, assets);
                if (draggingSfx)   applySlider(sfxSliderTrack, mx, false, assets);
            } else {
                draggingMusic = false;
                draggingSfx   = false;
                VolumeSettings.getInstance().save();
            }
        }
    }

    private boolean isOnSlider(Rectangle track, float mx, float my) {
        float expand = SLIDER_KNOB + 6f;
        return mx >= track.x - expand && mx <= track.x + track.width + expand
                && my >= track.y - expand && my <= track.y + track.height + expand;
    }

    private void applySlider(Rectangle track, float mx, boolean isMusic,
                             ArithmeticAssetManager assets) {
        float normalized = Math.max(0f, Math.min(1f, (mx - track.x) / track.width));
        VolumeSettings vol = VolumeSettings.getInstance();
        if (isMusic) {
            vol.setMusicVolume(normalized);
            if (assets != null) assets.applyMusicVolume();
        } else {
            vol.setSfxVolume(normalized);
        }
    }

    /** Returns true if either volume slider is being dragged. */
    public boolean isDraggingSlider() {
        return draggingMusic || draggingSfx;
    }

    // ── Drawing helpers ──────────────────────────────────────────────────

    private void drawButtonBg(Rectangle rect, boolean selected, boolean enabled) {
        if (!enabled) {
            shapeRenderer.setColor(0.10f, 0.08f, 0.18f, 0.5f);
        } else if (selected) {
            float pulse = 0.22f + 0.03f * MathUtils.sin(animTime * 4f);
            shapeRenderer.setColor(0.35f, 0.18f, 0.65f, 0.85f);
            shapeRenderer.rect(rect.x - 3, rect.y - 3, rect.width + 6, rect.height + 6);
            shapeRenderer.setColor(0.50f, 0.28f, 0.90f, 0.9f);
        } else {
            shapeRenderer.setColor(0.14f, 0.10f, 0.28f, 0.8f);
        }
        shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
    }

    private void drawButtonText(com.badlogic.gdx.graphics.g2d.SpriteBatch batch,
            Rectangle rect, String label, boolean selected, boolean enabled) {
        if (!enabled) {
            font.getData().setScale(1.3f);
            font.setColor(0.4f, 0.35f, 0.3f, 0.5f);
        } else {
            font.getData().setScale(selected ? 1.5f : 1.3f);
            font.setColor(selected ? new Color(1f, 0.95f, 0.6f, 1f) : new Color(0.7f, 0.65f, 0.55f, 1f));
        }
        GlyphLayout gl = new GlyphLayout(font, label);
        font.draw(batch, label,
                rect.x + rect.width / 2f - gl.width / 2f,
                rect.y + rect.height / 2f + gl.height / 2f);
    }

    private void drawPauseSlider(Rectangle track, float value, Color accent, boolean active) {
        float knobX = track.x + track.width * value;
        float centerY = track.y + track.height / 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Track bg
        shapeRenderer.setColor(0.12f, 0.10f, 0.22f, 0.85f);
        shapeRenderer.rect(track.x, track.y, track.width, track.height);
        // Filled
        shapeRenderer.setColor(accent.r, accent.g, accent.b, 0.75f);
        shapeRenderer.rect(track.x, track.y, track.width * value, track.height);
        shapeRenderer.end();

        // Knob
        float kr = active ? SLIDER_KNOB * 1.25f : SLIDER_KNOB;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(accent);
        shapeRenderer.circle(knobX, centerY, kr);
        shapeRenderer.setColor(1f, 1f, 1f, 0.35f);
        shapeRenderer.circle(knobX - 1f, centerY + 1f, kr * 0.4f);
        shapeRenderer.end();
    }

    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
    }

    /** Trigger the "Game Saved!" notification banner. */
    public void showSaveNotification() {
        saveNotifyTimer = SAVE_NOTIFY_DURATION;
    }
}
