package com.mobinspector.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MobInspectorConfig {
    public static final String[] ACCENT_NAMES = {"Emerald", "Cyan", "Violet", "Amber", "Rose"};
    public static final int[] ACCENT_RGB = {0x3DDC97, 0x38BDF8, 0xA78BFA, 0xFBBF24, 0xFB7185};
    public static final String[] ANCHOR_NAMES = {"Top Left", "Top Right", "Bottom Left", "Bottom Right"};
    public static final String[] ANIM_NAMES = {"Off", "Subtle", "Full"};
    public static final String[] FILTER_NAMES = {"All Mobs", "Hostile", "Non-Hostile"};
    public static final String[] AREA_NAMES = {"Center Point", "Overlap"};
    public static final String[] MODE_NAMES = {"On The Block", "In The Block", "Both"};

    public boolean enabled = true;
    public float scale = 1.0f;
    public int opacity = 72;          // Prozent
    public int accent = 0;
    public boolean showTarget = true;
    public boolean showMobCount = true;
    public boolean showBreakdown = true;
    public boolean showBlockData = true;
    public int anchor = 0;
    public int offsetX = 8;
    public int offsetY = 8;
    public int animation = 1;
    public int countFilter = 0;
    public boolean footprintMode = false;
    /** 0 = On The Block, 1 = In The Block, 2 = Both (Default entspricht dem bisherigen Verhalten). */
    public int detectionMode = 2;

    /** Wird bei jeder Aenderung erhoeht, damit das HUD sein Layout neu aufbaut. */
    public transient int revision;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static MobInspectorConfig instance = new MobInspectorConfig();

    public static MobInspectorConfig get() { return instance; }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("mobinspector.json");
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try {
                MobInspectorConfig c = GSON.fromJson(Files.readString(p), MobInspectorConfig.class);
                if (c != null) instance = c;
            } catch (Exception ignored) {
                instance = new MobInspectorConfig();
            }
        }
        instance.clamp();
        instance.save();
    }

    public void clamp() {
        scale = Math.max(0.5f, Math.min(2.0f, scale));
        opacity = Math.max(10, Math.min(100, opacity));
        accent = Math.floorMod(accent, ACCENT_RGB.length);
        anchor = Math.floorMod(anchor, ANCHOR_NAMES.length);
        animation = Math.floorMod(animation, ANIM_NAMES.length);
        countFilter = Math.floorMod(countFilter, FILTER_NAMES.length);
        detectionMode = Math.floorMod(detectionMode, MODE_NAMES.length);
        offsetX = Math.max(0, Math.min(400, offsetX));
        offsetY = Math.max(0, Math.min(400, offsetY));
    }

    public void save() {
        clamp();
        try {
            Files.writeString(path(), GSON.toJson(this));
        } catch (IOException ignored) {
        }
    }

    /**
     * Setzt alle gespeicherten Einstellungen auf die Feld-Initialisierer (= zentrale Standardwerte) zurueck.
     * Per Reflection, damit neue Felder automatisch mit abgedeckt sind. Die Instanz bleibt dieselbe.
     */
    public void resetToDefaults() {
        MobInspectorConfig d = new MobInspectorConfig();
        for (java.lang.reflect.Field f : MobInspectorConfig.class.getDeclaredFields()) {
            int m = f.getModifiers();
            if (java.lang.reflect.Modifier.isStatic(m) || java.lang.reflect.Modifier.isTransient(m)) continue;
            try {
                f.set(this, f.get(d));
            } catch (IllegalAccessException ignored) {
            }
        }
        revision++;
        save();
    }

    public int accentRgb() { return ACCENT_RGB[accent]; }
}
