package io.github.jhundeniel.ArithmeticHeroes.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plain data object that mirrors the JSON save file structure.
 * Serialized/deserialized by LibGDX's Json class.
 */
public class SaveData {

    /** Unique slot identifier (e.g. "run_1679832145000"). */
    public String slotId;

    /** User-visible label (e.g. "Run 1"). */
    public String displayName;

    /** Epoch milliseconds when this save was last written (for sorting). */
    public long timestamp;

    /** The stage index the player has reached (0-based). */
    public int currentStageIndex;

    /** The round number from TurnManager when saved. */
    public int currentRound;

    /** True if this save was made mid-battle (enemies still alive). */
    public boolean midBattle;

    /** True if the Boss 3 twin has already been spawned this run. */
    public boolean hasTwinSpawned;

    /** Turn Manager index mapping to preserve the turn queue. */
    public List<Integer> currentTurnQueue = new ArrayList<>();
    public List<Integer> nextRoundQueue = new ArrayList<>();
    public int currentEntityIndex = -1;

    /** Per-hero stats keyed by their CharacterData key (e.g. "HERO_ADDITION"). */
    public Map<String, HeroSaveData> heroes = new HashMap<>();

    /** Living enemies at save time (only populated when midBattle == true). */
    public List<EnemySaveData> enemies = new ArrayList<>();

    /**
     * Snapshot of a single hero's mutable stats.
     */
    public static class HeroSaveData {
        public int hp;
        public int mana;

        /** No-arg constructor required by LibGDX Json deserializer. */
        public HeroSaveData() {
        }

        public HeroSaveData(int hp, int mana) {
            this.hp = hp;
            this.mana = mana;
        }
    }

    /**
     * Snapshot of a single enemy's key and current HP.
     */
    public static class EnemySaveData {
        public String enemyKey;
        public int hp;

        /** No-arg constructor required by LibGDX Json deserializer. */
        public EnemySaveData() {
        }

        public EnemySaveData(String enemyKey, int hp) {
            this.enemyKey = enemyKey;
            this.hp = hp;
        }
    }
}
