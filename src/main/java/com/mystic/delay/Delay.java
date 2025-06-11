package com.mystic.delay;

import net.minecraft.world.effect.AbsoptionMobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod(Delay.MODID)
@Mod.EventBusSubscriber()
public class Delay {
    public static final String MODID = "delay";

    public Delay(FMLJavaModLoadingContext context) {
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private static final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private static final Map<UUID, Integer> preDamageHunger = new HashMap<>();
    private static final Map<UUID, Float> preDamageSaturation = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (player.level().isClientSide) return;

        UUID uuid = player.getUUID();
        Long lastHurt = lastDamageTime.get(uuid);
        if (lastHurt == null) return;

        long timeSinceDamage = player.level().getGameTime() - lastHurt;
        if (timeSinceDamage < Config.delay) {
            var foodData = player.getFoodData();

            // Block saturation gain during delay
            if (foodData.getSaturationLevel() > 0f) {
                foodData.setSaturation(0f);
            }

            if (!player.hasEffect(MobEffects.HUNGER)) {
                foodData.setFoodLevel(20); // Optional: cap visible food
            }

        } else {
            // Restore values after delay
            if (preDamageHunger.containsKey(uuid) && preDamageSaturation.containsKey(uuid)) {
                var foodData = player.getFoodData();
                foodData.setFoodLevel(preDamageHunger.remove(uuid));
                foodData.setSaturation(preDamageSaturation.remove(uuid));
            }

            lastDamageTime.remove(uuid);
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            UUID uuid = player.getUUID();
            preDamageHunger.put(uuid, player.getFoodData().getFoodLevel());
            preDamageSaturation.put(uuid, player.getFoodData().getSaturationLevel());
            lastDamageTime.put(uuid, player.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
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
    public static void onLivingHeal(LivingHealEvent event) {
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
