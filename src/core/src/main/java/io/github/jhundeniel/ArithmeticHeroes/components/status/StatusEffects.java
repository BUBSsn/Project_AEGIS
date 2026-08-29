package io.github.jhundeniel.ArithmeticHeroes.components.status;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;

/**
 * Static utility class for reading and writing status effects on entities.
 *
 * Every call site that used to do:
 *   {@code entity.getComponent(BuffComponent.class)}
 * now does:
 *   {@code StatusEffects.get(entity, Type.BUFF)}
 *
 * This keeps the migration diff minimal and provides a single,
 * auditable access point for all status effect operations.
 */
public final class StatusEffects {

    private static final ComponentMapper<StatusEffectComponent> MAPPER =
        ComponentMapper.getFor(StatusEffectComponent.class);

    private StatusEffects() {} // utility class

    // ── Core accessors ───────────────────────────────────────────────

    /** Get the StatusEffectComponent, or null if the entity doesn't have one. */
    public static StatusEffectComponent component(Entity entity) {
        return MAPPER.get(entity);
    }

    /** Check if the entity has an active effect of the given type. */
    public static boolean has(Entity entity, StatusEffect.Type type) {
        StatusEffectComponent sec = MAPPER.get(entity);
        return sec != null && sec.has(type);
    }

    /** Get the first effect of the given type, or null. */
    public static StatusEffect get(Entity entity, StatusEffect.Type type) {
        StatusEffectComponent sec = MAPPER.get(entity);
        return (sec != null) ? sec.get(type) : null;
    }

    /** Add an effect (replaces existing of same type). Creates component if missing. */
    public static void add(Entity entity, StatusEffect effect) {
        StatusEffectComponent sec = MAPPER.get(entity);
        if (sec == null) {
            sec = new StatusEffectComponent();
            entity.add(sec);
        }
        sec.add(effect);
    }

    /** Remove all effects of the given type. */
    public static void remove(Entity entity, StatusEffect.Type type) {
        StatusEffectComponent sec = MAPPER.get(entity);
        if (sec != null) {
            sec.remove(type);
        }
    }

    // ── Convenience getters (minimize diff at call sites) ────────────

    /** Returns the buff multiplier, or 1.0 if no buff is active. */
    public static double getBuffMultiplier(Entity entity) {
        StatusEffect buff = get(entity, StatusEffect.Type.BUFF);
        return (buff != null) ? buff.multiplier : 1.0;
    }

    /** Returns the additive bonus, or 0 if none is active. */
    public static int getAdditiveBonus(Entity entity) {
        StatusEffect bonus = get(entity, StatusEffect.Type.ADDITIVE_BONUS);
        return (bonus != null) ? bonus.additive : 0;
    }

    /** Returns the cost reduction multiplier, or 1.0 if none is active. */
    public static double getCostReductionMultiplier(Entity entity) {
        StatusEffect cr = get(entity, StatusEffect.Type.COST_REDUCTION);
        return (cr != null) ? cr.multiplier : 1.0;
    }

    /** Returns the reflect percent, or 0 if not active. */
    public static float getReflectPercent(Entity entity) {
        StatusEffect ref = get(entity, StatusEffect.Type.REFLECTION);
        return (ref != null) ? ref.reflectPercent : 0f;
    }

    /** Returns the squared multiplier, or 1.0 if not active. */
    public static double getSquaredMultiplier(Entity entity) {
        StatusEffect sq = get(entity, StatusEffect.Type.SQUARED);
        return (sq != null) ? sq.multiplier : 1.0;
    }

    /** Returns effectiveness multiplier: 0.5 if echo cast, else 1.0. */
    public static float getEffectiveness(Entity entity) {
        return has(entity, StatusEffect.Type.ECHO_CAST) ? 0.5f : 1.0f;
    }
}
