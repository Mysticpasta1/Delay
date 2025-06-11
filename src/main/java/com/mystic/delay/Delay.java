package com.mystic.delay;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
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

    private static final Map<UUID, Integer> preDamageHunger = new HashMap<>();
    private static final Map<UUID, Float> preDamageSaturation = new HashMap<>();
    private static final Map<UUID, Long> lastDamageTime = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        UUID uuid = player.getUUID();
        Long lastHurt = lastDamageTime.get(uuid);
        if (lastHurt == null) return;

        long timeSinceDamage = player.level().getGameTime() - lastHurt;

        if (timeSinceDamage < Config.delay) {
            var foodData = player.getFoodData();

            // Save food level before zeroing saturation (only once)
            if (!preDamageHunger.containsKey(uuid)) {
                preDamageHunger.put(uuid, foodData.getFoodLevel());
                preDamageSaturation.put(uuid, foodData.getSaturationLevel());
            }

            // Only prevent healing if the player is not starving
            if (foodData.getFoodLevel() > Config.starvationThreshold) {
                foodData.setSaturation(0f);
                foodData.setFoodLevel(20);
            }

            // Optional: prevent certain healing effects
            if (Config.stopAbsorption && player.hasEffect(MobEffects.ABSORPTION)) {
                player.removeEffect(MobEffects.ABSORPTION);
            }
            if (player.hasEffect(MobEffects.REGENERATION)) {
                player.removeEffect(MobEffects.REGENERATION);
            }
            if (player.hasEffect(MobEffects.SATURATION)) {
                player.removeEffect(MobEffects.SATURATION);
            }

        } else {
            // Restore hunger only if it was previously saved
            if (preDamageHunger.containsKey(uuid) && preDamageSaturation.containsKey(uuid)) {
                var foodData = player.getFoodData();

                int oldHunger = preDamageHunger.remove(uuid);
                float oldSaturation = preDamageSaturation.remove(uuid);

                foodData.setFoodLevel(oldHunger);
                foodData.setSaturation(oldSaturation);
                System.out.println("Restored hunger=" + oldHunger);
                System.out.println("Restored saturation=" + oldSaturation);
            }

            lastDamageTime.remove(uuid);
        }
    }

    @SubscribeEvent
    public void onPlayerHurt(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Player player) {
            UUID uuid = player.getUUID();
            if (!lastDamageTime.containsKey(uuid)) {
                // Only save if not already in delay
                preDamageHunger.put(uuid, player.getFoodData().getFoodLevel());
                preDamageSaturation.put(uuid, player.getFoodData().getSaturationLevel());
                System.out.println("Saved hunger=" + preDamageHunger.get(uuid));
                System.out.println("Saved saturation=" + preDamageSaturation.get(uuid));
            }
            lastDamageTime.put(uuid, player.level().getGameTime());
        }
    }

    @SubscribeEvent
    public void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity() instanceof Player player) {
            Long lastHurt = lastDamageTime.get(player.getUUID());
            if (lastHurt != null && player.level().getGameTime() - lastHurt < Config.delay) {
                if (event.getItem().getFoodProperties(player) != null) {
                    var food = player.getFoodData();
                    if (food.getFoodLevel() > Config.starvationThreshold) { // Allow eating only when starving
                        event.setCanceled(true);
                        player.releaseUsingItem(); // Stop the eating animation
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClickItem(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        Long lastHurt = lastDamageTime.get(player.getUUID());
        if (lastHurt != null && player.level().getGameTime() - lastHurt < Config.delay) {
            if (event.getItemStack().getFoodProperties(player) != null) {
                var food = player.getFoodData();
                if (food.getFoodLevel() > Config.starvationThreshold) { // Allow eating only when starving
                    event.setCanceled(true);
                    player.releaseUsingItem(); // only for Tick event
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
