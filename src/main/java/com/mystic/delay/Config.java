package com.mystic.delay;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Delay.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue DELAY = BUILDER.comment("tick of delay").defineInRange("ticksOfDelay", 60, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.BooleanValue STOP_ABSORPTION = BUILDER.comment("stop absorption too").define("stopAbsorption", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int delay;
    public static boolean stopAbsorption;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        delay = DELAY.get();
        stopAbsorption = STOP_ABSORPTION.get();
    }
}
