package io.github.jhundeniel.ArithmeticHeroes.battle;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import io.github.jhundeniel.ArithmeticHeroes.components.BattleUIComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.Operator;
import io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles target selection with mouse input and visual feedback
 */
public class TargetSelector {

    // Object fields
    private Entity hoveredTarget;
    private Entity selectedTarget;
    private final ShapeRenderer shapeRenderer;

    // Visual feedback colors
    private static final Color HOVER_COLOR  = new Color(1f, 1f, 0.3f, 0.5f); // Yellow
    private static final Color SELECT_COLOR = new Color(1f, 0.3f, 0.3f, 0.7f); // Red

    public TargetSelector() {
        this.shapeRenderer = new ShapeRenderer();
    }

    /**
     * Update hover target based on mouse position, and switch cursor accordingly.
     */
    public void update(List<Entity> possibleTargets) {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY(); // Flip Y

        hoveredTarget = null;

        for (Entity entity : possibleTargets) {
            VisualComponent visual = entity.getComponent(VisualComponent.class);
            if (visual == null) continue;

            Rectangle bounds = new Rectangle(visual.x, visual.y, visual.width, visual.height);

            if (bounds.contains(mouseX, mouseY)) {
                hoveredTarget = entity;
                break;
            }
        }

        // Cursor switching logic removed for global cursor.
    }

    /**
     * Select the currently hovered target.
     */
    public boolean selectHovered() {
        if (hoveredTarget != null) {
            selectedTarget = hoveredTarget;
            return true;
        }
        return false;
    }

    /**
     * Clear selection and reset cursor to arrow.
     */
    public void clearSelection() {
        selectedTarget = null;
        hoveredTarget  = null;
    }

    /**
     * Get currently selected target.
     */
    public Entity getSelectedTarget() {
        return selectedTarget;
    }

    /**
     * Get currently hovered target.
     */
    public Entity getHoveredTarget() {
        return hoveredTarget;
    }

    /**
     * Draw visual feedback for hover and selection.
     */
    public void renderTargetIndicators(com.badlogic.gdx.graphics.Camera camera) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // Draw selection box (red)
        if (selectedTarget != null) {
            VisualComponent visual = selectedTarget.getComponent(VisualComponent.class);
            if (visual != null) {
                shapeRenderer.setColor(SELECT_COLOR);

                // Draw thick outline
                for (int i = 0; i < 3; i++) {
                    shapeRenderer.rect(
                        visual.x - 5 - i,
                        visual.y - 5 - i,
                        visual.width  + 10 + i * 2,
                        visual.height + 10 + i * 2
                    );
                }

                // Draw targeting arrow above
                float arrowX = visual.x + visual.width / 2;
                float arrowY = visual.y + visual.height + 20;
                drawArrow(arrowX, arrowY + 10, arrowX, arrowY, SELECT_COLOR);
            }
        }

        // Draw hover box (yellow)
        if (hoveredTarget != null && hoveredTarget != selectedTarget) {
            VisualComponent visual = hoveredTarget.getComponent(VisualComponent.class);
            if (visual != null) {
                shapeRenderer.setColor(HOVER_COLOR);

                // Draw outline
                for (int i = 0; i < 2; i++) {
                    shapeRenderer.rect(
                        visual.x - 3 - i,
                        visual.y - 3 - i,
                        visual.width  + 6 + i * 2,
                        visual.height + 6 + i * 2
                    );
                }
            }
        }

        shapeRenderer.end();
    }

    /**
     * Draw an arrow pointing downward.
     */
    private void drawArrow(float x1, float y1, float x2, float y2, Color color) {
        shapeRenderer.setColor(color);

        // Main line
        shapeRenderer.line(x1, y1, x2, y2);

        // Arrow head
        float arrowSize = 5f;
        shapeRenderer.line(x2, y2, x2 - arrowSize, y2 + arrowSize);
        shapeRenderer.line(x2, y2, x2 + arrowSize, y2 + arrowSize);
    }

    /**
     * Get all valid targets based on skill type.
     */
    public static List<Entity> getValidTargets(List<Entity> allEntities,
                                               BattleUIComponent.SkillType skill,
                                               Entity caster) {
        List<Entity> validTargets = new ArrayList<>();

        TypeComponent casterType  = caster.getComponent(TypeComponent.class);
        boolean       casterIsHero = casterType != null && casterType.type != Operator.MOB;

        for (Entity entity : allEntities) {
            TypeComponent type = entity.getComponent(TypeComponent.class);
            if (type == null) continue;

            boolean isHero = type.type != Operator.MOB;

            switch (skill) {
                case HEAL:
                case AMPLIFY:
                    // Can only target allies
                    if (casterIsHero == isHero) {
                        validTargets.add(entity);
                    }
                    break;

                case POKE:
                case BURDEN:
                    // Can only target enemies
                    if (casterIsHero != isHero) {
                        validTargets.add(entity);
                    }
                    break;
            }
        }

        return validTargets;
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
