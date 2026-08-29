package io.github.jhundeniel.ArithmeticHeroes.managers;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SaveData;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles saving and loading player progress as JSON.
 *
 * <p>
 * Uses {@code Gdx.files.local()} so files are confined to the
 * application's sandbox directory (rules.md Rule 1).
 * NEVER uses Gdx.files.internal() — that is read-only at runtime.
 * </p>
 *
 * <p>
 * Writes follow the <b>atomic two-step</b> pattern (rules.md Rule 4):
 * data goes to a temp file first, then the temp file is renamed to the
 * real save path only after a successful write.
 * </p>
 *
 * <p>
 * Supports multiple save slots. Each slot has a unique slotId
 * and is stored as saves/{slotId}.json.
 * </p>
 */
public class SaveManager {

    private static final String SAVES_DIR = "saves/";
    // Legacy path kept for backward-compatible deletion
    private static final String LEGACY_SAVE_PATH = "saves/savegame.json";

    private static final ComponentMapper<StatsComponent> statsCM = ComponentMapper.getFor(StatsComponent.class);
    private static final ComponentMapper<TypeComponent> typeCM = ComponentMapper.getFor(TypeComponent.class);

    // ── Slot ID generation ────────────────────────────────────────────────

    /** Generate a unique slot ID based on current time. */
    public static String generateSlotId() {
        return "run_" + System.currentTimeMillis();
    }

    /**
     * Generate a display name like "Run 1", "Run 2", etc. based on highest existing
     * run number.
     */
    public static String generateDisplayName() {
        List<SaveData> existing = getAvailableSaves();
        int maxNum = 0;
        for (SaveData save : existing) {
            if (save.displayName != null && save.displayName.startsWith("Run ")) {
                try {
                    int num = Integer.parseInt(save.displayName.substring(4).trim());
                    if (num > maxNum)
                        maxNum = num;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return "Run " + (maxNum + 1);
    }

    // ── Save (full mid-battle) ────────────────────────────────────────────

    /**
     * Full save with mid-battle state support.
     *
     * @param slotId       unique slot identifier
     * @param displayName  user-visible label
     * @param stageIndex   current stage index
     * @param heroes       live hero entities
     * @param enemies      live enemy entities (may be empty)
     * @param currentRound round number from TurnManager
     * @param midBattle        true if saving mid-battle
     * @param hasTwinSpawned   true if Boss 3 twin was already spawned
     */
    public static void save(String slotId, String displayName, int stageIndex,
            List<Entity> heroes, List<Entity> enemies,
            TurnManager turnManager, boolean midBattle, boolean hasTwinSpawned) {

        SaveData data = new SaveData();
        data.slotId = slotId;
        data.displayName = displayName;
        data.timestamp = System.currentTimeMillis();
        data.currentStageIndex = stageIndex;
        data.midBattle = midBattle;
        data.hasTwinSpawned = hasTwinSpawned;

        // --- NEW: Capture Turn Queue state ---
        if (turnManager != null) {
            turnManager.captureQueuesForSave(data);
        }

        // Snapshot hero stats
        for (Entity hero : heroes) {
            StatsComponent stats = statsCM.get(hero);
            TypeComponent type = typeCM.get(hero);
            if (stats == null || type == null)
                continue;

            String heroKey = "HERO_" + type.type.name();
            data.heroes.put(heroKey, new SaveData.HeroSaveData(stats.hp, stats.mana));
        }

        // Snapshot enemy stats (only if mid-battle)
        if (midBattle && enemies != null) {
            for (Entity enemy : enemies) {
                StatsComponent stats = statsCM.get(enemy);
                TypeComponent type = typeCM.get(enemy);
                if (stats == null || type == null)
                    continue;

                String enemyKey = (type.registryKey != null) ? type.registryKey : "UNKNOWN";
                data.enemies.add(new SaveData.EnemySaveData(enemyKey, stats.hp));
            }
        }

        writeSlot(slotId, data);
    }

    /**
     * Backward-compatible save for auto-save on stage clear.
     * Uses the given slotId but sets midBattle = false and enemies empty.
     */
    public static void save(String slotId, String displayName,
            int stageIndex, List<Entity> heroes) {
        // We pass 'null' for TurnManager because at the start of a stage,
        // the turn order is generated fresh anyway.
        save(slotId, displayName, stageIndex, heroes,
                new ArrayList<Entity>(), null, false, false);
    }

    /**
     * Legacy save method (backward compatibility with StageSystem calls
     * that don't have a slotId yet). Creates a default slot.
     */
    public static void save(int stageIndex, List<Entity> heroes) {
        save("default", "Auto-Save", stageIndex, heroes);
    }

    // ── Load ──────────────────────────────────────────────────────────────

    /**
     * Load a specific save slot.
     *
     * @param slotId the slot to load
     * @return the deserialized SaveData, or null if not found/corrupted.
     */
    public static SaveData load(String slotId) {
        try {
            FileHandle file = Gdx.files.local(SAVES_DIR + slotId + ".json");
            if (!file.exists()) {
                System.out.println("[SaveManager] No save file for slot: " + slotId);
                return null;
            }

            Json json = new Json();
            json.setIgnoreUnknownFields(true);
            SaveData data = json.fromJson(SaveData.class, file);

            // Fallback defaults (Rule 5)
            if (data.heroes == null)
                data.heroes = new java.util.HashMap<>();
            if (data.enemies == null)
                data.enemies = new ArrayList<>();
            if (data.currentStageIndex < 0)
                data.currentStageIndex = 0;
            if (data.slotId == null)
                data.slotId = slotId;

            System.out.println("[SaveManager] Loaded slot: " + slotId
                    + " stageIndex=" + data.currentStageIndex
                    + " midBattle=" + data.midBattle);
            return data;
        } catch (Exception e) {
            System.err.println("[SaveManager] Failed to load slot: " + slotId);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Legacy load (loads the old savegame.json if it exists).
     */
    public static SaveData load() {
        // Try legacy path first
        try {
            FileHandle file = Gdx.files.local(LEGACY_SAVE_PATH);
            if (file.exists()) {
                Json json = new Json();
                json.setIgnoreUnknownFields(true);
                SaveData data = json.fromJson(SaveData.class, file);
                if (data.heroes == null)
                    data.heroes = new java.util.HashMap<>();
                if (data.enemies == null)
                    data.enemies = new ArrayList<>();
                if (data.currentStageIndex < 0)
                    data.currentStageIndex = 0;
                return data;
            }
        } catch (Exception e) {
            System.err.println("[SaveManager] Legacy save corrupted, ignoring.");
        }
        return null;
    }

    // ── List all saves ────────────────────────────────────────────────────

    /**
     * Scans the saves/ directory and returns all valid save slots,
     * sorted by timestamp descending (most recent first).
     *
     * Safety: creates dir if missing, skips unparseable files.
     * Gotcha: uses Gdx.files.local() exclusively (never internal()).
     */
    public static List<SaveData> getAvailableSaves() {
        List<SaveData> results = new ArrayList<>();

        FileHandle dir = Gdx.files.local(SAVES_DIR);
        // Safety (Fix #1): handle first-run when saves/ doesn't exist
        if (!dir.exists()) {
            dir.file().mkdirs();
            return results;
        }

        FileHandle[] files = dir.list(".json");
        Json json = new Json();
        json.setIgnoreUnknownFields(true);

        for (FileHandle file : files) {
            // Skip the leaderboard file
            if (file.name().equals("leaderboard.json"))
                continue;
            // Skip legacy temp files
            if (file.name().contains("_temp"))
                continue;

            // Safety (Fix #1): try-catch per file so one bad file doesn't crash
            try {
                SaveData data = json.fromJson(SaveData.class, file);
                if (data == null)
                    continue;

                // Fallback defaults for legacy/incomplete saves
                if (data.heroes == null)
                    data.heroes = new java.util.HashMap<>();
                if (data.enemies == null)
                    data.enemies = new ArrayList<>();
                if (data.slotId == null) {
                    // Derive slotId from filename (strip .json)
                    data.slotId = file.nameWithoutExtension();
                }
                if (data.displayName == null) {
                    data.displayName = data.slotId;
                }

                results.add(data);
            } catch (Exception e) {
                System.err.println("[SaveManager] Skipping unparseable file: "
                        + file.name() + " — " + e.getMessage());
            }
        }

        // Sort: Auto-Save always first, then "Run N" ascending by N
        results.sort((a, b) -> {
            boolean aIsAuto = "default".equals(a.slotId);
            boolean bIsAuto = "default".equals(b.slotId);
            if (aIsAuto && !bIsAuto)
                return -1; // Auto-Save always first
            if (!aIsAuto && bIsAuto)
                return 1;
            if (aIsAuto && bIsAuto)
                return 0;

            // Both are manual runs — sort ascending by run number
            int aNum = extractRunNumber(a.displayName);
            int bNum = extractRunNumber(b.displayName);
            return Integer.compare(aNum, bNum);
        });
        return results;
    }

    /**
     * Extracts the numeric suffix from a display name like "Run 3".
     * Returns Integer.MAX_VALUE if the name doesn't match the pattern,
     * so unknown names sort to the end.
     */
    private static int extractRunNumber(String displayName) {
        if (displayName != null && displayName.startsWith("Run ")) {
            try {
                return Integer.parseInt(displayName.substring(4).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return Integer.MAX_VALUE;
    }

    // ── Delete ────────────────────────────────────────────────────────────

    /**
     * Delete a specific save slot.
     */
    public static void deleteSave(String slotId) {
        FileHandle file = Gdx.files.local(SAVES_DIR + slotId + ".json");
        if (file.exists()) {
            file.delete();
            System.out.println("[SaveManager] Deleted slot: " + slotId);
        } else {
            System.out.println("[SaveManager] No save file for slot: " + slotId);
        }
    }

    /** Legacy: delete the old savegame.json */
    public static void deleteSave() {
        FileHandle file = Gdx.files.local(LEGACY_SAVE_PATH);
        if (file.exists()) {
            file.delete();
            System.out.println("[SaveManager] Legacy save file deleted.");
        }
    }

    /**
     * Delete ALL save files in the saves/ directory (except leaderboard.json).
     * Called by the Main Menu's "Reset Data" action.
     */
    public static void deleteAllSaves() {
        FileHandle dir = Gdx.files.local(SAVES_DIR);
        if (!dir.exists())
            return;

        FileHandle[] files = dir.list(".json");
        for (FileHandle file : files) {
            if (file.name().equals("leaderboard.json"))
                continue;
            if (file.name().contains("_temp"))
                continue;
            file.delete();
            System.out.println("[SaveManager] Deleted: " + file.name());
        }

        // Also delete legacy file if it exists
        deleteSave();
        System.out.println("[SaveManager] All save files deleted.");
    }

    // ── Query ─────────────────────────────────────────────────────────────

    /** @return true if any save file exists. */
    public static boolean hasSave() {
        return !getAvailableSaves().isEmpty()
                || Gdx.files.local(LEGACY_SAVE_PATH).exists();
    }

    /**
     * Scans ALL save slots and returns the highest currentStageIndex
     * found across any save. This represents how far the player has
     * ever progressed — stages below this index have been cleared.
     *
     * @return the highest stage index reached, or -1 if no saves exist.
     */
    public static int getHighestClearedStageIndex() {
        List<SaveData> saves = getAvailableSaves();
        int highest = -1;
        for (SaveData save : saves) {
            if (save.currentStageIndex > highest) {
                highest = save.currentStageIndex;
            }
        }
        return highest;
    }

    // ── Internal write ────────────────────────────────────────────────────

    private static void writeSlot(String slotId, SaveData data) {
        try {
            Json json = new Json();
            json.setOutputType(JsonWriter.OutputType.json);
            String jsonText = json.prettyPrint(data);

            String savePath = SAVES_DIR + slotId + ".json";
            String tempPath = SAVES_DIR + slotId + "_temp.json";

            // Step 1 — write to temp file (Gdx.files.local only!)
            FileHandle temp = Gdx.files.local(tempPath);
            temp.writeString(jsonText, false);

            // Step 2 — atomic rename
            FileHandle real = Gdx.files.local(savePath);
            if (real.exists())
                real.delete();
            temp.file().renameTo(real.file());

            System.out.println("[SaveManager] Saved slot: " + slotId
                    + " stageIndex=" + data.currentStageIndex
                    + " midBattle=" + data.midBattle);
        } catch (Exception e) {
            System.err.println("[SaveManager] Failed to save slot: " + slotId);
            e.printStackTrace();
        }
    }
}
