package io.github.jhundeniel.ArithmeticHeroes.data;

/**
 * A single line of dialogue in a cutscene event.
 * Deserialized from dialogues.json.
 */
public class DialogueLine {
    /** Display name of the speaker (e.g., "Addition", "???", "Boss"). */
    public String speaker;

    /** Which side of the screen the speaker label appears: "LEFT" or "RIGHT". */
    public String side;

    /** The dialogue text to display. */
    public String text;
}
