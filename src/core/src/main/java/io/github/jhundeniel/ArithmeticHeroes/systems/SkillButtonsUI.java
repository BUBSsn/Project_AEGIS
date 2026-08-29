package io.github.jhundeniel.ArithmeticHeroes.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import io.github.jhundeniel.ArithmeticHeroes.battle.BattleState;
import io.github.jhundeniel.ArithmeticHeroes.battle.PreviewCalculator;
import io.github.jhundeniel.ArithmeticHeroes.components.*;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent.ActionType;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.managers.SkillRegistry;
import io.github.jhundeniel.ArithmeticHeroes.managers.TurnManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;

import java.util.ArrayList;
import java.util.List;

public class SkillButtonsUI {

    // ── Layout ────────────────────────────────────────────────────────────
    private static final float PANEL_H = 170f;
    private static final float BTN_W = 215f;
    private static final float BTN_H = 60f;
    private static final float BTN_PAD = 8f;

    // ── Colors ────────────────────────────────────────────────────────────
    private static final Color PANEL_BG = new Color(0.12f, 0.13f, 0.17f, 0.97f);
    private static final Color DIVIDER = new Color(0.36f, 0.38f, 0.46f, 0.8f);
    private static final Color COL_MOVE = new Color(0.95f, 0.22f, 0.22f, 1f);
    private static final Color COL_TGT = new Color(1f, 1f, 1f, 1f);
    private static final Color COL_NAME = new Color(1f, 1f, 1f, 1f);
    private static final Color COL_COST = new Color(0.35f, 0.62f, 1f, 1f);
    private static final Color COL_BODY = new Color(1f, 1f, 1f, 1f);
    private static final Color COL_BTN = new Color(1f, 1f, 1f, 1f);
    private static final Color COL_DIS = new Color(0.45f, 0.45f, 0.45f, 0.7f);
    // BACK button colors
    private static final Color COL_BACK_ON = new Color(0.85f, 0.45f, 0.05f, 1f); // orange = active
    private static final Color COL_BACK_OFF = new Color(0.28f, 0.28f, 0.28f, 0.75f); // grey = no history

    // ── Assets ────────────────────────────────────────────────────────────
    private Texture btnTex;
    private Texture descTex;
    private Texture whiteTex;
    private Texture backOnTex;
    private Texture backOffTex;

    // ── Cursor (removed for global cursor) ────────────────────────────────

    // ── Rendering ─────────────────────────────────────────────────────────
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private BitmapFont fontSmall;
    private BitmapFont fontBig;
    private final GlyphLayout layout;

    // ── Scene2D ───────────────────────────────────────────────────────────
    private Stage btnStage;
    private Skin btnSkin;

    // ── Persistent HUD (pause button + stage label — always visible) ──────
    private Stage hudStage;
    private Skin hudSkin;
    private Runnable onPausePressed; // set via setPauseCallback()
    private String currentStageName = "";
    private Label stageLabel;
    private TextButton pauseBtn;

    // ── Description ───────────────────────────────────────────────────────
    private String dMove = "", dTarget = "", dName = "", dCost = "", dBody = "Select a skill.";

    // ── Dependencies ──────────────────────────────────────────────────────
    private final TurnManager turnManager;
    private final TargetingSystem targetingSystem;
    private final ActionLogSystem actionLog;
    private final List<Entity> heroes;
    private final FormulaBarUI formulaBarUI;
    private final ArithmeticAssetManager assets;

    // ── Group Burden UI tracking ───────────────────────────────────────
    private boolean groupBurdenUIShown = false;

    private final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<TypeComponent> tm = ComponentMapper.getFor(TypeComponent.class);

    // ── Button positions ──────────────────────────────────────────────────
    // bx[0..3] = 2×2 grid
    // bx[4] = PASS (top row, col 3)
    // bx[5] = BACK (bottom row, col 3) ← under PASS, always visible
    private final float[] bx = new float[6];
    private final float[] by = new float[6];

    // ─────────────────────────────────────────────────────────────────────

    public SkillButtonsUI(SpriteBatch batch, TurnManager turnManager,
            TargetingSystem targetingSystem, ActionLogSystem actionLog,
            List<Entity> heroes, FormulaBarUI formulaBarUI, ArithmeticAssetManager assets) {
        this.batch = batch;
        this.turnManager = turnManager;
        this.targetingSystem = targetingSystem;
        this.actionLog = actionLog;
        this.heroes = heroes;
        this.formulaBarUI = formulaBarUI;
        this.assets = assets;
        this.shapes = new ShapeRenderer();
        this.layout = new GlyphLayout();

        loadAssets();
        buildFonts();
        buildBtnStage();
        buildHudStage();
    }

    // ── Assets ────────────────────────────────────────────────────────────
    private void loadAssets() {
        btnTex = tryTex("ui/box_skill.png", "ui/box_skill.PNG");
        descTex = tryTex("ui/box_skill_des.png", "ui/box_skill_des.PNG");

        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        whiteTex = new Texture(px);
        px.dispose();

        Pixmap pxOn = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pxOn.setColor(COL_BACK_ON);
        pxOn.fill();
        backOnTex = new Texture(pxOn);
        pxOn.dispose();

        Pixmap pxOff = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pxOff.setColor(COL_BACK_OFF);
        pxOff.fill();
        backOffTex = new Texture(pxOff);
        pxOff.dispose();

        if (btnTex != null)
            btnTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        if (descTex != null)
            descTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    private Texture tryTex(String... paths) {
        for (String p : paths) {
            try {
                if (Gdx.files.internal(p).exists())
                    return new Texture(Gdx.files.internal(p));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    // ── Cursor helpers (removed)
    // ────────────────────────────────────────────────────

    private void buildFonts() {
        try {
            fontSmall = new BitmapFont(Gdx.files.internal("ui/font-small export.fnt"));
            fontSmall.getData().setScale(1.1f);
            fontBig = new BitmapFont(Gdx.files.internal("ui/font export.fnt"));
            fontBig.getData().setScale(1.2f);
        } catch (Exception e) {
            fontSmall = new BitmapFont();
            fontSmall.getData().setScale(1.3f);
            fontBig = new BitmapFont();
            fontBig.getData().setScale(1.5f);
        }
    }

    private void buildBtnStage() {
        btnSkin = new Skin();

        // Transparent style for skill buttons (image drawn manually)
        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(0, 0, 0, 0);
        px.fill();
        Texture clear = new Texture(px);
        px.dispose();

        TextButton.TextButtonStyle transparent = new TextButton.TextButtonStyle();
        transparent.font = fontSmall;
        transparent.fontColor = COL_BTN;
        transparent.overFontColor = new Color(0.8f, 0.9f, 1f, 1f);
        transparent.downFontColor = new Color(0.6f, 0.6f, 0.6f, 1f);
        transparent.disabledFontColor = COL_DIS;
        TextureRegionDrawable tr = new TextureRegionDrawable(new TextureRegion(clear));
        transparent.up = tr;
        transparent.over = tr;
        transparent.down = tr;
        transparent.disabled = tr;
        btnSkin.add("default", transparent);

        // Active BACK style (orange)
        TextButton.TextButtonStyle backOn = new TextButton.TextButtonStyle();
        backOn.font = fontSmall;
        backOn.fontColor = Color.WHITE;
        backOn.overFontColor = new Color(0.15f, 0.08f, 0f, 1f);
        backOn.up = new TextureRegionDrawable(new TextureRegion(backOnTex));
        backOn.over = new TextureRegionDrawable(new TextureRegion(backOnTex));
        backOn.down = new TextureRegionDrawable(new TextureRegion(backOnTex));
        btnSkin.add("back-on", backOn);

        // Disabled BACK style (grey)
        TextButton.TextButtonStyle backOff = new TextButton.TextButtonStyle();
        backOff.font = fontSmall;
        backOff.fontColor = COL_DIS;
        backOff.disabledFontColor = COL_DIS;
        backOff.up = new TextureRegionDrawable(new TextureRegion(backOffTex));
        backOff.over = new TextureRegionDrawable(new TextureRegion(backOffTex));
        backOff.down = new TextureRegionDrawable(new TextureRegion(backOffTex));
        backOff.disabled = new TextureRegionDrawable(new TextureRegion(backOffTex));
        btnSkin.add("back-off", backOff);

        btnStage = new Stage(new ScreenViewport(), batch);
    }

    // ── Persistent HUD stage (pause button + stage name) ──────────────────
    private void buildHudStage() {
        hudSkin = new Skin();

        // Pause button texture
        Texture pauseTex;
        try {
            pauseTex = new Texture(Gdx.files.internal("other_asset/pause_button.png"));
        } catch (Exception e) {
            Pixmap pxPause = new Pixmap(60, 60, Pixmap.Format.RGBA8888);
            pxPause.setColor(0.55f, 0.35f, 0.90f, 1f);
            pxPause.fill();
            pauseTex = new Texture(pxPause);
            pxPause.dispose();
        }

        hudSkin.add("pauseTex", pauseTex);
        hudSkin.add("pauseHovTex", pauseTex);

        BitmapFont hudFont = new BitmapFont();
        hudFont.getData().setScale(1.4f);
        hudSkin.add("default", hudFont);

        TextButton.TextButtonStyle pauseStyle = new TextButton.TextButtonStyle();
        pauseStyle.font = hudFont;
        pauseStyle.fontColor = new Color(0.85f, 0.75f, 1.00f, 1f);
        pauseStyle.overFontColor = new Color(1.00f, 0.95f, 1.00f, 1f);
        pauseStyle.downFontColor = new Color(0.50f, 0.40f, 0.70f, 1f);
        pauseStyle.up = new TextureRegionDrawable(new TextureRegion(pauseTex));
        pauseStyle.over = new TextureRegionDrawable(new TextureRegion(pauseTex)).tint(new Color(0.8f, 0.8f, 0.8f, 1f));
        pauseStyle.down = new TextureRegionDrawable(new TextureRegion(pauseTex)).tint(new Color(0.6f, 0.6f, 0.6f, 1f));
        hudSkin.add("pause", pauseStyle);

        // Label style for stage name
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = hudFont;
        labelStyle.fontColor = new Color(1f, 0.92f, 0.55f, 1f);
        hudSkin.add("default", labelStyle);

        hudStage = new Stage(new ScreenViewport(), batch);

        // Build the HUD table: [stage label] [⏸ PAUSE]
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().right().pad(8f);

        stageLabel = new Label("", hudSkin);
        stageLabel.setAlignment(Align.right);

        pauseBtn = new TextButton("", hudSkin, "pause");
        pauseBtn.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) {
                    assets.playSound(ArithmeticAssetManager.SFX_BUTTON_HOVER);
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {

            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                if (onPausePressed != null)
                    onPausePressed.run();
            }
        });

        hud.add(stageLabel).padRight(14f).right();
        hud.add(pauseBtn).size(85f, 85f);

        hudStage.addActor(hud);
    }

    /** Register the callback that fires when the pause button is clicked. */
    public void setPauseCallback(Runnable callback) {
        this.onPausePressed = callback;
    }

    /** Update the stage name shown in the HUD (call from spawnMobs). */
    public void setCurrentStageName(String name) {
        this.currentStageName = (name != null) ? name : "";
        if (stageLabel != null)
            stageLabel.setText(currentStageName);
    }

    // ── Positions ─────────────────────────────────────────────────────────
    //
    // [skill0] [skill1] [ PASS ] <- top row
    // [skill2] [skill3] [ BACK ] <- bottom row ← NEW
    //
    private void calcPositions(float half) {
        float gridW = BTN_W * 2 + BTN_PAD;
        float gridH = BTN_H * 2 + BTN_PAD;
        float startX = (half - gridW) / 2f;
        float botY = (PANEL_H - gridH) / 2f;
        float topY = botY + BTN_H + BTN_PAD;

        bx[0] = startX;
        by[0] = topY; // skill0 top-left
        bx[1] = startX + BTN_W + BTN_PAD;
        by[1] = topY; // skill1 top-right
        bx[2] = startX;
        by[2] = botY; // skill2 bot-left
        bx[3] = startX + BTN_W + BTN_PAD;
        by[3] = botY; // skill3 bot-right
        bx[4] = startX + (BTN_W + BTN_PAD) * 2;
        by[4] = topY; // PASS col-3 top
        bx[5] = startX + (BTN_W + BTN_PAD) * 2;
        by[5] = botY; // BACK col-3 bot
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void updateForCurrentTurn() {
        btnStage.clear();
        dMove = "";
        dTarget = "";
        dName = "";
        dCost = "";
        dBody = "Select a skill.";
        formulaBarUI.clearFormula();
        groupBurdenUIShown = false;

        if (turnManager.getState() != BattleState.WAIT_FOR_INPUT)
            return;
        Entity current = turnManager.getCurrentEntityTurn();
        if (current == null)
            return;
        TypeComponent type = tm.get(current);
        if (type == null || type.type == Operator.MOB)
            return;

        StatsComponent stats = sm.get(current);
        String heroName = stats != null ? stats.name.trim() : "???";
        List<SkillDef> defs = buildDefs(type.type, current);

        float half = Gdx.graphics.getWidth() / 2f;
        calcPositions(half);

        List<SkillDef> mainDefs = defs.subList(0, defs.size() - 1);
        final SkillDef passDef = defs.get(defs.size() - 1);
        final String fn = heroName;
        final Entity fc = current;

        // ── 4 main skill buttons ──────────────────────────────────────────
        for (int i = 0; i < mainDefs.size(); i++) {
            final SkillDef def = mainDefs.get(i);
            final boolean canUse = canUse(current, def.actionType);
            final int idx = i;

            TextButton btn = new TextButton(def.btnLabel, btnSkin);
            btn.setDisabled(!canUse);
            btn.setSize(BTN_W, BTN_H);
            btn.setPosition(bx[idx], by[idx]);
            btn.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent e, float x, float y, int pointer, Actor from) {
                    if (pointer == -1) {
                        if (canUse)
                            assets.playSound(ArithmeticAssetManager.SFX_BUTTON_HOVER);
                    }
                    if (canUse)
                        showDesc(fc, def);
                }

                @Override
                public void exit(InputEvent e, float x, float y, int pointer, Actor to) {

                }

                @Override
                public void clicked(InputEvent e, float x, float y) {
                    if (!canUse)
                        return;
                    assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);

                    showDesc(fc, def);

                    // ── GROUP_BURDEN intercept: show choice dialog first ───
                    if (def.actionType == ActionType.GROUP_BURDEN) {
                        targetingSystem.setPendingGroupBurdenChoice(fc);
                        turnManager.setState(BattleState.CHOOSE_VALUE);
                        showGroupBurdenChoiceUI(fc);
                        return;
                    }

                    boolean aoe = ActionRequestComponent.isAOE(def.actionType);
                    boolean two = ActionRequestComponent.needsTwoTargets(def.actionType);

                    if (aoe) {
                        fc.add(new ActionRequestComponent(def.actionType, null));
                        turnManager.setState(BattleState.ACTION_QUEUED);
                        updateForCurrentTurn();
                    } else if (two) {
                        targetingSystem.startTargetingTwo(fc, def.actionType);
                        turnManager.setState(BattleState.SELECT_TARGET);
                        showSelectTargetUI();
                    } else {
                        targetingSystem.startTargeting(fc, def.actionType);
                        turnManager.setState(BattleState.SELECT_TARGET);
                        showSelectTargetUI();
                    }
                }
            });
            btnStage.addActor(btn);
        }

        // ── PASS button (col 3 top) ───────────────────────────────────────
        TextButton passBtn = new TextButton(passDef.btnLabel, btnSkin);
        passBtn.setSize(BTN_W, BTN_H);
        passBtn.setPosition(bx[4], by[4]);
        passBtn.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent e, float x, float y, int pointer, Actor from) {
                if (pointer == -1) {
                    assets.playSound(ArithmeticAssetManager.SFX_BUTTON_HOVER);
                }
                showDesc(fc, passDef);
            }

            @Override
            public void exit(InputEvent e, float x, float y, int pointer, Actor to) {

            }

            @Override
            public void clicked(InputEvent e, float x, float y) {
                assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                Entity currentEntity = turnManager.getCurrentEntityTurn();
                PassiveComponent pc = currentEntity.getComponent(PassiveComponent.class);

                if (pc != null && pc.passive != null) {
                    pc.passive.onPass(currentEntity, heroes);
                }
                actionLog.addMessage(currentEntity.getComponent(StatsComponent.class).name.trim() + " passes.");
                turnManager.setState(BattleState.TURN_END);
            }
        });
        btnStage.addActor(passBtn);

        btnStage.addActor(passBtn);

        btnStage.getViewport().update(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(), true);
    }

    /** Shows only the cancel-targeting BACK button during SELECT_TARGET state. */
    public void showSelectTargetUI() {
        btnStage.clear();

        float half = Gdx.graphics.getWidth() / 2f;
        calcPositions(half);

        TextButton backBtn = new TextButton("< BACK", btnSkin, "back-on");
        backBtn.setSize(BTN_W, BTN_H);
        backBtn.setPosition(bx[5], by[5]);
        backBtn.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent e, float x, float y, int pointer, Actor from) {

            }

            @Override
            public void exit(InputEvent e, float x, float y, int pointer, Actor to) {

            }

            @Override
            public void clicked(InputEvent e, float x, float y) {
                targetingSystem.cancel();
                turnManager.setState(BattleState.WAIT_FOR_INPUT);
                updateForCurrentTurn();
            }
        });
        btnStage.addActor(backBtn);

        btnStage.getViewport().update(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(), true);
    }

    /** Shows 3 buttons (3, 4, 5) for the Additional Buff value chooser. */
    public void showBuffChooserUI() {
        btnStage.clear();

        float half = Gdx.graphics.getWidth() / 2f;
        calcPositions(half);

        int[] values = { 3, 4, 5 };
        // Use the 3 column X-positions: bx[0], bx[1], bx[4]
        float[] colX = { bx[0], bx[1], bx[4] };
        for (int i = 0; i < values.length; i++) {
            final int val = values[i];
            TextButton btn = new TextButton("+" + val, btnSkin);
            btn.setSize(BTN_W, BTN_H);
            btn.setPosition(colX[i], by[0]); // top row, 3 columns
            btn.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent e, float x, float y, int pointer, Actor from) {
                    if (pointer == -1) {
                        assets.playSound(ArithmeticAssetManager.SFX_BUTTON_HOVER);
                    }
                }

                @Override
                public void exit(InputEvent e, float x, float y, int pointer, Actor to) {

                }

                @Override
                public void clicked(InputEvent e, float x, float y) {
                    assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                    targetingSystem.submitWithValue(val);
                    turnManager.setState(BattleState.ACTION_QUEUED);
                    updateForCurrentTurn();
                }
            });
            btnStage.addActor(btn);
        }

        // BACK button to cancel
        TextButton backBtn = new TextButton("< BACK", btnSkin, "back-on");
        backBtn.setSize(BTN_W, BTN_H);
        backBtn.setPosition(bx[5], by[5]);
        backBtn.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent e, float x, float y, int pointer, Actor from) {

            }

            @Override
            public void exit(InputEvent e, float x, float y, int pointer, Actor to) {

            }

            @Override
            public void clicked(InputEvent e, float x, float y) {
                targetingSystem.cancel();
                turnManager.setState(BattleState.WAIT_FOR_INPUT);
                updateForCurrentTurn();
            }
        });
        btnStage.addActor(backBtn);

        btnStage.getViewport().update(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(), true);
    }

    /** Shows the Group Burden "2 or 3 Allies" choice buttons. */
    public void showGroupBurdenChoiceUI(final Entity caster) {
        btnStage.clear();
        groupBurdenUIShown = true;

        float half = Gdx.graphics.getWidth() / 2f;
        calcPositions(half);

        int livingAllies = targetingSystem.countLivingAllies(caster);
        boolean canPick3 = (livingAllies >= 3);

        // ── "2 Allies (25%)" button ───────────────────────────────────
        TextButton btn2 = new TextButton("2 Allies (25%)", btnSkin);
        btn2.setSize(BTN_W, BTN_H);
        btn2.setPosition(bx[0], by[0]);
        btn2.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent e, float x, float y, int pointer, Actor from) {
                if (pointer == -1)
                    assets.playSound(ArithmeticAssetManager.SFX_BUTTON_HOVER);
            }

            @Override
            public void exit(InputEvent e, float x, float y, int pointer, Actor to) {
            }

            @Override
            public void clicked(InputEvent e, float x, float y) {
                assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                targetingSystem.clearPendingGroupBurdenChoice();
                // Start 2-ally multi-targeting
                if (targetingSystem.startGroupBurdenTargeting(caster, 2)) {
                    turnManager.setState(BattleState.SELECT_TARGET);
                    showSelectTargetUI();
                } else {
                    // No valid targets
                    turnManager.setState(BattleState.WAIT_FOR_INPUT);
                    updateForCurrentTurn();
                }
            }
        });
        btnStage.addActor(btn2);

        // ── "3 Allies (15%)" button ───────────────────────────────────
        TextButton btn3 = new TextButton("3 Allies (15%)", btnSkin);
        btn3.setSize(BTN_W, BTN_H);
        btn3.setPosition(bx[1], by[0]);
        btn3.setDisabled(!canPick3);
        btn3.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent e, float x, float y, int pointer, Actor from) {
                if (pointer == -1 && canPick3)
                    assets.playSound(ArithmeticAssetManager.SFX_BUTTON_HOVER);
            }

            @Override
            public void exit(InputEvent e, float x, float y, int pointer, Actor to) {
            }

            @Override
            public void clicked(InputEvent e, float x, float y) {
                if (!canPick3)
                    return;
                assets.playSound(ArithmeticAssetManager.SFX_BUTTON_CLICK);
                targetingSystem.clearPendingGroupBurdenChoice();
                // Auto-cast: apply to all living allies
                caster.add(new ActionRequestComponent(ActionType.GROUP_BURDEN, null));
                turnManager.setState(BattleState.ACTION_QUEUED);
                updateForCurrentTurn();
            }
        });
        btnStage.addActor(btn3);

        // ── BACK button ─────────────────────────────────────────────
        TextButton backBtn = new TextButton("< BACK", btnSkin, "back-on");
        backBtn.setSize(BTN_W, BTN_H);
        backBtn.setPosition(bx[5], by[5]);
        backBtn.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent e, float x, float y, int pointer, Actor from) {
            }

            @Override
            public void exit(InputEvent e, float x, float y, int pointer, Actor to) {
            }

            @Override
            public void clicked(InputEvent e, float x, float y) {
                targetingSystem.clearPendingGroupBurdenChoice();
                targetingSystem.cancel();
                turnManager.setState(BattleState.WAIT_FOR_INPUT);
                updateForCurrentTurn();
            }
        });
        btnStage.addActor(backBtn);

        btnStage.getViewport().update(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(), true);
    }

    // 1. Add 'Entity user' to the parameters!
    private void showDesc(Entity user, SkillDef def) {
        boolean aoe = def.actionType != null && ActionRequestComponent.isAOE(def.actionType);
        dMove = "MOVE: " + def.btnLabel;
        dTarget = "  TARGET: " + (aoe ? "ALL" : "SINGLE");
        dName = def.skillName;
        String cr = def.costRed, cb = def.costBlue;
        dCost = (!cr.isEmpty() && !cb.isEmpty()) ? cr + "  " + cb : cr + cb;
        dBody = def.description;

        // --- PREDICTION INJECTION ---
        if (def.actionType != null) {

            // Look up the skill data directly — inverted skills have their own JSON entries
            io.github.jhundeniel.ArithmeticHeroes.data.SkillData jsonSkill = io.github.jhundeniel.ArithmeticHeroes.managers.SkillRegistry
                    .getByType(def.actionType);

            if (jsonSkill != null && jsonSkill.value >= 1.0f) {
                int baseValue = (int) jsonSkill.value;
                int predictedValue = io.github.jhundeniel.ArithmeticHeroes.battle.PreviewCalculator
                        .calculateExpected(user, jsonSkill);

                // Build formula breakdown for FormulaBarUI
                StringBuilder formula = new StringBuilder();
                formula.append(baseValue);

                io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect buffEff = io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects
                        .get(user, io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect.Type.BUFF);
                if (buffEff != null) {
                    int buffPct = Math.round((float) buffEff.multiplier * 100f);
                    formula.append(" x ").append(buffPct).append("%");
                }

                // Echo/Squared effectiveness penalty
                if (io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects.has(user,
                        io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect.Type.ECHO_CAST)) {
                    formula.append(" x50%");
                }

                int addBonus = io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects
                        .getAdditiveBonus(user);
                if (addBonus > 0) {
                    formula.append(" + ").append(addBonus);
                }

                // Berserker passive bonus (Subtraction below 50% HP)
                io.github.jhundeniel.ArithmeticHeroes.components.PassiveComponent pc = user
                        .getComponent(io.github.jhundeniel.ArithmeticHeroes.components.PassiveComponent.class);
                if (pc != null
                        && pc.passive instanceof io.github.jhundeniel.ArithmeticHeroes.passives.SubtractionPassive) {
                    StatsComponent uStats = sm.get(user);
                    if (uStats != null && (float) uStats.hp
                            / uStats.maxHp < io.github.jhundeniel.ArithmeticHeroes.config.GameConfig.BERSERKER_HP_THRESHOLD) {
                        float missingPct = 1.0f - ((float) uStats.hp / uStats.maxHp);
                        int berserkPct = Math.round(missingPct
                                * (io.github.jhundeniel.ArithmeticHeroes.config.GameConfig.BERSERKER_BONUS_MULTIPLIER
                                        * 100f));
                        formula.append(" +").append(berserkPct).append("% BERSERK");
                    }
                }

                if (io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects.has(user,
                        io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect.Type.SQUARED)) {
                    formula.append(" x2(50%)");
                }

                if (io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects.has(user,
                        io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect.Type.INVERSION)) {
                    formula.append(" [INV]");
                }

                formula.append(" = ").append(predictedValue);
                formulaBarUI.setFormula(formula.toString());

                // 2. Properly format the final display text based on skill category
                if (predictedValue > 0) {
                    boolean isHeal = (def.actionType == ActionType.HEAL ||
                            def.actionType == ActionType.GROUP_HEAL ||
                            def.actionType == ActionType.BLOOD_TRANSFER ||
                            def.actionType == ActionType.SACRIFICE);

                    boolean isDualDamageHeal = (def.actionType == ActionType.LIFESTEAL_ATTACK ||
                            def.actionType == ActionType.DEBT_TRANSFER);

                    // Inverted heals are damage skills (drain, siphon)
                    boolean isInvertedDamage = (def.actionType == ActionType.SINGLE_DRAIN ||
                            def.actionType == ActionType.LIFE_SIPHON);

                    if (isHeal) {
                        dBody += "\n\nFINAL HEAL: +" + predictedValue + " HP";
                    } else if (isDualDamageHeal) {
                        // Heal uses pre-berserker damage (skill computes heal before DamageSystem adds passive)
                        int baseDmg = io.github.jhundeniel.ArithmeticHeroes.battle.PreviewCalculator
                                .calculateBase(user, jsonSkill);
                        dBody += "\n\nFINAL DAMAGE: " + predictedValue + " (HEAL: " + (baseDmg / 2) + ")";
                    } else {
                        dBody += "\n\nFINAL DAMAGE: " + predictedValue;
                    }
                }
            } else {
                formulaBarUI.clearFormula();
            }
        } else {
            formulaBarUI.clearFormula();
        }
    }

    // ── Render ────────────────────────────────────────────────────────────
    public void render() {
        BattleState state = turnManager.getState();
        if (state != BattleState.WAIT_FOR_INPUT
                && state != BattleState.SELECT_TARGET
                && state != BattleState.CHOOSE_VALUE)
            return;

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();
        float half = sw / 2f;

        calcPositions(half);

        batch.begin();

        // Panel background
        batch.setColor(PANEL_BG);
        batch.draw(whiteTex, 0, 0, sw, PANEL_H);
        batch.setColor(Color.WHITE);

        if (state == BattleState.WAIT_FOR_INPUT) {
            // 4 main skill button images + PASS image
            Texture bt = (btnTex != null) ? btnTex : whiteTex;
            for (int i = 0; i < 5; i++) {
                batch.draw(bt, bx[i], by[i], BTN_W, BTN_H);
            }
            // Back button removed from this state.

        } else if (state == BattleState.CHOOSE_VALUE) {
            // Detect if keyboard triggered group burden choice — auto-show dialog
            if (targetingSystem.isPendingGroupBurdenChoice() && !groupBurdenUIShown) {
                groupBurdenUIShown = true;
                showGroupBurdenChoiceUI(targetingSystem.getGroupBurdenCaster());
            }

            // CHOOSE_VALUE state: draw value chooser buttons + prompt
            Texture bt = (btnTex != null) ? btnTex : whiteTex;
            float[] colX = { bx[0], bx[1], bx[4] };
            // Draw 2 buttons for group burden, 3 for buff chooser
            int buttonCount = targetingSystem.isPendingGroupBurdenChoice() ? 2 : 3;
            for (int i = 0; i < buttonCount; i++) {
                batch.draw(bt, colX[i], by[0], BTN_W, BTN_H);
            }
            // BACK button
            batch.setColor(COL_BACK_ON);
            batch.draw(whiteTex, bx[5], by[5], BTN_W, BTN_H);
            batch.setColor(Color.WHITE);

            // Contextual prompt text
            if (targetingSystem.isPendingGroupBurdenChoice()) {
                fontSmall.setColor(new Color(0.4f, 0.85f, 1f, 1f));
                fontSmall.draw(batch, "Select number of targets:", 20f, PANEL_H / 2f + 10f);
            } else {
                fontSmall.setColor(new Color(0.3f, 1f, 0.5f, 1f));
                fontSmall.draw(batch, "Choose buff amount:", 20f, PANEL_H / 2f + 10f);
            }
        } else {
            // SELECT_TARGET state: draw only BACK button + prompt
            batch.setColor(COL_BACK_ON);
            batch.draw(whiteTex, bx[5], by[5], BTN_W, BTN_H);
            batch.setColor(Color.WHITE);

            fontSmall.setColor(new Color(1f, 0.9f, 0.3f, 1f));
            fontSmall.draw(batch, "Select a target...", 20f, PANEL_H / 2f + 10f);
        }

        // Description box
        float dpad = 6f;
        float dx = half + dpad;
        float dy = dpad;
        float dw = half - dpad * 2f - 300f;
        float dh = PANEL_H - dpad * 2f;

        if (descTex != null) {
            batch.setColor(Color.WHITE);
            batch.draw(descTex, dx, dy, dw, dh);
        }

        float textX = half + 40f;
        float maxW = dw - 28f;
        float lineH = fontSmall.getLineHeight() * 1.15f;
        float bigH = fontBig.getLineHeight() * 1.15f;
        float ty = dy + dh - 20f;

        fontSmall.setColor(COL_MOVE);
        fontSmall.draw(batch, dMove, textX, ty);
        layout.setText(fontSmall, dMove);
        fontSmall.setColor(COL_TGT);
        fontSmall.draw(batch, dTarget, textX + layout.width, ty);
        ty -= lineH + 4f;

        fontBig.setColor(COL_NAME);
        fontBig.draw(batch, dName, textX, ty);
        ty -= bigH + 6f;

        fontSmall.setColor(COL_COST);
        fontSmall.draw(batch, dCost, textX, ty);
        ty -= lineH + 4f;

        fontSmall.setColor(COL_BODY);
        fontSmall.draw(batch, dBody, textX, ty, maxW, com.badlogic.gdx.utils.Align.left, true);

        batch.end();

        // Divider
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(DIVIDER);
        shapes.rect(half, 4, 2, PANEL_H - 8);
        shapes.end();

        btnStage.getViewport().update((int) sw, (int) sh, true);
        btnStage.act(Gdx.graphics.getDeltaTime());
        btnStage.draw();

        // ── Persistent HUD (always rendered over everything) ──────────────
        hudStage.getViewport().update((int) sw, (int) sh, true);
        hudStage.act(Gdx.graphics.getDeltaTime());
        hudStage.draw();
    }

    private void drawWrapped(SpriteBatch b, BitmapFont f, String text,
            float x, float y, float maxW) {
        if (text == null || text.isEmpty())
            return;
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float lineH = f.getLineHeight() * 1.15f;
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            layout.setText(f, test);
            if (layout.width > maxW && line.length() > 0) {
                f.draw(b, line.toString(), x, y);
                y -= lineH;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0)
            f.draw(b, line.toString(), x, y);
    }

    // ── Skill definitions ─────────────────────────────────────────────────
    private List<SkillDef> buildDefs(Operator op, Entity hero) {
        List<SkillDef> d = new ArrayList<>();
        boolean inv = io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects.has(hero,
                io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect.Type.INVERSION);

        if (op == Operator.ADDITION) {
            if (inv) {
                d.add(new SkillDef("DRAIN", ActionType.SINGLE_DRAIN, "10 Base Dmg", "", "3 mana",
                        "Remove HP from a single entity."));
                d.add(new SkillDef("LIFE SIPHON", ActionType.LIFE_SIPHON, "3 Base Dmg", "", "5 mana",
                        "Remove HP from ALL entities."));
                d.add(new SkillDef("ADD BUFF", ActionType.ADDITIONAL_BUFF, "Add Buff", "", "4 mana",
                        "Give an ALLY +3 to +5 additive bonus."));
                d.add(new SkillDef("MANA STEAL", ActionType.MANA_STEAL, "Mana Steal", "", "0 mana",
                        "Steal 5 mana from an ALLY."));
            } else {
                d.add(new SkillDef("HEAL", ActionType.HEAL, "10 Base Heal", "", "3 mana",
                        "Add missing HP to a single ally."));
                d.add(new SkillDef("GROUP HEAL", ActionType.GROUP_HEAL, "3 Base Heal", "", "5 mana",
                        "Add missing HP to ALL party members."));
                d.add(new SkillDef("ADD BUFF", ActionType.ADDITIONAL_BUFF, "Add Buff", "", "4 mana",
                        "Give an ALLY +3 to +5 additive bonus."));
                d.add(new SkillDef("MANA TRANSFER", ActionType.MANA_TRANSFER, "Mana Transfer", "", "0 mana",
                        "Give 5 mana to another ally by transferring it from another ally."));
            }
            d.add(new SkillDef("PASS", null, "Pass", "", "", "Skip this turn."));
        } else if (op == Operator.SUBTRACTION) {
            if (inv) {
                d.add(new SkillDef("BLOOD XFER", ActionType.BLOOD_TRANSFER, "10 Base Heal", "-10% HP", "",
                        "Sacrifice HP to heal an ALLY."));
                d.add(new SkillDef("SACRIFICE", ActionType.SACRIFICE, "5 Base Heal", "-5% HP", "",
                        "Sacrifice HP to heal ALL ALLIES."));
                d.add(new SkillDef("MANA NUKE", ActionType.MANA_NUKE, "20 Base Dmg", "", "7 mana",
                        "Damage enemy. Bonus dmg if HP>75%."));
                d.add(new SkillDef("DEBT XFER", ActionType.DEBT_TRANSFER, "15 Base Dmg", "", "5 mana",
                        "Damage enemy, heal ally for 50%."));
            } else {
                d.add(new SkillDef("POKE", ActionType.POKE, "10 Base Dmg", "-10% HP", "",
                        "Light damage to a single ENEMY."));
                d.add(new SkillDef("SLAM", ActionType.SLAM, "5 Base Dmg", "-5% HP", "", "AOE damage to ALL ENEMIES."));
                d.add(new SkillDef("CONDITIONAL", ActionType.CONDITIONAL_ATTACK, "20 Base Dmg", "", "7 mana",
                        "Strong hit. Only usable at half HP or below."));
                d.add(new SkillDef("LIFE STEAL", ActionType.LIFESTEAL_ATTACK, "15 Base Dmg", "", "5 mana",
                        "Deals damage & heals for HALF dealt."));
            }
            d.add(new SkillDef("PASS", null, "Pass", "", "", "Skip this turn."));
        } else if (op == Operator.MULTIPLICATION) {
            d.add(new SkillDef("SINGLE AMPLIFY", ActionType.AMPLIFY, "Single Amplify", "125%-150% Base", "5 mana",
                    "Amplify an ally's skill base value."));
            d.add(new SkillDef("INVERSION", ActionType.INVERSION, "Inversion", "", "3 mana",
                    "Flip sign of an ALLY's skill to +/-."));
            d.add(new SkillDef("GROUP AMPLIFY", ActionType.GROUP_AMPLIFY, "Group Amplify", "105%-120% Base", "7 mana",
                    "Amplify ALL ALLIES."));
            d.add(new SkillDef("SQUARED POWER", ActionType.SQUARED_POWER, "Squared Power", "", "8 mana",
                    "ALLY's next action casts TWICE at half power."));
            d.add(new SkillDef("PASS", null, "Pass", "", "", "Skip this turn."));
        } else if (op == Operator.DIVISION) {
            if (inv) {
                d.add(new SkillDef("REFLECT", ActionType.SINGLE_REFLECTION, "50% Reflect", "", "3 mana",
                        "Ally reflects 50% of next hit."));
                d.add(new SkillDef("GRP REFLECT", ActionType.GROUP_REFLECTION, "15% Reflect", "", "5 mana",
                        "All allies reflect incoming dmg."));
                d.add(new SkillDef("COST RED.", ActionType.COST_REDUCTION, "50% Skill Cost Reduction", "", "5 mana",
                        "Halve an ALLY's skill cost for 2 turns."));
                d.add(new SkillDef("UNFAIR BTL", ActionType.UNFAIR_BATTLE, "Unfair Battle", "", "6 mana",
                        "Redistribute HP between 2 enemies."));
            } else {
                d.add(new SkillDef("BURDEN", ActionType.BURDEN, "50% Damage Share", "", "3 mana",
                        "Share 50% of damage taken by an ALLY."));
                d.add(new SkillDef("GRP BURDEN", ActionType.GROUP_BURDEN, "25% Damage Sharing | 15% Damage Sharing", "",
                        "5 mana",
                        "Share damage among 2-3 ALLIES."));
                d.add(new SkillDef("COST RED.", ActionType.COST_REDUCTION, "50% Skill Cost Reduction", "", "5 mana",
                        "Halve an ALLY's skill cost for 2 turns."));
                d.add(new SkillDef("EQUALIZER", ActionType.BATTLE_EQUALIZER, "Battle Equalizer", "", "6 mana",
                        "Pool & split HP equally between 2 ALLIES."));
            }
            d.add(new SkillDef("PASS", null, "Pass", "", "", "Skip this turn."));
        }
        return d;
    }

    private boolean canUse(Entity c, ActionType t) {
        StatsComponent s = sm.get(c);
        if (s == null)
            return false;
        switch (t) {
            // Original skills
            case HEAL:
                return s.mana >= 3;
            case GROUP_HEAL:
                return s.mana >= 5;
            case ADDITIONAL_BUFF:
                return s.mana >= 4;
            case MANA_TRANSFER:
                return true;
            case POKE:
                return s.hp > (int) (s.maxHp * .10f);
            case SLAM:
                return s.hp > (int) (s.maxHp * .05f);
            case CONDITIONAL_ATTACK:
                return s.mana >= 7 && s.hp <= (s.maxHp / 2);
            case LIFESTEAL_ATTACK:
                return s.mana >= 5;
            case AMPLIFY:
                return s.mana >= 5;
            case GROUP_AMPLIFY:
                return s.mana >= 7;
            case INVERSION:
                return s.mana >= 3;
            case SQUARED_POWER:
                return s.mana >= 8;
            case BURDEN:
                return s.mana >= 3;
            case GROUP_BURDEN:
                return s.mana >= 5;
            case COST_REDUCTION:
                return s.mana >= 5;
            case BATTLE_EQUALIZER:
                return s.mana >= 6;

            // Inverted skills
            case SINGLE_DRAIN:
                return s.mana >= 3;
            case LIFE_SIPHON:
                return s.mana >= 5;
            case MANA_STEAL:
                return true;
            case BLOOD_TRANSFER:
                return s.hp > (int) (s.maxHp * .10f);
            case SACRIFICE:
                return s.hp > (int) (s.maxHp * .05f);
            case MANA_NUKE:
                return s.mana >= 7;
            case DEBT_TRANSFER:
                return s.mana >= 5;
            case SINGLE_REFLECTION:
                return s.mana >= 3;
            case GROUP_REFLECTION:
                return s.mana >= 5;
            case UNFAIR_BATTLE:
                return s.mana >= 6;

            default:
                return false;
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────
    public void resize(int w, int h) {
        btnStage.getViewport().update(w, h, true);
        hudStage.getViewport().update(w, h, true);
        BattleState state = turnManager.getState();
        if (state == BattleState.WAIT_FOR_INPUT)
            updateForCurrentTurn();
        else if (state == BattleState.SELECT_TARGET)
            showSelectTargetUI();
        else if (state == BattleState.CHOOSE_VALUE) {
            if (targetingSystem.isPendingGroupBurdenChoice()) {
                Entity caster = targetingSystem.getGroupBurdenCaster();
                if (caster != null)
                    showGroupBurdenChoiceUI(caster);
            } else {
                showBuffChooserUI();
            }
        }
    }

    public Stage getStage() {
        return btnStage;
    }

    public Stage getHudStage() {
        return hudStage;
    }

    public void dispose() {
        btnStage.dispose();
        hudStage.dispose();
        btnSkin.dispose();
        hudSkin.dispose();
        shapes.dispose();
        fontSmall.dispose();
        fontBig.dispose();
        if (btnTex != null)
            btnTex.dispose();
        if (descTex != null)
            descTex.dispose();
        if (whiteTex != null)
            whiteTex.dispose();
        if (backOnTex != null)
            backOnTex.dispose();
        if (backOffTex != null)
            backOffTex.dispose();
    }

    // ── Inner data class ──────────────────────────────────────────────────
    private static class SkillDef {
        final String btnLabel, skillName, costRed, costBlue, description;
        final ActionType actionType;

        SkillDef(String b, ActionType at, String sn, String cr, String cb, String desc) {
            btnLabel = b;
            actionType = at;
            skillName = sn;
            costRed = cr;
            costBlue = cb;
            description = desc;
        }
    }
}
