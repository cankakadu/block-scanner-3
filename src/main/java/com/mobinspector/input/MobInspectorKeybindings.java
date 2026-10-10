package com.mobinspector.input;

import com.mobinspector.MobInspectorClient;
import com.mobinspector.config.MobInspectorConfig;
import com.mobinspector.screen.SettingsScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class MobInspectorKeybindings {
    private static KeyBinding toggle;
    private static KeyBinding settings;

    private MobInspectorKeybindings() {}

    public static void register() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MobInspectorClient.MOD_ID, "main"));
        toggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mobinspector.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_I, category));
        settings = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mobinspector.settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
    }

    /** Setzt nur die beiden Mob-Inspector-Tasten auf ihren Standard zurueck; fremde Belegungen bleiben unberuehrt. */
    public static void resetToDefaults(MinecraftClient client) {
        if (toggle == null || settings == null) return;
        toggle.setBoundKey(toggle.getDefaultKey());
        settings.setBoundKey(settings.getDefaultKey());
        KeyBinding.updateKeysByCode();
        client.options.write();
    }

    /** wasPressed() liefert pro physischem Tastendruck genau einmal true (kein Repeat bei gehaltener Taste). */
    public static void poll(MinecraftClient client) {
        if (client.player == null) return;
        while (toggle.wasPressed()) {
            MobInspectorConfig cfg = MobInspectorConfig.get();
            cfg.enabled = !cfg.enabled;
            cfg.revision++;
            cfg.save();
            client.player.sendMessage(Text.translatable(cfg.enabled
                    ? "message.mobinspector.on" : "message.mobinspector.off"), true);
        }
        while (settings.wasPressed()) {
            client.setScreen(new SettingsScreen(client.currentScreen));
        }
    }
}
