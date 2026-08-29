package io.github.jhundeniel.ArithmeticHeroes.battle;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.components.PassiveComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.config.GameConfig;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.passives.SubtractionPassive;

/**
 * Predicts the final skill value (damage or heal) for the Formula Bar UI.
 *
 * The calculation mirrors the real pipeline:
 *   Skill:        finalValue = (base * buffMult * echoPenalty) + flatBonus
 *   DamageSystem: finalValue += berserkerPassiveBonus (SubtractionPassive)
 */
public class PreviewCalculator {

    private static final ComponentMapper<StatsComponent>   sm = ComponentMapper.getFor(StatsComponent.class);
    private static final ComponentMapper<PassiveComponent> pm = ComponentMapper.getFor(PassiveComponent.class);

    /**
     * Calculates the predicted final value for a skill.
     * Returns 0 for skills without a numeric base (buffs, utility, etc.).
     */
    public static int calculateExpected(Entity user, SkillData skill) {
        // Safety: skip percentage-based or zero-value skills (Burden 0.50, etc.)
        if (skill == null || skill.value < 1.0f) {
            return 0;
        }

        // 1. Base value from JSON
        float base = skill.value;

        // 2. Multiplicative buff (Amplify)
        double buffMultiplier = StatusEffects.getBuffMultiplier(user);

        // 3. Echo/Squared effectiveness penalty (half power on second cast)
        float effectivenessMultiplier = StatusEffects.getEffectiveness(user);

        // 4. Flat additive bonus (Additional Buff)
        int flatBonus = StatusEffects.getAdditiveBonus(user);

        // 5. Combine: same formula as the actual skill code
        int expected = (int) ((base * buffMultiplier * effectivenessMultiplier) + flatBonus);

        // 6. Subtraction berserker passive (only applies to DAMAGE skills,
        //    not to inverted heals like Blood Transfer or Sacrifice)
        if (pm.has(user) && skill.type != null && isDamageSkill(skill.type)) {
            PassiveComponent pc = pm.get(user);
            if (pc.passive instanceof SubtractionPassive) {
                StatsComponent stats = sm.get(user);
                if (stats != null) {
                    float hpPercent = (float) stats.hp / stats.maxHp;
                    if (hpPercent < GameConfig.BERSERKER_HP_THRESHOLD) {
                        float missingPercent = 1.0f - hpPercent;
                        int bonusDamage = (int) (expected * (missingPercent * GameConfig.BERSERKER_BONUS_MULTIPLIER));
                        expected += bonusDamage;
                    }
                }
            }
        }

        return expected;
    }

    /**
     * Returns the base predicted value WITHOUT passive bonuses (berserker).
     * Used for Life Steal / Debt Transfer heal predictions, since the
     * actual skill computes siphonedHP from the pre-berserker damage.
     */
    public static int calculateBase(Entity user, SkillData skill) {
        if (skill == null || skill.value < 1.0f) return 0;

        float base = skill.value;
        double buffMultiplier = StatusEffects.getBuffMultiplier(user);
        float effectivenessMultiplier = StatusEffects.getEffectiveness(user);
        int flatBonus = StatusEffects.getAdditiveBonus(user);

        return (int) ((base * buffMultiplier * effectivenessMultiplier) + flatBonus);
    }

    /**
     * Returns true for skill types that deal damage (Berserker bonus applies).
     * Inverted heal skills (Blood Transfer, Sacrifice) are NOT damage skills.
     */
    private static boolean isDamageSkill(
            io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent.ActionType type) {
        switch (type) {
            case POKE:
            case SLAM:
            case CONDITIONAL_ATTACK:
            case LIFESTEAL_ATTACK:
            case MANA_NUKE:
            case DEBT_TRANSFER:
            case SINGLE_DRAIN:
            case LIFE_SIPHON:
                return true;
            default:
                return false;
        }
    }
}
