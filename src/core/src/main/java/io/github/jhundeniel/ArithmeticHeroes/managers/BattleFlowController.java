package io.github.jhundeniel.ArithmeticHeroes.managers;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.Main;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleState;
import io.github.jhundeniel.ArithmeticHeroes.battle.cleanup.BattleCleanup;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.screens.StageSelectScreen;
import io.github.jhundeniel.ArithmeticHeroes.systems.StageSystem;

import java.util.List;

public class BattleFlowController {

    private final Main game;
    private final StageSystem stageSystem;
    private final TurnManager turnManager;
    private final BattleUIManager uiManager;
    private final BattleCleanup battleCleanup;
    private final List<Entity> heroes;
    private final List<Entity> activeMobs;
    private final boolean practiceMode;

    public BattleFlowController(Main game, StageSystem stageSystem, TurnManager turnManager,
            BattleUIManager uiManager, BattleCleanup battleCleanup,
            List<Entity> heroes, List<Entity> activeMobs, boolean practiceMode) {
        this.game = game;
        this.stageSystem = stageSystem;
        this.turnManager = turnManager;
        this.uiManager = uiManager;
        this.battleCleanup = battleCleanup;
        this.heroes = heroes;
        this.activeMobs = activeMobs;
        this.practiceMode = practiceMode;
    }

    /**
     * Called every frame to monitor if the battle has been won or lost.
     */
    public void checkEndGameConditions() {
        // ── 1. Practice Mode Victory Check ────────────────────────────────
        if (practiceMode && stageSystem.isPracticeComplete()) {
            System.out.println("[BattleFlow] Practice Stage Cleared! Returning to Stage Select.");
            battleCleanup.cleanup(heroes, activeMobs); // Safe to manually cleanup here because we aren't using the UI
                                                       // manager hook
            game.transitionToScreen(new StageSelectScreen(game));
            return;
        }

        // ── 2. Story Mode Victory Check ───────────────────────────────────
        if (!practiceMode && !uiManager.showingVictory && stageSystem.isGameComplete()) {
            System.out.println("[BattleFlow] Final Boss Defeated! Triggering Victory.");

            ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
            int aliveCount = heroes.size();
            int totalHp = 0;
            int totalMaxHp = 0;

            for (Entity hero : heroes) {
                StatsComponent s = sm.get(hero);
                if (s != null) {
                    totalHp += s.hp;
                    totalMaxHp += s.maxHp;
                }
            }
            totalMaxHp = Math.max(totalMaxHp, 1);
            int stagesCleared = stageSystem.getCurrentStageIndex();
            int totalStages = StageRegistry.getStageCount();
            int score = LeaderboardManager.calculateScore(stagesCleared, totalStages, aliveCount, totalHp, totalMaxHp);

            uiManager.triggerVictory(score, aliveCount, stagesCleared);
        }

        // ── 3. Defeat Check (Game Over) ───────────────────────────────────
        // BattleStateSystem removes dead heroes from the list before setting
        // GAME_OVER, so heroes will already be empty at this point.
        // We only need to check that the state is GAME_OVER and the overlay
        // hasn't been shown yet.
        if (!uiManager.showingGameOver && !uiManager.showingVictory
                && turnManager.getState() == BattleState.GAME_OVER) {
            uiManager.triggerGameOver(stageSystem.getCurrentStageIndex());
            System.out.println("[BattleFlow] All heroes defeated.");
        }
    }
}