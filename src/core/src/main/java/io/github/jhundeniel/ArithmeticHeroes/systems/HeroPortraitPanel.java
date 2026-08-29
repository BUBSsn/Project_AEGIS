package io.github.jhundeniel.ArithmeticHeroes.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.managers.TurnManager;

import java.util.List;

public class HeroPortraitPanel {

    private final SpriteBatch   batch;
    private final ShapeRenderer shapes;
    private final TurnManager   turnManager;
    private final List<Entity>  heroes;
    private final List<Texture> portraits;

    // Dedicated camera — always stays in screen pixel space
    private final OrthographicCamera camera;
    private final Viewport           viewport;

    private static final float PORT_SIZE    = 90f;
    private static final float PORT_GAP     = 10f;
    private static final float PORT_X       = 8f;
    private static final float BORDER_ACT   = 2f;
    private static final float BORDER_INACT = 1f;

    // Portraits start this many px below the top of screen (below action log)
    private static final float TOP_MARGIN = 145f;

    private static final Color BG_COL       = new Color(0.85f, 0.85f, 0.82f, 1f);
    private static final Color BORDER_ACT_C = new Color(1f,    0.92f, 0.15f, 1f);
    private static final Color BORDER_IN_C  = new Color(0.30f, 0.30f, 0.35f, 0.9f);
    private static final Color GLOW_C       = new Color(1f,    0.90f, 0.10f, 0.40f);
    private static final Color DIM          = new Color(0.35f, 0.35f, 0.40f, 1f);

    private float   glowTimer = 0f;
    private final Texture bgTex;

    private final ComponentMapper<VisualComponent> vm = ComponentMapper.getFor(VisualComponent.class);
    private final ComponentMapper<StatsComponent>  sm = ComponentMapper.getFor(StatsComponent.class);

    public HeroPortraitPanel(SpriteBatch batch, TurnManager turnManager,
                             List<Entity> heroes, List<Texture> portraits) {
        this.batch       = batch;
        this.turnManager = turnManager;
        this.heroes      = heroes;
        this.portraits   = portraits;
        this.shapes      = new ShapeRenderer();

        // Own camera that always maps 1:1 to screen pixels
        this.camera   = new OrthographicCamera();
        this.viewport = new ScreenViewport(camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(BG_COL); px.fill();
        bgTex = new Texture(px); px.dispose();
    }

    public void render(float delta) {
        glowTimer += delta;

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        // Always sync camera to current screen size
        viewport.update(sw, sh, true);
        camera.update();

        // Apply our camera's projection to both renderers
        Matrix4 proj = camera.combined;
        shapes.setProjectionMatrix(proj);
        batch.setProjectionMatrix(proj);

        Entity current = turnManager.getCurrentEntityTurn();

        // Anchor from top of screen — always below action log
        float startY = sh - TOP_MARGIN - PORT_SIZE;

        for (int i = 0; i < heroes.size(); i++) {
            Entity  hero        = heroes.get(i);
            float   portY       = startY - i * (PORT_SIZE + PORT_GAP);
            boolean isActive    = (hero == current);
            Texture portraitTex = (portraits != null && i < portraits.size())
                ? portraits.get(i) : null;
            drawPortrait(hero, portraitTex, PORT_X, portY, isActive);
        }
    }

    private void drawPortrait(Entity hero, Texture portraitTex,
                              float x, float y, boolean isActive) {
        float border = isActive ? BORDER_ACT : BORDER_INACT;
        float inner  = border + 1f;

        // 1. Glow + background (shapes)
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (isActive) {
            float pulse = (MathUtils.sin(glowTimer * 3f) + 1f) / 2f;
            float alpha = 0.30f + pulse * 0.65f;
            float glow  = 10f;
            shapes.setColor(GLOW_C.r, GLOW_C.g, GLOW_C.b, alpha);
            shapes.rect(x - glow, y - glow, PORT_SIZE + glow * 2, PORT_SIZE + glow * 2);
        }
        shapes.setColor(BG_COL);
        shapes.rect(x, y, PORT_SIZE, PORT_SIZE);
        shapes.end();

        // 2. Portrait image (batch)
        batch.begin();
        batch.setColor(isActive ? Color.WHITE : DIM);

        boolean usePortrait = portraitTex != null
            && portraitTex.getWidth() > 1
            && portraitTex.getHeight() > 1;

        if (usePortrait) {
            batch.draw(portraitTex,
                x + inner, y + inner,
                PORT_SIZE - inner * 2, PORT_SIZE - inner * 2);
        } else {
            VisualComponent v = vm.get(hero);
            if (v != null) {
                batch.draw(v.getCurrentFrame(),
                    x + inner, y + inner,
                    PORT_SIZE - inner * 2, PORT_SIZE - inner * 2);
            }
        }
        batch.setColor(Color.WHITE);
        batch.end();

        // 3. Border on top (shapes)
        Color bc = isActive ? BORDER_ACT_C : BORDER_IN_C;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(bc);
        shapes.rect(x,                      y,                      PORT_SIZE, border);
        shapes.rect(x,                      y + PORT_SIZE - border, PORT_SIZE, border);
        shapes.rect(x,                      y,                      border,    PORT_SIZE);
        shapes.rect(x + PORT_SIZE - border, y,                      border,    PORT_SIZE);
        shapes.end();
    }

    public void resize(int w, int h) {
        viewport.update(w, h, true);
        camera.update();
    }

    public void dispose() {
        shapes.dispose();
        bgTex.dispose();
    }
}
