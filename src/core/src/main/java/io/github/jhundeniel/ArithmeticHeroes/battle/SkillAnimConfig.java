package io.github.jhundeniel.ArithmeticHeroes.battle;

/**
 * Central registry of sprite-sheet frame data for every skill animation.
 *
 * All PNGs are horizontal strips of square frames:
 *   frames     = sheet.width / sheet.height
 *   frameDur   = tuned so the full clip plays in ~0.7–1.0 s
 *
 * Sheet sizes (pixels):
 *   Heal.png              1408 × 128  → 11 frames
 *   Shield.png            2000 × 125  → 16 frames
 *   Amplify.png           2000 × 125  → 16 frames
 *   Battle_Equalizer.png  1280 × 128  → 10 frames
 *   Squared.png           1024 × 128  →  8 frames
 *   Inverse.png           1536 × 128  → 12 frames
 *   Slam.png              1152 × 128  →  9 frames
 *   Poke.png               896 × 128  →  7 frames
 *   Mana_Transfer.png     1024 × 128  →  8 frames
 *   Life_Steal.png        1536 × 128  → 12 frames
 *   Life_Steal_Heal.png   1280 × 128  → 10 frames
 *   Conditional.png       1152 × 128  →  9 frames
 *   Additional_Buff.png   1920 × 128  → 15 frames
 */
public final class SkillAnimConfig {
    private SkillAnimConfig() {}

    // ── frame counts ──────────────────────────────────────────────
    public static final int HEAL_FRAMES           = 11;
    public static final int SHIELD_FRAMES         = 16;
    public static final int AMPLIFY_FRAMES        = 16;
    public static final int BATTLE_EQ_FRAMES      = 10;
    public static final int SQUARED_FRAMES        =  8;
    public static final int INVERSE_FRAMES        = 12;
    public static final int SLAM_FRAMES           =  9;
    public static final int POKE_FRAMES           =  7;
    public static final int MANA_TRANSFER_FRAMES  =  8;

    // ── new hero skill frame counts ───────────────────────────────
    public static final int LIFE_STEAL_FRAMES      = 12;  // 1536 × 128
    public static final int LIFE_STEAL_HEAL_FRAMES = 10;  // 1280 × 128
    public static final int CONDITIONAL_FRAMES     =  9;  // 1152 × 128
    public static final int ADDITIONAL_BUFF_FRAMES = 15;  // 1920 × 128

    // ── frame durations (seconds) ─────────────────────────────────
    public static final float HEAL_DUR           = 0.08f;  // 11 × 0.08 = 0.88 s
    public static final float SHIELD_DUR         = 0.06f;  // 16 × 0.06 = 0.96 s
    public static final float AMPLIFY_DUR        = 0.06f;  // 16 × 0.06 = 0.96 s
    public static final float BATTLE_EQ_DUR      = 0.09f;  // 10 × 0.09 = 0.90 s
    public static final float SQUARED_DUR        = 0.10f;  //  8 × 0.10 = 0.80 s
    public static final float INVERSE_DUR        = 0.08f;  // 12 × 0.08 = 0.96 s
    public static final float SLAM_DUR           = 0.09f;  //  9 × 0.09 = 0.81 s
    public static final float POKE_DUR           = 0.10f;  //  7 × 0.10 = 0.70 s
    public static final float MANA_TRANSFER_DUR  = 0.10f;  //  8 × 0.10 = 0.80 s

    // ── new hero skill frame durations ────────────────────────────
    public static final float LIFE_STEAL_DUR      = 0.07f;  // 12 × 0.07 = 0.84 s
    public static final float LIFE_STEAL_HEAL_DUR = 0.08f;  // 10 × 0.08 = 0.80 s
    public static final float CONDITIONAL_DUR     = 0.09f;  //  9 × 0.09 = 0.81 s
    public static final float ADDITIONAL_BUFF_DUR = 0.06f;  // 15 × 0.06 = 0.90 s

    // ── projectile travel time (Mana Transfer) ────────────────────
    public static final float MANA_TRANSFER_TRAVEL = 0.45f;
}
