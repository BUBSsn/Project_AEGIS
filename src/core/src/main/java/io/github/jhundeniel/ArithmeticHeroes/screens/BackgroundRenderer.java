package io.github.jhundeniel.ArithmeticHeroes.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;

public class BackgroundRenderer {

    private static final float VW = 1280f;
    private static final float VH = 720f;

    private static class Sparkle {
        float x, y, vx, vy, life, maxLife, size;
        Color color;
        int shape;

        Sparkle(float x, float y) {
            this.x = x;
            this.y = y;
            vx = MathUtils.random(-80f, 80f);
            vy = MathUtils.random(30f, 140f);
            maxLife = MathUtils.random(0.6f, 2.0f);
            life = maxLife;
            size = MathUtils.random(3f, 7f);
            shape = MathUtils.random(2);
            int c = MathUtils.random(5);
            switch (c) {
                case 0: color = new Color(0.85f, 0.20f, 1.00f, 1f); break;
                case 1: color = new Color(1.00f, 0.40f, 0.80f, 1f); break;
                case 2: color = new Color(0.30f, 0.85f, 1.00f, 1f); break;
                case 3: color = new Color(1.00f, 0.92f, 0.30f, 1f); break;
                case 4: color = new Color(0.40f, 1.00f, 0.60f, 1f); break;
                default: color = new Color(1.00f, 1.00f, 1.00f, 1f); break;
            }
        }

        void update(float d) {
            x += vx * d;
            y += vy * d;
            vy -= 120f * d;
            life -= d;
        }

        boolean isDead() { return life <= 0; }
        float getAlpha() { return MathUtils.clamp(life / maxLife, 0f, 1f); }
    }

    private static class FloatStar {
        float x, y, vy, life, maxLife, size, twinkle;
        Color color;

        FloatStar() { reset(); }

        void reset() {
            x = MathUtils.random(0f, VW);
            y = MathUtils.random(0f, VH * 0.7f);
            vy = MathUtils.random(8f, 25f);
            maxLife = MathUtils.random(3f, 6f);
            life = MathUtils.random(0f, maxLife);
            size = MathUtils.random(2f, 5f);
            twinkle = MathUtils.random(2f, 6f);
            int c = MathUtils.random(3);
            switch (c) {
                case 0: color = new Color(0.8f, 0.5f, 1.0f, 1f); break;
                case 1: color = new Color(0.5f, 0.9f, 1.0f, 1f); break;
                case 2: color = new Color(1.0f, 0.9f, 0.5f, 1f); break;
                default: color = new Color(1.0f, 1.0f, 1.0f, 1f); break;
            }
        }

        void update(float d) {
            y += vy * d;
            life -= d;
            if (isDead()) reset();
        }

        boolean isDead() { return life <= 0 || y > VH + 20; }
        float getAlpha(float t) {
            float base = MathUtils.clamp(life / maxLife, 0f, 1f);
            return base * (0.5f + 0.5f * MathUtils.sin(t * twinkle));
        }
    }

    private static class MagicOrb {
        float angle, radius, speed, size;
        Color color;
        float baseY;

        MagicOrb(float baseY, float radius, float startAngle, float speed, Color color, float size) {
            this.baseY = baseY;
            this.radius = radius;
            this.angle = startAngle;
            this.speed = speed;
            this.color = color;
            this.size = size;
        }

        void update(float d) { angle += speed * d; }
        float getX() { return VW / 2f + MathUtils.cos(angle) * radius; }
        float getY() { return baseY + MathUtils.sin(angle) * radius * 0.3f; }
    }

    private static class Rune {
        float x, y, pulse, phase, opacity;
        char glyph;
        Color color;

        Rune(float x, float y, char glyph, float phase, Color color) {
            this.x = x;
            this.y = y;
            this.glyph = glyph;
            this.phase = phase;
            this.color = color;
            this.pulse = MathUtils.random(0.8f, 2.0f);
            this.opacity = MathUtils.random(0.3f, 0.7f);
        }

        float getAlpha(float t) {
            return opacity * (0.4f + 0.6f * MathUtils.sin(t * pulse + phase));
        }
    }

    private float animationTime = 0f;
    private float ambientTimer = 0f;

    private final ArrayList<Sparkle> sparkles = new ArrayList<>();
    private final ArrayList<FloatStar> floatStars = new ArrayList<>();
    private final ArrayList<MagicOrb> orbs = new ArrayList<>();
    private final ArrayList<Rune> runes = new ArrayList<>();

    private final Texture background;

    public BackgroundRenderer() {
        for (int i = 0; i < 20; i++)
            floatStars.add(new FloatStar());

        float titleCentreY = VH - (VH * 0.28f) - VH * 0.02f;
        Color[] orbColors = {
                new Color(0.6f, 0.2f, 1.0f, 1f), new Color(0.2f, 0.8f, 1.0f, 1f),
                new Color(1.0f, 0.4f, 0.7f, 1f), new Color(1.0f, 0.85f, 0.2f, 1f),
                new Color(0.3f, 1.0f, 0.6f, 1f), new Color(1.0f, 0.5f, 0.2f, 1f),
        };
        for (int i = 0; i < 6; i++) {
            float startAngle = i * MathUtils.PI2 / 6f;
            float radius = 320f + MathUtils.random(-30f, 30f);
            float speed = MathUtils.random(0.4f, 0.9f) * (MathUtils.randomBoolean() ? 1 : -1);
            float size = MathUtils.random(5f, 11f);
            orbs.add(new MagicOrb(titleCentreY, radius, startAngle, speed, orbColors[i], size));
        }

        String runeChars = "+×÷−=∞★◆♦";
        Color[] runeColors = {
                new Color(0.7f, 0.3f, 1.0f, 1f),
                new Color(0.3f, 0.8f, 1.0f, 1f),
                new Color(1.0f, 0.85f, 0.3f, 1f),
        };
        float[][] runePositions = {
                { 80, 600 }, { 160, 520 }, { 60, 400 }, { 120, 280 }, { 200, 180 },
                { 1100, 600 }, { 1180, 500 }, { 1220, 360 }, { 1150, 220 }, { 1060, 150 },
                { 300, 80 }, { 500, 50 }, { 700, 70 }, { 900, 55 }, { 1050, 90 },
        };
        for (int i = 0; i < runePositions.length; i++) {
            char g = runeChars.charAt(MathUtils.random(runeChars.length() - 1));
            Color col = new Color(runeColors[MathUtils.random(runeColors.length - 1)]);
            runes.add(new Rune(runePositions[i][0], runePositions[i][1], g,
                    MathUtils.random(MathUtils.PI2), col));
        }

        background = tryTexture(
                "backgrounds/main.jpg", "backgrounds/main.jpg",
                "backgrounds/main.jpg", "backgrounds/main.jpg");
        if (background != null) {
            background.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        }
    }

    private Texture tryTexture(String... paths) {
        for (String p : paths) {
            try {
                if (Gdx.files.internal(p).exists()) {
                    return new Texture(Gdx.files.internal(p), true);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    public void update(float delta) {
        animationTime += delta;
        ambientTimer += delta;

        for (MagicOrb orb : orbs)
            orb.update(delta);
        for (FloatStar s : floatStars)
            s.update(delta);

        if (ambientTimer > 0.15f) {
            ambientTimer = 0f;
            for (MagicOrb orb : orbs) {
                if (MathUtils.randomBoolean(0.4f)) {
                    Sparkle s = new Sparkle(orb.getX(), orb.getY());
                    s.vx *= 0.3f;
                    s.vy *= 0.3f;
                    sparkles.add(s);
                }
            }
        }

        for (int i = sparkles.size() - 1; i >= 0; i--) {
            sparkles.get(i).update(delta);
            if (sparkles.get(i).isDead())
                sparkles.remove(i);
        }
    }

    public void renderBackground(SpriteBatch batch, BitmapFont font) {
        if (background != null) {
            batch.draw(background, 0, 0, VW, VH);
        }

        for (FloatStar s : floatStars) {
            float a = s.getAlpha(animationTime);
            batch.setColor(s.color.r, s.color.g, s.color.b, a);
        }
        batch.setColor(Color.WHITE);

        for (Rune r : runes) {
            float a = r.getAlpha(animationTime);
            font.getData().setScale(1.6f);
            font.setColor(r.color.r, r.color.g, r.color.b, a);
            font.draw(batch, String.valueOf(r.glyph), r.x, r.y);
        }
        font.setColor(Color.WHITE);
    }

    public void renderForeground(ShapeRenderer shapeRenderer) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (MagicOrb orb : orbs) {
            shapeRenderer.setColor(orb.color.r, orb.color.g, orb.color.b, 0.15f);
            shapeRenderer.circle(orb.getX(), orb.getY(), orb.size * 3f);
            shapeRenderer.setColor(orb.color.r, orb.color.g, orb.color.b, 0.35f);
            shapeRenderer.circle(orb.getX(), orb.getY(), orb.size * 1.8f);
            shapeRenderer.setColor(orb.color.r, orb.color.g, orb.color.b, 0.9f);
            shapeRenderer.circle(orb.getX(), orb.getY(), orb.size * 0.7f);
        }

        for (Sparkle s : sparkles) {
            float a = s.getAlpha();
            shapeRenderer.setColor(s.color.r, s.color.g, s.color.b, a * 0.9f);
            if (s.shape == 0) {
                shapeRenderer.rect(s.x, s.y, s.size, s.size);
            } else if (s.shape == 1) {
                float h = s.size / 2f;
                shapeRenderer.rect(s.x - h, s.y, s.size, s.size * 0.35f);
                shapeRenderer.rect(s.x, s.y - h, s.size * 0.35f, s.size);
            } else {
                shapeRenderer.rect(s.x - s.size, s.y - s.size * 0.2f, s.size * 2f, s.size * 0.4f);
                shapeRenderer.rect(s.x - s.size * 0.2f, s.y - s.size, s.size * 0.4f, s.size * 2f);
            }
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void dispose() {
        if (background != null) background.dispose();
    }
}
