package io.github.jhundeniel.ArithmeticHeroes.battle;

import com.badlogic.ashley.core.Entity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DamageTracker {

    private static final Map<Entity, Integer> damageDealt = new HashMap<>();

    public static void recordDamage(Entity hero, int amount) {
        damageDealt.put(hero, damageDealt.getOrDefault(hero, 0) + amount);
    }

    public static Entity getHighestThreat(List<Entity> heroes) {
        Entity threat = null;
        int highest = 0;
        for (Entity hero : heroes) {
            int dmg = damageDealt.getOrDefault(hero, 0);
            if (dmg > highest) {
                highest = dmg;
                threat = hero;
            }
        }
        return threat;
    }

    public static void reset() {
        damageDealt.clear();
    }
}