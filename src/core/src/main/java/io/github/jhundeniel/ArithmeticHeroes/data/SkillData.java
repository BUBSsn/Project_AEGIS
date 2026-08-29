package io.github.jhundeniel.ArithmeticHeroes.data;

import io.github.jhundeniel.ArithmeticHeroes.components.ActionRequestComponent;

public class SkillData {
    public String key;
    public String name;
    public String description;
    public ActionRequestComponent.ActionType type;

    // COSTS
    public int manaCost = 0;
    public float hpCostPct = 0f;  // 0.10 = 10% HP

    // VALUES
    public float value = 0f;      // Main number (Damage, Heal, Buff %)
    public float secondaryValue = 0f; // For complex skills (Group Burden 3-man rate)

    // RANGES (For random skills like Amplify)
    public float min = 0f;
    public float max = 0f;

    // FLAGS
    public boolean isGroup = false;
    public int duration = 0;      // For buffs/debuffs

    //Empty constructor for JSON parsing
    public SkillData(){}
}
