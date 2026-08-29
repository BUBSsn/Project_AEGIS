package io.github.jhundeniel.ArithmeticHeroes.components.status;

import com.badlogic.ashley.core.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Ashley Component that holds all active status effects for an entity.
 *
 * Replaces the 8 individual status component classes. Every combatant
 * entity gets one of these at spawn time; the list starts empty.
 */
public class StatusEffectComponent implements Component {

    public final List<StatusEffect> effects = new ArrayList<>();

    /** Check whether this entity has an effect of the given type. */
    public boolean has(StatusEffect.Type type) {
        for (StatusEffect effect : effects) {
            if (effect.type == type) return true;
        }
        return false;
    }

    /** Get the first effect of the given type, or null. */
    public StatusEffect get(StatusEffect.Type type) {
        for (StatusEffect effect : effects) {
            if (effect.type == type) return effect;
        }
        return null;
    }

    /** Add an effect. Replaces any existing effect of the same type. */
    public void add(StatusEffect effect) {
        remove(effect.type);
        effects.add(effect);
    }

    /** Remove all effects of the given type. Returns true if any were removed. */
    public boolean remove(StatusEffect.Type type) {
        return effects.removeIf(e -> e.type == type);
    }

    /** Remove all effects. */
    public void clearAll() {
        effects.clear();
    }
}
