package io.github.jhundeniel.ArithmeticHeroes.passives.bosses;

import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.Operator;
import io.github.jhundeniel.ArithmeticHeroes.components.DamageEventComponent;
import io.github.jhundeniel.ArithmeticHeroes.passives.Passive;
import java.util.List;

public class FinalEquationPassive implements Passive {

    // ── PHASE 1: INVULNERABILITY ──
    @Override
    public int onDefend(Entity defender, Entity attacker, int incomingDamage, int currentRound, List<Entity> enemies) {
        boolean minionsAlive = false;

        // Check if any mob that ISN'T Boss 3 is currently alive
        for (Entity enemy : enemies) {
            TypeComponent type = enemy.getComponent(TypeComponent.class);
            StatsComponent stats = enemy.getComponent(StatsComponent.class);
            if (type != null && type.type == Operator.MOB && !"ENEMY_BOSS3".equals(type.registryKey) && stats.hp > 0) {
                minionsAlive = true;
                break;
            }
        }

        if (minionsAlive) {
            System.out.println(">> BOSS 3 SHIELDED BY MINIONS!");
            io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics.notifyBuff(defender, "SHIELDED!");
            return 0; // Completely block the damage!
        }

        return incomingDamage; // Minions are dead, take damage normally
    }

    // ── PHASE 2 & 3: ROUND START MECHANICS ──
    @Override
    public void onRoundStart(Entity self, List<Entity> heroes, List<Entity> enemies, int currentRound) {
        Entity twin = null;

        // Find the other twin on the battlefield
        for (Entity enemy : enemies) {
            TypeComponent type = enemy.getComponent(TypeComponent.class);
            StatsComponent stats = enemy.getComponent(StatsComponent.class);
            if (enemy != self && type != null && "ENEMY_BOSS3".equals(type.registryKey) && stats != null
                    && stats.hp > 0) {
                twin = enemy;
                break;
            }
        }

        // ── PHASE 2: INVERSE REALITY (Every 5 rounds) ──
        // Tie-Breaker: Since there are 2 bosses, we only want ONE of them to cast this.
        // We let the one with the higher hashCode() be the "Leader" for this cast.
        boolean isLeadTwin = twin == null || self.hashCode() >= twin.hashCode();

        if (isLeadTwin && currentRound > 0 && currentRound % 5 == 0) {

            // 1. Gather all heroes that are still alive
            java.util.List<Entity> aliveHeroes = new java.util.ArrayList<>();
            for (Entity h : heroes) {
                StatsComponent s = h.getComponent(StatsComponent.class);
                if (s != null && s.hp > 0) {
                    aliveHeroes.add(h);
                }
            }

            // 2. Pick a random hero and apply Inversion!
            if (!aliveHeroes.isEmpty()) {
                Entity unluckyHero = aliveHeroes.get(com.badlogic.gdx.math.MathUtils.random(aliveHeroes.size() - 1));

                io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects.add(
                        unluckyHero,
                        io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect.inversion());

                String heroName = unluckyHero.getComponent(StatsComponent.class).name.trim();
                System.out.println(">> Final Equation warped reality! " + heroName + " received Inverse Reality!");
                io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics.notifyBuff(unluckyHero, "INVERTED!");
            }
        }

        // ── PHASE 3: THE WIPE ATTACK ──
        // If the twin exists, check the balance!
        if (twin != null) {
            int myHp = self.getComponent(StatsComponent.class).hp;
            int twinHp = twin.getComponent(StatsComponent.class).hp;

            if (Math.abs(myHp - twinHp) > 20) {
                System.out.println(">> TWINS ARE UNBALANCED! WIPE ATTACK TRIGGERED!");

                // Deal 9999 TRUE DAMAGE to all heroes!
                for (Entity hero : heroes) {
                    StatsComponent s = hero.getComponent(StatsComponent.class);
                    if (s != null && s.hp > 0) {
                        hero.add(new DamageEventComponent(9999, self, true));
                    }
                }
            }
        }
    }
}
