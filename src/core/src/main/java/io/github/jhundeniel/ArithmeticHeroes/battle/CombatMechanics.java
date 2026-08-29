package io.github.jhundeniel.ArithmeticHeroes.battle;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.components.DamageEventComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.VisualComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.PassiveComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.config.GameConfig;

public class CombatMechanics {

    // ── Static animation reference — set once in BattleScreen ─────
    private static BattleAnimations animations = null;

    public static void setAnimations(BattleAnimations anim) {
        animations = anim;
    }

    // ── Component mappers ─────────────────────────────────────────
    private static final ComponentMapper<StatsComponent>   sm  = ComponentMapper.getFor(StatsComponent.class);
    private static final ComponentMapper<VisualComponent>  vm  = ComponentMapper.getFor(VisualComponent.class);
    private static final ComponentMapper<PassiveComponent> pm  = ComponentMapper.getFor(PassiveComponent.class);

    // ── Cost helpers ──────────────────────────────────────────────
    public static int getFinalManaCost(Entity user, int baseCost) {
        // 1. ECHO CAST: second cast is 100% free
        if (StatusEffects.has(user, StatusEffect.Type.ECHO_CAST)) {
            System.out.println(">> FREE CAST: Echo active! Mana cost is 0.");
            return 0;
        }

        // 2. COST REDUCTION: discount
        if (StatusEffects.has(user, StatusEffect.Type.COST_REDUCTION)) {
            int newCost = (int)(baseCost * StatusEffects.getCostReductionMultiplier(user));
            System.out.println(">> COST REDUCTION: Mana " + baseCost + " -> " + newCost);
            return newCost;
        }

        // 3. BASE COST: full price
        return baseCost;
    }

    public static int getFinalHpCost(Entity user, int baseCost) {
        // 1. ECHO CAST: free second cast
        if (StatusEffects.has(user, StatusEffect.Type.ECHO_CAST)) {
            System.out.println(">> FREE CAST: Echo active! HP cost is 0.");
            return 0;
        }

        // 2. COST REDUCTION: discount
        if (StatusEffects.has(user, StatusEffect.Type.COST_REDUCTION)) {
            int newCost = (int)(baseCost * StatusEffects.getCostReductionMultiplier(user));
            System.out.println(">> COST REDUCTION: HP " + baseCost + " -> " + newCost);
            return newCost;
        }

        // 3. BASE COST: full price
        return baseCost;
    }

    // ── Buff helpers ──────────────────────────────────────────────
    public static double BuffConsumption(Entity user) {
        StatusEffect buff = StatusEffects.get(user, StatusEffect.Type.BUFF);
        if (buff != null) {
            double multiplier = buff.multiplier;
            System.out.printf(">> BUFF CONSUMED! Multiplier: %.0f%%\n", multiplier * 100);
            if (buff.consumeOnAction && !StatusEffects.has(user, StatusEffect.Type.SQUARED)) {
                StatusEffects.remove(user, StatusEffect.Type.BUFF);
            }
            return multiplier;
        }
        return 1.0;
    }

    public static int AdditiveBonusConsumption(Entity user) {
        StatusEffect bonus = StatusEffects.get(user, StatusEffect.Type.ADDITIVE_BONUS);
        if (bonus != null) {
            int additiveBonus = bonus.additive;
            System.out.printf(">> ADDITIVE BUFF: VALUE: %d\n", additiveBonus);
            if (bonus.consumeOnAction) {
                StatusEffects.remove(user, StatusEffect.Type.ADDITIVE_BONUS);
            }
            return additiveBonus;
        }
        return 0;
    }

    public static boolean checkAndConsumeInversion(Entity user) {
        if (StatusEffects.has(user, StatusEffect.Type.INVERSION)) {
            StatusEffects.remove(user, StatusEffect.Type.INVERSION);
            System.out.println(">> INVERSION ACTIVE: Action effects flipped!");
            return true;
        }
        return false;
    }

    public static float getEffectiveness(Entity user) {
        return StatusEffects.has(user, StatusEffect.Type.ECHO_CAST) ? GameConfig.ECHO_CAST_EFFECTIVENESS : 1.0f;
    }

    public static boolean isFreeCast(Entity user) {
        return StatusEffects.has(user, StatusEffect.Type.ECHO_CAST);
    }

    // ── Heal ──────────────────────────────────────────────────────
    public static void applyHeal(Entity target, int baseAmount) {
        if (!sm.has(target)) return;

        StatsComponent tStats = sm.get(target);
        int oldHp = tStats.hp;
        tStats.hp = Math.min(tStats.maxHp, tStats.hp + baseAmount);
        int actualHeal = tStats.hp - oldHp;

        System.out.printf(">> HEAL: %s +%d (HP: %d/%d)\n",
            tStats.name.trim(), actualHeal, tStats.hp, tStats.maxHp);

        // ── Floating heal number ──────────────────────────────────
        if (animations != null && vm.has(target)) {
            VisualComponent v = vm.get(target);
            float cx = v.x + v.width  / 2f;
            float cy = v.y + v.height + 10f;
            animations.showHeal(actualHeal, cx, cy);
        }
    }

    // ── Damage (Event Driven) ─────────────────────────────────────
    public static void applyDamage(Entity attacker, Entity target, int damageAmount) {
        if (!sm.has(target)) return;

        ComponentMapper<DamageEventComponent> demMapper = ComponentMapper.getFor(DamageEventComponent.class);
        if (demMapper.has(target)) {
            DamageEventComponent existing = demMapper.get(target);
            existing.amount += damageAmount;
            System.out.println(">> DAMAGE STACKED: +" + damageAmount + " (total pending: " + existing.amount + ")");
        } else {
            target.add(new DamageEventComponent(damageAmount, attacker));
        }
    }

    // ── Play Visuals (Triggered by DamageSystem) ──────────────────
    public static void playDamageVisuals(Entity target, int actualDamage) {
        if (animations != null && vm.has(target)) {
            VisualComponent v = vm.get(target);
            float cx = v.x + v.width  / 2f;
            float cy = v.y + v.height + 10f;
            boolean isBig = actualDamage > GameConfig.BIG_HIT_THRESHOLD;
            animations.showDamage(actualDamage, cx, cy, isBig);
            animations.flashEntity(target);
        }

        // Trigger Passive after we know the final damage amount
        if (pm.has(target)) {
            pm.get(target).passive.onTakeDamage(target, actualDamage);
        }
    }

    // ── Buff/debuff notifications ─────────────────────────────────
    public static void notifyBuff(Entity target, String text) {
        if (animations == null || !vm.has(target)) return;
        VisualComponent v = vm.get(target);
        float cx = v.x + v.width  / 2f;
        float cy = v.y + v.height + 10f;
        animations.showBuff(text, cx, cy);
    }

    public static void notifyDebuff(Entity target, String text) {
        if (animations == null || !vm.has(target)) return;
        VisualComponent v = vm.get(target);
        float cx = v.x + v.width  / 2f;
        float cy = v.y + v.height + 10f;
        animations.showDebuff(text, cx, cy);
    }
}
