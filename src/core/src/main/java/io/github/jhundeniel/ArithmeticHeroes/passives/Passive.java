package io.github.jhundeniel.ArithmeticHeroes.passives;

import com.badlogic.ashley.core.Entity;

import java.util.List;

public interface Passive {
    default void onHeal(Entity user, Entity target, int amount) {
    }

    default int onDealDamage(Entity attacker, Entity target, int baseDamage) {
        return 0;
    }

    default void onTakeDamage(Entity user, int amount) {
    }

    default int onDefend(Entity defender, Entity attacker, int incomingDamage, int currentRound, List<Entity> enemies) {
        return incomingDamage;
    }

    default void onPass(Entity user, List<Entity> allies) {
    }

    default void onTurnStart(Entity user) {
    }

    default void onRoundStart(Entity self, List<Entity> heroes, List<Entity> enemies, int currentRound) {
    }
}
