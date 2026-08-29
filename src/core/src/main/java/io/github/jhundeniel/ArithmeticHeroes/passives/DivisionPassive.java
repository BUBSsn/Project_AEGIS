package io.github.jhundeniel.ArithmeticHeroes.passives;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.config.GameConfig;

public class DivisionPassive implements Passive{
    private final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);

    @Override
    public void onTakeDamage(Entity victim, int damage) {
        StatsComponent uStats = sm.get(victim);

        int manaGain = damage / GameConfig.DIV_PASSIVE_DAMAGE_DIVISOR;

        if (manaGain > 0) {
            uStats.mana = Math.min(uStats.maxMana, uStats.mana + manaGain);
            System.out.printf("🦾!!PASSIVE (Division of Labor): Took %d dmg -> Gained %d Mana (%d/%d)\n", damage, manaGain, uStats.mana, uStats.maxMana);
        }

    }
}
