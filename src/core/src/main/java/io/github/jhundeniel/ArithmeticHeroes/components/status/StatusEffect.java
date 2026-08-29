package io.github.jhundeniel.ArithmeticHeroes.components.status;

import com.badlogic.ashley.core.Entity;

/**
 * A single status effect instance. Pure data — no behavior.
 *
 * Replaces the 8 individual status component classes with one flexible
 * data structure that can represent any effect type.
 */
public class StatusEffect {

    /** Every kind of status effect in the game. */
    public enum Type {
        BUFF, // Multiplicative damage/heal modifier (Amplify)
        ADDITIVE_BONUS, // Flat bonus added after multipliers (Addition Buff)
        BURDEN, // Damage-sharing link to a protector
        COST_REDUCTION, // Reduced mana/HP cost for N turns
        INVERSION, // Flag: skills are sign-flipped
        ECHO_CAST, // Flag: second cast at half power (Squared Power)
        SQUARED, // Marks that next action casts twice at half power
        REFLECTION // Reflects % of incoming damage back to attacker
    }

    public final Type type;

    // ── Shared numeric fields (used by different types) ──────────────

    /** Multiplicative modifier. Used by BUFF, COST_REDUCTION, SQUARED. */
    public double multiplier = 1.0;

    /** Flat additive value. Used by ADDITIVE_BONUS. */
    public int additive = 0;

    /** Reflect percentage (0.0–1.0). Used by REFLECTION. */
    public float reflectPercent = 0f;

    /** Protector entity for BURDEN. */
    public Entity protector = null;

    /** Damage share ratio for BURDEN (0.0–1.0). */
    public float shareRatio = 0f;

    /**
     * Remaining turns before auto-expiry. -1 = until consumed. Used by
     * COST_REDUCTION.
     */
    public int turnsRemaining = -1;

    /**
     * If true, this effect is consumed after the next skill use. Used by BUFF,
     * ADDITIVE_BONUS.
     */
    public boolean consumeOnAction = true;

    // ── Factory methods for readable construction ────────────────────

    /** Amplify / Enemy Buff: multiplicative modifier. */
    public static StatusEffect buff(double multiplier) {
        StatusEffect effect = new StatusEffect(Type.BUFF);
        effect.multiplier = multiplier;
        effect.consumeOnAction = true;
        return effect;
    }

    /** Amplify (persistent): multiplicative modifier that stays. */
    public static StatusEffect buff(double multiplier, boolean consumeOnAction) {
        StatusEffect effect = new StatusEffect(Type.BUFF);
        effect.multiplier = multiplier;
        effect.consumeOnAction = consumeOnAction;
        return effect;
    }

    /** Addition Buff: flat bonus consumed on next action. */
    public static StatusEffect additiveBonus(int additive) {
        StatusEffect effect = new StatusEffect(Type.ADDITIVE_BONUS);
        effect.additive = additive;
        effect.consumeOnAction = true;
        return effect;
    }

    /** Burden: damage-sharing link. */
    public static StatusEffect burden(Entity protector, float shareRatio) {
        StatusEffect effect = new StatusEffect(Type.BURDEN);
        effect.protector = protector;
        effect.shareRatio = shareRatio;
        return effect;
    }

    /** Cost Reduction: halve costs for N turns. */
    public static StatusEffect costReduction(float multiplier, int turns) {
        StatusEffect effect = new StatusEffect(Type.COST_REDUCTION);
        effect.multiplier = multiplier;
        effect.turnsRemaining = turns;
        return effect;
    }

    /** Inversion: flag effect, no extra data. */
    public static StatusEffect inversion() {
        return new StatusEffect(Type.INVERSION);
    }

    /** Echo Cast: flag for second cast at half power. */
    public static StatusEffect echoCast() {
        return new StatusEffect(Type.ECHO_CAST);
    }

    /** Squared Power: next action casts twice. */
    public static StatusEffect squared(float multiplier) {
        StatusEffect effect = new StatusEffect(Type.SQUARED);
        effect.multiplier = multiplier;

        // ── ADD THIS LINE! ──
        effect.consumeOnAction = true;

        return effect;
    }

    /** Reflection: reflects N% of incoming damage. */
    public static StatusEffect reflection(float reflectPercent) {
        StatusEffect effect = new StatusEffect(Type.REFLECTION);
        effect.reflectPercent = reflectPercent;
        return effect;
    }

    // ── Constructor (prefer factory methods above) ───────────────────

    public StatusEffect(Type type) {
        this.type = type;
    }
}
