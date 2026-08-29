package io.github.jhundeniel.ArithmeticHeroes.skills.addition;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class AdditionBuffSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ComponentMapper<ActionRequestComponent> am = ComponentMapper.getFor(ActionRequestComponent.class);
    private final ActionLogSystem actionLog;

    private final BattleAnimations animations;
    private final com.badlogic.gdx.graphics.Texture animSheet;

    public AdditionBuffSkill(ActionLogSystem actionLog,
                             BattleAnimations animations,
                             com.badlogic.gdx.graphics.Texture animSheet) {
        this.actionLog  = actionLog;
        this.animations = animations;
        this.animSheet  = animSheet;
    }

    @Override
    public void execute(Entity user, Entity target){
        StatsComponent uStats = sm.get(user);
        StatsComponent tStats = sm.get(target);

        //1. CHECKS IF USER HAS COMPONENT
        if(!skm.has(user)) {
            actionLog.addMessage("Error: No Component Detected");
            return;
        }

        //2. GET SPECIFIC DATA
        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.ADDITIONAL_BUFF);

        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);

        //3. MANA CHECK
        if (uStats.mana >= finalCost) {
            uStats.mana -= finalCost;

            // Use player-chosen value (from the 3/4/5 chooser) if available
            int buffValue;
            ActionRequestComponent request = am.get(user);
            if (request != null && request.chosenValue >= (int) data.min
                && request.chosenValue <= (int) data.max) {
                buffValue = request.chosenValue;
            } else {
                // Fallback: random roll between Min (3) and Max (5)
                int range = (int) (data.max - data.min + 1);
                buffValue = (int) ((Math.random() * range) + data.min);
            }
            StatusEffects.add(target, StatusEffect.additiveBonus(buffValue));

            if (animations != null && animSheet != null) {
                animations.playSkillAnim(animSheet,
                    io.github.jhundeniel.ArithmeticHeroes.battle.SkillAnimConfig.ADDITIONAL_BUFF_FRAMES,
                    io.github.jhundeniel.ArithmeticHeroes.battle.SkillAnimConfig.ADDITIONAL_BUFF_DUR,
                    target);
            }

            String msg = String.format("%s buffed %s! Next action has an additive bonus of +%d\n", uStats.name, tStats.name, buffValue);
            actionLog.addMessage(msg);
        } else {
            actionLog.addMessage("Not enough Mana!");
        }

    }
}
