package io.github.jhundeniel.ArithmeticHeroes.data;

/**
 * Plain data object representing a single leaderboard entry.
 * Serialized/deserialized by LibGDX's Json class.
 *
 * <p>Fields are public for direct LibGDX Json serialization.</p>
 */
public class LeaderboardEntry {

    /** The player's chosen name. */
    public String playerName;

    /** Composite score calculated by LeaderboardManager. */
    public int score;

    /** Number of stages the player cleared. */
    public int stagesCleared;

    /** Number of heroes still alive at the end of the run. */
    public int heroesAlive;

    /** Epoch millis when the entry was recorded. */
    public long timestamp;

    /** No-arg constructor required by LibGDX Json deserializer. */
    public LeaderboardEntry() {}

    /**
     * Full constructor for creating a new entry after a completed run.
     *
     * @param playerName   the name the player entered
     * @param score        composite score from the ranking formula
     * @param stagesCleared number of stages beaten
     * @param heroesAlive  surviving heroes at victory
     */
    public LeaderboardEntry(String playerName, int score, int stagesCleared, int heroesAlive) {
        this.playerName   = playerName;
        this.score        = score;
        this.stagesCleared = stagesCleared;
        this.heroesAlive  = heroesAlive;
        this.timestamp    = System.currentTimeMillis();
    }
}
