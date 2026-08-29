package io.github.jhundeniel.ArithmeticHeroes.passives;

import io.github.jhundeniel.ArithmeticHeroes.passives.bosses.FinalEquationPassive;
import io.github.jhundeniel.ArithmeticHeroes.passives.bosses.InverseRealityPassive;
import io.github.jhundeniel.ArithmeticHeroes.passives.bosses.ParityShieldPassive;

public class PassiveRegistry {
    // Translates the JSON string into the actual Java object
    public static Passive getPassive(String passiveKey) {
        if (passiveKey == null || passiveKey.isEmpty()) {
            return null; // No passive assigned in JSON
        }

        switch (passiveKey) {
            case "ADDITION_PASSIVE":
                return new AdditionPassive();
            case "SUBTRACTION_PASSIVE":
                return new SubtractionPassive();
            case "MULTIPLICATION_PASSIVE":
                return new MultiplicationPassive();
            case "DIVISION_PASSIVE":
                return new DivisionPassive();

            // ── BOSS PASSIVES ──────────────────────────────
            case "PASSIVE_PARITY_SHIELD": // Boss 1
                return new ParityShieldPassive();
            case "PASSIVE_INVERSE_REALITY": // Boss 2
                return new InverseRealityPassive();
            case "PASSIVE_FINAL_EQUATION": // Boss 3
                return new FinalEquationPassive();

            default:
                System.err.println(">> WARNING: Unknown passive key in JSON: " + passiveKey);
                return null;
        }
    }
}
