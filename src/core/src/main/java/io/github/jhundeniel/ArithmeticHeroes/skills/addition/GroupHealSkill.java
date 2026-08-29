package io.github.jhundeniel.ArithmeticHeroes.skills.addition;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.PassiveComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.PartyComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.passives.Passive;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class GroupHealSkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ComponentMapper<PartyComponent>  pm  = ComponentMapper.getFor(PartyComponent.class);
    private final ComponentMapper<PassiveComponent> ps = ComponentMapper.getFor(PassiveComponent.class);

    private final Engine           engine;
    private final ActionLogSystem  actionLog;
    private final BattleAnimations animations;
    private final Texture          healSheet;

    public GroupHealSkill(Engine engine, ActionLogSystem actionLog,
                          BattleAnimations animations, Texture healSheet) {
        this.engine     = engine;
        this.actionLog  = actionLog;
        this.animations = animations;
        this.healSheet  = healSheet;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);

        // 1. CHECK COMPONENT
        if (!skm.has(user)) {
            actionLog.addMessage("Error: " + uStats.name + " has no SkillsComponent!");
            return;
        }

        // 2. GET SKILL DATA
        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.GROUP_HEAL);

        // 3. MANA CHECK
        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);
        if (uStats.mana < finalCost) {
            actionLog.addMessage("Not enough Mana!");
            return;
        }
        uStats.mana -= finalCost;

        // 4. CALCULATE HEAL VALUE
        double multiplier = CombatMechanics.BuffConsumption(user);
        int    flatBonus  = CombatMechanics.AdditiveBonusConsumption(user);
        int    finalHeal  = (int) ((data.value * multiplier) + flatBonus);

        // 5. APPLY GROUP HEAL
        ImmutableArray<Entity> allAllies = engine.getEntitiesFor(
            Family.all(StatsComponent.class, PartyComponent.class).get());

        actionLog.addMessage(String.format("%s casts Group Heal! (+%d HP)",
            uStats.name.trim(), finalHeal));

        for (Entity ally : allAllies) {
            if (pm.has(ally) && pm.get(ally).isPlayer && sm.get(ally).hp > 0) {
                CombatMechanics.applyHeal(ally, finalHeal);

                // ── Play Heal animation on each healed ally ─────
                if (animations != null && healSheet != null)
                    animations.playSkillAnim(healSheet, 11, 0.08f, ally);

                if (ps.has(user)) {
                    Passive passive = ps.get(user).passive;
                    passive.onHeal(user, ally, finalHeal);
                }
            }
        }
    }
}
