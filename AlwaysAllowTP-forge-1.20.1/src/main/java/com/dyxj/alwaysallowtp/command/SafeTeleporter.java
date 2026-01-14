package com.dyxj.alwaysallowtp.command;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

public class SafeTeleporter {

    // 强化安全传送：玩家到玩家
    public static boolean safeTeleportPlayerToPlayer(ServerPlayer player, ServerPlayer target) {
        ServerLevel targetLevel = (ServerLevel) target.level();
        BlockPos targetPos = target.blockPosition();

        // 寻找安全位置
        Optional<BlockPos> safePos = findSafeTeleportPosition(targetLevel, targetPos);

        if (safePos.isPresent()) {
            return safeTeleportToPosition(player, targetLevel, safePos.get());
        } else {
            // 如果找不到安全位置，尝试传送但进行安全检查
            if (isPositionSafeForTeleport(targetLevel, targetPos)) {
                player.teleportTo(targetLevel,
                        target.getX(), target.getY(), target.getZ(),
                        target.getYRot(), target.getXRot());
                player.fallDistance = 0;
                player.setDeltaMovement(0, 0, 0);
                return true;
            } else {
                return false; // 位置不安全，拒绝传送
            }
        }
    }

    // 安全传送到家
    public static boolean safeTeleportToHome(ServerPlayer player) {
        Pair<ServerLevel, BlockPos> safeSpawn = findSafeSpawn(player);
        return safeTeleportToPosition(player, safeSpawn.getLeft(), safeSpawn.getRight());
    }

    // 安全传送到坐标
    public static boolean safeTeleportToLocation(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!isPositionSafeForTeleport(level, pos)) {
            return false; // 坐标位置不安全
        }

        return safeTeleportToPosition(player, level, pos);
    }

    // 核心传送方法
    private static boolean safeTeleportToPosition(ServerPlayer player, ServerLevel level, BlockPos pos) {
        Vec3 safeVec = getSafeTeleportVec(level, pos);

        // 最终安全检查
        BlockPos finalPos = BlockPos.containing(safeVec);
        if (!isPositionSafeForTeleport(level, finalPos)) {
            return false;
        }

        player.teleportTo(level,
                safeVec.x, safeVec.y, safeVec.z,
                player.getYRot(), player.getXRot());
        player.fallDistance = 0;
        player.setDeltaMovement(0, 0, 0);
        return true;
    }

    public static Pair<ServerLevel, BlockPos> findSafeSpawn(ServerPlayer player) {
        ServerLevel spawnLevel = player.server.getLevel(player.getRespawnDimension());
        BlockPos spawnPos = player.getRespawnPosition();

        if (spawnPos == null) {
            spawnLevel = player.server.overworld();
            spawnPos = spawnLevel.getSharedSpawnPos();
        }

        Optional<BlockPos> safePos = findSafeTeleportPosition(spawnLevel, spawnPos);
        return Pair.of(spawnLevel, safePos.orElse(spawnPos.above(3)));
    }

    // 增强的安全位置查找
    public static Optional<BlockPos> findSafeTeleportPosition(ServerLevel level, BlockPos center) {
        // 优先检查目标位置周围的安全点
        int searchRadius = 8;
        int searchHeight = 10;

        // 首先检查目标位置上下
        for (int yOffset = -3; yOffset <= 3; yOffset++) {
            BlockPos checkPos = center.offset(0, yOffset, 0);
            if (isPositionSafeForTeleport(level, checkPos)) {
                return Optional.of(checkPos);
            }
        }

        // 螺旋搜索周围区域
        for (int radius = 1; radius <= searchRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) == radius || Math.abs(z) == radius) {
                        for (int y = -searchHeight; y <= searchHeight; y++) {
                            BlockPos checkPos = center.offset(x, y, z);
                            if (isPositionSafeForTeleport(level, checkPos)) {
                                return Optional.of(checkPos);
                            }
                        }
                    }
                }
            }
        }

        // 如果找不到，尝试在Y轴上寻找安全高度
        for (int y = level.getMinBuildHeight() + 5; y < level.getMaxBuildHeight() - 5; y++) {
            BlockPos checkPos = new BlockPos(center.getX(), y, center.getZ());
            if (isPositionSafeForTeleport(level, checkPos)) {
                return Optional.of(checkPos);
            }
        }

        return Optional.empty();
    }

    // 增强的安全检查
    public static boolean isPositionSafeForTeleport(ServerLevel level, BlockPos pos) {
        // 检查Y坐标范围
        if (pos.getY() < level.getMinBuildHeight() + 2 || pos.getY() >= level.getMaxBuildHeight() - 3) {
            return false;
        }

        BlockState feetBlock = level.getBlockState(pos);
        BlockState belowBlock = level.getBlockState(pos.below());
        BlockState aboveBlock = level.getBlockState(pos.above());
        BlockState twoAboveBlock = level.getBlockState(pos.above(2));

        // 检查下方是否有支撑
        if (!isSupportBlock(belowBlock)) {
            return false;
        }

        // 检查脚下是否危险
        if (isDangerousBlock(feetBlock)) {
            return false;
        }

        // 检查下方是否危险
        if (isDangerousBlock(belowBlock)) {
            return false;
        }

        // 检查站立位置和上方是否有足够空间
        if (!feetBlock.isAir() || !aboveBlock.isAir() || !twoAboveBlock.isAir()) {
            return false;
        }

        // 检查周围环境（防止悬崖边缘）
        int unsafeEdges = 0;
        BlockPos[] adjacent = {
                pos.north(), pos.south(), pos.east(), pos.west(),
                pos.north().below(), pos.south().below(), pos.east().below(), pos.west().below()
        };

        for (BlockPos adjPos : adjacent) {
            BlockState adjState = level.getBlockState(adjPos);
            if (!adjState.isSolid() && !isDangerousBlock(adjState)) {
                unsafeEdges++;
            }
        }

        // 如果超过一半的相邻位置不安全，认为位置危险
        if (unsafeEdges > 4) {
            return false;
        }

        return true;
    }

    private static boolean isSupportBlock(BlockState state) {
        return state.isSolid() ||
                state.is(Blocks.WATER) ||
                state.is(Blocks.ICE) ||
                state.is(Blocks.PACKED_ICE) ||
                state.is(Blocks.BLUE_ICE);
    }

    private static boolean isDangerousBlock(BlockState state) {
        return state.is(Blocks.LAVA) ||
                state.is(Blocks.FIRE) ||
                state.is(Blocks.SOUL_FIRE) ||
                state.is(Blocks.CACTUS) ||
                state.is(Blocks.SWEET_BERRY_BUSH) ||
                state.is(Blocks.WITHER_ROSE) ||
                state.is(Blocks.MAGMA_BLOCK) ||
                state.is(Blocks.POWDER_SNOW) ||
                state.is(Blocks.WITHER_ROSE) ||
                state.is(Blocks.VOID_AIR) ||
                state.is(Blocks.CAVE_AIR) && state.getBlock() != Blocks.AIR; // 空气之外的虚空
    }

    private static Vec3 getSafeTeleportVec(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        double y = pos.getY();

        BlockPos checkPos = BlockPos.containing(x, y, z);

        // 向下寻找支撑面
        while (y > level.getMinBuildHeight() + 2 && !isSupportBlock(level.getBlockState(checkPos.below()))) {
            y--;
            checkPos = BlockPos.containing(x, y, z);
        }

        // 向上调整以确保站立位置是空气
        while (y < level.getMaxBuildHeight() - 3 && !level.getBlockState(checkPos).isAir()) {
            y++;
            checkPos = BlockPos.containing(x, y, z);
        }

        // 确保头顶有足够空间
        while (y < level.getMaxBuildHeight() - 3 &&
                (!level.getBlockState(checkPos.above()).isAir() ||
                        !level.getBlockState(checkPos.above(2)).isAir())) {
            y++;
            checkPos = BlockPos.containing(x, y, z);
        }

        return new Vec3(x, Math.max(y, level.getMinBuildHeight() + 2), z);
    }
}