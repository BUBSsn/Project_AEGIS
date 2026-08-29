package io.github.jhundeniel.ArithmeticHeroes.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.components.PortraitComponent;
import io.github.jhundeniel.ArithmeticHeroes.Main;
import io.github.jhundeniel.ArithmeticHeroes.battle.*;
import io.github.jhundeniel.ArithmeticHeroes.battle.cleanup.BattleCleanup;
import io.github.jhundeniel.ArithmeticHeroes.components.Operator;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.DialogueLine;
import io.github.jhundeniel.ArithmeticHeroes.data.LeaderboardEntry;
import io.github.jhundeniel.ArithmeticHeroes.data.SaveData;
import io.github.jhundeniel.ArithmeticHeroes.data.StageData;
import io.github.jhundeniel.ArithmeticHeroes.factories.EntityFactory;
import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.BattleFlowController;
import io.github.jhundeniel.ArithmeticHeroes.managers.BattleUIManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.DialogueManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.LeaderboardManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.SaveManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.StageRegistry;
import io.github.jhundeniel.ArithmeticHeroes.managers.TurnManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.WaveManager;
import io.github.jhundeniel.ArithmeticHeroes.systems.*;

public class BattleScreen extends ScreenAdapter {

    private final BattleCleanup battleCleanup;
    private final Main game;
    private final Engine engine;
    private final EntityFactory factory;
    private final ArithmeticAssetManager assets;
    private final TurnManager turnManager;
    private final TargetingSystem targetingSystem;

    private final ActionLogSystem actionLogSystem;
    private final EnemyIntentSystem enemyIntentSystem;
    private final SkillButtonsUI skillButtonsUI;
    private final MouseTargetingSystem mouseTargetingSystem;
    private final TurnOrderDisplay turnOrderDisplay;
    private final HeroPortraitPanel heroPortraitPanel;

    private final BattleAnimations battleAnimations;
    private final BattleRenderSystem battleRenderSystem;
    private final FormulaBarUI formulaBarUI;

    // ── Enemy AI + target highlight ───────────────────────────────────────
    private final EnemyAI enemyAI;
    private final EnemyTargetHighlight enemyTargetHighlight = new EnemyTargetHighlight();

    private final ComponentMapper<TypeComponent> typeCM = ComponentMapper.getFor(TypeComponent.class);

    private final List<Entity> activeMobs = new ArrayList<>();
    private final List<Entity> heroes = new ArrayList<>();
    private final List<Entity> allEntities = new ArrayList<>();
    private final List<Entity> benchedHeroes = new ArrayList<>();

    private BattleState lastState = null;
    private Entity lastEntity = null;

    // ── Save/Load ─────────────────────────────────────────────────────────
    private final String slotId;
    private final String displayName;

    // ── Managers ─────────────────────────────────────────────────────────
    private final BattleUIManager uiManager;
    private final WaveManager waveManager;
    private final BattleFlowController flowController;

    // ── Game Modes ─────────────────────────────────────────────────────────
    private boolean practiceMode = false;

    private final StageSystem stageSystemRef;

    private final WaveAnnouncer waveAnnouncer;

    private static class GraveData {
        Texture tex;
        int frames;
        Texture staticTex;

        GraveData(Texture tex, int frames, Texture staticTex) {
            this.tex = tex;
            this.frames = frames;
            this.staticTex = staticTex;
        }
    }

    private final java.util.Map<Entity, GraveData> heroGravestones = new java.util.LinkedHashMap<>();

    // ── Dialogue system ───────────────────────────────────────────────────
    private final DialogueManager dialogueManager;
    private DialogueOverlay dialogueOverlay;
    private boolean showingDialogue = false;
    private boolean combatBlocked = false; // true while pre-combat dialogue plays
    private final ChatBubbleSystem chatBubbles = new ChatBubbleSystem();
    private String pendingVictoryDialogue = null; // set when a wave has post-victory dialogue
    private boolean showingPostVictoryDialogue = false;

    /** Start a new game (no save data, generates a new slot). */
    public BattleScreen(Main game) {
        this(game, null);
    }

    /**
     * Start from a specific stage with fresh hero stats (stage select mode).
     * Does NOT restore saved HP/mana — heroes start at full stats.
     * Does NOT overwrite the main save file.
     *
     * @param startingStageIndex the 0-based stage index to begin at
     */
    public BattleScreen(Main game, int startingStageIndex) {
        this(game, createStageSelectSave(startingStageIndex));
        stageSystemRef.setHeroesRef(null); // prevents auto-save on stage advance
        stageSystemRef.setPracticeMode(true);
        this.practiceMode = true;
    }

    /**
     * Helper: creates a SaveData with only the starting stage set (no hero stats).
     */
    private static SaveData createStageSelectSave(int stageIndex) {
        SaveData save = new SaveData();
        save.currentStageIndex = stageIndex;
        return save;
    }

    /**
     * Main constructor — optionally accepts saved progress.
     *
     * @param save if non-null, hero stats and starting stage are restored.
     */
    public BattleScreen(Main game, SaveData save) {
        this.game = game;
        this.engine = new Engine();
        this.battleCleanup = new BattleCleanup(engine);
        this.assets = game.assetManager;
        this.factory = new EntityFactory(engine);
        this.turnManager = new TurnManager();

        // ── Slot ID management ────────────────────────────────────────────
        if (save != null && save.slotId != null) {
            this.slotId = save.slotId;
            this.displayName = save.displayName;
        } else {
            this.slotId = SaveManager.generateSlotId();
            this.displayName = SaveManager.generateDisplayName();
        }

        this.battleAnimations = new BattleAnimations();
        CombatMechanics.setAnimations(battleAnimations);

        this.actionLogSystem = new ActionLogSystem(game.batch);
        this.enemyAI = new EnemyAI(actionLogSystem);
        this.enemyIntentSystem = new EnemyIntentSystem(game.batch, engine);
        this.targetingSystem = new TargetingSystem(actionLogSystem, assets);
        this.turnOrderDisplay = new TurnOrderDisplay(game.batch, turnManager);

        StageSystem stageSystem = new StageSystem(assets, factory);
        stageSystem.setSlotId(this.slotId);
        stageSystem.setDisplayName(this.displayName);
        engine.addSystem(stageSystem);
        this.stageSystemRef = stageSystem;
        engine.addSystem(new CombatSystem(engine, turnManager, actionLogSystem,
                turnOrderDisplay, battleAnimations, assets));
        engine.addSystem(new DamageSystem(actionLogSystem, assets, turnManager));
        engine.addSystem(new BattleStateSystem(turnManager, enemyAI, actionLogSystem, heroes, activeMobs,
                battleAnimations, assets, factory));

        // AnimationSystem runs before BattleRenderSystem to update sprite frames
        engine.addSystem(new AnimationSystem());

        this.battleRenderSystem = new BattleRenderSystem(game.batch, stageSystem, assets);
        this.battleRenderSystem.setAnimations(battleAnimations);
        this.battleRenderSystem.setTurnManager(turnManager);
        this.waveAnnouncer = new WaveAnnouncer(game.batch);
        this.battleRenderSystem.setWaveAnnouncer(waveAnnouncer);
        this.battleRenderSystem.setTargetingSystem(targetingSystem);
        engine.addSystem(battleRenderSystem);

        // --- Hero creation ---
        Entity add = factory.createHero(
                "HERO_ADDITION",
                EntityFactory.H1_X, EntityFactory.GROUND_Y,
                assets.getTexture(ArithmeticAssetManager.ANIM_HERO_ADD), // animated sheet
                ArithmeticAssetManager.HERO_ADD_FRAMES, // 13 frames
                assets.getTexture(ArithmeticAssetManager.PORT_ADD));

        Entity sub = factory.createHero(
                "HERO_SUBTRACTION",
                EntityFactory.H2_X, EntityFactory.GROUND_Y,
                assets.getTexture(ArithmeticAssetManager.ANIM_HERO_SUB), // animated sheet
                ArithmeticAssetManager.HERO_SUB_FRAMES, // 12 frames
                assets.getTexture(ArithmeticAssetManager.PORT_SUB));

        Entity mult = factory.createHero(
                "HERO_MULTIPLICATION",
                EntityFactory.H3_X, EntityFactory.GROUND_Y,
                assets.getTexture(ArithmeticAssetManager.ANIM_HERO_MUL), // animated sheet
                ArithmeticAssetManager.HERO_MUL_FRAMES, // 13 frames
                assets.getTexture(ArithmeticAssetManager.PORT_MUL));

        Entity div = factory.createHero(
                "HERO_DIVISION",
                EntityFactory.H4_X, EntityFactory.GROUND_Y,
                assets.getTexture(ArithmeticAssetManager.ANIM_HERO_DIV), // animated sheet
                ArithmeticAssetManager.HERO_DIV_FRAMES, // 13 frames
                assets.getTexture(ArithmeticAssetManager.PORT_DIV));

        // Turn 1 order
        heroes.add(mult);
        heroes.add(div);
        heroes.add(add);
        heroes.add(sub);

        heroGravestones.put(add, new GraveData(assets.getTexture(ArithmeticAssetManager.GRAVE_ADD),
                ArithmeticAssetManager.GRAVE_ADD_FRAMES, assets.getTexture(ArithmeticAssetManager.GRAVE_ADD_STATIC)));
        heroGravestones.put(sub, new GraveData(assets.getTexture(ArithmeticAssetManager.GRAVE_SUB),
                ArithmeticAssetManager.GRAVE_SUB_FRAMES, assets.getTexture(ArithmeticAssetManager.GRAVE_SUB_STATIC)));
        heroGravestones.put(mult, new GraveData(assets.getTexture(ArithmeticAssetManager.GRAVE_MUL),
                ArithmeticAssetManager.GRAVE_MUL_FRAMES, assets.getTexture(ArithmeticAssetManager.GRAVE_MUL_STATIC)));
        heroGravestones.put(div, new GraveData(assets.getTexture(ArithmeticAssetManager.GRAVE_DIV),
                ArithmeticAssetManager.GRAVE_DIV_FRAMES, assets.getTexture(ArithmeticAssetManager.GRAVE_DIV_STATIC)));

        // ── Apply saved hero stats (if resuming) ──────────────────────────
        if (save != null && save.heroes != null) {
            ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
            ComponentMapper<TypeComponent> tm = ComponentMapper.getFor(TypeComponent.class);
            for (Entity hero : heroes) {
                TypeComponent type = tm.get(hero);
                if (type == null)
                    continue;
                String heroKey = "HERO_" + type.type.name();
                SaveData.HeroSaveData hsd = save.heroes.get(heroKey);
                if (hsd != null) {
                    StatsComponent stats = sm.get(hero);
                    stats.hp = hsd.hp;
                    stats.mana = hsd.mana;
                    System.out.println("[SaveLoad] Restored " + heroKey
                            + " HP=" + stats.hp + " Mana=" + stats.mana);
                }
            }
        }

        allEntities.addAll(heroes);

        if (save != null) {
            stageSystem.setStartingStage(save.currentStageIndex);
            // If mid-battle, pass saved enemies so spawnMobs restores them
            if (save.midBattle && save.enemies != null && !save.enemies.isEmpty()) {
                stageSystem.setSavedEnemies(save.enemies);
            }
        }
        stageSystem.setHeroesRef(heroes);

        // ── Wire allEntities into TargetingSystem for keyboard nav ────────
        targetingSystem.setAllEntities(allEntities);

        this.formulaBarUI = new FormulaBarUI(game.batch);

        this.skillButtonsUI = new SkillButtonsUI(game.batch, turnManager, targetingSystem,
                actionLogSystem, heroes, formulaBarUI, assets);

        // MouseTargetingSystem now uses camera for proper world-coordinate unprojection
        this.mouseTargetingSystem = new MouseTargetingSystem(turnManager, targetingSystem,
                actionLogSystem, battleRenderSystem.getCamera());

        List<Texture> portraits = new ArrayList<>();
        portraits.add(assets.getTexture(ArithmeticAssetManager.PORT_DIV));
        portraits.add(assets.getTexture(ArithmeticAssetManager.PORT_MUL));
        portraits.add(assets.getTexture(ArithmeticAssetManager.PORT_ADD));
        portraits.add(assets.getTexture(ArithmeticAssetManager.PORT_SUB));

        this.heroPortraitPanel = new HeroPortraitPanel(game.batch, turnManager, heroes, portraits);

        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(skillButtonsUI.getHudStage());
        mux.addProcessor(skillButtonsUI.getStage());
        mux.addProcessor(new BattleInputHandler(heroes, activeMobs, turnManager,
                targetingSystem, actionLogSystem));
        Gdx.input.setInputProcessor(mux);

        // ── Setup UI Manager & Callbacks ──────────────────────────────────
        this.uiManager = new BattleUIManager(game, assets, battleRenderSystem.getCamera(),
                () -> {
                },
                () -> {
                    if (!practiceMode) {
                        // Get the twin-spawned flag from BattleStateSystem
                        boolean twinFlag = false;
                        io.github.jhundeniel.ArithmeticHeroes.systems.BattleStateSystem bss =
                                engine.getSystem(io.github.jhundeniel.ArithmeticHeroes.systems.BattleStateSystem.class);
                        if (bss != null) twinFlag = bss.getHasTwinSpawned();

                        SaveManager.save(slotId, displayName, stageSystemRef.getCurrentStageIndex(),
                                heroes, activeMobs, turnManager, true, twinFlag);
                    }
                });

        skillButtonsUI.setPauseCallback(() -> {
            boolean canSave = !practiceMode && turnManager.getState() == BattleState.WAIT_FOR_INPUT;
            uiManager.togglePause(canSave);
        });

        // ── Dialogue manager ──────────────────────────────────────────────
        this.dialogueManager = new DialogueManager();

        // Wire chat bubbles into CombatSystem for in-combat speech bubbles
        CombatSystem combatSys = engine.getSystem(CombatSystem.class);
        if (combatSys != null) {
            combatSys.setChatBubbles(chatBubbles, dialogueManager);
        }

        // Wire chat bubbles into BattleStateSystem for Victory bubbles
        BattleStateSystem stateSys = engine.getSystem(BattleStateSystem.class);
        if (stateSys != null) {
            stateSys.setChatBubbles(chatBubbles, dialogueManager);
        }

        // ── Setup Wave Manager ────────────────────────────────────────────
        this.waveManager = new WaveManager(game, engine, factory, assets, stageSystemRef, battleCleanup, turnManager,
                actionLogSystem, skillButtonsUI, turnOrderDisplay, waveAnnouncer, dialogueManager, battleAnimations,
                heroes, activeMobs, allEntities, benchedHeroes, new WaveManager.WaveCallback() {
                    @Override
                    public void sortHeroes() {
                        sortHeroesForTurnOrder();
                    }

                    @Override
                    public void onDialogueTriggered(DialogueOverlay overlay) {
                        dialogueOverlay = overlay;
                        showingDialogue = true;
                        combatBlocked = true;
                        dialogueOverlay.setOnCompleteListener(() -> {
                            showingDialogue = false;
                            combatBlocked = false;
                        });
                    }

                    @Override
                    public void setPendingVictoryDialogue(String dialogueEvent) {
                        pendingVictoryDialogue = dialogueEvent;
                    }

                    @Override
                    public void restoreSavedTurnOrder() {
                        // "save" here refers to the SaveData object passed into the BattleScreen
                        // constructor
                        if (save != null && save.midBattle) {
                            turnManager.restoreQueuesFromSave(
                                    save.currentEntityIndex,
                                    save.currentTurnQueue,
                                    save.nextRoundQueue,
                                    save.currentRound);

                            // Restore the boss twin-spawn lock
                            if (save.hasTwinSpawned) {
                                io.github.jhundeniel.ArithmeticHeroes.systems.BattleStateSystem bss =
                                        engine.getSystem(io.github.jhundeniel.ArithmeticHeroes.systems.BattleStateSystem.class);
                                if (bss != null) bss.setHasTwinSpawned(true);
                            }
                        }
                    }
                });

        // ── Setup Battle Flow Controller ──────────────────────────────────
        this.flowController = new BattleFlowController(game, stageSystemRef, turnManager,
                uiManager, battleCleanup, heroes, activeMobs, practiceMode);

        // Tell the Stage System to use our new WaveManager instead of BattleScreen!
        stageSystem.setStageListener(waveManager::spawnMobs);

        stageSystem.triggerStart();
    }

    @Override
    public void render(float delta) {
        // ── ESC toggles pause — but not when overlays are active ──────────
        if (!uiManager.showingGameOver && !uiManager.showingVictory && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            boolean canSave = !practiceMode && turnManager.getState() == BattleState.WAIT_FOR_INPUT;
            uiManager.togglePause(canSave);
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // ── Normal game loop ──────────────────────────────────────────────
        allEntities.clear();
        allEntities.addAll(heroes);
        allEntities.addAll(activeMobs);

        battleAnimations.update(delta);
        mouseTargetingSystem.update(delta);
        engine.update(delta);

        // Hero death → gravestone
        com.badlogic.ashley.core.ComponentMapper<StatsComponent> smDeath = com.badlogic.ashley.core.ComponentMapper
                .getFor(StatsComponent.class);
        com.badlogic.ashley.core.ComponentMapper<io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent> vmDeath = com.badlogic.ashley.core.ComponentMapper
                .getFor(io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent.class);
        for (java.util.Map.Entry<Entity, GraveData> entry : heroGravestones.entrySet()) {
            Entity hero = entry.getKey();
            GraveData grave = entry.getValue();
            StatsComponent stats = smDeath.get(hero);
            if (stats != null && stats.hp <= 0) {
                io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent v = vmDeath.get(hero);
                if (v != null) {
                    float gw = v.width * 2.0f;
                    float gh = v.height * 2.0f;
                    float gx = v.x + (v.width - gw) / 2f;
                    float gy = v.y;
                    battleAnimations.showGravestone(grave.tex, grave.frames, gx, gy, gw, gh, grave.staticTex);
                }
                heroGravestones.remove(hero); // only trigger once
                break; // handle one per frame to avoid ConcurrentModificationException
            }
        }

        enemyTargetHighlight.render(battleRenderSystem.getCamera(), delta);
        mouseTargetingSystem.render(battleRenderSystem.getCamera());
        battleRenderSystem.getCamera().update();

        actionLogSystem.render();
        turnOrderDisplay.render();
        formulaBarUI.render();

        game.batch.setProjectionMatrix(battleRenderSystem.getCamera().combined);
        game.batch.begin();
        if (waveAnnouncer != null)
            waveAnnouncer.render(battleRenderSystem.getCamera());
        game.batch.end();

        // ── Combat chat bubbles ───────────────────────────────────────────
        chatBubbles.render(battleRenderSystem.getCamera());

        enemyIntentSystem.render(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (!combatBlocked)
            skillButtonsUI.render();

        // ── Dialogue overlay (pre-combat) ─────────────────────────────────
        if (showingDialogue && dialogueOverlay != null) {
            dialogueOverlay.render(battleRenderSystem.getCamera());
            dialogueOverlay.handleInput();
            if (dialogueOverlay.isComplete()) {
                showingDialogue = false;
                combatBlocked = false;
            }
            return; // block all other input while dialogue plays
        }

        // ── Post-victory dialogue (non-final stages) ──────────────────────
        if (showingPostVictoryDialogue && dialogueOverlay != null) {
            dialogueOverlay.render(battleRenderSystem.getCamera());
            dialogueOverlay.handleInput();
            if (dialogueOverlay.isComplete()) {
                showingPostVictoryDialogue = false;
                dialogueOverlay.dispose();
                dialogueOverlay = null;
            }
            return;
        }

        // ── State / turn change → update UI buttons ───────────────────────
        BattleState curState = turnManager.getState();
        Entity curEntity = turnManager.getCurrentEntityTurn();

        boolean stateChanged = (curState != lastState);
        boolean entityChanged = (curEntity != lastEntity);

        if (stateChanged || entityChanged) {
            if (curState == BattleState.WAIT_FOR_INPUT) {
                skillButtonsUI.updateForCurrentTurn();
            } else if (curState == BattleState.SELECT_TARGET) {
                skillButtonsUI.showSelectTargetUI();
            } else if (curState == BattleState.CHOOSE_VALUE) {
                if (targetingSystem.isPendingGroupBurdenChoice()) {
                    Entity caster = targetingSystem.getGroupBurdenCaster();
                    if (caster != null)
                        skillButtonsUI.showGroupBurdenChoiceUI(caster);
                } else {
                    skillButtonsUI.showBuffChooserUI();
                }
            } else {
                skillButtonsUI.getStage().clear();
            }
            lastState = curState;
            lastEntity = curEntity;
        }

        // ── Monitor Game State (Win/Loss) ──────────────────────────────────────
        flowController.checkEndGameConditions();

        // ── Render UI Overlays (Pause, Game Over, Victory) ─────────────────
        if (uiManager.render()) {
        } // If a menu is open, block all other input!
    }

    @Override
    public void resize(int w, int h) {
        battleRenderSystem.resize(w, h);
        actionLogSystem.resize(w, h);
        enemyIntentSystem.resize(w, h);
        skillButtonsUI.resize(w, h);
        heroPortraitPanel.resize(w, h);
    }

    @Override
    public void dispose() {
        battleCleanup.cleanup(heroes, activeMobs);
        actionLogSystem.dispose();
        enemyIntentSystem.dispose();
        skillButtonsUI.dispose();
        mouseTargetingSystem.dispose();
        battleAnimations.dispose();
        heroPortraitPanel.dispose();
        turnOrderDisplay.dispose();
        enemyTargetHighlight.dispose();
        if (uiManager != null)
            uiManager.dispose();
        if (dialogueOverlay != null)
            dialogueOverlay.dispose();
        chatBubbles.dispose();
        formulaBarUI.dispose();
        waveAnnouncer.dispose();
    }

    public BattleAnimations getAnimations() {
        return battleAnimations;
    }

    public ActionLogSystem getActionLogSystem() {
        return actionLogSystem;
    }

    public EnemyIntentSystem getEnemyIntentSystem() {
        return enemyIntentSystem;
    }

    public ChatBubbleSystem getChatBubbles() {
        return chatBubbles;
    }

    public DialogueManager getDialogueManager() {
        return dialogueManager;
    }

    // ── Turn Order Enforcement ─────────────────────────────────────────────
    private void sortHeroesForTurnOrder() {
        ComponentMapper<TypeComponent> tm = ComponentMapper.getFor(TypeComponent.class);
        heroes.sort((h1, h2) -> {
            TypeComponent t1 = tm.get(h1);
            TypeComponent t2 = tm.get(h2);

            int rank1 = getHeroSortRank(t1 != null ? t1.type : null);
            int rank2 = getHeroSortRank(t2 != null ? t2.type : null);

            return Integer.compare(rank1, rank2);
        });
    }

    private int getHeroSortRank(Operator type) {
        if (type == null)
            return 99;
        switch (type) {
            case DIVISION:
                return 1;
            case MULTIPLICATION:
                return 2;
            case ADDITION:
                return 3;
            case SUBTRACTION:
                return 4;
            default:
                return 99;
        }
    }
    // ───────────────────────────────────────────────────────────────────────
}
