package com.mobinspector.core;

import net.minecraft.util.math.BlockPos;

import java.util.List;

/** Unveraenderlicher Snapshot der Inspektion; wird vom Tick geschrieben und vom HUD gelesen. */
/** mode: 0 On, 1 In, 2 Both. mobCount/insideCount = -1 und blockData = null, wenn fuer den Modus nicht ermittelt. */
public record Inspection(BlockPos pos, String blockName, int mode, int mobCount, List<Entry> breakdown,
                         int insideCount, List<Entry> insideBreakdown, BlockMobData blockData, boolean multiplayer) {
    public record Entry(String name, int count) {}

    public record Field(String label, String value) {}

    /** storedMobs ist absichtlich nicht vorhanden: Es gibt keine verlaessliche Quelle dafuer (-> immer N/A). */
    public record BlockMobData(String kind, String spawnEntity, List<Field> fields) {}
}
