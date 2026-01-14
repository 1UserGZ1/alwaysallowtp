package com.dyxj.alwaysallowtp.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_CROSS_DIMENSION;
    public static final ForgeConfigSpec.BooleanValue STRICT_SAFETY_CHECK;
    public static final ForgeConfigSpec.IntValue SAFE_SEARCH_RADIUS;
    public static final ForgeConfigSpec.IntValue COOLDOWN_SECONDS;

    static {
        BUILDER.comment("始终允许传送模组配置").push("alwaysallowtp");

        ENABLE_CROSS_DIMENSION = BUILDER
                .comment("启用跨维度传送")
                .translation("config.alwaysallowtp.enableCrossDimension")
                .define("enableCrossDimension", true);

        STRICT_SAFETY_CHECK = BUILDER
                .comment("启用严格安全检查（防止传送到岩浆、虚空等危险位置）")
                .translation("config.alwaysallowtp.strictSafetyCheck")
                .define("strictSafetyCheck", true);

        SAFE_SEARCH_RADIUS = BUILDER
                .comment("安全位置搜索半径（格）")
                .translation("config.alwaysallowtp.safeSearchRadius")
                .defineInRange("safeSearchRadius", 8, 1, 20);

        COOLDOWN_SECONDS = BUILDER
                .comment("传送冷却时间（秒）")
                .translation("config.alwaysallowtp.cooldownSeconds")
                .defineInRange("cooldownSeconds", 3, 0, 30);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}