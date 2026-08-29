package io.github.jhundeniel.ArithmeticHeroes.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a pulsing red outline around the enemy's current target(s).
 * Call setTargets() when the AI picks its action, clearTargets() after the turn ends.
 */
public class EnemyTargetHighlight {

    private final ShapeRenderer shapes = new ShapeRenderer();

    private final ComponentMapper<VisualComponent> vm =
        ComponentMapper.getFor(VisualComponent.class);

    // The entity(ies) to highlight this enemy turn
    private final List<Entity> targets = new ArrayList<>();

    private float pulseTimer = 0f;

    private static final float SPRITE_SCALE = 2.0f; // must match BattleRenderSystem

    // Red outline color
    private static final Color BASE_COL = new Color(1f, 0.18f, 0.18f, 1f);

    // ── Public API ────────────────────────────────────────────────────────

    /** Highlight a single target (ENEMY_ATTACK). */
    public void setTarget(Entity target) {
        targets.clear();
        if (target != null) targets.add(target);
    }

    /** Highlight all heroes (ENEMY_AOE_ATTACK). */
    public void setTargets(List<Entity> targetList) {
        targets.clear();
        if (targetList != null) targets.addAll(targetList);
    }

    /** Remove all highlights (call after turn completes). */
    public void clearTargets() {
        targets.clear();
    }

    // ── Render ────────────────────────────────────────────────────────────

    /**
     * Call this every frame AFTER engine.update() and BEFORE batch.end() is
     * called by anything else — or simply after all other renders.
     * Needs the camera so ShapeRenderer lines up with sprites.
     */
    public void render(com.badlogic.gdx.graphics.Camera camera, float deltaTime) {
        if (targets.isEmpty()) return;

        pulseTimer += deltaTime;

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);

        for (Entity target : targets) {
            VisualComponent v = vm.get(target);
            if (v == null) continue;

            float drawW = v.width  * SPRITE_SCALE;
            float drawH = v.height * SPRITE_SCALE;
            float drawX = v.x + (v.width - drawW) / 2f;
            float drawY = v.y;

            // Pulse between 0.55 and 1.0 alpha
            float pulse = (float) Math.sin(pulseTimer * 5.0) * 0.22f + 0.78f;
            float pad   = 6f;

            // Draw 3 nested outlines for a thick glow effect
            for (int i = 0; i < 3; i++) {
                float p = pad + i;
                shapes.setColor(BASE_COL.r, BASE_COL.g, BASE_COL.b, pulse * (1f - i * 0.25f));
                shapes.rect(
                    drawX - p,
                    drawY - p,
                    drawW + p * 2f,
                    drawH + p * 2f
                );
            }

            // Downward arrow above the target
            drawArrow(drawX + drawW / 2f, drawY + drawH + 14f, pulse);
        }

        shapes.end();
    }

    private void drawArrow(float x, float topY, float alpha) {
        float h = 14f, w = 10f;
        shapes.setColor(BASE_COL.r, BASE_COL.g, BASE_COL.b, alpha);
        // V-shape pointing down
        shapes.line(x - w / 2f, topY + h, x,         topY);
        shapes.line(x + w / 2f, topY + h, x,         topY);
        shapes.line(x - w / 2f, topY + h, x + w / 2f, topY + h);
    }

    public void dispose() {
        shapes.dispose();
    }
}
