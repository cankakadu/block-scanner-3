package com.mobinspector.core;

import com.mobinspector.config.MobInspectorConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MobCountingService {
    public record Result(int total, List<Inspection.Entry> breakdown) {}

    private static final double FEET_TOLERANCE = 0.12;
    private static final double INSIDE_EPS = 0.01;

    private MobCountingService() {}

    /**
     * Zaehlt lebende MobEntities (keine Spieler/Items/Projektile), die auf der Oberseite des Blocks stehen:
     * am Boden (onGround), Fuesse auf Hoehe der Kollisions-Oberkante, und entweder Mittelpunkt in der
     * Blocksaeule oder (Overlap-Modus) Bounding Box ueberlappt die Saeule. Jede Entity wird nur einmal gezaehlt.
     * Raeumliche Abfrage ueber world.getEntitiesByClass mit kleiner Box.
     */
    public static Result count(ClientWorld world, BlockPos pos, BlockState state, MobInspectorConfig cfg) {
        VoxelShape shape = state.getCollisionShape(world, pos);
        double top = shape.isEmpty() ? pos.getY() + 1.0 : pos.getY() + shape.getMax(Direction.Axis.Y);
        Box search = new Box(pos.getX(), top - 0.25, pos.getZ(), pos.getX() + 1.0, top + 0.25, pos.getZ() + 1.0);

        List<MobEntity> found = world.getEntitiesByClass(MobEntity.class, search, e -> accept(e, pos, top, cfg));
        return toResult(found);
    }

    /**
     * "In The Block": alle lebenden MobEntities, deren Bounding Box das Volumen der Blockzelle schneidet
     * (voll oder teilweise, egal ob am Boden, im Sand versteckt oder schwebend). Entities, die nur auf der
     * Oberseite stehen oder an einer Flaeche anliegen, zaehlen nicht (Schnitt muss > INSIDE_EPS tief sein).
     * Raeumliche Abfrage nur ueber die Blockzelle; jede Entity wird einmal geliefert.
     */
    public static Result countInside(ClientWorld world, BlockPos pos, MobInspectorConfig cfg) {
        final double e = INSIDE_EPS;
        Box cell = new Box(pos.getX() + e, pos.getY() + e, pos.getZ() + e,
                pos.getX() + 1.0 - e, pos.getY() + 1.0 - e, pos.getZ() + 1.0 - e);
        List<MobEntity> found = world.getEntitiesByClass(MobEntity.class, cell,
                en -> en.isAlive() && !en.isSpectator() && passesFilter(en, cfg));
        return toResult(found);
    }

    private static boolean passesFilter(MobEntity e, MobInspectorConfig cfg) {
        boolean hostile = e instanceof Monster;
        if (cfg.countFilter == 1 && !hostile) return false;
        return !(cfg.countFilter == 2 && hostile);
    }

    private static Result toResult(List<MobEntity> found) {
        Map<EntityType<?>, Integer> byType = new LinkedHashMap<>();
        for (MobEntity e : found) byType.merge(e.getType(), 1, Integer::sum);
        List<Inspection.Entry> list = new ArrayList<>(byType.size());
        byType.forEach((t, n) -> list.add(new Inspection.Entry(t.getName().getString(), n)));
        list.sort(Comparator.comparingInt(Inspection.Entry::count).reversed()
                .thenComparing(Inspection.Entry::name));
        return new Result(found.size(), List.copyOf(list));
    }

    private static boolean accept(MobEntity e, BlockPos pos, double top, MobInspectorConfig cfg) {
        if (!e.isAlive() || e.isSpectator() || !e.isOnGround() || !passesFilter(e, cfg)) return false;

        Box bb = e.getBoundingBox();
        if (Math.abs(bb.minY - top) > FEET_TOLERANCE) return false;

        if (cfg.footprintMode) {
            return bb.maxX > pos.getX() && bb.minX < pos.getX() + 1
                    && bb.maxZ > pos.getZ() && bb.minZ < pos.getZ() + 1;
        }
        return MathHelper.floor(e.getX()) == pos.getX() && MathHelper.floor(e.getZ()) == pos.getZ();
    }
}
