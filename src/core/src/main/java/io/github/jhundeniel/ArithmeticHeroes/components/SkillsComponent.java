package io.github.jhundeniel.ArithmeticHeroes.components;

import com.badlogic.ashley.core.Component;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;
import java.util.HashMap;
import java.util.Map;


public class SkillsComponent implements Component {
    //Maps an ActionType to its Data (cost: 5, power 10)
    public Map<ActionRequestComponent.ActionType, SkillData> availableSkills = new HashMap<>();

    public void addSkill(SkillData skill) {
        if (skill != null) {
            availableSkills.put(skill.type, skill);
        }
    }

    public SkillData get(ActionRequestComponent.ActionType type) {
        return availableSkills.get(type);
    }

    public boolean hasSkill(ActionRequestComponent.ActionType type) {
        return availableSkills.containsKey(type);
    }
}
