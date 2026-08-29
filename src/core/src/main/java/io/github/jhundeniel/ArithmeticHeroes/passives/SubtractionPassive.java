package io.github.jhundeniel.ArithmeticHeroes.passives;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.components.PassiveComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.config.GameConfig;

public class SubtractionPassive implements Passive{
    private final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);

    @Override
    public int onDealDamage(Entity attacker, Entity target, int baseDamage) {
        StatsComponent uStats = sm.get(attacker);
        if (uStats == null) return 0;

        float hpPercent = (float) uStats.hp / uStats.maxHp;

        // BERSERKER THRESHOLD: Only activates if he is below threshold HP
        if (hpPercent < GameConfig.BERSERKER_HP_THRESHOLD) {
            float missingPercent = 1.0f - hpPercent;

            // He gets up to a 50% damage boost at exactly 0 HP.
            // At 25% HP, he gets a ~37% damage boost.
            int bonusDamage = (int) (baseDamage * (missingPercent * GameConfig.BERSERKER_BONUS_MULTIPLIER));

            if (bonusDamage > 0) {
                System.out.println("!!PASSIVE (Less is More): Subtraction deals " + bonusDamage + " bonus damage!");
                return bonusDamage;
            }
        }

        return 0; // No bonus if he is healthy
    }
}
