package com.dyxj.alwaysallowtp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AllowTPCommand {

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 3000; // 3秒冷却时间

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        // 主命令：/tps - 安全传送系统
        dispatcher.register(Commands.literal("tps")
                .requires(source -> source.hasPermission(0)) // 所有玩家可用

                // /tps <targetPlayer> - 传送到目标玩家
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> teleportSelfToPlayer(
                                ctx,
                                ctx.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(ctx, "target")))

                        .then(Commands.argument("destination", EntityArgument.player())
                                .executes(ctx -> teleportPlayerToPlayer(
                                        ctx,
                                        EntityArgument.getPlayer(ctx, "target"),
                                        EntityArgument.getPlayer(ctx, "destination")))
                        )

                        .then(Commands.literal("home")
                                .executes(ctx -> teleportPlayerToHome(
                                        ctx,
                                        EntityArgument.getPlayer(ctx, "target")))
                        )

                        .then(Commands.argument("location", Vec3Argument.vec3())
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    // 检查权限：非作弊模式下不允许坐标传送
                                    if (!source.hasPermission(2)) {
                                        source.sendFailure(Component.translatable("command.tps.no_cheat"));
                                        return 0;
                                    }
                                    return teleportPlayerToLocation(
                                            ctx,
                                            EntityArgument.getPlayer(ctx, "target"),
                                            Vec3Argument.getVec3(ctx, "location"));
                                })
                        )
                )

                // /tps - 显示帮助信息
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() ->
                                    Component.translatable("command.tps.usage"),
                            false);
                    return 1;
                })
        );

        // 快捷命令：/tpshome - 传送到自己的家
        dispatcher.register(Commands.literal("tpshome")
                .requires(source -> source.hasPermission(0))
                .executes(ctx -> teleportSelfToHome(ctx))
        );

        // 快捷命令：/tpshere <玩家> - 让玩家传送到自己
        dispatcher.register(Commands.literal("tpshere")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> teleportPlayerToSelf(
                                ctx,
                                EntityArgument.getPlayer(ctx, "player"),
                                ctx.getSource().getPlayerOrException()))
                )
        );

        // 快捷命令：/tpstome <玩家> - 同tpshere的别名
        dispatcher.register(Commands.literal("tpstome")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> teleportPlayerToSelf(
                                ctx,
                                EntityArgument.getPlayer(ctx, "player"),
                                ctx.getSource().getPlayerOrException()))
                )
        );
    }

    // ========== 命令执行方法 ==========

    private static int teleportPlayerToPlayer(CommandContext<CommandSourceStack> ctx,
                                              ServerPlayer target,
                                              ServerPlayer destination) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        if (checkCooldown(target)) {
            if (SafeTeleporter.safeTeleportPlayerToPlayer(target, destination)) {
                source.sendSuccess(() ->
                                Component.translatable("command.tps.success",
                                        target.getName().getString(),
                                        destination.getName().getString()),
                        true);
                return 1;
            } else {
                source.sendFailure(Component.translatable("command.tps.unsafe"));
                return 0;
            }
        } else {
            source.sendFailure(Component.translatable("command.tps.cooldown"));
            return 0;
        }
    }

    private static int teleportSelfToPlayer(CommandContext<CommandSourceStack> ctx,
                                            ServerPlayer player,
                                            ServerPlayer destination) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        if (checkCooldown(player)) {
            if (SafeTeleporter.safeTeleportPlayerToPlayer(player, destination)) {
                source.sendSuccess(() ->
                                Component.translatable("command.tps.success_self",
                                        destination.getName().getString()),
                        true);
                return 1;
            } else {
                source.sendFailure(Component.translatable("command.tps.unsafe"));
                return 0;
            }
        } else {
            source.sendFailure(Component.translatable("command.tps.cooldown"));
            return 0;
        }
    }

    private static int teleportPlayerToSelf(CommandContext<CommandSourceStack> ctx,
                                            ServerPlayer player,
                                            ServerPlayer self) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        if (checkCooldown(player)) {
            if (SafeTeleporter.safeTeleportPlayerToPlayer(player, self)) {
                source.sendSuccess(() ->
                                Component.translatable("command.tps.success_here",
                                        player.getName().getString()),
                        true);
                return 1;
            } else {
                source.sendFailure(Component.translatable("command.tps.unsafe"));
                return 0;
            }
        } else {
            source.sendFailure(Component.translatable("command.tps.cooldown"));
            return 0;
        }
    }

    private static int teleportPlayerToHome(CommandContext<CommandSourceStack> ctx,
                                            ServerPlayer player) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        if (checkCooldown(player)) {
            if (SafeTeleporter.safeTeleportToHome(player)) {
                source.sendSuccess(() ->
                                Component.translatable("command.tps.home_success",
                                        player.getName().getString()),
                        true);
                return 1;
            } else {
                source.sendFailure(Component.translatable("command.tps.unsafe"));
                return 0;
            }
        } else {
            source.sendFailure(Component.translatable("command.tps.cooldown"));
            return 0;
        }
    }

    private static int teleportSelfToHome(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();

        if (checkCooldown(player)) {
            if (SafeTeleporter.safeTeleportToHome(player)) {
                source.sendSuccess(() ->
                                Component.translatable("command.tps.home_success_self"),
                        true);
                return 1;
            } else {
                source.sendFailure(Component.translatable("command.tps.unsafe"));
                return 0;
            }
        } else {
            source.sendFailure(Component.translatable("command.tps.cooldown"));
            return 0;
        }
    }

    private static int teleportPlayerToLocation(CommandContext<CommandSourceStack> ctx,
                                                ServerPlayer player,
                                                Vec3 location) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos blockPos = BlockPos.containing(location);

        if (checkCooldown(player)) {
            if (SafeTeleporter.safeTeleportToLocation(player, level, blockPos)) {
                source.sendSuccess(() ->
                                Component.translatable("command.tps.position_success",
                                        player.getName().getString()),
                        true);
                return 1;
            } else {
                source.sendFailure(Component.translatable("command.tps.unsafe"));
                return 0;
            }
        } else {
            source.sendFailure(Component.translatable("command.tps.cooldown"));
            return 0;
        }
    }

    private static boolean checkCooldown(ServerPlayer player) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        if (cooldowns.containsKey(playerId)) {
            long lastTeleport = cooldowns.get(playerId);
            if (currentTime - lastTeleport < COOLDOWN_TIME) {
                return false;
            }
        }

        cooldowns.put(playerId, currentTime);
        return true;
    }
}