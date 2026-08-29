package io.github.jhundeniel.ArithmeticHeroes.managers;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;

public class ArithmeticAssetManager {
    public final AssetManager manager = new AssetManager();

    // ── Hero idle sheets ──────────────────────────────────────────
    public static final String ANIM_HERO_ADD = "sprites/addition_animation-Sheet.png";
    public static final String ANIM_HERO_SUB = "sprites/subtraction_animation-Sheet.png";
    public static final String ANIM_HERO_MUL = "sprites/multiplication_animation-Sheet.png";
    public static final String ANIM_HERO_DIV = "sprites/division_animation-Sheet.png";
    public static final int HERO_ADD_FRAMES = 13;
    public static final int HERO_SUB_FRAMES = 12;
    public static final int HERO_MUL_FRAMES = 13;
    public static final int HERO_DIV_FRAMES = 13;
    public static final float HERO_IDLE_FRAME_DUR = 0.10f;

    // ── Enemy / Boss idle sheets (13 frames, 2000×153) ────────────
    public static final String ANIM_MOB1 = "sprites/mob_1_animation-Sheet.png";
    public static final String ANIM_MOB2 = "sprites/mob_2_animation-Sheet.png";
    public static final String ANIM_MOB3 = "sprites/mob_3_animation-Sheet.png";
    public static final String ANIM_MOB4 = "sprites/mob_4_animation-Sheet.png";
    public static final String ANIM_MOB5 = "sprites/mob_5_animation-Sheet.png";
    public static final String ANIM_BOSS1 = "sprites/boss_1_animation-Sheet.png";
    public static final String ANIM_BOSS2 = "sprites/boss_2_animation-Sheet.png";
    public static final String ANIM_BOSS3 = "sprites/boss_3_animation-Sheet.png";
    public static final int ENEMY_FRAMES = 13;
    public static final float ENEMY_IDLE_FRAME_DUR = 0.10f;

    // ── Boss attack sheets ────────────────────────────────────────
    public static final String ANIM_BOSS1_ATTACK = "sprites/boss_1_attack_animation-Sheet.png";
    public static final String ANIM_BOSS2_ATTACK = "sprites/boss_2_attack_animation-Sheet.png";
    public static final String ANIM_BOSS3_ATTACK = "sprites/boss_3_attack_animation-Sheet.png";
    public static final int BOSS1_ATTACK_FRAMES = 22;
    public static final int BOSS2_ATTACK_FRAMES = 21;
    public static final int BOSS3_ATTACK_FRAMES = 26;
    public static final float BOSS_ATTACK_FRAME_DUR = 0.04f; // Boss 1
    public static final float BOSS2_ATTACK_FRAME_DUR = 0.04f; // Boss 2
    public static final float BOSS3_ATTACK_FRAME_DUR = 0.06f; // Boss 3

    // ── Enemy head icons for turn-order display (170×170) ─────────
    public static final String ICON_MOB1 = "sprites/mob_1_head_icon.png";
    public static final String ICON_MOB2 = "sprites/mob_2_head_icon.png";
    public static final String ICON_MOB3 = "sprites/mob_3_head_icon.png";
    public static final String ICON_MOB4 = "sprites/mob_4_head_icon.png";
    public static final String ICON_MOB5 = "sprites/mob_5_head_icon.png";
    public static final String ICON_BOSS1 = "sprites/boss_1_head_icon.png";
    public static final String ICON_BOSS2 = "sprites/boss_2_head_icon.png";
    public static final String ICON_BOSS3 = "sprites/boss_3_head_icon.png";

    // ── Hero gravestones (animations) ─────────────────────────────
    public static final String GRAVE_ADD = "sprites/addition_grave-Sheet.png";
    public static final String GRAVE_SUB = "sprites/subtraction_grave-Sheet.png";
    public static final String GRAVE_MUL = "sprites/multiplication_grave-Sheet.png";
    public static final String GRAVE_DIV = "sprites/division_grave-Sheet.png";
    public static final int GRAVE_ADD_FRAMES = 10;
    public static final int GRAVE_SUB_FRAMES = 11;
    public static final int GRAVE_MUL_FRAMES = 10;
    public static final int GRAVE_DIV_FRAMES = 11;
    // ── Hero gravestones (static, shown after animation) ──────────
    public static final String GRAVE_ADD_STATIC = "sprites/addition_gravestone.png";
    public static final String GRAVE_SUB_STATIC = "sprites/subtraction_gravestone.png";
    public static final String GRAVE_MUL_STATIC = "sprites/multiplication_gravestone.png";
    public static final String GRAVE_DIV_STATIC = "sprites/division_gravestone.png";

    // ── Backgrounds ───────────────────────────────────────────────
    public static final String BG_STAGE_0 = "backgrounds/first.png";
    public static final String BG_STAGE_1 = "backgrounds/bg_stage_1.jpeg";
    public static final String BG_STAGE_2 = "backgrounds/bg_stage2.png";
    public static final String BG_STAGE_3 = "backgrounds/bg_stage3.png";

    // ── Static / portrait textures ────────────────────────────────
    public static final String CHAR_ADD = "sprites/add.gif";
    public static final String CHAR_SUB = "sprites/sub.gif";
    public static final String CHAR_MUL = "sprites/mul.gif";
    public static final String CHAR_DIV = "sprites/div.gif";
    public static final String CHAR_MOB1 = "sprites/mob1.gif";
    public static final String CHAR_MOB2 = "sprites/char_mob2.png";
    public static final String PORT_ADD = "sprites/p_add.png";
    public static final String PORT_SUB = "sprites/p_sub.png";
    public static final String PORT_MUL = "sprites/p_mul.png";
    public static final String PORT_DIV = "sprites/p_div.png";

    // ── Skill animations ──────────────────────────────────────────
    public static final String ANIM_HEAL = "sprites/Heal.png";
    public static final String ANIM_MANA_TRANSFER = "sprites/Mana_Transfer.png";
    public static final String ANIM_SHIELD = "sprites/Shield.png";
    public static final String ANIM_EQUALIZER = "sprites/Battle_Equalizer.png";
    public static final String ANIM_SQUARED = "sprites/Squared.png";
    public static final String ANIM_AMPLIFY = "sprites/Amplify.png";
    public static final String ANIM_INVERSE = "sprites/Inverse.png";
    public static final String ANIM_SLAM = "sprites/Slam.png";
    public static final String ANIM_POKE = "sprites/Poke.png";
    public static final String ANIM_LIFE_STEAL = "sprites/Life_Steal.png";
    public static final String ANIM_LIFE_STEAL_HEAL = "sprites/Life_Steal_Heal.png";
    public static final String ANIM_CONDITIONAL = "sprites/Conditional.png";
    public static final String ANIM_ADDITIONAL_BUFF = "sprites/Additional_Buff.png";
    // Add to SkillAnimConfig
    public static final int LIFE_STEAL_FRAMES = 9; // Life_Steal.png
    public static final float LIFE_STEAL_DUR = 0.07f;
    public static final int LIFE_STEAL_HEAL_FRAMES = 9; // Life_Steal_Heal.png
    public static final float LIFE_STEAL_HEAL_DUR = 0.07f;
    public static final int CONDITIONAL_FRAMES = 8; // Conditional.png
    public static final float CONDITIONAL_DUR = 0.08f;
    public static final int ADDITIONAL_BUFF_FRAMES = 12; // Additional_Buff.png
    public static final float ADDITIONAL_BUFF_DUR = 0.07f;

    // ── Music ─────────────────────────────────────────────────────────────
    public static final String BGM_TITLE = "sounds/title_screen.ogg";
    public static final String BGM_TUTORIAL_STAGES = "sounds/tutorial_stages.ogg";
    public static final String BGM_STAGE1_WAVE1 = "sounds/stage1_wave1.ogg";
    public static final String BGM_STAGE1_WAVE2 = "sounds/stage1_wave2.ogg";
    public static final String BGM_STAGE2_WAVE1 = "sounds/stage2_wave1.ogg";
    public static final String BGM_STAGE2_WAVE2 = "sounds/stage2_wave2.ogg";
    public static final String BGM_STAGE3_WAVE1 = "sounds/stage3_wave1.ogg";
    public static final String BGM_STAGE3_WAVE2 = "sounds/stage3_wave2.ogg";
    public static final String BGM_VICTORY = "sounds/victory.ogg";
    public static final String BGM_GAME_OVER = "sounds/game_over.ogg";

    // ── Sound Effects ─────────────────────────────────────────────────────
    public static final String SFX_BUTTON_BACK = "sounds/button1.ogg";
    public static final String SFX_BUTTON_HOVER = "sounds/button2.ogg";
    public static final String SFX_BUTTON_CLICK = "sounds/button3.ogg";

    // ── Buff / Passive Icons ──────────────────────────────────────────────
    public static final String ICON_ADDITIONAL_BUFF = "skills_icon/additional_buff_icon.png";
    public static final String ICON_AMPLIFY = "skills_icon/amplify_icon.png";
    public static final String ICON_BURDEN = "skills_icon/burden_icon.png";
    public static final String ICON_INVERSION = "skills_icon/inversion_icon.png";
    public static final String ICON_MULT_PASSIVE = "skills_icon/multiplication_passive_icon.png";
    public static final String ICON_COST_RED = "skills_icon/skill_cost_reduction_icon.png";
    public static final String ICON_SQUARED = "skills_icon/squared_icon.png";
    public static final String ICON_SUB_PASSIVE = "skills_icon/subtraction_passive_icon.png";
    public static final String ICON_REFLECT = "skills_icon/reflect_icon.png";

    // ── Sounds for Skills (Addition) ──────────────────────────────────────
    public static final String SFX_ADDITIONAL_BUFF = "sounds/addition_skill_sound/ADDITIONAL_BUFF.wav";
    public static final String SFX_HEAL = "sounds/addition_skill_sound/HEAL.wav";
    public static final String SFX_LIFE_SIPHON = "sounds/addition_skill_sound/LIFE_SIPHON.wav";
    public static final String SFX_MANA_STEAL = "sounds/addition_skill_sound/MANA_STEAL.wav";
    public static final String SFX_MANA_TRANSFER = "sounds/addition_skill_sound/MANA_TRANSFER.wav";
    public static final String SFX_SINGLE_DRAIN = "sounds/addition_skill_sound/SINGLE_DRAIN.wav";

    // ── Sounds for Skills (Division) ──────────────────────────────────────
    public static final String SFX_BATTLE_EQUALIZER_UNFAIR_BATTLE = "sounds/division_skill_sound/BATTLE_EQUALIZER_UNFAIR_BATTLE.wav";
    public static final String SFX_BURDEN = "sounds/division_skill_sound/BURDEN.wav";
    public static final String SFX_REFLECT = "sounds/division_skill_sound/REFLECT.wav";
    public static final String SFX_SKILL_COST_REDUCTION = "sounds/division_skill_sound/SKILL_COST_REDUCTION.wav";

    // ── Sounds for Skills (Multiplication) ────────────────────────────────
    public static final String SFX_AMPLIFY = "sounds/multiplication_skill_sound/AMPLIFY.wav";
    public static final String SFX_INVERSION = "sounds/multiplication_skill_sound/INVERSION.wav";
    public static final String SFX_SQUARED_POWER = "sounds/multiplication_skill_sound/SQUARED_POWER.wav";

    // ── Sounds for Skills (Subtraction) ───────────────────────────────────
    public static final String SFX_BLOOD_TRANSFER = "sounds/subtraction_skill_sound/BLOOD_TRANSFER.wav";
    public static final String SFX_CONDITIONAL_ATTACK = "sounds/subtraction_skill_sound/CONDITIONAL_ATTACK.wav";
    public static final String SFX_DEBT_TRANSFER = "sounds/subtraction_skill_sound/DEBT_TRANSFER.wav";
    public static final String SFX_LIFE_STEAL = "sounds/subtraction_skill_sound/LIFE_STEAL.wav";
    public static final String SFX_MANA_NUKE = "sounds/subtraction_skill_sound/MANA_NUKE.wav";
    public static final String SFX_SACRIFICE = "sounds/subtraction_skill_sound/SACRIFICE.wav";
    public static final String SFX_POKE = "sounds/subtraction_skill_sound/POKE.wav";
    public static final String SFX_SLAM = "sounds/subtraction_skill_sound/SLAM.wav";

    // ── Sounds for Combat Events ──────────────────────────────────────────
    public static final String SFX_ENEMY_AOE = "sounds/ENEMY_AOE.wav";
    public static final String SFX_ENEMY_BASIC = "sounds/ENEMY_BASIC.wav";
    public static final String SFX_ENEMY_DEATH = "sounds/ENEMY_DEATH.wav";
    public static final String SFX_ENEMY_HEAL = "sounds/ENEMY_HEAL.wav";
    public static final String SFX_ENEMY_HIT = "sounds/ENEMY_HIT.wav";
    public static final String SFX_ENEMY_SUPPORT_BUFF = "sounds/ENEMY_SUPPORT_BUFF.wav";
    public static final String SFX_HERO_DEAD = "sounds/HERO_DEAD.wav";
    public static final String SFX_HERO_HURT = "sounds/HERO_HURT.wav";

    // Audio Transition State
    private Music currentMusic;
    private Music nextMusic;
    private String currentMusicName;
    private String nextMusicName;
    private boolean nextMusicLooping;
    private float fadeTimer = 0f;
    private final float fadeDuration = 1.0f; // 1 second fade transition
    private boolean isFadingOut = false;
    private boolean isFadingIn = false;
    /** Reads from VolumeSettings so sliders take effect immediately. */
    private float getMaxVolume() {
        return VolumeSettings.getInstance().getMusicVolume();
    }

    public void queueImages() {
        // Hero idle
        manager.load(ANIM_HERO_ADD, Texture.class);
        manager.load(ANIM_HERO_SUB, Texture.class);
        manager.load(ANIM_HERO_MUL, Texture.class);
        manager.load(ANIM_HERO_DIV, Texture.class);

        // Enemy / Boss idle
        manager.load(ANIM_MOB1, Texture.class);
        manager.load(ANIM_MOB2, Texture.class);
        manager.load(ANIM_MOB3, Texture.class);
        manager.load(ANIM_MOB4, Texture.class);
        manager.load(ANIM_MOB5, Texture.class);
        manager.load(ANIM_BOSS1, Texture.class);
        manager.load(ANIM_BOSS2, Texture.class);
        manager.load(ANIM_BOSS3, Texture.class);

        // Boss attack sheets
        manager.load(ANIM_BOSS1_ATTACK, Texture.class);
        manager.load(ANIM_BOSS2_ATTACK, Texture.class);
        manager.load(ANIM_BOSS3_ATTACK, Texture.class);

        // Enemy head icons
        manager.load(ICON_MOB1, Texture.class);
        manager.load(ICON_MOB2, Texture.class);
        manager.load(ICON_MOB3, Texture.class);
        manager.load(ICON_MOB4, Texture.class);
        manager.load(ICON_MOB5, Texture.class);
        manager.load(ICON_BOSS1, Texture.class);
        manager.load(ICON_BOSS2, Texture.class);
        manager.load(ICON_BOSS3, Texture.class);

        // Gravestones (animation sheets)
        manager.load(GRAVE_ADD, Texture.class);
        manager.load(GRAVE_SUB, Texture.class);
        manager.load(GRAVE_MUL, Texture.class);
        manager.load(GRAVE_DIV, Texture.class);
        // Gravestones (static, shown after animation)
        manager.load(GRAVE_ADD_STATIC, Texture.class);
        manager.load(GRAVE_SUB_STATIC, Texture.class);
        manager.load(GRAVE_MUL_STATIC, Texture.class);
        manager.load(GRAVE_DIV_STATIC, Texture.class);

        // Backgrounds
        manager.load(BG_STAGE_0, Texture.class);
        manager.load(BG_STAGE_1, Texture.class);
        manager.load(BG_STAGE_2, Texture.class);
        manager.load(BG_STAGE_3, Texture.class);

        // Portraits / static
        manager.load(CHAR_ADD, Texture.class);
        manager.load(CHAR_SUB, Texture.class);
        manager.load(CHAR_MUL, Texture.class);
        manager.load(CHAR_DIV, Texture.class);
        manager.load(CHAR_MOB1, Texture.class);
        manager.load(CHAR_MOB2, Texture.class);
        manager.load(PORT_ADD, Texture.class);
        manager.load(PORT_SUB, Texture.class);
        manager.load(PORT_MUL, Texture.class);
        manager.load(PORT_DIV, Texture.class);

        // Skill animations
        manager.load(ANIM_HEAL, Texture.class);
        manager.load(ANIM_MANA_TRANSFER, Texture.class);
        manager.load(ANIM_SHIELD, Texture.class);
        manager.load(ANIM_EQUALIZER, Texture.class);
        manager.load(ANIM_SQUARED, Texture.class);
        manager.load(ANIM_AMPLIFY, Texture.class);
        manager.load(ANIM_INVERSE, Texture.class);
        manager.load(ANIM_SLAM, Texture.class);
        manager.load(ANIM_POKE, Texture.class);
        manager.load(ANIM_LIFE_STEAL, Texture.class);
        manager.load(ANIM_LIFE_STEAL_HEAL, Texture.class);
        manager.load(ANIM_CONDITIONAL, Texture.class);
        manager.load(ANIM_ADDITIONAL_BUFF, Texture.class);

        // Music
        manager.load(BGM_TITLE, Music.class);
        manager.load(BGM_TUTORIAL_STAGES, Music.class);
        manager.load(BGM_STAGE1_WAVE1, Music.class);
        manager.load(BGM_STAGE1_WAVE2, Music.class);
        manager.load(BGM_STAGE2_WAVE1, Music.class);
        manager.load(BGM_STAGE2_WAVE2, Music.class);
        manager.load(BGM_STAGE3_WAVE1, Music.class);
        manager.load(BGM_STAGE3_WAVE2, Music.class);
        manager.load(BGM_VICTORY, Music.class);
        manager.load(BGM_GAME_OVER, Music.class);

        manager.load(SFX_BUTTON_BACK, Sound.class);
        manager.load(SFX_BUTTON_HOVER, Sound.class);
        manager.load(SFX_BUTTON_CLICK, Sound.class);

        manager.load(ICON_ADDITIONAL_BUFF, Texture.class);
        manager.load(ICON_AMPLIFY, Texture.class);
        manager.load(ICON_BURDEN, Texture.class);
        manager.load(ICON_INVERSION, Texture.class);
        manager.load(ICON_MULT_PASSIVE, Texture.class);
        manager.load(ICON_COST_RED, Texture.class);
        manager.load(ICON_SQUARED, Texture.class);
        manager.load(ICON_SUB_PASSIVE, Texture.class);
        manager.load(ICON_REFLECT, Texture.class);

        manager.load(SFX_ADDITIONAL_BUFF, Sound.class);
        manager.load(SFX_HEAL, Sound.class);
        manager.load(SFX_LIFE_SIPHON, Sound.class);
        manager.load(SFX_MANA_STEAL, Sound.class);
        manager.load(SFX_MANA_TRANSFER, Sound.class);
        manager.load(SFX_SINGLE_DRAIN, Sound.class);
        manager.load(SFX_BATTLE_EQUALIZER_UNFAIR_BATTLE, Sound.class);
        manager.load(SFX_BURDEN, Sound.class);
        manager.load(SFX_REFLECT, Sound.class);
        manager.load(SFX_SKILL_COST_REDUCTION, Sound.class);
        manager.load(SFX_AMPLIFY, Sound.class);
        manager.load(SFX_INVERSION, Sound.class);
        manager.load(SFX_SQUARED_POWER, Sound.class);
        manager.load(SFX_BLOOD_TRANSFER, Sound.class);
        manager.load(SFX_CONDITIONAL_ATTACK, Sound.class);
        manager.load(SFX_DEBT_TRANSFER, Sound.class);
        manager.load(SFX_LIFE_STEAL, Sound.class);
        manager.load(SFX_MANA_NUKE, Sound.class);
        manager.load(SFX_SACRIFICE, Sound.class);
        manager.load(SFX_POKE, Sound.class);
        manager.load(SFX_SLAM, Sound.class);
        manager.load(SFX_ENEMY_AOE, Sound.class);
        manager.load(SFX_ENEMY_BASIC, Sound.class);
        manager.load(SFX_ENEMY_DEATH, Sound.class);
        manager.load(SFX_ENEMY_HEAL, Sound.class);
        manager.load(SFX_ENEMY_HIT, Sound.class);
        manager.load(SFX_ENEMY_SUPPORT_BUFF, Sound.class);
        manager.load(SFX_HERO_DEAD, Sound.class);
        manager.load(SFX_HERO_HURT, Sound.class);
    }

    public void playMusic(String name, boolean looping) {
        if (name == null) {
            stopMusic();
            return;
        }
        if (currentMusicName != null && currentMusicName.equals(name))
            return; // already playing
        if (nextMusicName != null && nextMusicName.equals(name))
            return; // already queued

        Music targetMusic = manager.get(name, Music.class);
        if (targetMusic == null)
            return;

        if (currentMusic != null && currentMusic.isPlaying()) {
            nextMusic = targetMusic;
            nextMusicName = name;
            nextMusicLooping = looping;
            isFadingOut = true;
            isFadingIn = false;
            fadeTimer = fadeDuration;
        } else {
            currentMusic = targetMusic;
            currentMusicName = name;
            currentMusic.setLooping(looping);
            currentMusic.setVolume(0f);
            currentMusic.play();
            isFadingIn = true;
            isFadingOut = false;
            fadeTimer = fadeDuration;
        }
    }

    public void stopMusic() {
        if (currentMusic != null && !isFadingOut) {
            nextMusic = null;
            nextMusicName = null;
            isFadingOut = true;
            isFadingIn = false;
            fadeTimer = fadeDuration;
        }
    }

    public void updateMusic(float delta) {
        if (isFadingOut) {
            fadeTimer -= delta;
            if (fadeTimer <= 0) {
                if (currentMusic != null) {
                    currentMusic.stop();
                }
                currentMusic = null;
                currentMusicName = null;
                isFadingOut = false;

                if (nextMusic != null) {
                    currentMusic = nextMusic;
                    currentMusicName = nextMusicName;
                    currentMusic.setLooping(nextMusicLooping);
                    currentMusic.setVolume(0f);
                    currentMusic.play();

                    nextMusic = null;
                    nextMusicName = null;

                    isFadingIn = true;
                    fadeTimer = fadeDuration;
                }
            } else {
                if (currentMusic != null) {
                    currentMusic.setVolume(getMaxVolume() * (fadeTimer / fadeDuration));
                }
            }
        } else if (isFadingIn) {
            fadeTimer -= delta;
            if (fadeTimer <= 0) {
                if (currentMusic != null) {
                    currentMusic.setVolume(getMaxVolume());
                }
                isFadingIn = false;
            } else {
                if (currentMusic != null) {
                    currentMusic.setVolume(getMaxVolume() * (1f - (fadeTimer / fadeDuration)));
                }
            }
        }
    }

    public void playSound(String name) {
        if (name != null) {
            Sound s = manager.get(name, Sound.class);
            if (s != null) {
                s.play(VolumeSettings.getInstance().getSfxVolume());
            }
        }
    }

    /**
     * Immediately applies the current music volume from VolumeSettings
     * to the playing track. Call this when the slider value changes.
     */
    public void applyMusicVolume() {
        if (currentMusic != null && !isFadingOut && !isFadingIn) {
            currentMusic.setVolume(getMaxVolume());
        }
    }

    public void finishLoading() {
        manager.finishLoading();
    }

    public Texture getTexture(String name) {
        return manager.get(name, Texture.class);
    }

    public void dispose() {
        manager.dispose();
    }
}
