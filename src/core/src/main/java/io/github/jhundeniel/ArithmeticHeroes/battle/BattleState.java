package io.github.jhundeniel.ArithmeticHeroes.battle;

public enum BattleState {
    ROUND_START,      // Calculates turn order (shuffles if round > 1)
    WAIT_FOR_INPUT,   // Player's turn to pick a skill
    SELECT_TARGET,    // Player is picking a target (CAN USE 'BACK' BUTTON HERE)
    CHOOSE_VALUE,     // Player is choosing a numeric value (e.g., Additional Buff 3-5)
    ENEMY_TURN,       // AI is thinking
    ACTION_QUEUED,    // Skill and target are locked in. No more inputs allowed.
    ANIMATING,        // Playing animations AND waiting for Action Log text (The Pokémon delay)
    CHECK_WIN_LOSS,   // Did all enemies or all heroes die?
    TURN_END,         // Ticks down buffs (like Burden/Cost Reduction)
    GAME_OVER         // All heroes have been defeated — show the Game Over overlay
}
