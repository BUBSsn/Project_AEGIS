package io.github.jhundeniel.ArithmeticHeroes.passives.bosses;

import java.util.List;

import com.badlogic.ashley.core.Entity;

import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.passives.Passive;

public class InverseRealityPassive implements Passive {
    @Override
    public int onDealDamage(Entity attacker, Entity target, int damage) {
        return 0;
    }

    @Override
    public void onTakeDamage(Entity self, int damage) {
    }

    // ── GIMMICK LOGIC: Permanent Field Effect! ───────────────
    @Override
    public void onRoundStart(Entity self, List<Entity> heroes, List<Entity> enemies, int currentRound) {

        // We removed the modulo check! Now it applies every single round.
        // This ensures that even if a hero dies and is revived, or uses a "Cleanse"
        // skill,
        // the boss will instantly re-invert them at the start of the next round.
        for (Entity hero : heroes) {
            // Only apply if they aren't already inverted!
            if (StatusEffects.get(hero, StatusEffect.Type.INVERSION) == null) {
                StatusEffects.add(hero, StatusEffect.inversion());
            }
        }

        // We only show the dialogue on Round 1 so it doesn't spam the chat box
        if (currentRound == 1) {
            System.out.println(">> BOSS 2 EMITS A PERMANENT INVERSION AURA!");
            // actionLog.addMessage("The arena is permanently Inverted!");
        }
    }
}
