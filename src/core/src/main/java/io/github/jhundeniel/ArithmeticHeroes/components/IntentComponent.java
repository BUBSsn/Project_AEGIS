package io.github.jhundeniel.ArithmeticHeroes.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class IntentComponent implements Component {
    public ActionRequestComponent.ActionType actionType;
    public Entity target;
    public boolean isAttack;

    public IntentComponent(ActionRequestComponent.ActionType actionType, Entity target, boolean isAttack){
        this.actionType = actionType;
        this.target = target;
        this.isAttack = isAttack;
    }
}
