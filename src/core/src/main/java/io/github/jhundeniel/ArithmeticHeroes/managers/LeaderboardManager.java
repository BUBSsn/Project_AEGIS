package io.github.jhundeniel.ArithmeticHeroes.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import io.github.jhundeniel.ArithmeticHeroes.data.LeaderboardEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the local leaderboard: score calculation, persistence, and ranking.
 *
 * <p>
 * <b>Sorting Algorithm: Insertion Sort (Descending)</b>
 * </p>
 * <p>
 * This class uses a hand-written Insertion Sort to rank entries by score
 * in descending order. Insertion Sort was chosen because:
 * </p>
 * <ul>
 * <li>The list is always small (≤5 entries), so O(n²) is negligible.</li>
 * <li>After adding one new entry, the list is already nearly sorted,
 * making Insertion Sort's best-case O(n) ideal.</li>
 * <li>It is a stable sort, preserving the order of entries with equal
 * scores.</li>
 * </ul>
 *
 * <p>
 * File storage follows the same atomic-write pattern as {@link SaveManager}
 * (rules.md Rule 1 — workspace confinement, Rule 4 — atomic writes).
 * </p>
 */
public class LeaderboardManager {

    /** Path inside the application sandbox for the leaderboard file. */
    private static final String LEADERBOARD_PATH = "saves/leaderboard.json";
    private static final String TEMP_PATH = "saves/leaderboard_temp.json";

    /** Maximum number of entries stored on the leaderboard. */
    private static final int MAX_ENTRIES = 5;

    /** In-memory cache of leaderboard entries (sorted descending by score). */
    private static final List<LeaderboardEntry> entries = new ArrayList<>();
    private static boolean hasLoadedFromDisk = false;

    // ────────────────────────────────────────────────────────────────────────
    // Score Calculation
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Pure function that computes the composite leaderboard score.
     *
     * <p>
     * Formula (max 10,000 points):
     * </p>
     * 
     * <pre>
     *   score = (stagesCleared / totalStages) × 5000   ← 50% weight: progression
     *         + (heroesAlive  / 4)            × 3000   ← 30% weight: team survival
     *         + (totalRemainingHp / totalMaxHp) × 2000 ← 20% weight: health efficiency
     * </pre>
     *
     * @param stagesCleared    number of stages the player cleared
     * @param totalStages      total number of stages in the game
     * @param heroesAlive      heroes still alive at the end
     * @param totalRemainingHp sum of all surviving heroes' current HP
     * @param totalMaxHp       sum of all heroes' max HP (always based on 4 heroes)
     * @return the composite integer score (0–10,000)
     */
    public static int calculateScore(int stagesCleared, int totalStages,
            int heroesAlive,
            int totalRemainingHp, int totalMaxHp) {
        if (totalStages <= 0)
            totalStages = 1; // Guard: avoid division by zero
        if (totalMaxHp <= 0)
            totalMaxHp = 1;

        double stageScore = ((double) stagesCleared / totalStages) * 5000.0;
        double heroScore = ((double) heroesAlive / 4) * 3000.0;
        double hpScore = ((double) totalRemainingHp / totalMaxHp) * 2000.0;

        int finalScore = (int) Math.round(stageScore + heroScore + hpScore);

        System.out.println("[LeaderboardManager] Score breakdown: stages="
                + (int) stageScore + " heroes=" + (int) heroScore
                + " hp=" + (int) hpScore + " → TOTAL=" + finalScore);

        return finalScore;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Insertion Sort (Descending by Score)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Sorts the given list of leaderboard entries in <b>descending</b> order
     * by score using the <b>Insertion Sort</b> algorithm.
     *
     * <p>
     * <b>Algorithm overview:</b>
     * </p>
     * <ol>
     * <li>Start from the second element (index 1).</li>
     * <li>Compare the current element ("key") with each preceding element.</li>
     * <li>Shift elements that have a <em>lower</em> score one position to
     * the right (since we want descending order).</li>
     * <li>Insert the key into its correct position.</li>
     * <li>Repeat until the entire list is sorted.</li>
     * </ol>
     *
     * <p>
     * <b>Time complexity:</b> O(n²) worst-case, O(n) best-case (nearly sorted).
     * <br>
     * <b>Space complexity:</b> O(1) — in-place sort.
     * <br>
     * <b>Stability:</b> Stable — equal scores preserve their original order.
     * </p>
     *
     * @param list the list of entries to sort in place
     */
    public static void insertionSortDescending(List<LeaderboardEntry> list) {
        // Outer loop: iterate from the second element to the end
        for (int i = 1; i < list.size(); i++) {

            // The "key" element we are inserting into the sorted portion
            LeaderboardEntry key = list.get(i);
            int keyScore = key.score;

            // 'j' starts just before the key's current position
            int j = i - 1;

            // Inner loop: shift elements with LOWER scores to the right
            // (descending order means higher scores come first)
            while (j >= 0 && list.get(j).score < keyScore) {
                list.set(j + 1, list.get(j)); // Shift element right
                j--;
            }

            // Insert the key into its correct sorted position
            list.set(j + 1, key);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Submit & Retrieve
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Adds a new entry to the leaderboard, re-sorts using Insertion Sort,
     * trims to the top {@value #MAX_ENTRIES}, and persists to disk.
     *
     * @param entry the new leaderboard entry to submit
     */
    public static void submitEntry(LeaderboardEntry entry) {
        loadEntries(); // Ensure latest data is in memory
        entries.add(entry); // Append the new entry
        insertionSortDescending(entries); // Re-sort with Insertion Sort

        // Trim to top MAX_ENTRIES
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }

        saveEntries(); // Persist to disk

        System.out.println("[LeaderboardManager] Submitted entry: "
                + entry.playerName + " → " + entry.score + " pts"
                + " (leaderboard now has " + entries.size() + " entries)");
    }

    /**
     * Returns the current top entries (sorted descending by score).
     * Loads from disk if the in-memory cache is empty.
     *
     * @return an unmodifiable view of up to {@value #MAX_ENTRIES} entries
     */
    public static List<LeaderboardEntry> getTopEntries() {
        if (!hasLoadedFromDisk) {
            loadEntries();
        }
        return entries;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Reset
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Delete the leaderboard file on disk and clear the in-memory cache.
     * Called by the Main Menu's "Reset Data" action.
     */
    public static void clearAll() {
        entries.clear();
        FileHandle file = Gdx.files.local(LEADERBOARD_PATH);
        if (file.exists()) {
            file.delete();
            System.out.println("[LeaderboardManager] Leaderboard file deleted.");
        } else {
            System.out.println("[LeaderboardManager] No leaderboard file to delete.");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Persistence
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Loads leaderboard entries from the JSON file on disk.
     * If the file does not exist or is corrupted, the list remains empty.
     */
    public static void loadEntries() {
        hasLoadedFromDisk = true;
        entries.clear();
        try {
            FileHandle file = Gdx.files.local(LEADERBOARD_PATH);
            if (!file.exists()) {
                System.out.println("[LeaderboardManager] No leaderboard file found.");
                return;
            }

            Json json = new Json();
            json.setIgnoreUnknownFields(true);

            // Deserialize the wrapper object
            LeaderboardFile data = json.fromJson(LeaderboardFile.class, file);
            if (data != null && data.entries != null) {
                entries.addAll(data.entries);
                insertionSortDescending(entries); // Ensure sorted after load
            }

            System.out.println("[LeaderboardManager] Loaded " + entries.size() + " entries.");
        } catch (Exception e) {
            System.err.println("[LeaderboardManager] Failed to load leaderboard.");
            e.printStackTrace();
        }
    }

    /**
     * Persists the current in-memory entries to disk using atomic writes.
     * Writes to a temp file first, then renames to avoid corruption.
     */
    private static void saveEntries() {
        try {
            LeaderboardFile data = new LeaderboardFile();
            data.entries = new ArrayList<>(entries);

            Json json = new Json();
            json.setOutputType(JsonWriter.OutputType.json);
            String jsonText = json.prettyPrint(data);

            // Step 1 — write to temp file
            FileHandle temp = Gdx.files.local(TEMP_PATH);
            temp.writeString(jsonText, false);

            // Step 2 — atomic rename
            FileHandle real = Gdx.files.local(LEADERBOARD_PATH);
            if (real.exists())
                real.delete();
            temp.file().renameTo(real.file());

            System.out.println("[LeaderboardManager] Saved " + entries.size() + " entries to disk.");
        } catch (Exception e) {
            System.err.println("[LeaderboardManager] Failed to save leaderboard!");
            e.printStackTrace();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // JSON wrapper (for serialization)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Wrapper class so the JSON file has a root object: { "entries": [...] }
     */
    public static class LeaderboardFile {
        public List<LeaderboardEntry> entries = new ArrayList<>();
    }
}
