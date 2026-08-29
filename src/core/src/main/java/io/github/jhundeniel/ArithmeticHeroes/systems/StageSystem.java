package io.github.jhundeniel.ArithmeticHeroes.systems;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;

import io.github.jhundeniel.ArithmeticHeroes.data.StageData;
import io.github.jhundeniel.ArithmeticHeroes.factories.EntityFactory;
import io.github.jhundeniel.ArithmeticHeroes.managers.ArithmeticAssetManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.SaveManager;
import io.github.jhundeniel.ArithmeticHeroes.managers.StageRegistry;

public class StageSystem extends EntitySystem {

    private int currentStageIndex = 0;
    private final ArithmeticAssetManager assetManager;
    private StageChangeListener listener;
    private List<Entity> heroesRef;
    private boolean gameComplete = false;

    /** When true, advanceStage() will NOT proceed to the next stage. */
    private boolean practiceMode = false;
    /** Set to true when the practice stage is beaten (only in practiceMode). */
    private boolean practiceComplete = false;

    // Mid-battle restore
    private List<io.github.jhundeniel.ArithmeticHeroes.data.SaveData.EnemySaveData> savedEnemies;

    // Save slot
    private String slotId;
    private String displayName;

    public interface StageChangeListener {
        void onStageChanged(int newStageIndex);
    }

    public StageSystem(ArithmeticAssetManager assetManager, EntityFactory factory) {
        this.assetManager = assetManager;
    }

    public void setStageListener(StageChangeListener l) {
        this.listener = l;
    }

    public void setStartingStage(int index) {
        this.currentStageIndex = index;
    }

    public void setHeroesRef(List<Entity> heroes) {
        this.heroesRef = heroes;
    }

    public void setSlotId(String id) {
        this.slotId = id;
    }

    public void setDisplayName(String name) {
        this.displayName = name;
    }

    public void setPracticeMode(boolean pm) {
        this.practiceMode = pm;
    }

    public void setSavedEnemies(List<io.github.jhundeniel.ArithmeticHeroes.data.SaveData.EnemySaveData> e) {
        this.savedEnemies = e;
    }

    public List<io.github.jhundeniel.ArithmeticHeroes.data.SaveData.EnemySaveData> getSavedEnemies() {
        return savedEnemies;
    }

    public void clearSavedEnemies() {
        this.savedEnemies = null;
    }

    @Override
    public void update(float deltaTime) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            System.out.println("'N' pressed — advancing stage.");
            advanceStage();
        }
    }

    public void advanceStage() {
        // In practice mode, don't advance to the next stage — just mark as done
        if (practiceMode) {
            practiceComplete = true;
            System.out.println("[StageSystem] Practice stage beaten! Returning to menu.");
            return;
        }

        currentStageIndex++;
        System.out.println("ADVANCING TO STAGE INDEX: " + currentStageIndex);

        if (StageRegistry.getStage(currentStageIndex) == null) {
            gameComplete = true;
            System.out.println("[StageSystem] ALL STAGES CLEARED!");
        }

        if (listener != null)
            listener.onStageChanged(currentStageIndex);
        if (heroesRef != null)
            SaveManager.save(currentStageIndex, heroesRef);
    }

    public void triggerStart() {
        if (listener != null)
            listener.onStageChanged(currentStageIndex);
    }

    /** Background resolved from stages.json backgroundPath. */
    public Texture getCurrentBackground() {
        StageData stage = StageRegistry.getStage(currentStageIndex);
        if (stage == null || stage.backgroundPath == null)
            return null;
        String key = resolveBackgroundKey(stage.backgroundPath);
        return key != null ? assetManager.getTexture(key) : null;
    }

    private String resolveBackgroundKey(String path) {
        if (path.contains("first"))
            return ArithmeticAssetManager.BG_STAGE_0;
        if (path.contains("bg_stage_1"))
            return ArithmeticAssetManager.BG_STAGE_1;
        if (path.contains("bg_stage2"))
            return ArithmeticAssetManager.BG_STAGE_2;
        if (path.contains("bg_stage3"))
            return ArithmeticAssetManager.BG_STAGE_3;
        return null;
    }

    /** Animated idle sprite sheet for the given enemy key. */
    public Texture getEnemySheet(String enemyKey) {
        switch (enemyKey) {
            case "ENEMY_MOB1":
                return assetManager.getTexture(ArithmeticAssetManager.ANIM_MOB1);
            case "ENEMY_MOB2":
                return assetManager.getTexture(ArithmeticAssetManager.ANIM_MOB2);
            case "ENEMY_MOB3":
                return assetManager.getTexture(ArithmeticAssetManager.ANIM_MOB3);
            case "ENEMY_MOB4":
                return assetManager.getTexture(ArithmeticAssetManager.ANIM_MOB4);
            case "ENEMY_MOB5":
                return assetManager.getTexture(ArithmeticAssetManager.ANIM_MOB5);
            case "ENEMY_BOSS1":
                return assetManager.getTexture(ArithmeticAssetManager.ANIM_BOSS1);
            case "ENEMY_BOSS2":
                return assetManager.getTexture(ArithmeticAssetManager.ANIM_BOSS2);
            case "ENEMY_BOSS3":
                return assetManager.getTexture(ArithmeticAssetManager.ANIM_BOSS3);
            default:
                return assetManager.getTexture(ArithmeticAssetManager.CHAR_MOB1);
        }
    }

    /** Head icon (170×170 PNG) for the turn-order display. */
    public Texture getEnemyIcon(String enemyKey) {
        switch (enemyKey) {
            case "ENEMY_MOB1":
                return assetManager.getTexture(ArithmeticAssetManager.ICON_MOB1);
            case "ENEMY_MOB2":
                return assetManager.getTexture(ArithmeticAssetManager.ICON_MOB2);
            case "ENEMY_MOB3":
                return assetManager.getTexture(ArithmeticAssetManager.ICON_MOB3);
            case "ENEMY_MOB4":
                return assetManager.getTexture(ArithmeticAssetManager.ICON_MOB4);
            case "ENEMY_MOB5":
                return assetManager.getTexture(ArithmeticAssetManager.ICON_MOB5);
            case "ENEMY_BOSS1":
                return assetManager.getTexture(ArithmeticAssetManager.ICON_BOSS1);
            case "ENEMY_BOSS2":
                return assetManager.getTexture(ArithmeticAssetManager.ICON_BOSS2);
            case "ENEMY_BOSS3":
                return assetManager.getTexture(ArithmeticAssetManager.ICON_BOSS3);
            default:
                return assetManager.getTexture(ArithmeticAssetManager.CHAR_MOB1);
        }
    }

    public boolean isGameComplete() {
        return gameComplete;
    }

    public boolean isPracticeComplete() {
        return practiceComplete;
    }

    public int getCurrentStageIndex() {
        return currentStageIndex;
    }
}
