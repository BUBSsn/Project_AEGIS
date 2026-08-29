package io.github.jhundeniel.ArithmeticHeroes.config;

/**
 * Central registry of all tunable gameplay constants.
 *
 * Every magic number that affects balance, gameplay feel, or combat
 * mechanics lives here. Change a value once and it propagates everywhere.
 *
 * UI layout constants (panel heights, button sizes, colors) are NOT
 * included here — they belong in their respective UI classes.
 */
public final class GameConfig {

    private GameConfig() {
    } // utility class

    // ═══════════════════════════════════════════════════════════════════
    // PASSIVE: SUBTRACTION — "Less is More" (Berserker)
    // ═══════════════════════════════════════════════════════════════════

    /** HP threshold below which the berserker passive activates (50%). */
    public static final float BERSERKER_HP_THRESHOLD = 0.50f;

    /** Max damage bonus multiplier at 0 HP (scales linearly with missing HP). */
    public static final float BERSERKER_BONUS_MULTIPLIER = 0.50f;

    // ═══════════════════════════════════════════════════════════════════
    // PASSIVE: MULTIPLICATION — "Mana Siphon"
    // ═══════════════════════════════════════════════════════════════════

    /** Percentage of highest ally HP restored as mana on pass. */
    public static final float MULT_PASSIVE_MANA_PERCENT = 0.10f;

    /** Buff multiplier granted when mana is already capped. */
    public static final double MULT_PASSIVE_CAPPED_BUFF = 1.05;

    // ═══════════════════════════════════════════════════════════════════
    // PASSIVE: ADDITION — "Perfect 10"
    // ═══════════════════════════════════════════════════════════════════

    /** HP divisor for the "Perfect 10" check (hp % N == 0). */
    public static final int ADD_PASSIVE_DIVISOR = 10;

    /** Mana restored when the Perfect 10 condition triggers. */
    public static final int ADD_PASSIVE_MANA_REWARD = 3;

    // ═══════════════════════════════════════════════════════════════════
    // PASSIVE: DIVISION — "Division of Labor"
    // ═══════════════════════════════════════════════════════════════════

    /** Fraction of incoming damage converted to mana (damage / N). */
    public static final int DIV_PASSIVE_DAMAGE_DIVISOR = 3;

    // ═══════════════════════════════════════════════════════════════════
    // COMBAT: ECHO CAST / SQUARED POWER
    // ═══════════════════════════════════════════════════════════════════

    /** Effectiveness multiplier for the second (echo) cast. */
    public static final float ECHO_CAST_EFFECTIVENESS = 0.5f;

    /** Delay (seconds) before the echo animation plays. */
    public static final float ECHO_ANIM_DELAY = 0.8f;

    // ═══════════════════════════════════════════════════════════════════
    // COMBAT: DAMAGE VISUALS
    // ═══════════════════════════════════════════════════════════════════

    /** Damage threshold above which "big" hit visuals play. */
    public static final int BIG_HIT_THRESHOLD = 20;

    // ═══════════════════════════════════════════════════════════════════
    // BATTLE STATE TIMING
    // ═══════════════════════════════════════════════════════════════════

    /** Delay (seconds) between end-of-turn text events. */
    public static final float TEXT_DELAY = 1.5f;

    /** Duration (seconds) for the victory/defeat screen to remain. */
    public static final float RESULT_DISPLAY_TIME = 2.0f;
}
