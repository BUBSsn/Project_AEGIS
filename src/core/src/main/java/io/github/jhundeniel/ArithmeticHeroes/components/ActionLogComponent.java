package io.github.jhundeniel.ArithmeticHeroes.components;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.ashley.core.Component;

/**
 * Pure-data component that stores action log messages for display.
 *
 * All queue-management logic (inserting at front, enforcing max size,
 * clearing) lives in ActionLogSystem — not here.
 */
public class ActionLogComponent implements Component {
    public List<String> messages = new ArrayList<>();
    public int maxMessages = 5;
}
