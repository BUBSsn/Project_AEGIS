package io.github.jhundeniel.ArithmeticHeroes.battle;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent;

import java.util.*;

public class BattleAnimations {

    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<EntityFlash> flashes = new ArrayList<>();
    private final List<SkillAnim> skillAnims = new ArrayList<>();
    private final List<GravestoneAnim> gravestones = new ArrayList<>();
    private final List<DelayedSound> delayedSounds = new ArrayList<>();
    private final ScreenShake screenShake = new ScreenShake();
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();

    // ── Boss attack tracking (for freeze/unfreeze of idle anim) ──
    private Entity attackingBoss = null;
    private SkillAnim bossAttackAnim = null;

    private static final ComponentMapper<VisualComponent> vm = ComponentMapper.getFor(VisualComponent.class);

    // ── Echo cast delay — when > 0, new playSkillAnim calls start delayed ──
    private float echoDelay = 0f;

    public void setEchoDelay(float delay) {
        this.echoDelay = delay;
    }

    public void clearEchoDelay() {
        this.echoDelay = 0f;
    }

    /** Schedule a sound to play after a delay (syncs with echo animation). */
    public void scheduleSound(Runnable soundCallback, float delay) {
        delayedSounds.add(new DelayedSound(soundCallback, delay));
    }

    public void showDamage(int amount, float x, float y, boolean isBig) {
        Color color = isBig ? new Color(1f, 0.4f, 0f, 1f)
                : new Color(1f, 0.2f, 0.2f, 1f);
        floatingTexts.add(new FloatingText("-" + amount, x, y, color, isBig ? 2.2f : 1.6f));
        spawnHitParticles(x, y, color, isBig ? 8 : 5);
        if (isBig)
            screenShake.shake(0.3f, 12f);
        else
            screenShake.shake(0.1f, 5f);
    }

    public void showHeal(int amount, float x, float y) {
        floatingTexts.add(new FloatingText("+" + amount, x, y,
                new Color(0.3f, 1f, 0.3f, 1f), 1.6f));
        spawnHealParticles(x, y, 6);
    }

    public void showBuff(String text, float x, float y) {
        floatingTexts.add(new FloatingText(text, x, y,
                new Color(0.3f, 0.7f, 1f, 1f), 1.4f));
        spawnMagicParticles(x, y, new Color(0.5f, 0.5f, 1f, 1f), 8);
    }

    public void showDebuff(String text, float x, float y) {
        floatingTexts.add(new FloatingText(text, x, y,
                new Color(0.8f, 0.3f, 0.8f, 1f), 1.4f));
        spawnMagicParticles(x, y, new Color(0.6f, 0.2f, 0.6f, 1f), 8);
    }

    public void flashEntity(Entity entity) {
        if (vm.has(entity))
            flashes.add(new EntityFlash(entity, 0.18f));
    }

    /** Fixed-position one-shot animation on an entity. */
    public void playSkillAnim(Texture sheet, int frameCount, float frameDuration, Entity target) {
        if (!vm.has(target))
            return;
        VisualComponent v = vm.get(target);
        float animW = v.width * 2f;
        float animH = v.height * 2f;
        float animX = v.x + (v.width - animW) / 2f;
        float animY = v.y + (v.height - animH) / 2f;
        SkillAnim anim = new SkillAnim(sheet, frameCount, frameDuration,
                animX, animY, animW, animH);
        if (echoDelay > 0)
            anim.stateTime = -echoDelay;
        skillAnims.add(anim);
    }

    /**
     * Delayed one-shot animation — waits 'delay' seconds before starting to play.
     */
    public void playSkillAnimDelayed(Texture sheet, int frameCount, float frameDuration,
            Entity target, float delay) {
        if (!vm.has(target))
            return;
        VisualComponent v = vm.get(target);
        float animW = v.width * 2f;
        float animH = v.height * 2f;
        float animX = v.x + (v.width - animW) / 2f;
        float animY = v.y + (v.height - animH) / 2f;
        SkillAnim anim = new SkillAnim(sheet, frameCount, frameDuration,
                animX, animY, animW, animH);
        anim.stateTime = -delay; // negative = waiting to start
        skillAnims.add(anim);
    }

    /**
     * Fixed-position animation at explicit world coordinates (for boss attacks).
     */
    public void playSkillAnimAt(Texture sheet, int frameCount, float frameDuration,
            float x, float y, float w, float h) {
        skillAnims.add(new SkillAnim(sheet, frameCount, frameDuration, x, y, w, h));
    }

    /**
     * Boss-specific attack overlay.
     * Registers the boss entity so BattleRenderSystem can unfreeze its idle
     * animation once the attack sheet finishes playing.
     *
     * @param boss the Entity whose idle anim was frozen before this call
     */
    public void playBossAttackAnim(Texture sheet, int frameCount, float frameDuration,
            float x, float y, float w, float h, Entity boss) {
        SkillAnim anim = new SkillAnim(sheet, frameCount, frameDuration, x, y, w, h);
        skillAnims.add(anim);
        this.bossAttackAnim = anim;
        this.attackingBoss = boss;
    }

    /** Returns the boss entity currently playing its attack anim, or null. */
    public Entity getAttackingBoss() {
        return attackingBoss;
    }

    /** True once the registered boss attack anim has finished all frames. */
    public boolean isBossAttackDone() {
        return bossAttackAnim == null || bossAttackAnim.isDone();
    }

    /** Call after unfreezing the boss idle to reset tracking state. */
    public void clearBossAttack() {
        if (bossAttackAnim != null) {
            skillAnims.remove(bossAttackAnim); // now safe — won't be double-removed
        }
        bossAttackAnim = null;
        attackingBoss = null;
    }

    /**
     * Projectile that travels from caster to target over travelDuration seconds.
     */
    public void playProjectileAnim(Texture sheet, int frameCount, float frameDuration,
            Entity caster, Entity target) {
        if (!vm.has(caster) || !vm.has(target))
            return;
        VisualComponent cv = vm.get(caster);
        VisualComponent tv = vm.get(target);

        float projW = cv.width * 1.5f;
        float projH = cv.height * 1.5f;
        float startX = cv.x + cv.width / 2f - projW / 2f;
        float startY = cv.y + cv.height / 2f - projH / 2f;
        float endX = tv.x + tv.width / 2f - projW / 2f;
        float endY = tv.y + tv.height / 2f - projH / 2f;

        skillAnims.add(new SkillAnim(sheet, frameCount, frameDuration,
                startX, startY, projW, projH, endX, endY, 0.4f));
    }

    /**
     * Replace a dead hero's sprite with their gravestone animation.
     * The gravestone plays its animation and persists until the battle ends.
     *
     * @param graveSheet the hero-specific gravestone PNG animation sheet
     * @param frameCount the number of frames in the sheet
     * @param x,           y world position (same as the hero's VisualComponent x/y)
     * @param w,           h draw size (use hero HERO_W * SPRITE_SCALE, etc.)
     */
    public void showGravestone(Texture graveSheet, int frameCount, float x, float y, float w, float h, Texture staticTex) {
        gravestones.add(new GravestoneAnim(graveSheet, frameCount, x, y, w, h, staticTex));
    }

    // ── Update ────────────────────────────────────────────────────
    public void update(float dt) {
        floatingTexts.removeIf(t -> {
            t.update(dt);
            return t.isDead();
        });
        particles.removeIf(p -> {
            p.update(dt);
            return p.isDead();
        });
        flashes.removeIf(f -> {
            f.update(dt);
            return f.isDead();
        });

        // Tick all skill anims once; only auto-remove non-boss ones
        for (SkillAnim a : skillAnims)
            a.update(dt);
        skillAnims.removeIf(a -> a.isDone() && a != bossAttackAnim);

        for (GravestoneAnim g : gravestones)
            g.update(dt);
        delayedSounds.removeIf(ds -> {
            ds.update(dt);
            return ds.isDone();
        });
        screenShake.update(dt);
    }

    public void renderText(SpriteBatch batch, BitmapFont font) {
        for (FloatingText t : floatingTexts)
            t.render(batch, font);
    }

    public void renderFlashes(SpriteBatch batch) {
        for (EntityFlash f : flashes)
            f.render(batch);
    }

    public void renderSkillAnims(SpriteBatch batch) {
        for (SkillAnim a : skillAnims)
            a.render(batch);
    }

    public void renderGravestones(SpriteBatch batch) {
        for (GravestoneAnim g : gravestones)
            g.render(batch);
    }

    public void renderParticles(com.badlogic.gdx.graphics.Camera camera) {
        if (particles.isEmpty())
            return;
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Particle p : particles)
            p.render(shapeRenderer);
        shapeRenderer.end();
    }

    public void clearGravestones() {
        gravestones.clear();
    }

    public Vector2 getShakeOffset() {
        return screenShake.getOffset();
    }

    public void dispose() {
        shapeRenderer.dispose();
    }

    // ── Particle spawners ─────────────────────────────────────────
    private void spawnHitParticles(float x, float y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            float speed = 50f + (float) Math.random() * 120f;
            particles.add(new Particle(x, y,
                    (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed,
                    color, 0.5f, 3f));
        }
    }

    private void spawnHealParticles(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            float speed = 30f + (float) Math.random() * 50f;
            Color c = new Color(0.4f + (float) Math.random() * 0.6f, 1f,
                    0.4f + (float) Math.random() * 0.6f, 1f);
            particles.add(new Particle(x, y,
                    (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed + 80f,
                    c, 0.8f, 2f));
        }
    }

    private void spawnMagicParticles(float x, float y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            float speed = 40f + (float) Math.random() * 80f;
            particles.add(new Particle(x, y,
                    (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed,
                    color, 1.0f, 2.5f));
        }
    }

    // ═══════════════ INNER CLASSES ════════════════════════════════

    // ── GravestoneAnim ────────────────────────────────────────────
    private static class GravestoneAnim {
        Animation<TextureRegion> animation;
        float stateTime = 0f;
        final float x, y, w, h;
        boolean finished = false;
        final Texture staticTex; // shown after animation completes
        final TextureRegion staticRegion;

        GravestoneAnim(Texture sheet, int frameCount, float x, float y, float w, float h, Texture staticTex) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.staticTex = staticTex;
            this.staticRegion = staticTex != null ? new TextureRegion(staticTex) : null;

            int fw = sheet.getWidth() / frameCount;
            int fh = sheet.getHeight();
            TextureRegion[][] tmp = TextureRegion.split(sheet, fw, fh);
            TextureRegion[] frames = new TextureRegion[frameCount];
            System.arraycopy(tmp[0], 0, frames, 0, frameCount);
            animation = new Animation<>(0.10f, frames);
            animation.setPlayMode(Animation.PlayMode.NORMAL); // halt on last frame
        }

        void update(float dt) {
            stateTime += dt;
            if (animation.isAnimationFinished(stateTime)) {
                finished = true;
            }
        }

        void render(SpriteBatch batch) {
            batch.setColor(Color.WHITE);
            if (finished && staticRegion != null) {
                // After animation completes, show the static gravestone image
                batch.draw(staticRegion, x, y, w, h);
            } else {
                batch.draw(animation.getKeyFrame(stateTime), x, y, w, h);
            }
        }
    }

    // ── SkillAnim ─────────────────────────────────────────────────
    static class SkillAnim {
        Animation<TextureRegion> animation;
        float stateTime = 0f;
        float x, y, width, height;

        boolean isProjectile = false;
        float startX, startY, endX, endY;
        float travelDuration, travelTime = 0f;

        SkillAnim(Texture sheet, int frameCount, float frameDuration,
                float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            buildAnimation(sheet, frameCount, frameDuration, false);
        }

        SkillAnim(Texture sheet, int frameCount, float frameDuration,
                float startX, float startY, float width, float height,
                float endX, float endY, float travelDuration) {
            this.isProjectile = true;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.travelDuration = travelDuration;
            this.width = width;
            this.height = height;
            this.x = startX;
            this.y = startY;
            buildAnimation(sheet, frameCount, frameDuration, true);
        }

        private void buildAnimation(Texture sheet, int frameCount,
                float frameDuration, boolean loop) {
            int fw = sheet.getWidth() / frameCount;
            int fh = sheet.getHeight();
            TextureRegion[][] tmp = TextureRegion.split(sheet, fw, fh);
            TextureRegion[] frames = new TextureRegion[frameCount];
            System.arraycopy(tmp[0], 0, frames, 0, frameCount);
            animation = new Animation<>(frameDuration, frames);
            animation.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
        }

        void update(float dt) {
            stateTime += dt;
            if (isProjectile) {
                travelTime += dt;
                float t = Math.min(travelTime / travelDuration, 1f);
                x = startX + (endX - startX) * t;
                y = startY + (endY - startY) * t;
            }
        }

        void render(SpriteBatch batch) {
            if (stateTime < 0)
                return; // still waiting for delay
            batch.setColor(Color.WHITE);
            batch.draw(animation.getKeyFrame(stateTime), x, y, width, height);
        }

        boolean isDone() {
            if (stateTime < 0)
                return false; // still waiting for delay
            if (isProjectile)
                return travelTime >= travelDuration;
            return animation.isAnimationFinished(stateTime);
        }
    }

    // ── FloatingText ──────────────────────────────────────────────
    private static class FloatingText {
        String text;
        float x, y;
        Color color;
        float lifetime, maxLifetime, scale;

        FloatingText(String text, float x, float y, Color color, float scale) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = new Color(color);
            this.scale = scale;
            this.maxLifetime = this.lifetime = 1.3f;
        }

        void update(float dt) {
            lifetime -= dt;
            y += 55f * dt;
            color.a = Math.max(0, lifetime / maxLifetime);
        }

        void render(SpriteBatch batch, BitmapFont font) {
            font.setColor(color);
            font.getData().setScale(scale);
            font.draw(batch, text, x - (text.length() * 4f * scale), y);
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
        }

        boolean isDead() {
            return lifetime <= 0;
        }
    }

    // ── EntityFlash ───────────────────────────────────────────────
    private static class EntityFlash {
        Entity entity;
        float lifetime, maxLifetime;
        private static final ComponentMapper<VisualComponent> vm = ComponentMapper.getFor(VisualComponent.class);

        EntityFlash(Entity entity, float duration) {
            this.entity = entity;
            this.maxLifetime = this.lifetime = duration;
        }

        void update(float dt) {
            lifetime -= dt;
        }

        void render(SpriteBatch batch) {
            if (!vm.has(entity))
                return;
            VisualComponent v = vm.get(entity);
            float alpha = (lifetime / maxLifetime) * 0.7f;
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(v.region, v.x, v.y, v.width, v.height);
            batch.setColor(Color.WHITE);
        }

        boolean isDead() {
            return lifetime <= 0;
        }
    }

    // ── Particle ──────────────────────────────────────────────────
    private static class Particle {
        float x, y, vx, vy, lifetime, maxLifetime, size;
        Color color;

        Particle(float x, float y, float vx, float vy,
                Color color, float lifetime, float size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = new Color(color);
            this.maxLifetime = this.lifetime = lifetime;
            this.size = size;
        }

        void update(float dt) {
            x += vx * dt;
            y += vy * dt;
            vy -= 200f * dt;
            vx *= 0.95f;
            vy *= 0.95f;
            lifetime -= dt;
            color.a = Math.max(0, lifetime / maxLifetime);
        }

        void render(ShapeRenderer sr) {
            sr.setColor(color);
            sr.circle(x, y, size);
        }

        boolean isDead() {
            return lifetime <= 0;
        }
    }

    // ── ScreenShake ───────────────────────────────────────────────
    private static class ScreenShake {
        float duration, intensity;
        final Vector2 offset = new Vector2();

        void shake(float dur, float inten) {
            duration = Math.max(duration, dur);
            intensity = Math.max(intensity, inten);
        }

        void update(float dt) {
            if (duration > 0) {
                duration -= dt;
                offset.x = MathUtils.random(-intensity, intensity);
                offset.y = MathUtils.random(-intensity, intensity);
                intensity *= 0.88f;
            } else {
                offset.set(0, 0);
                intensity = 0;
            }
        }

        Vector2 getOffset() {
            return offset;
        }
    }

    // ── DelayedSound ──────────────────────────────────────────────
    private static class DelayedSound {
        Runnable callback;
        float timer;
        boolean fired = false;

        DelayedSound(Runnable callback, float delay) {
            this.callback = callback;
            this.timer = delay;
        }

        void update(float dt) {
            if (fired)
                return;
            timer -= dt;
            if (timer <= 0) {
                callback.run();
                fired = true;
            }
        }

        boolean isDone() {
            return fired;
        }
    }
}
