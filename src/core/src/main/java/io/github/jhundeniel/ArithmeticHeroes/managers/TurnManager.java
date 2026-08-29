package io.github.jhundeniel.ArithmeticHeroes.managers;

import java.util.*;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;

import io.github.jhundeniel.ArithmeticHeroes.battle.DamageTracker;
import io.github.jhundeniel.ArithmeticHeroes.battle.BattleState;
import io.github.jhundeniel.ArithmeticHeroes.battle.DamageTracker;
import io.github.jhundeniel.ArithmeticHeroes.components.Operator;
import io.github.jhundeniel.ArithmeticHeroes.components.TypeComponent;
import io.github.jhundeniel.ArithmeticHeroes.components.StatsComponent;

public class TurnManager {

    private BattleState currentState = BattleState.ROUND_START;
    private final Queue<Entity> turnQueue = new LinkedList<>();
    private final List<Entity> nextRoundQueue = new ArrayList<>();
    private Entity currentEntityTurn;

    private int currentRound = 1;
    private List<Entity> aliveHeroes;
    private List<Entity> aliveEnemies;

    // Tracks the last hero who acted so we can go back to them
    private final Stack<Entity> previousHeroes = new Stack<>();
    private static final int MAX_HISTORY = 8; // don't let stack grow unbounded
    private final List<Entity> allEntities = new java.util.ArrayList<>();

    private final ComponentMapper<StatsComponent> sm = ComponentMapper.getFor(StatsComponent.class);
    private final ComponentMapper<TypeComponent> tm = ComponentMapper.getFor(TypeComponent.class);

    // ── Turn management ───────────────────────────────────────────────────

    // Call this ONCE when the stage starts
    public void initBattle(List<Entity> heroes, List<Entity> enemies) {
        this.aliveHeroes = heroes;
        this.aliveEnemies = enemies;
        this.currentRound = 1;

        turnQueue.clear();
        nextRoundQueue.clear();
        allEntities.clear();
        previousHeroes.clear();

        allEntities.addAll(heroes);
        allEntities.addAll(enemies);

        // Pre-Roll ROund 2
        nextRoundQueue.addAll(allEntities);
        Collections.shuffle(nextRoundQueue);

        this.currentState = BattleState.ROUND_START;
    }

    // Handles Round 1 vs Round 2+ Logic
    public void startNextRound() {
        turnQueue.clear();

        // Defensive: if initBattle() was never called, there's nothing to queue
        if (aliveHeroes == null || aliveEnemies == null) {
            System.err.println("[TurnManager] startNextRound called but battle not initialized.");
            return;
        }

        // Inversion is consumed per-use in CombatSystem.executeAction()

        if (currentRound == 1) {
            // Round 1:
            turnQueue.addAll(aliveHeroes);
            turnQueue.addAll(aliveEnemies);
        } else {
            // Uses the pre-rolled queue for this round
            turnQueue.addAll(nextRoundQueue);

            // --- MANA REGEN AT THE END OF THE ROUND (Start of Round 2+) ---
            for (Entity hero : aliveHeroes) {
                StatsComponent stats = sm.get(hero);
                if (stats != null && stats.hp > 0) {
                    stats.mana = Math.min(stats.maxMana, stats.mana + 2);
                    System.out.println(">> 💧 " + stats.name.trim() + " regained 2 mana for the new round! ("
                            + stats.mana + "/" + stats.maxMana + ")");
                }
            }
        }

        // NOW PRE-ROLL THE *NEXT* ROUND
        nextRoundQueue.clear();
        List<Entity> allCombatants = new ArrayList<>();
        allCombatants.addAll(aliveEnemies);
        allCombatants.addAll(aliveHeroes);
        Collections.shuffle(allCombatants);
        nextRoundQueue.addAll(allCombatants);

        System.out.println("=== ROUND " + currentRound + " START ===");
        currentEntityTurn = turnQueue.poll();
        currentRound++;
    }

    public void advanceTurn() {
        // Push current hero onto history BEFORE moving on (only heroes, not mobs)
        if (currentEntityTurn != null) {
            TypeComponent type = tm.get(currentEntityTurn);
            if (type != null && type.type != Operator.MOB) {
                if (previousHeroes.size() >= MAX_HISTORY) {
                    // Remove oldest entry to keep stack bounded
                    previousHeroes.remove(0);
                }
                previousHeroes.push(currentEntityTurn);
            }
        }

        if (turnQueue.isEmpty()) {
            currentState = BattleState.ROUND_START;
        } else {
            // Poll exactly ONCE
            currentEntityTurn = turnQueue.poll();

            // NOTE: We do NOT force setState(WAIT_FOR_INPUT) here anymore!
            // BattleStateSystem will call determineWhoseTurn() to figure out
            // if it should be WAIT_FOR_INPUT or ENEMY_TURN.
        }
    }

    /**
     * Go back to the previous hero's turn.
     * Puts the current entity back at the FRONT of the queue and
     * restores the previous hero as the active entity.
     * Only works when the previous entity was a hero (not a mob).
     */
    public void goToPreviousTurn() {
        if (!canGoBack())
            return;

        // Put current entity back at the front of the queue
        // (cast to LinkedList to use addFirst)
        ((LinkedList<Entity>) turnQueue).addFirst(currentEntityTurn);

        // Pop the previous hero and make them current
        currentEntityTurn = previousHeroes.pop();
        setState(BattleState.WAIT_FOR_INPUT);

        if (sm.has(currentEntityTurn)) {
            System.out.println(">> Went back to: " + sm.get(currentEntityTurn).name.trim());
        }
    }

    /**
     * Returns true if there is a previous hero to go back to,
     * and the current entity is a hero (can't go back during enemy turns).
     */
    public boolean canGoBack() {
        if (previousHeroes.isEmpty())
            return false;
        if (currentEntityTurn == null)
            return false;
        TypeComponent type = tm.get(currentEntityTurn);
        // Only allow going back when it's currently a hero's turn
        return type != null && type.type != Operator.MOB;
    }

    public void removeEntity(Entity entity) {
        // 1. Remove from the master list
        allEntities.remove(entity);

        // 2. Scrub them from the CURRENT round's queue
        if (turnQueue != null) {
            turnQueue.remove(entity);
        }

        // 3. Scrub them from the NEXT round's queue
        if (nextRoundQueue != null) {
            nextRoundQueue.remove(entity);
        }
    }

    // Inject entities mid battle
    public void addEntityMidBattle(Entity newEntity) {
        allEntities.add(newEntity);
        // Put them at the back of the line for the current round
        ((LinkedList<Entity>) turnQueue).addLast(newEntity);
        // Put them in the next round queue so the UI updates immediately!
        nextRoundQueue.add(newEntity);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    // Removed the helper methods

    // ── Accessors ─────────────────────────────────────────────────────────

    public Entity getCurrentEntityTurn() {
        return currentEntityTurn;
    }

    public void setState(BattleState s) {
        this.currentState = s;
    }

    public BattleState getState() {
        return currentState;
    }

    public List<Entity> getUpcomingTurnOrder() {
        List<Entity> ordered = new java.util.ArrayList<>();
        if (currentEntityTurn != null)
            ordered.add(currentEntityTurn);
        ordered.addAll(turnQueue);
        return ordered;
    }

    public List<Entity> getNextRoundTurnOrder() {
        return nextRoundQueue;
    }

    public List<Entity> getAllEntities() {
        return allEntities;
    }

    public int getCurrentRound() {
        return currentRound - 1;
    }

    // ── Save/Load Integration ─────────────────────────────────────────────
    public void captureQueuesForSave(io.github.jhundeniel.ArithmeticHeroes.data.SaveData data) {
        data.currentTurnQueue.clear();
        for (Entity e : turnQueue)
            data.currentTurnQueue.add(allEntities.indexOf(e));

        data.nextRoundQueue.clear();
        for (Entity e : nextRoundQueue)
            data.nextRoundQueue.add(allEntities.indexOf(e));

        data.currentEntityIndex = allEntities.indexOf(currentEntityTurn);
        data.currentRound = this.currentRound;
    }

    public void restoreQueuesFromSave(int currentIndex, java.util.List<Integer> turnQueueIndices,
            java.util.List<Integer> nextRoundIndices, int savedRound) {
        if (currentIndex < 0 && turnQueueIndices.isEmpty() && nextRoundIndices.isEmpty())
            return;

        // Restore the Round Number
        this.currentRound = savedRound;

        // Restore current turn
        this.currentEntityTurn = (currentIndex >= 0 && currentIndex < allEntities.size())
                ? allEntities.get(currentIndex)
                : null;

        // Restore this round
        this.turnQueue.clear();
        for (int idx : turnQueueIndices) {
            if (idx >= 0 && idx < allEntities.size())
                this.turnQueue.add(allEntities.get(idx));
        }

        // Restore next round
        this.nextRoundQueue.clear();
        for (int idx : nextRoundIndices) {
            if (idx >= 0 && idx < allEntities.size())
                this.nextRoundQueue.add(allEntities.get(idx));
        }

        // Resume from the saved turn — skip ROUND_START so the state machine
        // doesn't re-call startNextRound() and double-increment the round.
        this.currentState = BattleState.WAIT_FOR_INPUT;
    }
}
