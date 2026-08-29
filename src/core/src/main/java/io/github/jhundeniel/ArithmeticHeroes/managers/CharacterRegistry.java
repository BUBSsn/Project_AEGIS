package io.github.jhundeniel.ArithmeticHeroes.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.jhundeniel.ArithmeticHeroes.data.CharacterData;

import java.util.HashMap;
import java.util.Map;

public class CharacterRegistry {
    private static final Map<String, CharacterData> characterMap = new HashMap<>();

    public static void loadCharacters() {
        System.out.println("Attempting to load characters...");
        try {
            Json json = new Json();
            json.setIgnoreUnknownFields(true);
            JsonReader reader = new JsonReader();

            // Load Heroes
            JsonValue heroBase = reader.parse(Gdx.files.internal("data/heroes.json"));
            for (JsonValue val : heroBase.get("heroes")) {
                CharacterData data = json.readValue(CharacterData.class, val);
                characterMap.put(data.key, data);
                System.out.println("Loaded Hero: " + data.name);
            }

            // Load Enemies
            JsonValue enemyBase = reader.parse(Gdx.files.internal("data/enemies.json"));
            for (JsonValue val : enemyBase.get("enemies")) {
                CharacterData data = json.readValue(CharacterData.class, val);
                characterMap.put(data.key, data);
                System.out.println("Loaded Enemy: " + data.name);
            }

        } catch (Exception e) {
            System.err.println("🚨 CRASH WHILE LOADING CHARACTERS 🚨");
            e.printStackTrace();
        }
    }

    public static CharacterData get(String key) {
        return characterMap.get(key);
    }
}
