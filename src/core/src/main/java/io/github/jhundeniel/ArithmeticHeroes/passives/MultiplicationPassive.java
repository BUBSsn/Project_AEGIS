package io.github.jhundeniel.ArithmeticHeroes.passives;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.components.PassiveComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.config.GameConfig;

import java.util.List;

public class MultiplicationPassive implements  Passive{
    private final ComponentMapper<StatsComponent> sm =  ComponentMapper.getFor(StatsComponent.class);

    @Override
    public void onPass(Entity user, List<Entity> allies) {
        StatsComponent uStats = sm.get(user);
        if (uStats == null) return;

        int highestHp = 0;
        for (Entity ally : allies) {
            StatsComponent aStats = sm.get(ally);
            if (aStats != null && aStats.hp > highestHp) {
                highestHp = aStats.hp;
            }
        }

        int manaGain = (int) (highestHp * GameConfig.MULT_PASSIVE_MANA_PERCENT);

        if (uStats.mana >= uStats.maxMana) {
            //Capped! Add bonus (Multiplier) to next skill
            StatusEffects.add(user, StatusEffect.buff(GameConfig.MULT_PASSIVE_CAPPED_BUFF, true));
            System.out.println("!!PASSIVE (Multiplication): Mana capped! Gained 5% skill buff.");
        } else {
            uStats.mana = Math.min(uStats.maxMana, uStats.mana + manaGain);
            System.out.println("!!PASSIVE (Multiplication): Passed turn. Gained " + manaGain + " Mana.");
        }
    }
}
