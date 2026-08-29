package io.github.jhundeniel.ArithmeticHeroes.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

import io.github.jhundeniel.ArithmeticHeroes.battle.BattleState;
import io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.managers.TurnManager;

public class MouseTargetingSystem {

    private final TurnManager     turnManager;
    private final TargetingSystem targetingSystem;
    private final ActionLogSystem actionLog;
    private final Camera          camera;

    private final ComponentMapper<VisualComponent> vm = ComponentMapper.getFor(VisualComponent.class);
    private final ComponentMapper<StatsComponent>  sm = ComponentMapper.getFor(StatsComponent.class);

    private final Vector3 mouseCoords = new Vector3();
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private Entity previousHoveredTarget = null;
    private boolean wasInSelectTarget = false;

    public MouseTargetingSystem(TurnManager turnManager, TargetingSystem targetingSystem,
                                ActionLogSystem actionLog, Camera camera) {
        this.turnManager     = turnManager;
        this.targetingSystem = targetingSystem;
        this.actionLog       = actionLog;
        this.camera          = camera;
    }

    public void update(float deltaTime) {

        if (turnManager.getState() != BattleState.SELECT_TARGET) {
            if (wasInSelectTarget) {
                wasInSelectTarget = false;
            }
            return;
        }

        wasInSelectTarget = true;
        int currentMouseX = Gdx.input.getX();
        int currentMouseY = Gdx.input.getY();

        Entity currentlyHoveredTarget = null;

        // 1. The "Resting Mouse" Fix
        // Only run hover hitboxing if the mouse physically moved this frame.
        // This prevents the mouse from overriding the user's keyboard navigation.
        if (currentMouseX != lastMouseX || currentMouseY != lastMouseY) {
            lastMouseX = currentMouseX;
            lastMouseY = currentMouseY;

            // 2. The camera.unproject Z-Axis GC Fix
            mouseCoords.set(currentMouseX, currentMouseY, 0);
            camera.unproject(mouseCoords);
            float worldX = mouseCoords.x;
            float worldY = mouseCoords.y;

            // Hover detection
            for (Entity entity : targetingSystem.validTargets) {
                VisualComponent visual = vm.get(entity);
                if (visual == null) continue;

                // Increase hit area artificially for easier selecting
                Rectangle bounds = new Rectangle(visual.x - 10, visual.y - 10, visual.width + 20, visual.height + 20);
                if (bounds.contains(worldX, worldY)) {
                    targetingSystem.setTargetIndex(entity);
                    currentlyHoveredTarget = entity;
                    break;
                }
            }
        } else {
            // Restore actual hovered state from selection index if not moving mouse
            currentlyHoveredTarget = targetingSystem.getCurrentTarget();
        }

        if (previousHoveredTarget != currentlyHoveredTarget) {
            previousHoveredTarget = currentlyHoveredTarget;
        }

        // Left-click: confirm target
        if (Gdx.input.justTouched()) {
            Entity currentTarget = targetingSystem.getCurrentTarget();
            if (currentTarget != null) {
                // Must ensure the mouse is actually clicking ON the current target.
                // Re-unproject mouse to world just to confirm click in bounds
                mouseCoords.set(currentMouseX, currentMouseY, 0);
                camera.unproject(mouseCoords);
                VisualComponent visual = vm.get(currentTarget);

                if (visual != null) {
                    Rectangle bounds = new Rectangle(visual.x - 10, visual.y - 10, visual.width + 20, visual.height + 20);
                    if (bounds.contains(mouseCoords.x, mouseCoords.y)) {
                        confirmTarget(currentTarget);
                    }
                }
            }
        }

        // Right-click: cancel
        if (Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.RIGHT)) {
            targetingSystem.cancel();
            turnManager.setState(BattleState.WAIT_FOR_INPUT);
        }
    }

    private void confirmTarget(Entity target) {
        if (!isValidTarget(target)) {
            StatsComponent stats = sm.get(target);
            actionLog.addMessage("Invalid target: " + (stats != null ? stats.name.trim() : "???"));
            return;
        }

        targetingSystem.confirmTarget(target);

        if (targetingSystem.isWaitingForValue()) {
            turnManager.setState(BattleState.CHOOSE_VALUE);
        } else if (!targetingSystem.isTargeting()) {
            // Only transition when targeting is fully done
            // (group burden mode stays in SELECT_TARGET until all targets are locked)
            turnManager.setState(BattleState.ACTION_QUEUED);
        }
        // else: still picking targets (group burden or two-target), stay in SELECT_TARGET
    }

    private boolean isValidTarget(Entity target) {
        if (!targetingSystem.isTargeting()) return false;
        StatsComponent stats = sm.get(target);
        if (stats == null || stats.hp <= 0) return false;
        return targetingSystem.isValidTarget(target);
    }

    public void render(Camera camera) {
        // Render removed — visually unified into BattleRenderSystem JRPG arrow
    }

    public void dispose() {
        // No heavy objects to dispose here anymore
    }
}
