package com.mobinspector.core;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class TargetBlockService {
    public record Target(BlockPos pos, BlockState state) {}

    private TargetBlockService() {}

    /**
     * Offizieller Block-Raycast der Kamera-Entity (ignoriert Entities, damit das HUD
     * nicht verschwindet, wenn man direkt auf einen Mob zielt).
     */
    public static Target find(MinecraftClient client) {
        if (client.world == null || client.player == null) return null;
        Entity cam = client.getCameraEntity();
        if (cam == null) return null;
        HitResult hit = cam.raycast(client.player.getBlockInteractionRange(), 1.0f, false);
        if (hit instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK) {
            BlockPos p = b.getBlockPos();
            BlockState s = client.world.getBlockState(p);
            if (!s.isAir()) return new Target(p.toImmutable(), s);
        }
        return null;
    }
}
