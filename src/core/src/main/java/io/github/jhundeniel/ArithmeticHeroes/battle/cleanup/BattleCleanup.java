package io.github.jhundeniel.ArithmeticHeroes.battle.cleanup;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import java.util.List;

/**
 * Handles removal of battle entities from the Ashley engine
 * when a battle ends (game over, victory, retry, or exit to menu).
 *
 * Call cleanup() before any screen transition out of BattleScreen.
 */
public class BattleCleanup {

    private final Engine engine;

    public BattleCleanup(Engine engine) {
        this.engine = engine;
    }

    /**
     * Removes all heroes and enemies from the engine so the JVM
     * can garbage collect them. Always call this before setScreen().
     */
    public void cleanup(List<Entity> heroes, List<Entity> activeMobs) {
        for (Entity hero : heroes)
            engine.removeEntity(hero);
        for (Entity mob : activeMobs)
            engine.removeEntity(mob);
        engine.update(0f);
        heroes.clear();
        activeMobs.clear();
        System.out.println("[BattleCleanup] All battle entities removed from engine.");
    }

    /**
     * Safely removes ONLY the enemies from the engine between waves.
     * Leaves the heroes untouched!
     */
    public void cleanupEnemies(List<Entity> activeMobs) {
        for (Entity mob : activeMobs) {
            engine.removeEntity(mob);
        }
        activeMobs.clear();
        System.out.println("[BattleCleanup] Previous wave enemies garbage collected.");
    }
}