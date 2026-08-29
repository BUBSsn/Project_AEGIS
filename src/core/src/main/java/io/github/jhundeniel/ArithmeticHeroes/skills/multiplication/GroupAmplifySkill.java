package io.github.jhundeniel.ArithmeticHeroes.skills.multiplication;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Texture;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleAnimations;
import io.github.jhundeniel.ArithmeticHeroes.battle.CombatMechanics;
import io.github.jhundeniel.ArithmeticHeroes.battle.SkillAnimConfig;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.PartyComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffect;
import io.github.jhundeniel.ArithmeticHeroes.components.status.StatusEffects;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import io.github.jhundeniel.ArithmeticHeroes.skills.SkillStrategy;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;

public class GroupAmplifySkill implements SkillStrategy {
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ComponentMapper<PartyComponent>  pm  = ComponentMapper.getFor(PartyComponent.class);

    private final Engine           engine;
    private final ActionLogSystem  actionLog;
    private final BattleAnimations animations;
    private final Texture          amplifySheet;

    public GroupAmplifySkill(Engine engine, ActionLogSystem actionLog,
                             BattleAnimations animations, Texture amplifySheet) {
        this.engine       = engine;
        this.actionLog    = actionLog;
        this.animations   = animations;
        this.amplifySheet = amplifySheet;
    }

    @Override
    public void execute(Entity user, Entity target) {
        StatsComponent uStats = sm.get(user);

        // 1. CHECK COMPONENT
        if (!skm.has(user)) {
            actionLog.addMessage("Error: No Component Detected");
            return;
        }

        // 2. GET SKILL DATA
        SkillsComponent skills = skm.get(user);
        SkillData data = skills.get(ActionRequestComponent.ActionType.GROUP_AMPLIFY);

        // 3. MANA CHECK
        int finalCost = CombatMechanics.getFinalManaCost(user, data.manaCost);
        if (uStats.mana < finalCost) {
            actionLog.addMessage(uStats.name.trim() + " doesn't have enough Mana!");
            return;
        }
        uStats.mana -= finalCost;

        double rolledValue = data.min + Math.random() * (data.max - data.min);

        ImmutableArray<Entity> allEntities = engine.getEntitiesFor(
            Family.all(StatsComponent.class, PartyComponent.class).get());

        int buffedCount = 0;
        for (Entity entity : allEntities) {
            if (pm.has(entity) && pm.get(entity).isPlayer && sm.get(entity).hp > 0) {
                StatusEffects.add(entity, StatusEffect.buff(rolledValue));
                buffedCount++;

                // ── Amplify.png: 16 frames × 125 px, 2000 px wide ───────
                // Skip animation on the caster — only show on buffed allies
                if (entity != user && animations != null && amplifySheet != null) {
                    animations.playSkillAnim(amplifySheet,
                        SkillAnimConfig.AMPLIFY_FRAMES,
                        SkillAnimConfig.AMPLIFY_DUR,
                        entity);
                }
            }
        }

        String msg = String.format("%s uses Group Amplify! %d allies buffed x%.2f!",
            uStats.name.trim(), buffedCount, rolledValue);
        actionLog.addMessage(msg);
    }
}
