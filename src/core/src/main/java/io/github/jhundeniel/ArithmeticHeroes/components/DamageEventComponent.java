package io.github.jhundeniel.ArithmeticHeroes.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class DamageEventComponent implements Component {
    public int amount;
    public Entity source;
    public boolean isTrueDamage;

    public DamageEventComponent(int amount, Entity source) {
        this.amount = amount;
        this.source = source;
        this.isTrueDamage = false;
    }

    public DamageEventComponent(int amount, Entity source, boolean isTrueDamage) {
        this.amount = amount;
        this.source = source;
        this.isTrueDamage = isTrueDamage;
    }
}
