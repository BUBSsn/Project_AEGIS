package io.github.jhundeniel.ArithmeticHeroes.screens;

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
import io.github.jhundeniel.ArithmeticHeroes.data.DialogueLine;
import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;
import com.badlogic.gdx.graphics.Texture;

import java.util.List;

/**
 * Semi-transparent overlay that displays a sequence of dialogue lines.
 * Rendered on top of BattleScreen before/after combat.
 *
 * Advances on click, Enter, or Space. Fires a callback when exhausted.
 */
public class DialogueOverlay {

    // ── Callback ──────────────────────────────────────────────────────
    public interface OnCompleteListener {
        void onDialogueComplete();
    }

    // ── State ─────────────────────────────────────────────────────────
    private final List<DialogueLine> lines;
    private int currentIndex = 0;
    private boolean complete = false;
    private OnCompleteListener listener;

    // ── Rendering ─────────────────────────────────────────────────────
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final GlyphLayout layout;

    // ── Animation ─────────────────────────────────────────────────────
    private float animTime = 0f;
    private float fadeAlpha = 0f;
    private static final float FADE_SPEED = 2.5f;

    // ── Text typewriter ───────────────────────────────────────────────
    private float charReveal = 0f;
    private static final float CHARS_PER_SEC = 40f;
    private boolean lineFullyRevealed = false;

    // ── Layout constants (fractions of viewport) ──────────────────────
    private static final float BOX_H_FRAC    = 0.28f;
    private static final float BOX_MARGIN     = 0.03f;
    private static final float NAME_PAD       = 14f;

    // ── Assets ────────────────────────────────────────────────────────
    private final ArithmeticAssetManager assets;

    public DialogueOverlay(List<DialogueLine> lines, ArithmeticAssetManager assets) {
        this.lines  = lines;
        this.assets = assets;

        shapes = new ShapeRenderer();
        batch  = new SpriteBatch();
        font   = new BitmapFont();
        layout = new GlyphLayout();
    }

    public void setOnCompleteListener(OnCompleteListener listener) {
        this.listener = listener;
    }

    public boolean isComplete() { return complete; }

    // ── Update & Render ───────────────────────────────────────────────

    public void render(OrthographicCamera camera) {
        if (complete) return;

        float dt = Gdx.graphics.getDeltaTime();
        animTime += dt;
        fadeAlpha = Math.min(1f, fadeAlpha + dt * FADE_SPEED);

        DialogueLine line = lines.get(currentIndex);
        int totalChars = line.text.length();

        // Typewriter effect
        if (!lineFullyRevealed) {
            charReveal += dt * CHARS_PER_SEC;
            if (charReveal >= totalChars) {
                charReveal = totalChars;
                lineFullyRevealed = true;
            }
        }

        float vw = camera.viewportWidth;
        float vh = camera.viewportHeight;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);

        // 1. Dim overlay
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.02f, 0.06f, 0.55f * fadeAlpha);
        shapes.rect(0, 0, vw, vh);
        shapes.end();

        // 2. Dialogue box
        float boxMargin = vw * BOX_MARGIN;
        float boxW = vw - boxMargin * 2f;
        float boxH = vh * BOX_H_FRAC;
        float boxX = boxMargin;
        float boxY = boxMargin;

        // Box shadow
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.50f * fadeAlpha);
        shapes.rect(boxX + 4f, boxY - 4f, boxW, boxH);
        shapes.end();

        // Box body — dark purple glass
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.06f, 0.04f, 0.14f, 0.92f * fadeAlpha);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();

        // Box border — gold accent
        float borderPulse = 0.50f + 0.20f * MathUtils.sin(animTime * 2.5f);
        drawBorder(boxX, boxY, boxW, boxH, 2f,
            1f, 0.84f, 0.20f, borderPulse * fadeAlpha);

        // Top accent bar
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 0.84f, 0.20f, 0.85f * fadeAlpha);
        shapes.rect(boxX, boxY + boxH - 3f, boxW, 3f);
        shapes.end();

        // Load portrait
        Texture portrait = getPortrait(line.speaker);
        float portW = 0f;
        float portPadding = 0f;

        // 3. Name tag
        boolean isLeft = !"RIGHT".equalsIgnoreCase(line.side);

        if (portrait != null) {
            float maxPortH = boxH - 10f;
            float portRatio = (float)portrait.getWidth() / portrait.getHeight();
            float actualPortH = maxPortH;
            portW = actualPortH * portRatio;
            portPadding = portW + 15f;
            // Removed textPadX adjustment here so we can do it later
        }

        float nameTagPad = 8f;
        float nameScale = vh * 0.0028f;
        font.getData().setScale(nameScale);
        layout.setText(font, line.speaker);
        float nameTagW = layout.width + nameTagPad * 2f + 10f;
        float nameTagH = layout.height + nameTagPad * 2f;
        float nameTagX = isLeft ? boxX + NAME_PAD : boxX + boxW - NAME_PAD - nameTagW;
        float nameTagY = boxY + boxH - 3f; // sits on top of the box

        // Name tag bg
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.12f, 0.08f, 0.28f, 0.95f * fadeAlpha);
        shapes.rect(nameTagX, nameTagY, nameTagW, nameTagH);
        shapes.end();

        // Name tag border
        drawBorder(nameTagX, nameTagY, nameTagW, nameTagH, 1.5f,
            1f, 0.84f, 0.20f, 0.65f * fadeAlpha);

        // 4. Text rendering
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (portrait != null) {
            batch.setColor(1f, 1f, 1f, fadeAlpha);
            float actualPortH = boxH - 10f;
            batch.draw(portrait, boxX + 10f, boxY + 5f, portW, actualPortH);
            batch.setColor(1f, 1f, 1f, 1f);
        }

        // Speaker name
        font.getData().setScale(nameScale);
        font.setColor(1f, 0.90f, 0.50f, fadeAlpha);
        font.draw(batch, line.speaker,
            nameTagX + nameTagPad + 5f,
            nameTagY + nameTagH / 2f + layout.height / 2f);

        // Dialogue text (typewriter)
        float textScale = vh * 0.0024f;
        font.getData().setScale(textScale);

        int revealedChars = (int) charReveal;
        String visibleText = line.text.substring(0, Math.min(revealedChars, totalChars));

        float textPadX = NAME_PAD + 12f;
        if (portrait != null && isLeft) {
            textPadX += portPadding; // shift ONLY the dialogue text, not the name tag
        }

        float textPadTop = 20f;
        float wrapWidth = boxW - textPadX - 20f;

        font.setColor(0.92f, 0.90f, 0.96f, fadeAlpha);
        font.draw(batch, visibleText,
            boxX + textPadX,
            boxY + boxH - textPadTop - 8f,
            wrapWidth, -1, true);

        // "Click to continue" indicator
        if (lineFullyRevealed) {
            float indicatorAlpha = 0.3f + 0.4f * MathUtils.sin(animTime * 3.5f);
            font.getData().setScale(vh * 0.0018f);
            font.setColor(1f, 1f, 1f, indicatorAlpha * fadeAlpha);
            String hint = currentIndex < lines.size() - 1 ? "▶ Click to continue" : "▶ Click to close";
            layout.setText(font, hint);
            font.draw(batch, hint,
                boxX + boxW - layout.width - 16f,
                boxY + 14f + layout.height);
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ── Input ──────────────────────────────────────────────────────────

    public void handleInput() {
        if (complete) return;

        boolean advance = Gdx.input.justTouched()
            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        if (!advance) return;

        if (assets != null) {
            assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
        }

        if (!lineFullyRevealed) {
            // Skip typewriter — reveal full line immediately
            charReveal = lines.get(currentIndex).text.length();
            lineFullyRevealed = true;
            return;
        }

        // Advance to next line
        currentIndex++;
        if (currentIndex >= lines.size()) {
            complete = true;
            if (listener != null) listener.onDialogueComplete();
        } else {
            charReveal = 0f;
            lineFullyRevealed = false;
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private void drawBorder(float x, float y, float w, float h,
                            float t, float r, float g, float b, float a) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(r, g, b, a);
        shapes.rect(x,         y + h - t, w, t);
        shapes.rect(x,         y,         w, t);
        shapes.rect(x,         y,         t, h);
        shapes.rect(x + w - t, y,         t, h);
        shapes.end();
    }

    private Texture getPortrait(String speaker) {
        if (assets == null) return null;
        if (speaker == null) return null;
        String s = speaker.toUpperCase();
        if (s.contains("ADDITION") || s.contains("ADD")) return assets.getTexture(ArithmeticAssetManager.PORT_ADD);
        if (s.contains("SUBTRACTION") || s.contains("SUB")) return assets.getTexture(ArithmeticAssetManager.PORT_SUB);
        if (s.contains("MULTIPLICATION") || s.contains("MUL")) return assets.getTexture(ArithmeticAssetManager.PORT_MUL);
        if (s.contains("DIVISION") || s.contains("DIV")) return assets.getTexture(ArithmeticAssetManager.PORT_DIV);
        
        if (s.contains("BALDO") || s.contains("MOB1") || s.contains("SLIME")) return assets.getTexture(ArithmeticAssetManager.CHAR_MOB1);
        if (s.contains("NORA") || s.contains("MOB2") || s.contains("BAT")) return assets.getTexture(ArithmeticAssetManager.CHAR_MOB2);
        
        return null;
    }

    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}
