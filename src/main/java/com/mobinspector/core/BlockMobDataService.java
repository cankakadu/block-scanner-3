package com.mobinspector.core;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Liest ausschliesslich Daten, die der Server tatsaechlich an den Client synchronisiert hat.
 * Fehlende Felder werden nicht erfunden, sondern weggelassen bzw. als nicht verfuegbar markiert.
 */
public final class BlockMobDataService {
    private BlockMobDataService() {}

    private static final String[][] SPAWNER_KEYS = {
            {"SpawnCount", "Spawn Count"}, {"MaxNearbyEntities", "Max Nearby"},
            {"RequiredPlayerRange", "Player Range"}, {"SpawnRange", "Spawn Range"},
            {"MinSpawnDelay", "Min Delay (t)"}, {"MaxSpawnDelay", "Max Delay (t)"}};
    private static final String[][] TRIAL_KEYS = {
            {"required_player_range", "Player Range"}, {"target_cooldown_length", "Cooldown (t)"}};

    public static Inspection.BlockMobData read(ClientWorld world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        String kind;
        String[][] keys;
        if (be instanceof MobSpawnerBlockEntity) { kind = "Spawner"; keys = SPAWNER_KEYS; }
        else if (be instanceof TrialSpawnerBlockEntity) { kind = "Trial Spawner"; keys = TRIAL_KEYS; }
        else return null;

        NbtCompound nbt;
        try {
            nbt = be.createNbt(world.getRegistryManager());
        } catch (RuntimeException ex) {
            return new Inspection.BlockMobData(kind, null, List.of());
        }

        List<Inspection.Field> fields = new ArrayList<>();
        for (String[] k : keys) {
            Optional<Integer> v = nbt.getInt(k[0]);
            if (v.isPresent()) fields.add(new Inspection.Field(k[1], String.valueOf(v.get())));
        }
        if (be instanceof MobSpawnerBlockEntity) {
            // Vanilla synchronisiert SpawnPotentials nicht zum Client -> ehrlich als nicht verfuegbar anzeigen.
            fields.add(new Inspection.Field("Spawn Potentials",
                    nbt.getList("SpawnPotentials").map(l -> String.valueOf(l.size())).orElse("N/A")));
        }
        return new Inspection.BlockMobData(kind, readEntityName(nbt), List.copyOf(fields));
    }

    private static String readEntityName(NbtCompound nbt) {
        for (String key : new String[]{"SpawnData", "spawn_data"}) {
            Optional<String> id = nbt.getCompound(key)
                    .flatMap(c -> c.getCompound("entity"))
                    .flatMap(e -> e.getString("id"));
            if (id.isPresent()) {
                Identifier ident = Identifier.tryParse(id.get());
                if (ident != null && Registries.ENTITY_TYPE.containsId(ident)) {
                    return Registries.ENTITY_TYPE.get(ident).getName().getString();
                }
                return id.get();
            }
        }
        return null;
    }
}
