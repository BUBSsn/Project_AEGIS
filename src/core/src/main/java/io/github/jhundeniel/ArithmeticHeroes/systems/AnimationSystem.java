package io.github.jhundeniel.ArithmeticHeroes.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

import io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent;

/**
 * AnimationSystem — advances sprite-sheet animations every frame.
 *
 * Runs before BattleRenderSystem so that the current frame is always
 * up-to-date before it is drawn. Static sprites are a no-op.
 *
 * This keeps VisualComponent as a pure data bag (ECS best practice):
 * all mutation of animation state lives here, not inside the component.
 */
public class AnimationSystem extends IteratingSystem {

    private final ComponentMapper<VisualComponent> visualMapper =
        ComponentMapper.getFor(VisualComponent.class);

    public AnimationSystem() {
        super(Family.all(VisualComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        VisualComponent visual = visualMapper.get(entity);

        // Only advance when the entity has a spritesheet animation,
        // the animation object is present, and the idle is not frozen
        // (frozen is set during boss attack overlays).
        if (visual.frozen || !visual.isAnimated || visual.animation == null) {
            return;
        }

        visual.stateTime   += deltaTime;
        visual.currentFrame = visual.animation.getKeyFrame(visual.stateTime);
        visual.region       = visual.currentFrame;
    }
}
