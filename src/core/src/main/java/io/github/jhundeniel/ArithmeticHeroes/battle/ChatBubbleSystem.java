package io.github.jhundeniel.ArithmeticHeroes.battle;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Renders floating speech bubbles above entities during combat.
 *
 * Usage:
 *   chatBubbles.showBubble(entity, "Take this!");
 *
 * Bubbles auto-dismiss after a set duration. Multiple bubbles per entity
 * stack vertically.
 */
public class ChatBubbleSystem {

    private static final float BUBBLE_DURATION = 2.2f;
    private static final float FADE_DURATION   = 0.4f;
    private static final float BUBBLE_PAD_X    = 10f;
    private static final float BUBBLE_PAD_Y    = 6f;
    private static final float BUBBLE_OFFSET_Y = 20f;
    private static final float MAX_BUBBLE_WIDTH = 200f;

    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final GlyphLayout layout;
    private final ComponentMapper<VisualComponent> vm = ComponentMapper.getFor(VisualComponent.class);

    private final List<Bubble> activeBubbles = new ArrayList<>();

    private static class Bubble {
        Entity speaker;
        String text;
        float elapsed;
        float duration;

        Bubble(Entity speaker, String text) {
            this.speaker  = speaker;
            this.text     = text;
            this.elapsed  = 0f;
            this.duration = BUBBLE_DURATION;
        }
    }

    public ChatBubbleSystem() {
        shapes = new ShapeRenderer();
        batch  = new SpriteBatch();
        font   = new BitmapFont();
        layout = new GlyphLayout();
    }

    /**
     * Show a speech bubble above the given entity.
     */
    public void showBubble(Entity speaker, String text) {
        if (speaker == null || text == null || text.isEmpty()) return;

        // Remove any existing bubble for this speaker
        activeBubbles.removeIf(b -> b.speaker == speaker);

        activeBubbles.add(new Bubble(speaker, text));
    }

    /**
     * Update and render all active bubbles.
     */
    public void render(OrthographicCamera camera) {
        float dt = Gdx.graphics.getDeltaTime();

        // Update timers and remove expired
        Iterator<Bubble> it = activeBubbles.iterator();
        while (it.hasNext()) {
            Bubble b = it.next();
            b.elapsed += dt;
            if (b.elapsed >= b.duration + FADE_DURATION) {
                it.remove();
            }
        }

        if (activeBubbles.isEmpty()) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        float textScale = Math.max(0.001f, camera.viewportHeight * 0.0020f);

        for (Bubble b : activeBubbles) {
            VisualComponent v = vm.get(b.speaker);
            if (v == null) continue;

            // Fade in/out
            float alpha;
            if (b.elapsed < 0.15f) {
                alpha = b.elapsed / 0.15f; // quick fade-in
            } else if (b.elapsed > b.duration) {
                alpha = 1f - ((b.elapsed - b.duration) / FADE_DURATION);
            } else {
                alpha = 1f;
            }
            alpha = Math.max(0f, Math.min(1f, alpha));

            // Position above entity sprite
            float cx = v.x + v.width / 2f;
            float topY = v.y + v.height + BUBBLE_OFFSET_Y;

            // Measure text
            font.getData().setScale(textScale);
            layout.setText(font, b.text, Color.WHITE, MAX_BUBBLE_WIDTH, -1, true);
            float textW = layout.width;
            float textH = layout.height;

            float bubW = textW + BUBBLE_PAD_X * 2f;
            float bubH = textH + BUBBLE_PAD_Y * 2f;
            float bubX = cx - bubW / 2f;
            float bubY = topY;

            // Bubble background
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.10f, 0.07f, 0.22f, 0.88f * alpha);
            shapes.rect(bubX, bubY, bubW, bubH);
            shapes.end();

            // Bubble border
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            float borderThick = 1.5f;
            shapes.setColor(0.70f, 0.60f, 1.00f, 0.55f * alpha);
            shapes.rect(bubX, bubY + bubH - borderThick, bubW, borderThick);
            shapes.rect(bubX, bubY, bubW, borderThick);
            shapes.rect(bubX, bubY, borderThick, bubH);
            shapes.rect(bubX + bubW - borderThick, bubY, borderThick, bubH);
            shapes.end();

            // Tail triangle pointing down
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.10f, 0.07f, 0.22f, 0.88f * alpha);
            shapes.triangle(
                cx - 5f, bubY,
                cx + 5f, bubY,
                cx, bubY - 6f
            );
            shapes.end();

            // Text
            batch.begin();
            font.getData().setScale(textScale);
            font.setColor(0.95f, 0.92f, 1.00f, alpha);
            font.draw(batch, b.text,
                bubX + BUBBLE_PAD_X,
                bubY + bubH - BUBBLE_PAD_Y,
                MAX_BUBBLE_WIDTH, -1, true);
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void clear() {
        activeBubbles.clear();
    }

    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}
