package io.github.jhundeniel.ArithmeticHeroes.managers;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.Main;
import io.github.jhundeniel.ArithmeticHeroes.battle.cleanup.BattleCleanup;
import io.github.jhundeniel.ArithmeticHeroes.components.Operator;
import io.github.jhundeniel.ArithmeticHeroes.components.PortraitComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.DialogueLine;
import io.github.jhundeniel.ArithmeticHeroes.data.SaveData;
import io.github.jhundeniel.ArithmeticHeroes.data.StageData;
import io.github.jhundeniel.ArithmeticHeroes.factories.EntityFactory;
import io.github.jhundeniel.ArithmeticHeroes.systems.SkillButtonsUI;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.screens.DialogueOverlay;
import io.github.jhundeniel.ArithmeticHeroes.systems.StageSystem;
import io.github.jhundeniel.ArithmeticHeroes.systems.TurnOrderDisplay;
import io.github.jhundeniel.ArithmeticHeroes.battle.WaveAnnouncer;

import java.util.List;

public class WaveManager {

    private final Main game;
    private final Engine engine;
    private final EntityFactory factory;
    private final ArithmeticAssetManager assets;
    private final StageSystem stageSystemRef;
    private final BattleCleanup battleCleanup;
    private final TurnManager turnManager;
    private final ActionLogSystem actionLogSystem;
    private final SkillButtonsUI skillButtonsUI;
    private final TurnOrderDisplay turnOrderDisplay;
    private final WaveAnnouncer waveAnnouncer;
    private final DialogueManager dialogueManager;
    private final BattleAnimations battleAnimations;

    private final List<Entity> heroes;
    private final List<Entity> activeMobs;
    private final List<Entity> allEntities;
    private final List<Entity> benchedHeroes;

    // The Callback Interface so WaveManager can talk back to BattleScreen
    public interface WaveCallback {
        void sortHeroes();

        void onDialogueTriggered(DialogueOverlay overlay);

        void setPendingVictoryDialogue(String dialogueEvent);

        void restoreSavedTurnOrder();
    }

    private final WaveCallback callback;

    public WaveManager(Main game, Engine engine, EntityFactory factory, ArithmeticAssetManager assets,
            StageSystem stageSystemRef, BattleCleanup battleCleanup, TurnManager turnManager,
            ActionLogSystem actionLogSystem, SkillButtonsUI skillButtonsUI, TurnOrderDisplay turnOrderDisplay,
            WaveAnnouncer waveAnnouncer, DialogueManager dialogueManager, BattleAnimations battleAnimations,
            List<Entity> heroes, List<Entity> activeMobs, List<Entity> allEntities, List<Entity> benchedHeroes,
            WaveCallback callback) {
        this.game = game;
        this.engine = engine;
        this.factory = factory;
        this.assets = assets;
        this.stageSystemRef = stageSystemRef;
        this.battleCleanup = battleCleanup;
        this.turnManager = turnManager;
        this.actionLogSystem = actionLogSystem;
        this.skillButtonsUI = skillButtonsUI;
        this.turnOrderDisplay = turnOrderDisplay;
        this.waveAnnouncer = waveAnnouncer;
        this.dialogueManager = dialogueManager;
        this.battleAnimations = battleAnimations;
        this.heroes = heroes;
        this.activeMobs = activeMobs;
        this.allEntities = allEntities;
        this.benchedHeroes = benchedHeroes;
        this.callback = callback;
    }

    public void spawnMobs(int stageIndex) {
        String bgmToPlay = null;
        switch (stageIndex) {
            case 0:
            case 1:
            case 2:
                bgmToPlay = ArithmeticAssetManager.BGM_TUTORIAL_STAGES;
                break;
            case 3:
                bgmToPlay = ArithmeticAssetManager.BGM_STAGE1_WAVE1;
                break;
            case 4:
                bgmToPlay = ArithmeticAssetManager.BGM_STAGE1_WAVE2;
                break;
            case 5:
                bgmToPlay = ArithmeticAssetManager.BGM_STAGE2_WAVE1;
                break;
            case 6:
                bgmToPlay = ArithmeticAssetManager.BGM_STAGE2_WAVE2;
                break;
            case 7:
                bgmToPlay = ArithmeticAssetManager.BGM_STAGE3_WAVE1;
                break;
            case 8:
                bgmToPlay = ArithmeticAssetManager.BGM_STAGE3_WAVE2;
                break;
        }
        if (bgmToPlay != null)
            game.assetManager.playMusic(bgmToPlay, true);

        // Remove previous wave entities
        allEntities.removeAll(activeMobs);
        battleCleanup.cleanupEnemies(activeMobs);
        activeMobs.clear();
        battleAnimations.clearGravestones();

        // Un-bench heroes
        for (Entity hidden : benchedHeroes) {
            engine.addEntity(hidden);
            heroes.add(hidden);
        }
        benchedHeroes.clear();

        StageData currentStage = StageRegistry.getStage(stageIndex);
        if (currentStage == null) {
            actionLogSystem.addMessage("Victory! You cleared all stages!");
            return;
        }

        // ── 1. FILTER TUTORIAL HEROES FIRST! ──
        ComponentMapper<TypeComponent> tm = ComponentMapper.getFor(TypeComponent.class);
        if (currentStage.isTutorial && currentStage.tutorialHeroes != null) {
            java.util.Set<String> allowed = new java.util.HashSet<>(
                    java.util.Arrays.asList(currentStage.tutorialHeroes));
            java.util.List<Entity> heroesToRemove = new java.util.ArrayList<>();

            for (Entity hero : heroes) {
                TypeComponent type = tm.get(hero);
                if (type != null && !allowed.contains("HERO_" + type.type.name())) {
                    heroesToRemove.add(hero);
                }
            }
            for (Entity ghost : heroesToRemove) {
                engine.removeEntity(ghost);
                heroes.remove(ghost);
                benchedHeroes.add(ghost);
            }
        }

        // ── 2. STAGE 3 SPACING FIX ──
        ComponentMapper<io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent> vm = ComponentMapper
                .getFor(io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent.class);
        for (Entity hero : heroes) {
            io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent v = vm.get(hero);
            TypeComponent type = tm.get(hero);
            if (v != null && type != null) {
                float defaultX = 0f;
                if (type.type == Operator.ADDITION)
                    defaultX = EntityFactory.H1_X;
                else if (type.type == Operator.SUBTRACTION)
                    defaultX = EntityFactory.H2_X;
                else if (type.type == Operator.MULTIPLICATION)
                    defaultX = EntityFactory.H3_X;
                else if (type.type == Operator.DIVISION)
                    defaultX = EntityFactory.H4_X;

                v.x = (stageIndex >= 7) ? defaultX - 80f : defaultX;
            }
        }

        // ── 3. MID-BATTLE RESTORE CHECK ──
        java.util.List<SaveData.EnemySaveData> savedEnemies = stageSystemRef.getSavedEnemies();
        if (savedEnemies != null && !savedEnemies.isEmpty()) {
            ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
            int enemyCount = savedEnemies.size();

            for (int i = 0; i < enemyCount; i++) {
                SaveData.EnemySaveData esd = savedEnemies.get(i);
                Texture sheet = stageSystemRef.getEnemySheet(esd.enemyKey);
                float startX = getEnemyStartX(stageIndex, i, enemyCount, isBoss(esd.enemyKey));
                float startY = getEnemyStartY(stageIndex, i, enemyCount, isBoss(esd.enemyKey));
                Entity mob = factory.createEnemy(esd.enemyKey, startX, startY, sheet,
                        ArithmeticAssetManager.ENEMY_FRAMES);

                ComponentMapper.getFor(PortraitComponent.class).get(mob).texture = stageSystemRef
                        .getEnemyIcon(esd.enemyKey);
                StatsComponent stats = sm.get(mob);
                if (stats != null)
                    stats.hp = esd.hp;

                activeMobs.add(mob);
                allEntities.add(mob);
            }

            stageSystemRef.clearSavedEnemies();
            actionLogSystem.addMessage("Battle resumed!");
            skillButtonsUI.setCurrentStageName("Stage " + getStageNumber(stageIndex) + "  ·  Resumed");
            turnOrderDisplay.setCurrentStage(stageIndex);

            callback.sortHeroes();
            turnManager.initBattle(heroes, activeMobs);
            callback.restoreSavedTurnOrder();
            skillButtonsUI.updateForCurrentTurn();
            return;
        }

        // ── 4. NORMAL WAVE SPAWN ──
        // Clear all buffs/debuffs from previous waves
        for (Entity hero : heroes) {
            io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffectComponent sec = io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects
                    .component(hero);
            if (sec != null) {
                sec.clearAll();
            }
        }

        boolean shouldHeal = currentStage.isTutorial;
        if (!currentStage.isTutorial && stageIndex > 0) {
            StageData prevStage = StageRegistry.getStage(stageIndex - 1);
            if (prevStage != null && prevStage.isTutorial)
                shouldHeal = true;
        }

        if (shouldHeal) {
            ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
            for (Entity hero : heroes) {
                StatsComponent stats = sm.get(hero);
                if (stats != null) {
                    stats.hp = stats.maxHp;
                    stats.mana = stats.maxMana;
                }
            }
            System.out.println(">> Tutorial/Transition: All heroes restored to max HP/MP!");
        }

        int enemyCount = currentStage.enemies.length;
        for (int i = 0; i < enemyCount; i++) {
            String enemyKey = currentStage.enemies[i];
            Texture sheet = stageSystemRef.getEnemySheet(enemyKey);
            float startX = getEnemyStartX(stageIndex, i, enemyCount, isBoss(enemyKey));
            float startY = getEnemyStartY(stageIndex, i, enemyCount, isBoss(enemyKey));
            Entity mob = factory.createEnemy(enemyKey, startX, startY, sheet, ArithmeticAssetManager.ENEMY_FRAMES);
            ComponentMapper.getFor(PortraitComponent.class).get(mob).texture = stageSystemRef.getEnemyIcon(enemyKey);

            activeMobs.add(mob);
            allEntities.add(mob);
        }

        callback.setPendingVictoryDialogue(currentStage.victoryDialogue);
        String stageName = currentStage.stageName;
        String topLine = "Stage " + getStageNumber(stageIndex);
        String botLine = getWaveLine(stageName);
        waveAnnouncer.show(topLine, botLine);
        skillButtonsUI.setCurrentStageName(topLine + "  ·  " + botLine);
        actionLogSystem.addMessage(stageName + " begins!");
        turnOrderDisplay.setCurrentStage(stageIndex);

        callback.sortHeroes();
        turnManager.initBattle(heroes, activeMobs);
        skillButtonsUI.updateForCurrentTurn();

        // ── 5. COMBINE & SHOW DIALOGUE ──
        java.util.List<DialogueLine> combinedLines = new java.util.ArrayList<>();
        if (stageIndex > 0 && dialogueManager != null) {
            StageData prevStage = StageRegistry.getStage(stageIndex - 1);
            if (prevStage != null && prevStage.victoryDialogue != null) {
                java.util.List<DialogueLine> victoryLines = dialogueManager.getEvent(prevStage.victoryDialogue);
                if (victoryLines != null)
                    combinedLines.addAll(victoryLines);
            }
        }
        if (dialogueManager != null && currentStage.dialogueEvent != null) {
            java.util.List<DialogueLine> introLines = dialogueManager.getEvent(currentStage.dialogueEvent);
            if (introLines != null)
                combinedLines.addAll(introLines);
        }
        if (!combinedLines.isEmpty()) {
            DialogueOverlay overlay = new DialogueOverlay(combinedLines, assets);
            callback.onDialogueTriggered(overlay);
        }
    }

    // ── Helper Methods ──
    private String getStageNumber(int idx) {
        return String.valueOf((idx / 2) + 1);
    }

    private String getWaveLine(String stageName) {
        if (stageName.contains("Final Boss"))
            return "Final Boss!";
        if (stageName.contains("Boss"))
            return "Boss Wave!";
        if (stageName.contains("Wave 1"))
            return "Wave 1";
        if (stageName.contains("Wave 2"))
            return "Wave 2";
        return stageName;
    }

    private boolean isBoss(String enemyKey) {
        return enemyKey != null && enemyKey.contains("BOSS");
    }

    private float getEnemyStartX(int stageIndex, int index, int total, boolean isBoss) {
        float startOfEnemiesX = 1150f;
        float spacingBetweenEnemies = 280f;

        if (stageIndex <= 4) {
            startOfEnemiesX = 1450f;
            spacingBetweenEnemies = 300f;
        } else if (stageIndex == 5) {
            startOfEnemiesX = 1350f;
            spacingBetweenEnemies = 300f;
        } else if (stageIndex == 6) {
            startOfEnemiesX = 1250f;
            spacingBetweenEnemies = 350f;
        } else if (stageIndex == 7) {
            startOfEnemiesX = 1050f;
            spacingBetweenEnemies = 260f;
        } else if (stageIndex >= 8) {
            startOfEnemiesX = 950f;
            spacingBetweenEnemies = 300f;
        }

        return startOfEnemiesX + (index * spacingBetweenEnemies);
    }

    private float getEnemyStartY(int stageIndex, int index, int total, boolean isBoss) {
        return EntityFactory.GROUND_Y - 20f;
    }
}