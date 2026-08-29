package io.github.jhundeniel.ArithmeticHeroes.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * VisualComponent — sprite position, size, and optional frame animation.
 * All existing constructors preserved. EntityFactory needs no changes.
 *
 * To enable animation:
 *   entity.add(new VisualComponent(x, y, 120f, 120f, sheet, frameCount, 0.12f));
 */
public class VisualComponent implements Component {

    public float x, y, width, height;
    /** @deprecated Use getCurrentFrame() instead */
    @Deprecated public TextureRegion region;

    /**
     * Current frame to render. Updated each tick by AnimationSystem
     * for animated sprites; set once in the constructor for static sprites.
     */
    public TextureRegion            currentFrame;

    /** Spritesheet animation (null for static sprites). */
    public Animation<TextureRegion> animation;

    /** Elapsed animation time (seconds). Advanced by AnimationSystem. */
    public float                    stateTime  = 0f;

    /** True when this entity uses a spritesheet animation. */
    public boolean                  isAnimated = false;

    /**
     * When true, the idle animation is paused on its current frame.
     * Set to true before a boss attack overlay plays, false when it finishes.
     */
    public boolean frozen = false;

    /** Default 120×120 static sprite */
    public VisualComponent(float x, float y, Texture texture) {
        this.x = x; this.y = y; this.width = 120f; this.height = 120f;
        this.currentFrame = new TextureRegion(texture);
        this.region = this.currentFrame;
    }

    /** Custom-size static sprite (used by enemies and heroes with offsets) */
    public VisualComponent(float x, float y, float width, float height,
                           float textXOffset, float textYOffset, Texture texture) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        this.currentFrame = new TextureRegion(texture);
        this.region = this.currentFrame;
    }

    /**
     * Animated spritesheet constructor.
     * @param spritesheet  Single-row horizontal PNG spritesheet
     * @param frameCount   Number of columns (frames)
     * @param frameDuration Seconds per frame (0.12f ≈ 8fps)
     */
    public VisualComponent(float x, float y, float width, float height,
                           Texture spritesheet, int frameCount, float frameDuration) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        this.isAnimated = true;
        int fw = spritesheet.getWidth() / frameCount;
        int fh = spritesheet.getHeight();
        TextureRegion[][] tmp    = TextureRegion.split(spritesheet, fw, fh);
        TextureRegion[]   frames = new TextureRegion[frameCount];
        System.arraycopy(tmp[0], 0, frames, 0, frameCount);
        this.animation = new Animation<>(frameDuration, frames);
        this.animation.setPlayMode(Animation.PlayMode.LOOP);
        this.currentFrame = frames[0];
        this.region = this.currentFrame;
    }



    public TextureRegion getCurrentFrame() { return currentFrame; }
}
