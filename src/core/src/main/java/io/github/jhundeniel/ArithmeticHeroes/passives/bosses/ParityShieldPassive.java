package io.github.jhundeniel.ArithmeticHeroes.passives.bosses;

import java.util.List;

import com.badlogic.ashley.core.Entity;

import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.passives.Passive;

public class ParityShieldPassive implements Passive {
    @Override
    public int onDealDamage(Entity attacker, Entity target, int damage) {
        return 0;
    }

    @Override
    public void onTakeDamage(Entity self, int damage) {
    }

    // ── GIMMICK LOGIC: Block damage based on the round! ───────────────
    @Override
    public int onDefend(Entity defender, Entity attacker, int incomingDamage, int currentRound, List<Entity> enemies) {
        boolean isEvenRound = (currentRound % 2 == 0);
        boolean isEvenDamage = (incomingDamage % 2 == 0);

        if (isEvenRound != isEvenDamage) {
            CombatMechanics.notifyBuff(defender, "BLOCKED!");
            System.out.println(">> PARITY SHIELD BLOCKED THE ATTACK!");
            return 0; // Return 0 to completely block the damage!
        }

        return incomingDamage; // Passes the parity check, take full damage
    }
}
