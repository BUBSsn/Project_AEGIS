package io.github.jhundeniel.ArithmeticHeroes.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.jhundeniel.ArithmeticHeroes.data.DialogueLine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and provides access to dialogue events and combat chat lines.
 *
 * Dialogue events are stored in assets/data/dialogues.json.
 * Combat lines are stored in assets/data/combat_lines.json.
 */
public class DialogueManager {

    private final Map<String, List<DialogueLine>> events = new HashMap<>();
    private final Map<String, Map<String, List<String>>> combatLines = new HashMap<>();

    public DialogueManager() {
        loadDialogues();
        loadCombatLines();
    }

    // ── Dialogue Events ───────────────────────────────────────────────────

    private void loadDialogues() {
        if (!Gdx.files.internal("data/dialogues.json").exists()) {
            System.out.println("[DialogueManager] No dialogues.json found — skipping.");
            return;
        }

        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(Gdx.files.internal("data/dialogues.json"));
        JsonValue eventsNode = root.get("events");

        if (eventsNode == null) return;

        for (JsonValue eventEntry = eventsNode.child; eventEntry != null; eventEntry = eventEntry.next) {
            String eventId = eventEntry.name;
            List<DialogueLine> lines = new ArrayList<>();

            for (JsonValue lineNode = eventEntry.child; lineNode != null; lineNode = lineNode.next) {
                DialogueLine line = new DialogueLine();
                line.speaker = lineNode.getString("speaker", "???");
                line.side    = lineNode.getString("side", "LEFT");
                line.text    = lineNode.getString("text", "...");
                lines.add(line);
            }

            events.put(eventId, lines);
        }

        System.out.println("[DialogueManager] Loaded " + events.size() + " dialogue events.");
    }

    /**
     * Returns the dialogue lines for the given event ID, or null if not found.
     */
    public List<DialogueLine> getEvent(String eventId) {
        if (eventId == null || eventId.equals("NONE")) return null;
        return events.get(eventId);
    }

    // ── Combat Chat Lines ──────────────────────────────────────────────────

    private void loadCombatLines() {
        if (!Gdx.files.internal("data/combat_lines.json").exists()) {
            System.out.println("[DialogueManager] No combat_lines.json found — skipping.");
            return;
        }

        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(Gdx.files.internal("data/combat_lines.json"));

        for (JsonValue triggerNode = root.child; triggerNode != null; triggerNode = triggerNode.next) {
            String trigger = triggerNode.name;
            Map<String, List<String>> keyMap = new HashMap<>();

            for (JsonValue keyEntry = triggerNode.child; keyEntry != null; keyEntry = keyEntry.next) {
                String key = keyEntry.name;
                List<String> lineList = new ArrayList<>();

                for (JsonValue lineVal = keyEntry.child; lineVal != null; lineVal = lineVal.next) {
                    lineList.add(lineVal.asString());
                }

                keyMap.put(key, lineList);
            }

            combatLines.put(trigger, keyMap);
        }

        System.out.println("[DialogueManager] Loaded " + combatLines.size() + " combat line categories.");
    }

    /**
     * Returns a random combat line for the given trigger and key.
     * Example: getCombatLine("onSkillUse", "HEAL") → "Hold still!"
     *
     * @return a random line, or null if no lines exist for that trigger/key.
     */
    public String getCombatLine(String trigger, String key) {
        Map<String, List<String>> keyMap = combatLines.get(trigger);
        if (keyMap == null) return null;

        List<String> lines = keyMap.get(key);
        if (lines == null || lines.isEmpty()) return null;

        int index = (int) (Math.random() * lines.size());
        return lines.get(index);
    }
}
