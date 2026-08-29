package io.github.jhundeniel.ArithmeticHeroes.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.jhundeniel.ArithmeticHeroes.data.SkillData;

import java.util.HashMap;

public class SkillRegistry {
    public static HashMap<String, SkillData> skillMap = new HashMap<>();

    public static void loadSkills() {
        // Debug print
        System.out.println("Looking for assets at: " + Gdx.files.getLocalStoragePath());

        // Try catching the error to see the full message
        try {
            Json json = new Json();
            JsonReader reader = new JsonReader();
            json.setIgnoreUnknownFields(true);

            // Use 'internal' for read-only assets
            JsonValue base = reader.parse(Gdx.files.internal("data/skills.json"));

            //Iterate through the "skills" array
            for (JsonValue skillVal : base.get("skills")) {
                SkillData data = json.readValue(SkillData.class, skillVal);
                skillMap.put(data.key, data);
                System.out.println("Loaded Skill: " + data.name);
            }

        } catch (Exception e) {
            System.err.println("CRASH LOADING SKILLS!");
            e.printStackTrace(); // This prints the REAL error to your console
        }
    }

    public static SkillData get(String key) {
        return skillMap.get(key);
    }

    // Searches the loaded JSON skills by their ActionType enum!
    public static SkillData getByType(io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent.ActionType targetType) {
        for (SkillData data : skillMap.values()) {
            if (data.type == targetType) {
                return data;
            }
        }
        return null; // Returns null if it couldn't find a match
    }
}
