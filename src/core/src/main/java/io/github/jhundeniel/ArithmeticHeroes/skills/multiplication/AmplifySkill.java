package io.github.jhundeniel.ArithmeticHeroes.skills.multiplication;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class AmplifySkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);

    private final ActionLogSystem  actionLog;
    private final BattleAnimations animations;
    private final Texture          amplifySheet;

    public AmplifySkill(ActionLogSystem actionLog,
                        BattleAnimations animations, Texture amplifySheet) {
        this.actionLog    = actionLog;
        this.animations   = animations;
        this.amplifySheet = amplifySheet;
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
        SkillData data = skills.get(ActionRequestComponent.ActionType.AMPLIFY);

        // 3. MANA CHECK
        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);

        if (uStats.mana >= finalCost) {
            uStats.mana -= finalCost;

            double rolledValue = data.min + Math.random() * (data.max - data.min);
            StatusEffects.add(target, StatusEffect.buff(rolledValue));

            String msg = String.format("%s buffed %s! Next action x%.2f (%.2f%%)\n",
                uStats.name, tStats.name, rolledValue, (rolledValue * 100));
            actionLog.addMessage(msg);

            // ── Play Amplify animation on the target ───────────
            if (animations != null && amplifySheet != null)
                animations.playSkillAnim(amplifySheet, 16, 0.08f, target);

        } else {
            actionLog.addMessage("Not enough Mana!");
        }
    }
}
