package io.github.jhundeniel.ArithmeticHeroes.factories;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;

import io.github.jhundeniel.ArithmeticHeroes.components.*;
import io.github.jhundeniel.ArithmeticHeroes.data.CharacterData;
import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.CharacterRegistry;
import io.github.jhundeniel.ArithmeticHeroes.managers.SkillRegistry;
import io.github.jhundeniel.ArithmeticHeroes.passives.Passive;
import io.github.jhundeniel.ArithmeticHeroes.passives.PassiveRegistry;

public class EntityFactory {
    private final Engine engine;

    // ── Layout constants ───────────────────────────────────────────
    public static final float GROUND_Y = 220f;
    public static final float HERO_W = 120f;
    public static final float HERO_H = 120f;

    public static final float H1_X = 180f;
    public static final float H2_X = 380f;
    public static final float H3_X = 610f;
    public static final float H4_X = 850f;

    public static final float ENEMY_X = 1332f;
    public static final float ENEMY_Y = 200f;
    public static final float ENEMY_W = 200f;
    public static final float ENEMY_H = 220f;

    public EntityFactory(Engine engine) {
        this.engine = engine;
    }

    // ══════════════════════════════════════════════════════════════
    // HERO CREATION
    // ══════════════════════════════════════════════════════════════

    /**
     * Creates a hero with an animated idle sprite sheet.
     */
    public Entity createHero(String characterKey, float x, float y,
            Texture idleSheet, int frameCount, Texture portrait) {
        CharacterData data = CharacterRegistry.get(characterKey);
        if (data == null)
            throw new IllegalArgumentException("Hero key not found: " + characterKey);

        Entity entity = new Entity();
        entity.add(new StatsComponent(data.name, data.maxHp, data.maxMana));
        entity.add(new TypeComponent(Operator.valueOf(data.operatorType), characterKey));
        entity.add(new PartyComponent(true));
        entity.add(new PortraitComponent(portrait));

        Passive assignedPassive = PassiveRegistry.getPassive(data.passiveKey);
        if (assignedPassive != null)
            entity.add(new PassiveComponent(assignedPassive));

        SkillsComponent skills = new SkillsComponent();
        if (data.skills != null)
            for (String skillKey : data.skills)
                skills.addSkill(SkillRegistry.get(skillKey));
        entity.add(skills);

        entity.add(new VisualComponent(
                x, y, HERO_W, HERO_H,
                idleSheet, frameCount,
                ArithmeticAssetManager.HERO_IDLE_FRAME_DUR));

        engine.addEntity(entity);
        return entity;
    }

    /**
     * @deprecated Prefer the animated overload. Kept for backward compatibility.
     */
    @Deprecated
    public Entity createHero(String characterKey, float x, float y,
            Texture tex, Texture portrait) {
        CharacterData data = CharacterRegistry.get(characterKey);
        if (data == null)
            throw new IllegalArgumentException("Hero key not found: " + characterKey);

        Entity entity = new Entity();
        entity.add(new StatsComponent(data.name, data.maxHp, data.maxMana));
        entity.add(new TypeComponent(Operator.valueOf(data.operatorType), characterKey));
        entity.add(new PartyComponent(true));
        entity.add(new PortraitComponent(portrait));

        Passive assignedPassive = PassiveRegistry.getPassive(data.passiveKey);
        if (assignedPassive != null)
            entity.add(new PassiveComponent(assignedPassive));

        SkillsComponent skills = new SkillsComponent();
        if (data.skills != null)
            for (String skillKey : data.skills)
                skills.addSkill(SkillRegistry.get(skillKey));
        entity.add(skills);

        entity.add(new VisualComponent(x, y, HERO_W, HERO_H, 0, 0, tex));
        engine.addEntity(entity);
        return entity;
    }

    // ══════════════════════════════════════════════════════════════
    // ENEMY CREATION
    // ══════════════════════════════════════════════════════════════

    /**
     * Creates an enemy with an animated idle sprite sheet.
     *
     * @param idleSheet  the animation sheet PNG for this enemy key
     * @param frameCount number of frames (13 for all current enemies)
     */
    public Entity createEnemy(String characterKey, float x, float y,
            Texture idleSheet, int frameCount) {
        CharacterData data = CharacterRegistry.get(characterKey);
        if (data == null)
            throw new IllegalArgumentException("Enemy key not found: " + characterKey);

        Entity entity = new Entity();
        entity.add(new StatsComponent(data.name, data.maxHp));
        entity.add(new TypeComponent(Operator.valueOf(data.operatorType), characterKey));
        entity.add(new PartyComponent(false));
        entity.add(new PortraitComponent(null)); // portrait = first frame of sheet

        // Boss Passives
        Passive assignedPassive = PassiveRegistry.getPassive(data.passiveKey);
        if (assignedPassive != null) {
            entity.add(new PassiveComponent(assignedPassive));
        }

        SkillsComponent skills = new SkillsComponent();
        if (data.skills != null)
            for (String skillKey : data.skills)
                skills.addSkill(SkillRegistry.get(skillKey));
        entity.add(skills);

        entity.add(new VisualComponent(
                x, y, ENEMY_W, ENEMY_H,
                idleSheet, frameCount,
                ArithmeticAssetManager.ENEMY_IDLE_FRAME_DUR));

        engine.addEntity(entity);
        return entity;
    }

    /**
     * @deprecated Prefer the animated overload. Kept so old call sites compile.
     */
    @Deprecated
    public Entity createEnemy(String characterKey, float x, float y, Texture tex) {
        CharacterData data = CharacterRegistry.get(characterKey);
        if (data == null)
            throw new IllegalArgumentException("Enemy key not found: " + characterKey);

        Entity entity = new Entity();
        entity.add(new StatsComponent(data.name, data.maxHp));
        entity.add(new TypeComponent(Operator.valueOf(data.operatorType), characterKey));
        entity.add(new PartyComponent(false));
        entity.add(new PortraitComponent(tex));

        // Boss Passives
        Passive assignedPassive = PassiveRegistry.getPassive(data.passiveKey);
        if (assignedPassive != null) {
            entity.add(new PassiveComponent(assignedPassive));
        }

        SkillsComponent skills = new SkillsComponent();
        if (data.skills != null)
            for (String skillKey : data.skills)
                skills.addSkill(SkillRegistry.get(skillKey));
        entity.add(skills);

        entity.add(new VisualComponent(x, y, ENEMY_W, ENEMY_H, 0, 0, tex));
        engine.addEntity(entity);
        return entity;
    }
}
