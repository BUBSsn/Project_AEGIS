package io.github.jhundeniel.ArithmeticHeroes.skills.division;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.PartyComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class BattleEqualizerSKill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>         sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent>        skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ComponentMapper<PartyComponent>         pm  = ComponentMapper.getFor(PartyComponent.class);
    private final ComponentMapper<ActionRequestComponent> am  = ComponentMapper.getFor(ActionRequestComponent.class);

    private final ActionLogSystem  actionLog;
    private final BattleAnimations animations;
    private final Texture          equalizerSheet;

    public BattleEqualizerSKill(ActionLogSystem actionLog,
                                BattleAnimations animations, Texture equalizerSheet) {
        this.actionLog      = actionLog;
        this.animations     = animations;
        this.equalizerSheet = equalizerSheet;
    }

    @Override
    public void execute(Entity user, Entity target1) {
        StatsComponent uStats = sm.get(user);

        // 1. CHECKS IF USER HAS COMPONENT
        if (!skm.has(user)) {
            actionLog.addMessage("Error: No Component Detected");
            return;
        }

        // 2. GET SECOND TARGET
        if (!am.has(user)) {
            actionLog.addMessage("Error: Equalizer requires an action request!");
            return;
        }
        Entity target2 = am.get(user).secondaryTarget;
        if (target2 == null) {
            actionLog.addMessage("Error: Equalizer requires TWO targets!");
            return;
        }

        // 3. GET SPECIFIC DATA
        SkillsComponent skills  = skm.get(user);
        StatsComponent  t1Stats = sm.get(target1);
        StatsComponent  t2Stats = sm.get(target2);
        SkillData       data    = skills.get(ActionRequestComponent.ActionType.BATTLE_EQUALIZER);

        // 4. MANA CHECK
        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);

        if (uStats.mana >= finalCost) {
            uStats.mana -= finalCost;

            int totalHP   = t1Stats.hp + t2Stats.hp;
            int average   = totalHP / 2;
            int remainder = totalHP % 2;
            int oldHp1    = t1Stats.hp;
            int oldHp2    = t2Stats.hp;

            t1Stats.hp = Math.min(t1Stats.maxHp, average + remainder);
            t2Stats.hp = Math.min(t2Stats.maxHp, average);

            actionLog.addMessage(String.format(
                "BATTLE EQUALIZER: Combined %d HP. Split into %d and %d.\n",
                totalHP, t1Stats.hp, t2Stats.hp));
            actionLog.addMessage(String.format(
                "   %s: %d -> %d\n", t1Stats.name, oldHp1, t1Stats.hp));
            actionLog.addMessage(String.format(
                "   %s: %d -> %d\n", t2Stats.name, oldHp2, t2Stats.hp));

            // ── Play Equalizer animation on both targets ─────────
            if (animations != null && equalizerSheet != null) {
                animations.playSkillAnim(equalizerSheet, 10, 0.09f, target1);
                animations.playSkillAnim(equalizerSheet, 10, 0.09f, target2);
            }

        } else {
            actionLog.addMessage("Not Enough Mana!");
        }
    }
}
