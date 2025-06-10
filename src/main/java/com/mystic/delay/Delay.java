package com.mystic.delay;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod(Delay.MODID)
public class Delay {
    public static final String MODID = "delay";

    public Delay(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private static final Map<UUID, Long> lastDamageTime = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        Long lastHurt = lastDamageTime.get(player.getUUID());
        if (lastHurt != null && player.level().getGameTime() - lastHurt < Config.delay) {
            var foodData = player.getFoodData();

            // Block saturation
            if (foodData.getSaturationLevel() > 0f) {
                foodData.setSaturation(0f);
            }

            if (!player.hasEffect(MobEffects.HUNGER)) {
                foodData.setFoodLevel(20);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerHurt(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player player) {
            lastDamageTime.put(player.getUUID(), player.level().getGameTime());
        }
    }

    @SubscribeEvent
    public void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player) {
            Long lastHurt = lastDamageTime.get(player.getUUID());
            if (lastHurt != null && player.level().getGameTime() - lastHurt < Config.delay) {
                if (event.getItem().getFoodProperties(player) != null) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof Player player) {
            Long lastHurt = lastDamageTime.get(player.getUUID());
            if (lastHurt != null && player.level().getGameTime() - lastHurt < Config.delay) {
                event.setCanceled(true);

                if (Config.stopAbsorption && player.hasEffect(MobEffects.ABSORPTION)) {
                    player.removeEffect(MobEffects.ABSORPTION);
                }

                if (player.hasEffect(MobEffects.SATURATION)) {
                    player.removeEffect(MobEffects.SATURATION);
                }
            }
        }
    }
}
