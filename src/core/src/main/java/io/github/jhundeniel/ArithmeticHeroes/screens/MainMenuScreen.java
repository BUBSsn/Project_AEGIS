package io.github.jhundeniel.ArithmeticHeroes.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.jhundeniel.ArithmeticHeroes.Main;
import io.github.jhundeniel.ArithmeticHeroes.managers.LeaderboardManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.SaveManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.VolumeSettings;

public class MainMenuScreen implements Screen {

    private static final float VW = 1280f;
    private static final float VH = 720f;

    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;
    private final Viewport viewport;

    private final Texture background;
    private final Texture titleLogo;
    private final Texture buttonBg;
    private final BitmapFont font;

    private int selectedOption = 0;
    private static final int MAX_OPTIONS = 5;

    private Rectangle newGameBtnRect;
    private Rectangle loadGameBtnRect;
    private Rectangle stageSelectBtnRect;
    private Rectangle leaderboardBtnRect;
    private Rectangle exitBtnRect;
    private Rectangle resetBtnRect;

    // ── Volume sliders (top-right) ────────────────────────────────────────
    private static final float SLIDER_W     = 120f;
    private static final float SLIDER_H     = 5f;
    private static final float KNOB_RADIUS  = 7f;
    private static final float SLIDER_GAP   = 24f;

    private final Rectangle musicSliderTrack = new Rectangle();
    private final Rectangle sfxSliderTrack   = new Rectangle();
    private boolean draggingMusic = false;
    private boolean draggingSfx   = false;

    private float animationTime = 0f;

    private boolean isTransitioning = false;

    // ── Particle / effect inner classes ──────────────────────────────────

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
                case 0:
                    color = new Color(0.85f, 0.20f, 1.00f, 1f);
                    break;
                case 1:
                    color = new Color(1.00f, 0.40f, 0.80f, 1f);
                    break;
                case 2:
                    color = new Color(0.30f, 0.85f, 1.00f, 1f);
                    break;
                case 3:
                    color = new Color(1.00f, 0.92f, 0.30f, 1f);
                    break;
                case 4:
                    color = new Color(0.40f, 1.00f, 0.60f, 1f);
                    break;
                default:
                    color = new Color(1.00f, 1.00f, 1.00f, 1f);
                    break;
            }
        }

        void update(float d) {
            x += vx * d;
            y += vy * d;
            vy -= 120f * d;
            life -= d;
        }

        boolean isDead() {
            return life <= 0;
        }

        float getAlpha() {
            return MathUtils.clamp(life / maxLife, 0f, 1f);
        }
    }

    private static class FloatStar {
        float x, y, vy, life, maxLife, size, twinkle;
        Color color;

        FloatStar() {
            reset();
        }

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
                case 0:
                    color = new Color(0.8f, 0.5f, 1.0f, 1f);
                    break;
                case 1:
                    color = new Color(0.5f, 0.9f, 1.0f, 1f);
                    break;
                case 2:
                    color = new Color(1.0f, 0.9f, 0.5f, 1f);
                    break;
                default:
                    color = new Color(1.0f, 1.0f, 1.0f, 1f);
                    break;
            }
        }

        void update(float d) {
            y += vy * d;
            life -= d;
            if (isDead())
                reset();
        }

        boolean isDead() {
            return life <= 0 || y > VH + 20;
        }

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

        void update(float d) {
            angle += speed * d;
        }

        float getX() {
            return VW / 2f + MathUtils.cos(angle) * radius;
        }

        float getY() {
            return baseY + MathUtils.sin(angle) * radius * 0.3f;
        }
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

    private float auraPhase = 0f;

    private final java.util.ArrayList<Sparkle> sparkles = new java.util.ArrayList<>();
    private final java.util.ArrayList<FloatStar> floatStars = new java.util.ArrayList<>();
    private final java.util.ArrayList<MagicOrb> orbs = new java.util.ArrayList<>();
    private final java.util.ArrayList<Rune> runes = new java.util.ArrayList<>();

    private float sparkleTimer = 0f;
    private float ambientTimer = 0f;

    private final Main game;

    // ── Constructor ───────────────────────────────────────────────────────

    public MainMenuScreen(Main game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new FitViewport(VW, VH, camera); // FitViewport keeps 16:9, no stretching
        viewport.apply(true);

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        background = tryTexture(
                "backgrounds/main.jpg", "backgrounds/main.jpg",
                "backgrounds/main.jpg", "backgrounds/main.jpg");
        titleLogo = tryTexture("ui/title.png", "ui/Title.png", "ui/12-removebg-preview.png");
        buttonBg = tryTexture("ui/box.png", "ui/Box.png", "ui/13-removebg-preview.png");

        if (background != null)
            background.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        setLinear(titleLogo);
        setLinear(buttonBg);

        font = new BitmapFont();

        buildLayout();
        initEffects();
    }

    // ── Layout ────────────────────────────────────────────────────────────

    private void buildLayout() {
        float btnW = VW * 0.20f;
        float btnH = VH * 0.09f;
        float gap = VH * 0.005f;

        float centerX = VW / 2f - btnW / 2f;

        float newGameY = VH * 0.42f;
        float loadGameY = newGameY - btnH - gap;
        float stageSelectY = loadGameY - btnH - gap;
        float leaderboardY = stageSelectY - btnH - gap;
        float exitY = leaderboardY - btnH - gap;

        newGameBtnRect = new Rectangle(centerX, newGameY, btnW, btnH);
        loadGameBtnRect = new Rectangle(centerX, loadGameY, btnW, btnH);
        stageSelectBtnRect = new Rectangle(centerX, stageSelectY, btnW, btnH);
        leaderboardBtnRect = new Rectangle(centerX, leaderboardY, btnW, btnH);
        exitBtnRect = new Rectangle(centerX, exitY, btnW, btnH);

        resetBtnRect = new Rectangle(15f, 15f, 160f, 36f);

        // Volume sliders — top-right corner
        float sliderX = VW - SLIDER_W - 40f;
        float sfxSliderY   = VH - 30f;
        float musicSliderY = sfxSliderY - SLIDER_GAP;
        musicSliderTrack.set(sliderX, musicSliderY, SLIDER_W, SLIDER_H);
        sfxSliderTrack.set(sliderX, sfxSliderY, SLIDER_W, SLIDER_H);
    }
    // ── Effects init ──────────────────────────────────────────────────────

    private void initEffects() {
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
    }

    // ── Texture helpers ───────────────────────────────────────────────────

    private Texture tryTexture(String... paths) {
        for (String p : paths) {
            try {
                if (Gdx.files.internal(p).exists()) {
                    Texture t = new Texture(Gdx.files.internal(p), true);
                    Gdx.app.log("Menu", "Loaded: " + p);
                    return t;
                }
            } catch (Exception e) {
                Gdx.app.log("Menu", "Error: " + p + " – " + e.getMessage());
            }
        }
        return null;
    }

    private void setLinear(Texture t) {
        if (t != null)
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    // ── Screen lifecycle ──────────────────────────────────────────────────

    @Override
    public void show() {
        game.assetManager.playMusic(io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager.BGM_TITLE,
                true);
    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0.05f, 0.08f, 0.18f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // 1. Background image + decorative text
        batch.begin();
        if (background != null)
            batch.draw(background, 0, 0, VW, VH);
        drawFloatStars();
        drawTitleLogo();
        drawRuneGlyphs();
        batch.end();

        // 3. Buttons on top of panel
        batch.begin();
        drawButton(newGameBtnRect, "NEW GAME", selectedOption == 0);
        drawButton(loadGameBtnRect, "LOAD GAME", selectedOption == 1);
        drawButton(stageSelectBtnRect, "STAGE SELECT", selectedOption == 2);
        drawButton(leaderboardBtnRect, "LEADERBOARD", selectedOption == 3);
        drawButton(exitBtnRect, "EXIT", selectedOption == 4);
        batch.end();

        // 4. Reset button (bottom-left corner)
        drawResetButton();

        // 4b. Volume sliders (bottom-right corner)
        drawVolumeSliders();

        // 5. Foreground particle effects on top of everything
        drawForegroundEffects();
    }

    // ── Draw single button ────────────────────────────────────────────────

    private void drawButton(Rectangle rect, String label, boolean selected) {
        float scale = selected ? 1.05f : 1.0f;
        float ew = rect.width * (scale - 1f);
        float eh = rect.height * (scale - 1f);
        float bx = rect.x - ew / 2f;
        float by = rect.y - eh / 2f;
        float bw = rect.width + ew;
        float bh = rect.height + eh;

        // Button image background
        if (buttonBg != null) {
            batch.setColor(selected
                    ? new Color(1.00f, 0.92f, 0.35f, 1f) // bright gold — selected
                    : new Color(0.60f, 0.52f, 0.22f, 1f)); // muted gold — idle
            batch.draw(buttonBg, bx, by, bw, bh);
            batch.setColor(Color.WHITE);
        } else {
            // Fallback solid rectangle if image is missing
            batch.end();
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(selected
                    ? new Color(0.75f, 0.60f, 0.10f, 0.95f)
                    : new Color(0.25f, 0.20f, 0.08f, 0.90f));
            shapeRenderer.rect(bx, by, bw, bh);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
            batch.begin();
        }

        // Font — fixed small scale so text always fits
        float fScale = selected ? 1.8f : 1.5f;
        font.getData().setScale(fScale);
        font.setColor(selected
                ? new Color(0.06f, 0.02f, 0.00f, 1f) // very dark text on bright gold
                : new Color(0.12f, 0.07f, 0.00f, 1f));

        GlyphLayout gl = new GlyphLayout(font, label);

        // Safety clamp: if text is still wider than button, shrink further
        if (gl.width > bw - 20f) {
            font.getData().setScale(fScale * ((bw - 20f) / gl.width));
            gl = new GlyphLayout(font, label);
        }

        font.draw(batch, label,
                bx + bw / 2f - gl.width / 2f,
                by + bh / 2f + gl.height / 2f);
    }

    // ── Visual effects ────────────────────────────────────────────────────

    private void drawForegroundEffects() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Magic orbs
        for (MagicOrb orb : orbs) {
            shapeRenderer.setColor(orb.color.r, orb.color.g, orb.color.b, 0.15f);
            shapeRenderer.circle(orb.getX(), orb.getY(), orb.size * 3f);
            shapeRenderer.setColor(orb.color.r, orb.color.g, orb.color.b, 0.35f);
            shapeRenderer.circle(orb.getX(), orb.getY(), orb.size * 1.8f);
            shapeRenderer.setColor(orb.color.r, orb.color.g, orb.color.b, 0.9f);
            shapeRenderer.circle(orb.getX(), orb.getY(), orb.size * 0.7f);
        }

        // Selected button aura
        Rectangle selRect;
        switch (selectedOption) {
            case 0:
                selRect = newGameBtnRect;
                break;
            case 1:
                selRect = loadGameBtnRect;
                break;
            case 2:
                selRect = stageSelectBtnRect;
                break;
            case 3:
                selRect = leaderboardBtnRect;
                break;
            default:
                selRect = exitBtnRect;
                break;
        }
        float aCx = selRect.x + selRect.width / 2f;
        float aCy = selRect.y + selRect.height / 2f;
        for (int ring = 0; ring < 3; ring++) {
            float rPhase = auraPhase + ring * MathUtils.PI2 / 3f;
            float rPulse = 1f + 0.10f * MathUtils.sin(rPhase);
            float alpha = 0.05f - ring * 0.015f;
            shapeRenderer.setColor(1f, 0.85f, 0.2f, alpha);
            shapeRenderer.ellipse(
                    aCx - selRect.width * 0.58f * rPulse,
                    aCy - selRect.height * 0.72f * rPulse,
                    selRect.width * 1.16f * rPulse,
                    selRect.height * 1.44f * rPulse);
        }

        // Sparkles
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

    private void drawFloatStars() {
        // Stars are just tinted dots using the batch color — no texture needed
        for (FloatStar s : floatStars) {
            float a = s.getAlpha(animationTime);
            batch.setColor(s.color.r, s.color.g, s.color.b, a);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawRuneGlyphs() {
        for (Rune r : runes) {
            float a = r.getAlpha(animationTime);
            font.getData().setScale(1.6f);
            font.setColor(r.color.r, r.color.g, r.color.b, a);
            font.draw(batch, String.valueOf(r.glyph), r.x, r.y);
        }
    }

    private void drawTitleLogo() {
        if (titleLogo != null) {
            float logoW = VW * 0.75f;
            float logoH = logoW * ((float) titleLogo.getHeight() / titleLogo.getWidth());
            float logoX = VW / 2f - logoW / 2f;
            float logoY = VH - logoH - VH * 0.01f + MathUtils.sin(animationTime * 1.3f) * 10f;
            float shimmer = 0.92f + 0.08f * MathUtils.sin(animationTime * 2.5f);
            batch.setColor(shimmer, shimmer, shimmer * 0.85f, 1f);
            batch.draw(titleLogo, logoX, logoY, logoW, logoH);
            batch.setColor(Color.WHITE);
        } else {
            // Fallback text title
            font.getData().setScale(5f);
            font.setColor(1f, 0.88f, 0.25f, 1f);
            GlyphLayout gl = new GlyphLayout(font, "ARITHMETIC HEROES");
            float ty = VH - 70 + MathUtils.sin(animationTime * 1.3f) * 10f;
            font.draw(batch, "ARITHMETIC HEROES", VW / 2f - gl.width / 2f, ty);
        }
    }

    // ── Reset button ──────────────────────────────────────────────────────

    private void drawResetButton() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.4f, 0.12f, 0.12f, 0.7f);
        shapeRenderer.rect(resetBtnRect.x, resetBtnRect.y,
                resetBtnRect.width, resetBtnRect.height);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.8f, 0.3f, 0.3f, 0.6f);
        shapeRenderer.rect(resetBtnRect.x, resetBtnRect.y,
                resetBtnRect.width, resetBtnRect.height);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.getData().setScale(0.9f);
        font.setColor(0.9f, 0.5f, 0.5f, 0.9f);
        GlyphLayout gl = new GlyphLayout(font, "RESET DATA");
        font.draw(batch, "RESET DATA",
                resetBtnRect.x + resetBtnRect.width / 2f - gl.width / 2f,
                resetBtnRect.y + resetBtnRect.height / 2f + gl.height / 2f);
        batch.end();
    }

    // ── Update / input ────────────────────────────────────────────────────

    private void update(float delta) {
        animationTime += delta;
        sparkleTimer += delta;
        ambientTimer += delta;
        auraPhase += delta * 3.0f;

        for (MagicOrb orb : orbs)
            orb.update(delta);
        for (FloatStar s : floatStars)
            s.update(delta);

        // Keyboard navigation
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selectedOption = (selectedOption - 1 + MAX_OPTIONS) % MAX_OPTIONS;
            spawnBurst(btnCenter(), 25);
            game.assetManager.playSound(ArithmeticAssetManager.SFX_BUTTON_HOVER);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selectedOption = (selectedOption + 1) % MAX_OPTIONS;
            spawnBurst(btnCenter(), 25);
            game.assetManager.playSound(ArithmeticAssetManager.SFX_BUTTON_HOVER);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.assetManager.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
            selectOption();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            if (Gdx.graphics.isFullscreen())
                Gdx.graphics.setWindowedMode(1280, 720);
            else
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }

        // Mouse / touch — unproject through FitViewport correctly
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse,
                viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight());
        float mx = mouse.x, my = mouse.y;

        int oldSelectedOption = selectedOption;

        if (newGameBtnRect.contains(mx, my))
            selectedOption = 0;
        else if (loadGameBtnRect.contains(mx, my))
            selectedOption = 1;
        else if (stageSelectBtnRect.contains(mx, my))
            selectedOption = 2;
        else if (leaderboardBtnRect.contains(mx, my))
            selectedOption = 3;
        else if (exitBtnRect.contains(mx, my))
            selectedOption = 4;

        if (oldSelectedOption != selectedOption) {
            game.assetManager.playSound(ArithmeticAssetManager.SFX_BUTTON_HOVER);
        }

        if (Gdx.input.justTouched()) {
            if (newGameBtnRect.contains(mx, my)) {
                selectedOption = 0;
                game.assetManager.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                selectOption();
            } else if (loadGameBtnRect.contains(mx, my)) {
                selectedOption = 1;
                game.assetManager.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                selectOption();
            } else if (stageSelectBtnRect.contains(mx, my)) {
                selectedOption = 2;
                game.assetManager.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                selectOption();
            } else if (leaderboardBtnRect.contains(mx, my)) {
                selectedOption = 3;
                game.assetManager.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                selectOption();
            } else if (exitBtnRect.contains(mx, my)) {
                selectedOption = 4;
                game.assetManager.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                selectOption();
            } else if (resetBtnRect.contains(mx, my)) {
                game.assetManager.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                resetAllData();
            }

            // Volume slider click-to-set
            if (isOnSlider(musicSliderTrack, mx, my)) {
                draggingMusic = true;
                updateSliderValue(musicSliderTrack, mx, true);
            } else if (isOnSlider(sfxSliderTrack, mx, my)) {
                draggingSfx = true;
                updateSliderValue(sfxSliderTrack, mx, false);
            }
        }

        // Volume slider dragging
        if (draggingMusic || draggingSfx) {
            if (Gdx.input.isTouched()) {
                // Re-unproject since we need fresh coords each frame while dragging
                Vector3 dragMouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                camera.unproject(dragMouse,
                        viewport.getScreenX(), viewport.getScreenY(),
                        viewport.getScreenWidth(), viewport.getScreenHeight());
                if (draggingMusic) updateSliderValue(musicSliderTrack, dragMouse.x, true);
                if (draggingSfx)   updateSliderValue(sfxSliderTrack, dragMouse.x, false);
            } else {
                // Released — persist to disk
                draggingMusic = false;
                draggingSfx   = false;
                VolumeSettings.getInstance().save();
            }
        }

        // Sparkle emitter near selected button
        if (sparkleTimer > 0.08f) {
            sparkleTimer = 0f;
            float[] c = btnCenter();
            for (int i = 0; i < 2; i++) {
                sparkles.add(new Sparkle(
                        c[0] + MathUtils.random(-60f, 60f),
                        c[1] + MathUtils.random(-15f, 15f)));
            }
        }

        // Ambient orb sparkles
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

    private float[] btnCenter() {
        Rectangle r;
        switch (selectedOption) {
            case 0:
                r = newGameBtnRect;
                break;
            case 1:
                r = loadGameBtnRect;
                break;
            case 2:
                r = stageSelectBtnRect;
                break;
            case 3:
                r = leaderboardBtnRect;
                break;
            default:
                r = exitBtnRect;
                break;
        }
        return new float[] { r.x + r.width / 2f, r.y + r.height / 2f };
    }

    private void spawnBurst(float[] c, int count) {
        for (int i = 0; i < count; i++)
            sparkles.add(new Sparkle(c[0], c[1]));
    }

    private void selectOption() {
        // Prevents Spam Clicks
        if (isTransitioning)
            return;
        isTransitioning = true;

        spawnBurst(btnCenter(), 50);
        switch (selectedOption) {
            case 0:
                game.transitionToScreen(new BattleScreen(game));
                break;
            case 1:
                game.transitionToScreen(new LoadGameScreen(game));
                break;
            case 2:
                game.transitionToScreen(new StageSelectScreen(game));
                break;
            case 3:
                game.transitionToScreen(new LeaderboardScreen(game));
                break;
            case 4:
                Gdx.app.exit();
                break;
        }
    }

    private void resetAllData() {
        // Prevents Spam Clicks
        if (isTransitioning)
            return;
        isTransitioning = true;

        SaveManager.deleteAllSaves();
        LeaderboardManager.clearAll();
        System.out.println("[RESET] All data cleared! Reloading menu...");
        game.transitionToScreen(new MainMenuScreen(game));
    }

    // ── Volume sliders ─────────────────────────────────────────────────────

    private void drawVolumeSliders() {
        VolumeSettings vol = VolumeSettings.getInstance();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Panel background behind sliders
        float panelPad = 10f;
        float panelX = musicSliderTrack.x - panelPad - 50f;
        float panelY = musicSliderTrack.y - panelPad - 4f;
        float panelW = SLIDER_W + panelPad * 2 + 85f;
        float panelH = SLIDER_GAP + SLIDER_H + panelPad * 2 + 8f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.04f, 0.03f, 0.10f, 0.75f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.5f, 0.3f, 0.8f, 0.5f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.end();

        // Draw each slider
        drawSlider(musicSliderTrack, vol.getMusicVolume(), new Color(0.4f, 0.7f, 1.0f, 1f), draggingMusic);
        drawSlider(sfxSliderTrack, vol.getSfxVolume(), new Color(1.0f, 0.7f, 0.3f, 1f), draggingSfx);

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Labels
        batch.begin();
        font.getData().setScale(0.75f);

        // Music label
        font.setColor(0.7f, 0.8f, 1.0f, 0.9f);
        GlyphLayout musicLabel = new GlyphLayout(font, "Music");
        font.draw(batch, "Music",
                musicSliderTrack.x - musicLabel.width - 10f,
                musicSliderTrack.y + SLIDER_H / 2f + musicLabel.height / 2f);

        // Music percentage
        int musicPct = Math.round(vol.getMusicVolume() * 100f);
        font.setColor(0.5f, 0.65f, 0.85f, 0.8f);
        font.draw(batch, musicPct + "%",
                musicSliderTrack.x + SLIDER_W + 8f,
                musicSliderTrack.y + SLIDER_H / 2f + musicLabel.height / 2f);

        // SFX label
        font.setColor(1.0f, 0.8f, 0.5f, 0.9f);
        GlyphLayout sfxLabel = new GlyphLayout(font, "SFX");
        font.draw(batch, "SFX",
                sfxSliderTrack.x - sfxLabel.width - 10f,
                sfxSliderTrack.y + SLIDER_H / 2f + sfxLabel.height / 2f);

        // SFX percentage
        int sfxPct = Math.round(vol.getSfxVolume() * 100f);
        font.setColor(0.85f, 0.65f, 0.3f, 0.8f);
        font.draw(batch, sfxPct + "%",
                sfxSliderTrack.x + SLIDER_W + 8f,
                sfxSliderTrack.y + SLIDER_H / 2f + sfxLabel.height / 2f);

        batch.end();
    }

    private void drawSlider(Rectangle track, float value, Color accentColor, boolean active) {
        float knobX = track.x + track.width * value;
        float centerY = track.y + track.height / 2f;

        // Track background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.15f, 0.12f, 0.25f, 0.9f);
        shapeRenderer.rect(track.x, track.y, track.width, track.height);

        // Filled portion
        shapeRenderer.setColor(accentColor.r, accentColor.g, accentColor.b, 0.8f);
        shapeRenderer.rect(track.x, track.y, track.width * value, track.height);
        shapeRenderer.end();

        // Knob
        float kr = active ? KNOB_RADIUS * 1.2f : KNOB_RADIUS;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(accentColor.r * 0.3f, accentColor.g * 0.3f, accentColor.b * 0.3f, 0.6f);
        shapeRenderer.circle(knobX + 2f, centerY - 2f, kr); // shadow
        shapeRenderer.setColor(accentColor);
        shapeRenderer.circle(knobX, centerY, kr);
        shapeRenderer.setColor(1f, 1f, 1f, 0.4f);
        shapeRenderer.circle(knobX - 1f, centerY + 1f, kr * 0.45f); // highlight
        shapeRenderer.end();
    }

    /** Returns true if (mx, my) is within the slider's clickable area. */
    private boolean isOnSlider(Rectangle track, float mx, float my) {
        float expand = KNOB_RADIUS + 6f;
        return mx >= track.x - expand && mx <= track.x + track.width + expand
                && my >= track.y - expand && my <= track.y + track.height + expand;
    }

    /** Updates VolumeSettings from the slider position and applies it live. */
    private void updateSliderValue(Rectangle track, float mx, boolean isMusic) {
        float normalized = (mx - track.x) / track.width;
        normalized = Math.max(0f, Math.min(1f, normalized));

        VolumeSettings vol = VolumeSettings.getInstance();
        if (isMusic) {
            vol.setMusicVolume(normalized);
            game.assetManager.applyMusicVolume();
        } else {
            vol.setSfxVolume(normalized);
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void resize(int w, int h) {
        viewport.update(w, h, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        if (background != null)
            background.dispose();
        if (titleLogo != null)
            titleLogo.dispose();
        if (buttonBg != null)
            buttonBg.dispose();
    }
}
