package com.anotherstar.lolipickaxe.event;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.asm.LoliErasureState;
import com.anotherstar.lolipickaxe.asm.LoliInventoryGuard;
import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import com.anotherstar.lolipickaxe.item.SmallLoliPickaxeItem;
import com.anotherstar.lolipickaxe.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

 
@Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID)
public final class LegacyLivingEvents {
    private static final Set<UUID> MOD_GRANTED_FLIGHT = new HashSet<>();
    private static final UUID LOLI_BLOCK_REACH = UUID.fromString("8f588fcb-8a72-4cd6-b1da-55f0435f0ad0");
    private static final UUID LOLI_ENTITY_REACH = UUID.fromString("a3f3a7d0-6179-4fa2-909f-fc91566540a8");
    private static final Map<UUID, Long> LAST_AUTO_KILL_TICK = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getTarget() instanceof LivingEntity protectedTarget
                && LoliPickaxeItem.hasLoliProtection(protectedTarget)) {
             
             
             
            LoliPickaxeItem.retaliateFromAttacker(protectedTarget, event.getEntity());
            LoliErasureState.stabilizeProtected(protectedTarget);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;

        if (LoliPickaxeItem.hasLoliProtection(target)) {
             
             
            LoliPickaxeItem.retaliate(target, event.getSource());
            event.setCanceled(true);
            return;
        }

        if (target instanceof Player player) {
            ItemStack small = findSmallPickaxe(player);
            if (!small.isEmpty()) {
                CompoundTag tag = small.getTag();
                double chance = tag != null && tag.contains("LoliAntiInjury")
                        ? SmallLoliPickaxeItem.getDoubleTransformValue("LoliAntiInjury", tag.getInt("LoliAntiInjury")) : 0.0D;
                if (player.getRandom().nextDouble() < chance) {
                    Entity attacker = event.getSource().getEntity();
                    if (attacker instanceof LivingEntity living && attacker != player) {
                        float previous = player.getAttackStrengthScale(0.0F);
                        player.attack(living);
                        float heal = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.5F;
                        player.heal(heal);
                         
                        if (previous >= 1.0F) player.resetAttackStrengthTicker();
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        if (LoliPickaxeItem.hasLoliProtection(target)) {
            event.setCanceled(true);
            return;
        }
        if (!(target instanceof Player player)) return;
        ItemStack small = findSmallPickaxe(player);
        if (small.isEmpty()) return;
        CompoundTag tag = small.getTag();
        boolean canFly = tag != null && tag.contains("LoliFly")
                && SmallLoliPickaxeItem.getTransformValue("LoliFly", tag.getInt("LoliFly")) == 0;
        if (canFly && event.getSource().is(DamageTypeTags.IS_FALL)) {
            event.setCanceled(true);
            return;
        }
        double dodge = tag != null && tag.contains("LoliDodge")
                ? SmallLoliPickaxeItem.getDoubleTransformValue("LoliDodge", tag.getInt("LoliDodge")) : 0.0D;
        if (player.getRandom().nextDouble() < dodge) event.setCanceled(true);
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && LoliPickaxeItem.hasLoliProtection(event.getEntity())) {
            event.setCanceled(true);
            LoliErasureState.stabilizeProtected(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (LoliPickaxeItem.hasLoliProtection(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingFall(LivingFallEvent event) {
        if (LoliPickaxeItem.hasLoliProtection(event.getEntity())) {
            event.setDistance(0.0F);
            event.setDamageMultiplier(0.0F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide) return;
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END) return;
        ejectForeignPickaxes(player);
         
         
         
        findDroppedOwnerPickaxe(player);
        if (player instanceof ServerPlayer serverPlayer) {
            LoliInventoryGuard.tick(serverPlayer);
            com.anotherstar.lolipickaxe.asm.LoliKlassDefense.tick(serverPlayer);
        }
        ItemStack loli = activePremiumSettings(player);
        ItemStack small = findSmallPickaxe(player);
        boolean grantFlight = false;

        if (!loli.isEmpty()) {
            LoliErasureState.stabilizeProtected(player);
            if (player instanceof ServerPlayer serverPlayer) runAutoKill(serverPlayer, loli);
            applyMainPickaxeEffects(player, loli);
            updatePremiumReach(player, loli);
            grantFlight = true;
        } else if (!small.isEmpty()) {
            clearPremiumReach(player);
            CompoundTag tag = small.getTag();
            if (tag != null) {
                grantFlight = tag.contains("LoliFly")
                        && SmallLoliPickaxeItem.getTransformValue("LoliFly", tag.getInt("LoliFly")) == 0;
                int buff = tag.contains("LoliBuff")
                        ? SmallLoliPickaxeItem.getTransformValue("LoliBuff", tag.getInt("LoliBuff")) : -1;
                if (buff >= 1) addLegacyEffect(player, 16, 0);
                if (buff >= 2) addLegacyEffect(player, 13, 0);
                if (buff >= 3) fillFood(player);
            }
        } else {
            clearPremiumReach(player);
        }
        updateFlight(player, grantFlight);
    }

    private static void runAutoKill(ServerPlayer player, ItemStack loli) {
        if (!(player.level() instanceof ServerLevel level)
                || !LoliPickaxeItem.settingEnabled(loli, "loliPickaxeAutoKillRangeEntity",
                LoliConfig.AUTO_KILL_RANGE_ENTITY.get())) return;
        long gameTime = level.getGameTime();
        if (LAST_AUTO_KILL_TICK.getOrDefault(player.getUUID(), Long.MIN_VALUE) == gameTime) return;
        LAST_AUTO_KILL_TICK.put(player.getUUID(), gameTime);
        int range = Math.max(0, com.anotherstar.lolipickaxe.config.LegacyLoliSettings.integer(
                loli, "loliPickaxeAutoKillRange", LoliConfig.AUTO_KILL_RANGE.get()));
        try {
             
             
            LoliPickaxeItem.killRangeEntity(level, player, loli, range);
        } catch (Throwable error) {
            LoliPickaxe.LOGGER.warn("Automatic Loli range erasure failed for {}", player.getGameProfile().getName(), error);
        }
    }

    private static ItemStack activePremiumSettings(Player player) {
        ItemStack present = LoliPickaxeItem.findOwnedPickaxe(player);
        if (!present.isEmpty()) return present;
        return LoliInventoryGuard.rememberedPickaxe(player);
    }

    private static void updatePremiumReach(Player player, ItemStack stack) {
        double blockDistance = com.anotherstar.lolipickaxe.config.LegacyLoliSettings.decimal(
                stack, "loliPickaxeBlockReachDistance", LoliConfig.BLOCK_REACH_DISTANCE.get());
        updateReachModifier(player, ForgeMod.BLOCK_REACH.get(), LOLI_BLOCK_REACH, blockDistance);

         
         
         
        double entityDistance = LoliPickaxeItem.settingEnabled(stack, "loliPickaxeKillFacing", LoliConfig.KILL_FACING.get())
                ? com.anotherstar.lolipickaxe.config.LegacyLoliSettings.integer(
                        stack, "loliPickaxeKillFacingRange", LoliConfig.KILL_FACING_RANGE.get())
                : blockDistance;
        updateReachModifier(player, ForgeMod.ENTITY_REACH.get(), LOLI_ENTITY_REACH, entityDistance);
    }

    private static void clearPremiumReach(Player player) {
        removeModifier(player, ForgeMod.BLOCK_REACH.get(), LOLI_BLOCK_REACH);
        removeModifier(player, ForgeMod.ENTITY_REACH.get(), LOLI_ENTITY_REACH);
    }

    private static void updateReachModifier(Player player, Attribute attribute, UUID id, double requestedTotal) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (requestedTotal <= 0.0D) {
            instance.removeModifier(id);
            return;
        }
        double amount = requestedTotal - instance.getBaseValue();
        AttributeModifier current = instance.getModifier(id);
        if (current != null && Double.compare(current.getAmount(), amount) == 0) return;
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, "Loli Pickaxe reach", amount,
                AttributeModifier.Operation.ADDITION));
    }

    private static void removeModifier(Player player, Attribute attribute, UUID id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }

    private static void applyMainPickaxeEffects(Player player, ItemStack stack) {
        Set<MobEffect> allowed = new HashSet<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("LoliPotion", Tag.TAG_LIST)) {
            ListTag list = tag.getList("LoliPotion", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag element = list.getCompound(i);
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.byId(element.getShort("id"));
                if (effect != null) {
                    allowed.add(effect);
                    player.addEffect(new MobEffectInstance(effect, 410, element.getByte("lvl"), false, false));
                }
            }
        }
        for (MobEffectInstance active : new ArrayList<>(player.getActiveEffects())) {
            if (!allowed.contains(active.getEffect())) player.removeEffect(active.getEffect());
        }
        fillFood(player);
    }

    private static void addLegacyEffect(Player player, int id, int amplifier) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.byId(id);
        if (effect != null) player.addEffect(new MobEffectInstance(effect, 410, amplifier, false, false));
    }

    private static void fillFood(Player player) {
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
    }

    private static void updateFlight(Player player, boolean grant) {
        UUID id = player.getUUID();
        if (grant) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                MOD_GRANTED_FLIGHT.add(id);
                if (player instanceof ServerPlayer serverPlayer) serverPlayer.onUpdateAbilities();
            }
        } else if (MOD_GRANTED_FLIGHT.remove(id) && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.onUpdateAbilities();
        }
    }

    private static void ejectForeignPickaxes(Player player) {
        ejectForeign(player, player.getInventory().items);
        ejectForeign(player, player.getInventory().armor);
        ejectForeign(player, player.getInventory().offhand);
    }

    private static void ejectForeign(Player player, java.util.List<ItemStack> compartment) {
        for (int i = 0; i < compartment.size(); i++) {
            ItemStack stack = compartment.get(i);
            if (stack.is(ModItems.LOLI_PICKAXE.get()) && LoliPickaxeItem.hasOwner(stack)
                    && !LoliPickaxeItem.isOwner(stack, player)) {
                if (player instanceof ServerPlayer) player.drop(stack.copy(), true, false);
                compartment.set(i, ItemStack.EMPTY);
            }
        }
    }

    private static void findDroppedOwnerPickaxe(Player player) {
        if (!LoliConfig.FIND_OWNER.get() || !(player.level() instanceof ServerLevel level)) return;
        int range = LoliConfig.FIND_OWNER_RANGE.get();
        AABB area = player.getBoundingBox().inflate(range);
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack stack = itemEntity.getItem();
            if (stack.is(ModItems.LOLI_PICKAXE.get()) && LoliPickaxeItem.hasOwner(stack)
                    && LoliPickaxeItem.isOwner(stack, player)) itemEntity.playerTouch(player);
        }
    }

    private static ItemStack findSmallPickaxe(Player player) {
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.items) if (stack.is(ModItems.SMALL_LOLI_PICKAXE.get())) return stack;
        for (ItemStack stack : inventory.armor) if (stack.is(ModItems.SMALL_LOLI_PICKAXE.get())) return stack;
        for (ItemStack stack : inventory.offhand) if (stack.is(ModItems.SMALL_LOLI_PICKAXE.get())) return stack;
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item)) return;
        if (item.getItem().is(ModItems.LOLI_PICKAXE.get())) {
            item.setInvulnerable(true);
            if (LoliConfig.FIND_OWNER.get()) item.setPickUpDelay(0);
        }
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        ItemStack stack = event.getItem().getItem();
        Player player = event.getEntity();
        if (stack.is(ModItems.LOLI_PICKAXE.get()) && LoliPickaxeItem.hasOwner(stack)
                && !LoliPickaxeItem.isOwner(stack, player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (entity.getRandom().nextDouble() < LoliConfig.CARD_DROP_PROBABILITY.get())
            event.getDrops().add(drop(entity, new ItemStack(ModItems.LOLI_CARD.get())));
        if (entity.getRandom().nextDouble() < LoliConfig.CARD_ALBUM_DROP_PROBABILITY.get())
            event.getDrops().add(drop(entity, new ItemStack(ModItems.LOLI_CARD_ALBUM.get())));
        if (entity instanceof net.minecraft.world.entity.monster.Creeper
                && entity.getRandom().nextDouble() < LoliConfig.RECORD_DROP_PROBABILITY.get())
            event.getDrops().add(drop(entity, new ItemStack(ModItems.LOLI_RECORD.get())));
        if (entity.getRandom().nextDouble() < LoliConfig.ENTITY_SOUL_DROP_PROBABILITY.get())
            event.getDrops().add(drop(entity, new ItemStack(ModItems.LOLI_ENTITY_SOUL_ADDON.get())));
    }

    private static ItemEntity drop(LivingEntity entity, ItemStack stack) {
        return new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LoliInventoryGuard.forget(event.getEntity());
        LAST_AUTO_KILL_TICK.remove(event.getEntity().getUUID());
    }

    private LegacyLivingEvents() {}
}
