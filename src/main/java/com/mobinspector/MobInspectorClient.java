package com.mobinspector;

import com.mobinspector.config.MobInspectorConfig;
import com.mobinspector.core.InspectionService;
import com.mobinspector.hud.MobInspectorHud;
import com.mobinspector.input.MobInspectorKeybindings;
import com.mobinspector.screen.SettingsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public final class MobInspectorClient implements ClientModInitializer {
    public static final String MOD_ID = "mobinspector";

    @Override
    public void onInitializeClient() {
        MobInspectorConfig.load();
        MobInspectorKeybindings.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MobInspectorKeybindings.poll(client);
            InspectionService.onTick(client);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> InspectionService.clear());

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.of(MOD_ID, "hud"), new MobInspectorHud());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("mobinspector").executes(ctx -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    mc.send(() -> mc.setScreen(new SettingsScreen(null)));
                    return 1;
                })));
    }
}
