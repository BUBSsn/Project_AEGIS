package io.github.jhundeniel.ArithmeticHeroes.skills.multiplication;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.PartyComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class InversionSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ComponentMapper<PartyComponent> pm = ComponentMapper.getFor(PartyComponent.class);

    private final ActionLogSystem actionLog;
    private final BattleAnimations animations;
    private final Texture inverseSheet;

    public InversionSkill(ActionLogSystem actionLog,
            BattleAnimations animations, Texture inverseSheet) {
        this.actionLog = actionLog;
        this.animations = animations;
        this.inverseSheet = inverseSheet;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);
        StatsComponent tStats = sm.get(target);

        // 1. CHECKS IF USER HAS COMPONENT
        if (!skm.has(user)) {
            actionLog.addMessage("Error: No Component Detected");
            return;
        }

        // 2. GET SPECIFIC DATA
        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.INVERSION);

        if (data == null) {
            actionLog.addMessage("Error: User missing Inversion data!");
            return;
        }

        // 3. MANA CHECK
        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);

        if (uStats.mana >= finalCost) {
            uStats.mana -= finalCost;

            // ── THE INVERSION TOGGLE LOGIC ──────────────────────────────────
            if (StatusEffects.has(target, StatusEffect.Type.INVERSION)) {
                // They are inverted -> Cleanse!
                StatusEffects.remove(target, StatusEffect.Type.INVERSION);
                CombatMechanics.notifyBuff(target, "CLEANSED!");

                String msg = String.format("%s cleansed %s of Inversion!\n",
                        uStats.name.trim(), tStats.name.trim());
                actionLog.addMessage(msg);

            } else {
                // They are normal -> Invert!
                StatusEffects.add(target, StatusEffect.inversion());
                CombatMechanics.notifyBuff(target, "INVERTED!");

                String msg = String.format("%s used Inversion on %s! Their next effect will be flipped.\n",
                        uStats.name.trim(), tStats.name.trim());
                actionLog.addMessage(msg);
            }
            // ────────────────────────────────────────────────────────────────

            // ── Play Inversion animation on the target ──────────
            if (animations != null && inverseSheet != null)
                animations.playSkillAnim(inverseSheet, 12, 0.08f, target);

        } else {
            actionLog.addMessage("Not enough Mana!");
        }
    }
}
