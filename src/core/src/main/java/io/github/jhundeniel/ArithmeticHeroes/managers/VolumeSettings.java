package io.github.jhundeniel.ArithmeticHeroes.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Singleton that stores and persists music/SFX volume levels.
 *
 * <p>
 * Uses {@link Preferences} (sandboxed key-value store provided by LibGDX)
 * so no raw filesystem paths are needed (rules.md Rule 1).
 * </p>
 *
 * <p>
 * Both volumes are stored as floats in [0.0, 1.0].
 * Changes are applied immediately but only flushed to disk when
 * {@link #save()} is called (typically on slider release).
 * </p>
 */
public class VolumeSettings {

    private static final String PREFS_NAME = "ArithmeticHeroesSettings";
    private static final String KEY_MUSIC  = "musicVolume";
    private static final String KEY_SFX    = "sfxVolume";

    private static final float DEFAULT_MUSIC = 0.5f;
    private static final float DEFAULT_SFX   = 1.0f;

    private static VolumeSettings instance;

    private float musicVolume;
    private float sfxVolume;

    private VolumeSettings() {
        load();
    }

    /** Returns the singleton instance, creating it on first call. */
    public static VolumeSettings getInstance() {
        if (instance == null) {
            instance = new VolumeSettings();
        }
        return instance;
    }

    // ── Getters ────────────────────────────────────────────────────────

    public float getMusicVolume() {
        return musicVolume;
    }

    public float getSfxVolume() {
        return sfxVolume;
    }

    // ── Setters (apply immediately, but do NOT flush to disk) ──────────

    public void setMusicVolume(float volume) {
        this.musicVolume = clamp01(volume);
    }

    public void setSfxVolume(float volume) {
        this.sfxVolume = clamp01(volume);
    }

    // ── Persistence ────────────────────────────────────────────────────

    /** Load volumes from LibGDX Preferences. Safe to call before Gdx.app is ready. */
    public void load() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
            musicVolume = clamp01(prefs.getFloat(KEY_MUSIC, DEFAULT_MUSIC));
            sfxVolume   = clamp01(prefs.getFloat(KEY_SFX,   DEFAULT_SFX));
        } catch (Exception e) {
            // Gdx.app may not be ready yet (e.g. during unit tests)
            musicVolume = DEFAULT_MUSIC;
            sfxVolume   = DEFAULT_SFX;
        }
    }

    /** Flush current volumes to disk. Call on slider release, not every frame. */
    public void save() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
            prefs.putFloat(KEY_MUSIC, musicVolume);
            prefs.putFloat(KEY_SFX,   sfxVolume);
            prefs.flush();
        } catch (Exception e) {
            System.err.println("[VolumeSettings] Failed to save preferences: " + e.getMessage());
        }
    }

    // ── Utility ────────────────────────────────────────────────────────

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
