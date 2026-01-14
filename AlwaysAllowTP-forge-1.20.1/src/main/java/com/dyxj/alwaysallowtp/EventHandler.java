package com.dyxj.alwaysallowtp;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AlwaysAllowTP.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {

    // 可以在这里添加其他事件处理
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        // 玩家加入游戏时的处理
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        // 玩家离开游戏时的处理
    }
}