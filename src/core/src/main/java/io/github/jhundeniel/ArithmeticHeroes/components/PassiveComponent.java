package io.github.jhundeniel.ArithmeticHeroes.components;

import com.badlogic.ashley.core.Component;
import io.github.jhundeniel.ArithmeticHeroes.passives.Passive;

public class PassiveComponent implements Component {
    public Passive passive;

    public PassiveComponent(Passive passive) {
        this.passive = passive;
    }
}
