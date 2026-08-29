package io.github.jhundeniel.ArithmeticHeroes.skills.addition;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class ManaTransferSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>         sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent>        skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ComponentMapper<ActionRequestComponent> am  = ComponentMapper.getFor(ActionRequestComponent.class);

    private final ActionLogSystem  actionLog;
    private final BattleAnimations animations;
    private final Texture          manaSheet;

    public ManaTransferSkill(ActionLogSystem actionLog, BattleAnimations animations, Texture manaSheet) {
        this.actionLog  = actionLog;
        this.animations = animations;
        this.manaSheet  = manaSheet;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);
        StatsComponent tStats = sm.get(target);
        Entity sourceEntity   = am.get(user).secondaryTarget;

        // 1. CHECK COMPONENT
        if (!skm.has(user)) {
            actionLog.addMessage("Error: No Component Detected");
            return;
        }

        // 2. VALIDATION
        if (sourceEntity == null) {
            actionLog.addMessage("Error: Mana Transfer requires a source (Secondary Target)!");
            return;
        }
        if (sourceEntity == target) {
            actionLog.addMessage("Invalid: Cannot transfer mana from a target to themselves!");
            return;
        }

        // 3. GET DATA
        StatsComponent  sourceStats = sm.get(sourceEntity);
        SkillsComponent skills      = skm.get(user);
        SkillData       data        = skills.get(ActionRequestComponent.ActionType.MANA_TRANSFER);

        // 4. MANA CHECKS
        if (tStats.mana >= tStats.maxMana) {
            actionLog.addMessage("Invalid: Target already has max mana!");
            return;
        }
        if (sourceStats.mana < data.manaCost) {
            actionLog.addMessage(String.format("%s does not have enough mana (%d) to donate!",
                sourceStats.name, data.manaCost));
            return;
        }

        // 5. EXECUTE TRANSFER
        sourceStats.mana -= data.manaCost;

        double multiplier    = CombatMechanics.BuffConsumption(user);
        int    flatBonus     = CombatMechanics.AdditiveBonusConsumption(user);
        float  effectiveMult = CombatMechanics.getEffectiveness(user);
        int    finalManaGain = (int) ((data.value * multiplier * effectiveMult) + flatBonus);

        tStats.mana = Math.min(tStats.maxMana, tStats.mana + finalManaGain);

        actionLog.addMessage(String.format("MANA TRANSFER: %s drained %d mana from %s -> Gave %d mana to %s!",
            uStats.name, data.manaCost, sourceStats.name, finalManaGain, tStats.name));

        // ── Play Mana Transfer animation on the target ─────────
        if (animations != null && manaSheet != null) {
            animations.playSkillAnim(manaSheet, 8, 0.10f, target);
        }
    }
}
