package io.github.jhundeniel.ArithmeticHeroes.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import java.util.ArrayList;
import java.util.List;

public class BattleUIComponent implements Component {

    // UI refresh flags
    public boolean turnOrderDirty = true;
    public boolean statusDirty = true;
    public boolean skillMenuDirty = true;
    public boolean calculationDirty = false;

    // UI State
    public enum BattlePhase {
        PLAYER_TURN,
        ENEMY_TURN,
        CALCULATING,
        ANIMATING
    }

    public enum SkillType {
        HEAL("Heal", "Restore HP with addition"),
        POKE("Poke", "Light damage with subtraction"),
        AMPLIFY("Amplify", "Multiply damage"),
        BURDEN("Burden", "Divide enemy stats");

        public final String name;
        public final String description;

        SkillType(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }

    public enum TargetMode {
        SINGLE,
        ALL
    }

    // Turn order tracking
    public List<Entity> turnOrder = new ArrayList<>();
    public int currentTurnIndex = 0;

    // Current action state
    public BattlePhase currentPhase = BattlePhase.PLAYER_TURN;
    public SkillType selectedSkill = null;
    public TargetMode targetMode = TargetMode.SINGLE;
    public Entity selectedTarget = null;

    // Calculation display
    public String calculationText = "";
    public float finalValue = 0f;
    public boolean showCalculation = false;
    public float calculationTimer = 0f;

    // UI Animation
    public float uiAnimationTimer = 0f;
    public boolean skillMenuVisible = true;

    // Message log for ACTION LOG display
    public List<String> battleLog = new ArrayList<>();

    // Enemy Intent Display
    public String enemyMove = null;      // What skill the enemy will use
    public Entity enemyTarget = null;    // Who the enemy will target

    public BattleUIComponent() {
        battleLog.add("Battle begins!");
    }

    public void setCalculation(String calculation, float value) {
        this.calculationText = calculation;
        this.finalValue = value;
        this.showCalculation = true;
        this.calculationTimer = 2.5f;
        this.calculationDirty = true;
    }

    public void addLogMessage(String message) {
        battleLog.add(message);
        if (battleLog.size() > 10) { // Keep last 10 messages
            battleLog.remove(0);
        }
        statusDirty = true;
    }

    public void setEnemyIntent(String move, Entity target) {
        this.enemyMove = move;
        this.enemyTarget = target;
    }

    public void clearEnemyIntent() {
        this.enemyMove = null;
        this.enemyTarget = null;
    }

    public void nextTurn() {
        currentTurnIndex++;
        if (currentTurnIndex >= turnOrder.size()) {
            currentTurnIndex = 0;
        }
        turnOrderDirty = true;
        statusDirty = true;
    }

    public Entity getCurrentTurnEntity() {
        if (turnOrder.isEmpty()) return null;
        return turnOrder.get(currentTurnIndex);
    }

    public void selectSkill(SkillType skill) {
        this.selectedSkill = skill;
        this.skillMenuDirty = true;
        this.statusDirty = true;
    }

    public void setTargetMode(TargetMode mode) {
        this.targetMode = mode;
        this.statusDirty = true;
    }

    public void setPhase(BattlePhase phase) {
        this.currentPhase = phase;
        this.skillMenuDirty = true;
        this.statusDirty = true;
    }
}
