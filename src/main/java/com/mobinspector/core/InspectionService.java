package com.mobinspector.core;

import com.mobinspector.config.MobInspectorConfig;
import com.mobinspector.screen.SettingsScreen;
import net.minecraft.client.MinecraftClient;

import java.util.List;

public final class InspectionService {
    private static volatile Inspection current;
    private static int tick;
    private static int lastRevision = -1;

    private InspectionService() {}

    public static Inspection current() { return current; }

    public static void clear() { current = null; }

    /** Wird pro Client-Tick aufgerufen; die teure Entity-Abfrage laeuft nur alle 4 Ticks (5 Hz). */
    public static void onTick(MinecraftClient client) {
        MobInspectorConfig cfg = MobInspectorConfig.get();
        if (!cfg.enabled || client.world == null || client.player == null || (client.currentScreen != null && !(client.currentScreen instanceof SettingsScreen))) {
            current = null;
            return;
        }
        TargetBlockService.Target t = TargetBlockService.find(client);
        if (t == null) { current = null; return; }

        Inspection prev = current;
        boolean samePos = prev != null && prev.pos().equals(t.pos());
        if (samePos && cfg.revision == lastRevision && (++tick & 3) != 0) return;
        lastRevision = cfg.revision;
        tick = 0;

        int mode = cfg.detectionMode;
        int count = -1;
        List<Inspection.Entry> breakdown = List.of();
        if (mode != 1) { // On The Block / Both
            MobCountingService.Result r = MobCountingService.count(client.world, t.pos(), t.state(), cfg);
            count = r.total();
            breakdown = r.breakdown();
        }
        int inside = -1;
        List<Inspection.Entry> insideBreakdown = List.of();
        Inspection.BlockMobData data = null;
        if (mode != 0) { // In The Block / Both: echte Entities im Blockvolumen + ggf. Spawner-/Blockdaten
            MobCountingService.Result in = MobCountingService.countInside(client.world, t.pos(), cfg);
            inside = in.total();
            insideBreakdown = in.breakdown();
            data = BlockMobDataService.read(client.world, t.pos());
        }
        Inspection next = new Inspection(t.pos(), t.state().getBlock().getName().getString(), mode,
                count, breakdown, inside, insideBreakdown, data, !client.isInSingleplayer());
        if (!next.equals(prev)) current = next;
    }
}
