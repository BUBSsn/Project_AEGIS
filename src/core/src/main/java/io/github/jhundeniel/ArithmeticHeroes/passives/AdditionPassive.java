package io.github.jhundeniel.ArithmeticHeroes.passives;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.config.GameConfig;

public class AdditionPassive implements Passive {
    private final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);

    @Override
    public void onHeal(Entity user, Entity target, int amount) {
        // FIX: guard against null target (AOE heals pass each ally individually now,
        // but this safety check prevents any future crash)
        if (target == null || !sm.has(target)) return;

        StatsComponent tStats = sm.get(target);
        StatsComponent uStats = sm.get(user);

        // If target's resulting HP is a multiple of 10, restore 3 mana
        if (tStats.hp % GameConfig.ADD_PASSIVE_DIVISOR == 0) {
            uStats.mana = Math.min(uStats.maxMana, uStats.mana + GameConfig.ADD_PASSIVE_MANA_REWARD);
            System.out.println("😇!!PASSIVE (Addition): Perfect 10! Restored " + GameConfig.ADD_PASSIVE_MANA_REWARD + " Mana");
        }
    }
}
