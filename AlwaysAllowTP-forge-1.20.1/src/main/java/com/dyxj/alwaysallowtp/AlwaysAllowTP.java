package com.dyxj.alwaysallowtp;

import com.dyxj.alwaysallowtp.command.AllowTPCommand;
import com.dyxj.alwaysallowtp.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AlwaysAllowTP.MODID)
public class AlwaysAllowTP {
    public static final String MODID = "alwaysallowtp";
    public static final String MODNAME = "始终允许传送";
    public static final Logger LOGGER = LogManager.getLogger();

    public AlwaysAllowTP() {
        // 注册配置
        var modLoadingContext = ModLoadingContext.get();
        modLoadingContext.registerConfig(Type.COMMON, ModConfig.SPEC, MODID + "-common.toml");

        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("{} 模组已加载", MODNAME);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        AllowTPCommand.register(event.getDispatcher(), event.getBuildContext());
    }
}