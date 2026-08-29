package io.github.jhundeniel.ArithmeticHeroes.systems;

import io.github.jhundeniel.ArithmeticHeroes.battle.DamageTracker;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.DamageEventComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.Operator;
import io.github.jhundeniel.ArithmeticHeroes.components.PassiveComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.TurnManager;
import io.github.jhundeniel.ArithmeticHeroes.passives.Passive;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffectComponent;

public class DamageSystem extends IteratingSystem {
    private final ComponentMapper<DamageEventComponent> dem = ComponentMapper.getFor(DamageEventComponent.class);
    private final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<PassiveComponent> pm = ComponentMapper.getFor(PassiveComponent.class);

    private final ActionLogSystem actionLog;
    private final ArithmeticAssetManager assets;
    private final TurnManager turnManager;

    public DamageSystem(ActionLogSystem actionLog, ArithmeticAssetManager assets, TurnManager turnManager) {
        super(Family.all(DamageEventComponent.class, StatsComponent.class).get());
        this.actionLog = actionLog;
        this.assets = assets;
        this.turnManager = turnManager;
    }

    @Override
    protected void processEntity(Entity target, float deltaTime) {
        DamageEventComponent damageEvent = dem.get(target);
        StatsComponent targetStats = sm.get(target);

        int finalDamage = damageEvent.amount;

        // ATTACKER PASSIVES (Less is More, etc.) ---
        if (damageEvent.source != null && pm.has(damageEvent.source)) {
            Passive attackerPassive = pm.get(damageEvent.source).passive;

            if (attackerPassive != null) {
                finalDamage += attackerPassive.onDealDamage(damageEvent.source, target, finalDamage);
            }
        }

        // ── AUTO-CONSUME ONE-HIT BUFFS ───────────────────
        if (damageEvent.source != null) {
            StatusEffectComponent sec = StatusEffects.component(damageEvent.source);

            if (sec != null) {
                // Optional: Loop through quickly just to print to the console so you can see it
                // working
                for (StatusEffect effect : sec.effects) {
                    if (effect.consumeOnAction) {
                        System.out.println(">> Auto-consumed charge: " + effect.type.name());
                    }
                }

                // The Magic Line: Instantly deletes EVERY buff that is flagged to consume!
                sec.effects.removeIf(effect -> effect.consumeOnAction);
            }
        }
        // ────────────────────────────────────────────────────────────────

        // ── GATHER ENEMIES FOR PASSIVES ──
        java.util.List<Entity> enemiesList = new java.util.ArrayList<>();
        for (Entity e : turnManager.getAllEntities()) {
            TypeComponent t = e.getComponent(TypeComponent.class);
            if (t != null && t.type == Operator.MOB) {
                enemiesList.add(e);
            }
        }

        // DEFENDER PASSIVES (Parity Shield)
        if (pm.has(target)) {
            Passive defenderPassive = pm.get(target).passive;
            if (defenderPassive != null) {
                // Pass the new enemiesList in as the 5th argument!
                finalDamage = defenderPassive.onDefend(target, damageEvent.source, finalDamage,
                        turnManager.getCurrentRound(), enemiesList);
            }
        }

        // If a passive (like Parity) reduced the damage to 0, block the attack
        if (finalDamage <= 0) {
            actionLog.addMessage("ATTACK BLOCKED!");
            target.remove(DamageEventComponent.class); // Prevent infinite loop
            return; // Exit out of the damage process
        }

        // 2. Burden Interception
        StatusEffect burden = StatusEffects.get(target, StatusEffect.Type.BURDEN);
        if (burden != null && !damageEvent.isTrueDamage) {
            StatsComponent protectorStats = sm.get(burden.protector);
            if (protectorStats != null && protectorStats.hp > 0) {
                int sharedDamage = Math.round(finalDamage * burden.shareRatio);
                finalDamage -= sharedDamage;

                // Send TRUE DAMAGE to protector so they don't infinite loop
                burden.protector.add(new DamageEventComponent(sharedDamage, damageEvent.source, true));
                actionLog.addMessage(
                        "BURDEN: " + protectorStats.name.trim() + " intercepted " + sharedDamage + " damage!");

                // --- Remove the shield because they took damage! ---
                StatusEffects.remove(target, StatusEffect.Type.BURDEN);
            }
        }

        // 2.5 REFLECTION Interception
        StatusEffect reflect = StatusEffects.get(target, StatusEffect.Type.REFLECTION);
        if (reflect != null && !damageEvent.isTrueDamage && damageEvent.source != null) {
            int reflectedDamage = Math.round(finalDamage * reflect.reflectPercent);
            if (reflectedDamage > 0) {
                // Reflect damage back to attacker as TRUE DAMAGE (prevents infinite loop)
                damageEvent.source.add(new DamageEventComponent(reflectedDamage, target, true));
                StatsComponent attackerStats = sm.get(damageEvent.source);
                String attackerName = (attackerStats != null) ? attackerStats.name.trim() : "???";
                actionLog.addMessage("REFLECT: " + targetStats.name.trim()
                        + " reflected " + reflectedDamage + " damage back to " + attackerName + "!");
            }
            // Consume the reflection (one-hit)
            StatusEffects.remove(target, StatusEffect.Type.REFLECTION);
        }

        // 3. APPLY HP LOSS
        int oldHp = targetStats.hp;
        targetStats.hp -= finalDamage;
        if (targetStats.hp < 0)
            targetStats.hp = 0;
        int actualDamage = oldHp - targetStats.hp;

        if (damageEvent.source != null) {
    io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent srcType =
        damageEvent.source.getComponent(
            io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent.class);
    if (srcType != null && srcType.type !=
        io.github.jhundeniel.ArithmeticHeroes.components.Operator.MOB) {
        DamageTracker.recordDamage(damageEvent.source, actualDamage);
    }
}

        // 4. LOG RESULT
        if (finalDamage > 0) {
            actionLog.addMessage(">> HIT: " + targetStats.name.trim() + " took " + actualDamage + " damage! ("
                    + targetStats.hp + "/" + targetStats.maxHp + ")");
            io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent tc = target
                    .getComponent(io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent.class);
            if (tc != null && tc.type == io.github.jhundeniel.ArithmeticHeroes.components.Operator.MOB) {
                assets.playSound(io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager.SFX_ENEMY_HIT);
            } else {
                assets.playSound(io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager.SFX_HERO_HURT);
            }
        }

        // 5. TRIGGER VISUALS & PASSIVES
        CombatMechanics.playDamageVisuals(target, actualDamage);

        // 6. TODO: Apply Attacker Passives

        // 7. CLEAN UP
        target.remove(DamageEventComponent.class);
    }
}
