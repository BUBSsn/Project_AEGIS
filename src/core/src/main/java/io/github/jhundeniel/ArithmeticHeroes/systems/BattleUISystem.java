package io.github.jhundeniel.ArithmeticHeroes.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.jhundeniel.ArithmeticHeroes.components.BattleUIComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent;

import java.util.ArrayList;
import java.util.List;

public class BattleUISystem extends IteratingSystem {

    private final Stage stage;
    private final Skin skin;
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch batch;

    private final ComponentMapper<StatsComponent> statsMapper = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<VisualComponent> visualMapper = ComponentMapper.getFor(VisualComponent.class);

    // UI Tables
    private Table rootTable;
    private Table actionLogTable;
    private Table enemyIntentTable;
    private Table skillButtonsTable;
    private Table turnOrderTable;

    // Action Log
    private Label actionLogTitle;
    private final List<Label> actionLogLines;
    private static final int MAX_LOG_LINES = 5;

    // Enemy Intent
    private Label enemyIntentTitle;
    private Label enemyMoveLabel;
    private Label enemyTargetLabel;

    // Skill Buttons
    private TextButton healBtn;
    private TextButton pokeBtn;
    private TextButton singleBtn;
    private TextButton allBtn;

    // Colors
    private static final Color ACTION_LOG_BG = new Color(0.1f, 0.05f, 0f, 0.9f);
    private static final Color ENEMY_INTENT_BG = new Color(0.3f, 0f, 0f, 0.9f);
    private static final Color SKILL_BTN_BG = new Color(0.15f, 0.35f, 0.25f, 0.95f);
    private static final Color RETRO_YELLOW = new Color(1f, 0.95f, 0.3f, 1f);
    private static final Color RETRO_GREEN = new Color(0.3f, 0.95f, 0.3f, 1f);

    private BattleUIComponent uiComponent;

    public BattleUISystem(SpriteBatch batch) {
        super(Family.all(BattleUIComponent.class).get());

        this.batch = batch;
        this.stage = new Stage(new ScreenViewport(), batch);
        this.shapeRenderer = new ShapeRenderer();
        this.skin = createSkin();
        this.actionLogLines = new ArrayList<>();

        setupUI();
    }

    private Skin createSkin() {
        Skin skin = new Skin();

        // Load fonts
        try {
            BitmapFont pixelFont = new BitmapFont(Gdx.files.internal("ui/font export.fnt"));
            pixelFont.getData().setScale(1.0f);
            skin.add("default", pixelFont, BitmapFont.class);

            BitmapFont pixelFontSmall = new BitmapFont(Gdx.files.internal("ui/font-small export.fnt"));
            pixelFontSmall.getData().setScale(1.0f);
            skin.add("small", pixelFontSmall, BitmapFont.class);

            System.out.println("✓ Pixel fonts loaded!");
        } catch (Exception e) {
            System.out.println("⚠ Using fallback fonts");
            BitmapFont defaultFont = new BitmapFont();
            defaultFont.getData().setScale(1.5f);
            skin.add("default", defaultFont, BitmapFont.class);

            BitmapFont smallFont = new BitmapFont();
            smallFont.getData().setScale(1.0f);
            skin.add("small", smallFont, BitmapFont.class);
        }

        // Create textures
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        // Action log background
        pixmap.setColor(ACTION_LOG_BG);
        pixmap.fill();
        skin.add("action-log-bg", new Texture(pixmap));

        // Enemy intent background
        pixmap.setColor(ENEMY_INTENT_BG);
        pixmap.fill();
        skin.add("enemy-intent-bg", new Texture(pixmap));

        // Button backgrounds
        pixmap.setColor(SKILL_BTN_BG);
        pixmap.fill();
        skin.add("button-bg", new Texture(pixmap));

        pixmap.setColor(new Color(0.2f, 0.5f, 0.35f, 0.95f));
        pixmap.fill();
        skin.add("button-over", new Texture(pixmap));

        pixmap.setColor(new Color(0.1f, 0.25f, 0.15f, 0.95f));
        pixmap.fill();
        skin.add("button-down", new Texture(pixmap));

        pixmap.dispose();

        // Styles
        Label.LabelStyle defaultLabel = new Label.LabelStyle();
        defaultLabel.font = skin.getFont("default");
        defaultLabel.fontColor = Color.WHITE;
        skin.add("default", defaultLabel);

        Label.LabelStyle smallLabel = new Label.LabelStyle();
        smallLabel.font = skin.getFont("small");
        smallLabel.fontColor = Color.WHITE;
        skin.add("small", smallLabel);

        Label.LabelStyle yellowLabel = new Label.LabelStyle();
        yellowLabel.font = skin.getFont("default");
        yellowLabel.fontColor = RETRO_YELLOW;
        skin.add("yellow", yellowLabel);

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = skin.getFont("small");
        btnStyle.fontColor = Color.WHITE;
        btnStyle.up = new TextureRegionDrawable(new TextureRegion(skin.get("button-bg", Texture.class)));
        btnStyle.over = new TextureRegionDrawable(new TextureRegion(skin.get("button-over", Texture.class)));
        btnStyle.down = new TextureRegionDrawable(new TextureRegion(skin.get("button-down", Texture.class)));
        skin.add("default", btnStyle);

        return skin;
    }

    private void setupUI() {
        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        setupActionLog();
        setupEnemyIntent();
        setupTurnOrder();
        setupSkillButtons();
    }

    /**
     * ACTION LOG - Bottom left corner
     */
    private void setupActionLog() {
        actionLogTable = new Table();
        actionLogTable.setBackground(new TextureRegionDrawable(
            new TextureRegion(skin.get("action-log-bg", Texture.class))));

        // Title
        actionLogTitle = new Label("ACTION LOG", skin, "yellow");
        actionLogTitle.setAlignment(Align.left);
        actionLogTable.add(actionLogTitle).pad(10).left().row();

        // Initialize log lines
        for (int i = 0; i < MAX_LOG_LINES; i++) {
            Label logLine = new Label("", skin, "small");
            logLine.setAlignment(Align.left);
            logLine.setWrap(true);
            actionLogLines.add(logLine);
            actionLogTable.add(logLine).pad(2).left().width(480).row();
        }

        // Position bottom-left
        actionLogTable.setSize(500, 150);
        actionLogTable.setPosition(10, 10);
        stage.addActor(actionLogTable);
    }

    /**
     * ENEMY INTENT - Top right corner
     */
    private void setupEnemyIntent() {
        enemyIntentTable = new Table();
        enemyIntentTable.setBackground(new TextureRegionDrawable(
            new TextureRegion(skin.get("enemy-intent-bg", Texture.class))));

        // Title
        enemyIntentTitle = new Label("ENEMY INTENT", skin, "yellow");
        enemyIntentTitle.setAlignment(Align.center);
        enemyIntentTable.add(enemyIntentTitle).pad(10).center().row();

        // Move
        enemyMoveLabel = new Label("Move: NONE", skin, "small");
        enemyMoveLabel.setAlignment(Align.center);
        enemyIntentTable.add(enemyMoveLabel).pad(5).center().row();

        // Target
        enemyTargetLabel = new Label("Target: SINGLE", skin, "small");
        enemyTargetLabel.setAlignment(Align.center);
        enemyIntentTable.add(enemyTargetLabel).pad(5).center();

        // Position top-right
        enemyIntentTable.setSize(200, 100);
        enemyIntentTable.setPosition(
            Gdx.graphics.getWidth() - 210,
            Gdx.graphics.getHeight() - 110
        );
        stage.addActor(enemyIntentTable);
    }

    /**
     * TURN ORDER - Top center
     */
    private void setupTurnOrder() {
        turnOrderTable = new Table();

        Label title = new Label("NEXT TURN ORDER", skin, "yellow");
        title.setAlignment(Align.center);
        turnOrderTable.add(title).pad(5).colspan(6).row();

        turnOrderTable.pack();
        turnOrderTable.setPosition(
            Gdx.graphics.getWidth() / 2 - turnOrderTable.getWidth() / 2,
            Gdx.graphics.getHeight() - turnOrderTable.getHeight() - 10
        );
        stage.addActor(turnOrderTable);
    }

    /**
     * SKILL BUTTONS - Bottom center
     */
    private void setupSkillButtons() {
        skillButtonsTable = new Table();

        // Main skill row
        Table skillRow = new Table();

        healBtn = new TextButton("HEAL", skin);
        healBtn.getLabel().setColor(RETRO_GREEN);
        healBtn.pad(10);
        skillRow.add(healBtn).pad(5).width(100).height(40);

        pokeBtn = new TextButton("POKE", skin);
        pokeBtn.getLabel().setColor(RETRO_YELLOW);
        pokeBtn.pad(10);
        skillRow.add(pokeBtn).pad(5).width(100).height(40);

        skillButtonsTable.add(skillRow).row();

        // Target mode row
        Table targetRow = new Table();

        singleBtn = new TextButton("SINGLE", skin);
        singleBtn.pad(8);
        targetRow.add(singleBtn).pad(5).width(90).height(30);

        allBtn = new TextButton("ALL", skin);
        allBtn.pad(8);
        targetRow.add(allBtn).pad(5).width(90).height(30);

        skillButtonsTable.add(targetRow).padTop(5);

        // Position bottom-center
        skillButtonsTable.pack();
        skillButtonsTable.setPosition(
            Gdx.graphics.getWidth() / 2 - skillButtonsTable.getWidth() / 2,
            20
        );
        stage.addActor(skillButtonsTable);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        uiComponent = entity.getComponent(BattleUIComponent.class);
        if (uiComponent == null) return;

        updateActionLog();
        updateEnemyIntent();
        updateTurnOrder();
    }

    /**
     * Update action log from console messages
     */
    private void updateActionLog() {
        if (uiComponent.battleLog.isEmpty()) return;

        // Get last N messages
        int startIndex = Math.max(0, uiComponent.battleLog.size() - MAX_LOG_LINES);
        List<String> recentMessages = uiComponent.battleLog.subList(startIndex, uiComponent.battleLog.size());

        // Update labels
        for (int i = 0; i < MAX_LOG_LINES; i++) {
            if (i < recentMessages.size()) {
                actionLogLines.get(i).setText(recentMessages.get(i));
            } else {
                actionLogLines.get(i).setText("");
            }
        }
    }

    /**
     * Update enemy intent display
     */
    private void updateEnemyIntent() {
        if (uiComponent.enemyMove != null) {
            enemyMoveLabel.setText("Move: " + uiComponent.enemyMove);
        } else {
            enemyMoveLabel.setText("Move: NONE");
        }

        if (uiComponent.enemyTarget != null) {
            StatsComponent targetStats = statsMapper.get(uiComponent.enemyTarget);
            if (targetStats != null) {
                enemyTargetLabel.setText("Target: " + targetStats.name);
            }
        } else {
            enemyTargetLabel.setText("Target: SINGLE");
        }
    }

    /**
     * Update turn order indicators
     */
    private void updateTurnOrder() {
        turnOrderTable.clear();

        Label title = new Label("NEXT TURN ORDER", skin, "yellow");
        title.setAlignment(Align.center);
        turnOrderTable.add(title).pad(5).colspan(Math.min(6, uiComponent.turnOrder.size())).row();

        for (int i = 0; i < uiComponent.turnOrder.size() && i < 6; i++) {
            Label indicator = new Label("[" + (i+1) + "]", skin, "small");

            if (i == uiComponent.currentTurnIndex) {
                indicator.setColor(RETRO_YELLOW);
                indicator.setFontScale(1.3f);
            } else {
                indicator.setColor(Color.LIGHT_GRAY);
            }

            turnOrderTable.add(indicator).pad(3).width(50);
        }
    }

    /**
     * Add message to action log
     */
    public void addLogMessage(String message) {
        if (uiComponent != null) {
            uiComponent.addLogMessage(message);
        }
    }

    /**
     * Set enemy intent
     */
    public void setEnemyIntent(String move, Entity target) {
        if (uiComponent != null) {
            uiComponent.enemyMove = move;
            uiComponent.enemyTarget = target;
        }
    }

    public void render() {
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public Stage getStage() {
        return stage;
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);

        // Reposition elements
        if (enemyIntentTable != null) {
            enemyIntentTable.setPosition(
                width - 210,
                height - 110
            );
        }

        if (turnOrderTable != null) {
            turnOrderTable.setPosition(
                width / 2 - turnOrderTable.getWidth() / 2,
                height - turnOrderTable.getHeight() - 10
            );
        }

        if (skillButtonsTable != null) {
            skillButtonsTable.setPosition(
                width / 2 - skillButtonsTable.getWidth() / 2,
                20
            );
        }
    }

    public void dispose() {
        stage.dispose();
        shapeRenderer.dispose();
        skin.dispose();
    }

    public TextButton getHealButton() { return healBtn; }
    public TextButton getPokeButton() { return pokeBtn; }
    public TextButton getSingleButton() { return singleBtn; }
    public TextButton getAllButton() { return allBtn; }
}
