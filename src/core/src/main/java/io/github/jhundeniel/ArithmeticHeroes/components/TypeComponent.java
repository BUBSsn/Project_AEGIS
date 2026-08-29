package io.github.jhundeniel.ArithmeticHeroes.components;

import com.badlogic.ashley.core.Component;

public class TypeComponent implements Component{
    public Operator type;

    /** The CharacterRegistry key (e.g. "HERO_ADDITION", "ENEMY_MOB1"). */
    public String registryKey;

    public TypeComponent(){
    }

    public TypeComponent(Operator type){
        this.type = type;
    }

    public TypeComponent(Operator type, String registryKey){
        this.type = type;
        this.registryKey = registryKey;
    }
}

