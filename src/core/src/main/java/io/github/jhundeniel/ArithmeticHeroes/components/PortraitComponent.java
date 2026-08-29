package io.github.jhundeniel.ArithmeticHeroes.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Texture;

public class PortraitComponent implements Component {
    public Texture texture;

    public PortraitComponent(Texture texture){
        this.texture = texture;
    }
}
