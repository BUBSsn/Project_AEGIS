package io.github.jhundeniel.ArithmeticHeroes.components;

import com.badlogic.ashley.core.Component;

public class StatsComponent implements Component {
    public int hp, maxHp, mana, maxMana;
    public String name;

    //FOR THE PARTY MEMBERS
    public StatsComponent(String name, int hp, int mana) {
        this.name = name;
        this.hp = this.maxHp = hp;
        this.mana = this.maxMana = mana;
    }

    //FOR THE ENEMIES
    public StatsComponent(String name, int hp) {
        this.name = name;
        this.hp = this.maxHp = hp;
    }
}
