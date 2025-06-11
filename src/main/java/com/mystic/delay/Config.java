package com.mystic.delay;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Delay.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue DELAY = BUILDER.comment("tick of delay").defineInRange("ticksOfDelay", 60, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.BooleanValue STOP_ABSORPTION = BUILDER.comment("stop absorption too").define("stopAbsorption", true);
    private static final ModConfigSpec.IntValue STARVATION_THRESHOLD = BUILDER
            .comment("Minimum food level below which eating is allowed, even during delay.")
            .defineInRange("starvationThreshold", 1, 1, 20);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int delay;
    public static boolean stopAbsorption;
    public static int starvationThreshold;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        delay = DELAY.get();
        stopAbsorption = STOP_ABSORPTION.get();
        starvationThreshold = STARVATION_THRESHOLD.get();
    }
}
