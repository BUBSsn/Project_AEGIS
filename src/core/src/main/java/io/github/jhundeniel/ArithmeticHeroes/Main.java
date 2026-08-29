package io.github.jhundeniel.ArithmeticHeroes;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.CharacterRegistry;
import io.github.jhundeniel.ArithmeticHeroes.managers.SkillRegistry;
import io.github.jhundeniel.ArithmeticHeroes.managers.StageRegistry;
import io.github.jhundeniel.ArithmeticHeroes.screens.MainMenuScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {

    public SpriteBatch batch;
    public ArithmeticAssetManager assetManager;
    public Skin skin;
    
    public float fadeAlpha = 0f;
    public float fadeSpeed = 1.8f;
    public boolean fadingOut = false;
    public boolean fadingIn = false;
    private Screen pendingScreen = null;
    private Texture blackTex;

    @Override
    public void create() {
        batch = new SpriteBatch();
        skin = new Skin(Gdx.files.internal("ui/skin.json"));
        
        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(Color.BLACK);
        px.fill();
        blackTex = new Texture(px);
        px.dispose();

        try {
            com.badlogic.gdx.graphics.Pixmap orig = new com.badlogic.gdx.graphics.Pixmap(Gdx.files.internal("other_asset/mouse_cursor.png"));
            int size = 32; 
            com.badlogic.gdx.graphics.Pixmap scaled = new com.badlogic.gdx.graphics.Pixmap(size, size, orig.getFormat());
            scaled.setFilter(com.badlogic.gdx.graphics.Pixmap.Filter.BiLinear);
            scaled.drawPixmap(orig, 0, 0, orig.getWidth(), orig.getHeight(), 0, 0, scaled.getWidth(), scaled.getHeight());
            com.badlogic.gdx.graphics.Cursor customCursor = Gdx.graphics.newCursor(scaled, 0, 0);
            Gdx.graphics.setCursor(customCursor);
            orig.dispose();
            scaled.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //1. Load Assets
        assetManager = new ArithmeticAssetManager();
        assetManager.queueImages();
        assetManager.finishLoading();

        //2. Load Skills & All Data
        SkillRegistry.loadSkills();
        CharacterRegistry.loadCharacters();
        StageRegistry.loadStages();

        //3. Start Game with Main Menu
        this.fadingIn = true;
        this.fadeAlpha = 1f;
        this.setScreen(new MainMenuScreen(this));
    }

    public void transitionToScreen(Screen nextScreen) {
        if (fadingOut) return;
        pendingScreen = nextScreen;
        fadingOut = true;
        fadingIn = false;
        fadeAlpha = 0f;
    }

    @Override
    public void render() {
        super.render();
        if (assetManager != null) {
            assetManager.updateMusic(Gdx.graphics.getDeltaTime());
        }

        if (fadingOut) {
            fadeAlpha += Gdx.graphics.getDeltaTime() * fadeSpeed;
            if (fadeAlpha >= 1f) {
                fadeAlpha = 1f;
                fadingOut = false;
                fadingIn = true;
                if (pendingScreen != null) {
                    setScreen(pendingScreen);
                    pendingScreen = null;
                }
            }
        } else if (fadingIn) {
            fadeAlpha -= Gdx.graphics.getDeltaTime() * fadeSpeed;
            if (fadeAlpha <= 0f) {
                fadeAlpha = 0f;
                fadingIn = false;
            }
        }

        if (fadeAlpha > 0f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.begin();
            batch.setColor(0, 0, 0, fadeAlpha);
            batch.draw(blackTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        skin.dispose();
        if (assetManager != null) {
            assetManager.dispose();
        }
        if (blackTex != null) {
            blackTex.dispose();
        }
    }
}
