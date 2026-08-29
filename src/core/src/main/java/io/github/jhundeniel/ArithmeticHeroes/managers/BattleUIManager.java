package io.github.jhundeniel.ArithmeticHeroes.managers;

import com.badlogic.gdx.graphics.OrthographicCamera;
import io.github.jhundeniel.ArithmeticHeroes.Main;
import io.github.jhundeniel.ArithmeticHeroes.screens.GameOverOverlay;
import io.github.jhundeniel.ArithmeticHeroes.screens.LeaderboardScreen;
import io.github.jhundeniel.ArithmeticHeroes.screens.MainMenuScreen;
import io.github.jhundeniel.ArithmeticHeroes.screens.PauseMenuOverlay;
import io.github.jhundeniel.ArithmeticHeroes.screens.VictoryOverlay;
import io.github.jhundeniel.ArithmeticHeroes.screens.BattleScreen;
import io.github.jhundeniel.ArithmeticHeroes.data.LeaderboardEntry;

public class BattleUIManager {

    private final Main game;
    private final ArithmeticAssetManager assets;
    private final OrthographicCamera camera;

    // These let the UI tell BattleScreen to clean up memory before leaving
    private final Runnable cleanupAction;
    private final Runnable saveAction;

    private final PauseMenuOverlay pauseOverlay;
    private GameOverOverlay gameOverOverlay;
    private VictoryOverlay victoryOverlay;

    public boolean paused = false;
    public boolean showingGameOver = false;
    public boolean showingVictory = false;

    public BattleUIManager(Main game, ArithmeticAssetManager assets, OrthographicCamera camera,
            Runnable cleanupAction, Runnable saveAction) {
        this.game = game;
        this.assets = assets;
        this.camera = camera;
        this.cleanupAction = cleanupAction;
        this.saveAction = saveAction;

        this.pauseOverlay = new PauseMenuOverlay(false, assets);
    }

    // ── TRIGGER METHODS ──────────────────────────────────────────────
    public void togglePause(boolean canSave) {
        if (!showingGameOver && !showingVictory) {
            paused = !paused;
            if (paused) {
                pauseOverlay.setCanSave(canSave, assets);
            }
        }
    }

    public void triggerGameOver(int stagesCleared) {
        showingGameOver = true;
        game.assetManager.playMusic(ArithmeticAssetManager.BGM_GAME_OVER, false);
        gameOverOverlay = new GameOverOverlay(stagesCleared, assets);
    }

    public void triggerVictory(int score, int aliveCount, int stagesCleared) {
        showingVictory = true;
        game.assetManager.playMusic(ArithmeticAssetManager.BGM_VICTORY, false);
        victoryOverlay = new VictoryOverlay(score, aliveCount, stagesCleared, assets);
    }

    // ── RENDER LOOP ──────────────────────────────────────────────────
    /**
     * Renders active overlays.
     * Returns TRUE if an overlay is taking over the screen (so BattleScreen can
     * pause updates).
     */
    public boolean render() {
        if (showingGameOver && gameOverOverlay != null) {
            gameOverOverlay.render(camera);
            GameOverOverlay.GameOverAction goAction = gameOverOverlay.handleInput(camera, assets);
            if (goAction == GameOverOverlay.GameOverAction.RETRY) {
                cleanupAction.run();
                game.transitionToScreen(new BattleScreen(game));
            } else if (goAction == GameOverOverlay.GameOverAction.MAIN_MENU) {
                cleanupAction.run();
                game.transitionToScreen(new MainMenuScreen(game));
            }
            return true; // Overlay active, block normal game updates
        }

        if (showingVictory && victoryOverlay != null) {
            victoryOverlay.render(camera);
            VictoryOverlay.VictoryAction vAction = victoryOverlay.handleInput(camera, assets);
            if (vAction == VictoryOverlay.VictoryAction.SUBMIT) {
                LeaderboardEntry entry = new LeaderboardEntry(
                        victoryOverlay.getPlayerName(),
                        victoryOverlay.getFinalScore(),
                        victoryOverlay.getStagesCleared(),
                        victoryOverlay.getHeroesAlive());
                LeaderboardManager.submitEntry(entry);

                cleanupAction.run();
                game.transitionToScreen(new LeaderboardScreen(game));
            }
            return true; // Overlay active, block normal game updates
        }

        if (paused) {
            pauseOverlay.render(camera, assets);
            pauseOverlay.updateSliders(camera, assets);

            // Only process button clicks when not dragging a volume slider
            if (!pauseOverlay.isDraggingSlider()) {
                PauseMenuOverlay.PauseAction action = pauseOverlay.handleInput(camera, assets);
                if (action == PauseMenuOverlay.PauseAction.RESUME) {
                    paused = false;
                } else if (action == PauseMenuOverlay.PauseAction.SAVE) {
                    saveAction.run();
                    pauseOverlay.showSaveNotification();
                } else if (action == PauseMenuOverlay.PauseAction.EXIT_TO_MENU) {
                    cleanupAction.run();
                    game.transitionToScreen(new MainMenuScreen(game));
                }
            }
            return true; // Overlay active, block normal game updates
        }

        return false; // No overlays active, game continues normally
    }

    // ── CLEANUP ──────────────────────────────────────────────────────
    public void dispose() {
        if (pauseOverlay != null)
            pauseOverlay.dispose();
        if (gameOverOverlay != null)
            gameOverOverlay.dispose();
        if (victoryOverlay != null)
            victoryOverlay.dispose();
    }
}
