package io.github.jhundeniel.ArithmeticHeroes.skills;

import com.badlogic.ashley.core.Entity;

public interface SkillStrategy {
    void execute(Entity user, Entity target);
}
