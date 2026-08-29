package io.github.jhundeniel.ArithmeticHeroes.battle;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import io.github.jhundeniel.ArithmeticHeroes.systems.ActionLogSystem;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent.ActionType;
import io.github.jhundeniel.ArithmeticHeroes.components.Operator;
import io.github.jhundeniel.ArithmeticHeroes.components.SkillsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EnemyAI {
    private final ActionLogSystem actionLog;
    private final ComponentMapper<SkillsComponent> skm = ComponentMapper.getFor(SkillsComponent.class);
    private final ComponentMapper<StatsComponent>  sm  = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<TypeComponent>   tm  = ComponentMapper.getFor(TypeComponent.class);
    private final Random random = new Random();

    private static final float FOCUS_FIRE_THRESHOLD = 0.30f;

    public EnemyAI(ActionLogSystem actionLogSystem) {
        this.actionLog = actionLogSystem;
    }

    public ActionRequestComponent decideAction(Entity self, List<Entity> heroes, List<Entity> enemies, boolean log) {
        SkillsComponent skillsComp = skm.get(self);

        if (skillsComp == null || skillsComp.availableSkills == null || skillsComp.availableSkills.isEmpty()) {
            return fallbackAttack(heroes);
        }

        List<SkillData> validSkills = new ArrayList<>();
        for (SkillData sd : skillsComp.availableSkills.values()) {
            if (sd != null && sd.type != null) validSkills.add(sd);
        }

        if (validSkills.isEmpty()) {
            System.err.println(">> AI WARNING: Enemy has skills in JSON, but none loaded properly!");
            return fallbackAttack(heroes);
        }

        SkillData chosenSkill = validSkills.get(random.nextInt(validSkills.size()));
        ActionType type = chosenSkill.type;

        Entity target;

        if (type == ActionType.ENEMY_AOE_ATTACK) {
            return new ActionRequestComponent(type, null);
        } else if (isOffensive(type)) {
            target = pickBestHeroTarget(heroes, log);
        } else {
            target = getLowestHp(enemies);
        }

        if (target == null) target = self;
        if (target == null) return null;

        return new ActionRequestComponent(type, target);
    }

    private Entity pickBestHeroTarget(List<Entity> heroes, boolean log) {
        List<Entity> alive = getAlive(heroes);
        if (alive.isEmpty()) return null;

        // Rule 1: Focus fire — always fires if someone is below 30% HP
        Entity focusTarget = getFocusFireTarget(alive);
        if (focusTarget != null) {
            if (log && actionLog != null) actionLog.addMessage("FOCUS FIRE: Finishing " + name(focusTarget) + "!");
            return focusTarget;
        }

        // Rules 2, 3, 4 — weighted random
        Entity threatTarget = DamageTracker.getHighestThreat(alive);
        Entity wornTarget   = getWeakestLink(alive);
        Entity healerTarget = getHealerTarget(alive);

        // Healer only enters the pool if battle data exists (threat or worn down available)
        // This prevents healer disruption from firing in round 1 when no data exists yet
        boolean battleDataExists = threatTarget != null || wornTarget != null;

        List<float[]> pool = new ArrayList<>();
        if (threatTarget != null) pool.add(new float[]{0.50f, 0});
        if (healerTarget != null && battleDataExists && random.nextFloat() < 0.40f) pool.add(new float[]{0.30f, 1});
        if (wornTarget   != null) pool.add(new float[]{0.20f, 2});

        if (!pool.isEmpty()) {
            // Normalize weights based on what's available
            float totalWeight = 0;
            for (float[] entry : pool) totalWeight += entry[0];

            float roll = random.nextFloat() * totalWeight;
            float cumulative = 0;

            for (float[] entry : pool) {
                cumulative += entry[0];
                if (roll < cumulative) {
                    int index = (int) entry[1];
                    if (index == 0) {
                        if (log && actionLog != null) actionLog.addMessage("HIGH THREAT: Targeting " + name(threatTarget) + "!");
                        return threatTarget;
                    } else if (index == 1) {
                        if (log && actionLog != null) actionLog.addMessage("DISRUPTION: Targeting healer " + name(healerTarget) + "!");
                        return healerTarget;
                    } else {
                        if (log && actionLog != null) actionLog.addMessage("WORN DOWN: Targeting " + name(wornTarget) + "!");
                        return wornTarget;
                    }
                }
            }
        }

        // Fallback — random, no log since no rule fired
        return alive.get(random.nextInt(alive.size()));
    }

    private Entity getFocusFireTarget(List<Entity> alive) {
        Entity best = null;
        float lowestHp = FOCUS_FIRE_THRESHOLD;
        for (Entity e : alive) {
            StatsComponent s = sm.get(e);
            if (s == null) continue;
            float pct = (float) s.hp / s.maxHp;
            if (pct < lowestHp) {
                lowestHp = pct;
                best = e;
            }
        }
        return best;
    }

    private Entity getHealerTarget(List<Entity> alive) {
        for (Entity e : alive) {
            TypeComponent type = tm.get(e);
            if (type != null && type.type == Operator.ADDITION) return e;
        }
        return null;
    }

    private Entity getWeakestLink(List<Entity> alive) {
        List<Entity> weakest = new ArrayList<>();
        float lowestPct = 1.0f;

        for (Entity e : alive) {
            StatsComponent s = sm.get(e);
            if (s != null) {
                float pct = (float) s.hp / s.maxHp;
                if (pct < lowestPct) {
                    lowestPct = pct;
                    weakest.clear();
                    weakest.add(e);
                } else if (pct == lowestPct) {
                    weakest.add(e);
                }
            }
        }

        if (weakest.isEmpty() || lowestPct >= 1.0f) return null;
        return weakest.get(random.nextInt(weakest.size()));
    }

    private ActionRequestComponent fallbackAttack(List<Entity> heroes) {
        List<Entity> alive = getAlive(heroes);
        if (alive.isEmpty()) return null;
        return new ActionRequestComponent(ActionType.ENEMY_ATTACK,
            alive.get(random.nextInt(alive.size())));
    }

    private boolean isOffensive(ActionType type) {
        return type == ActionType.ENEMY_ATTACK || type == ActionType.ENEMY_AOE_ATTACK;
    }

    private List<Entity> getAlive(List<Entity> pool) {
        List<Entity> alive = new ArrayList<>();
        for (Entity e : pool) {
            StatsComponent s = sm.get(e);
            if (s != null && s.hp > 0) alive.add(e);
        }
        return alive;
    }

    private Entity getLowestHp(List<Entity> pool) {
        Entity weakest = null;
        float lowestPct = 1.0f;
        for (Entity e : pool) {
            StatsComponent s = sm.get(e);
            if (s != null && s.hp > 0) {
                float pct = (float) s.hp / s.maxHp;
                if (pct < lowestPct) { lowestPct = pct; weakest = e; }
            }
        }
        return weakest != null ? weakest : (getAlive(pool).isEmpty() ? null
            : getAlive(pool).get(random.nextInt(getAlive(pool).size())));
    }

    private String name(Entity e) {
        StatsComponent s = sm.get(e);
        return s != null ? s.name.trim() : "?";
    }
}
