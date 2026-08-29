package io.github.jhundeniel.ArithmeticHeroes.components;

import com.badlogic.ashley.core.Component;

public class PartyComponent implements Component {
    public boolean isPlayer;

    public PartyComponent(boolean isPlayer) {
        this.isPlayer = isPlayer;
    }
}
