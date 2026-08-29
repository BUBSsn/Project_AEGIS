package io.github.jhundeniel.ArithmeticHeroes.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.jhundeniel.ArithmeticHeroes.data.StageData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StageRegistry {

    //List for "Stage 0", "Stage 1", etc.
    private static final List<StageData> stageList = new ArrayList<>();

    public static void loadStages() {
        System.out.println("Trying to load stages...");
        try {
            Json json = new Json();
            json.setIgnoreUnknownFields(true);
            JsonReader reader = new JsonReader();

            JsonValue base = reader.parse(Gdx.files.internal("data/stages.json"));
            for (JsonValue val : base.get("stages")) {
                StageData data = json.readValue(StageData.class, val);
                stageList.add(data);
                System.out.println("Loaded Stage:" + data.stageId);
            }
        } catch (Exception e) {
            System.out.println("Failed to load stages.");
            e.printStackTrace();
        }
    }

    public static StageData getStage(int index) {
        if (index >= 0 && index < stageList.size()) {
            return stageList.get(index);
        }
        return null; //Returns null if you beat all stages!
    }

    /** @return the total number of stages loaded from stages.json. */
    public static int getStageCount() {
        return stageList.size();
    }

    /** @return an unmodifiable view of all loaded stages. */
    public static List<StageData> getStageList() {
        return Collections.unmodifiableList(stageList);
    }
}
