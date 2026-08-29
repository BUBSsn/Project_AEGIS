package io.github.jhundeniel.ArithmeticHeroes.utils;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;

import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;

/**
 * Utility class to create formatted combat messages
 */
public class ActionLogger {
    private static final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);

    public static String heal(Entity healer, Entity target, int amount) {
        String healerName = sm.has(healer) ? sm.get(healer).name : "Unknown";
        String targetName = sm.has(target) ? sm.get(target).name : "Unknown";
        return healerName + " healed " + targetName + " for " + amount + " HP!";
    }

    public static String damage(Entity attacker, Entity target, int amount) {
        String attackerName = sm.has(attacker) ? sm.get(attacker).name : "Unknown";
        String targetName = sm.has(target) ? sm.get(target).name : "Unknown";
        return attackerName + " dealt " + amount + " damage to " + targetName + "!";
    }

    public static String buff(Entity caster, Entity target, String buffType, int amount) {
        String casterName = sm.has(caster) ? sm.get(caster).name : "Unknown";
        String targetName = sm.has(target) ? sm.get(target).name : "Unknown";
        return casterName + " gave " + targetName + " +" + amount + " " + buffType + "!";
    }

    public static String debuff(Entity caster, Entity target, String debuffType) {
        String casterName = sm.has(caster) ? sm.get(caster).name : "Unknown";
        String targetName = sm.has(target) ? sm.get(target).name : "Unknown";
        return casterName + " applied " + debuffType + " to " + targetName + "!";
    }

    public static String manaTransfer(Entity source, Entity target, int amount) {
        String sourceName = sm.has(source) ? sm.get(source).name : "Unknown";
        String targetName = sm.has(target) ? sm.get(target).name : "Unknown";
        return sourceName + " transferred " + amount + " mana to " + targetName + "!";
    }

    public static String custom(String message) {
        return message;
    }

    public static String death(Entity entity) {
        String name = sm.has(entity) ? sm.get(entity).name : "Unknown";
        return name + " has been defeated!";
    }

    public static String turnStart(Entity entity) {
        String name = sm.has(entity) ? sm.get(entity).name : "Unknown";
        return ">>> " + name + "'s turn!";
    }
}
