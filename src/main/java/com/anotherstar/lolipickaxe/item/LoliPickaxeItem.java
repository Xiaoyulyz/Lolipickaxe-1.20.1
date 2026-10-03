package com.anotherstar.lolipickaxe.item;

import com.anotherstar.lolipickaxe.asm.LoliErasureState;
import com.anotherstar.lolipickaxe.asm.LoliInventoryGuard;
import com.anotherstar.lolipickaxe.config.LegacyLoliSettings;
import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.registry.ModItems;
import com.anotherstar.lolipickaxe.entity.LoliEntity;
import com.anotherstar.lolipickaxe.registry.ModEnchantments;
import com.anotherstar.lolipickaxe.damage.ModDamageTypes;
import com.anotherstar.lolipickaxe.registry.ModSounds;
import com.anotherstar.lolipickaxe.network.ModNetwork;
import com.anotherstar.lolipickaxe.network.SafeAttackEffect;
import com.anotherstar.lolipickaxe.network.SafeAttackEffectPacket;
import com.anotherstar.lolipickaxe.network.RefreshHeldItemPacket;
import com.anotherstar.lolipickaxe.util.LegacyTextComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LoliPickaxeItem extends PickaxeItem {
     
    public static final String TAG_RANGE = "range";
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_OWNER_UUID = "OwnerUUID";
    private static final ThreadLocal<Boolean> RETALIATION_GUARD = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final Map<UUID, Long> PREMIUM_CLICK_TICKS = new ConcurrentHashMap<>();

    public LoliPickaxeItem() {
        super(LoliTier.INSTANCE, 1, -2.8F, new Properties().stacksTo(1));
    }

    public static int getRange(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_RANGE) ? tag.getInt(TAG_RANGE) : 1;
    }

    public static void applyLegacyDefaults(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!EnchantmentHelper.getEnchantments(stack).containsKey(Enchantments.BLOCK_FORTUNE)) {
            Map<net.minecraft.world.item.enchantment.Enchantment, Integer> enchantments = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
            enchantments.put(Enchantments.BLOCK_FORTUNE, 32);
            enchantments.put(ModEnchantments.AUTO_FURNACE.get(), 1);
            EnchantmentHelper.setEnchantments(enchantments, stack);
        }
         
        if (!tag.contains("LoliPotion")) {
            ListTag list = new ListTag();
            CompoundTag strength = new CompoundTag();
            strength.putShort("id", (short) 16);
            strength.putByte("lvl", (byte) 0);
            list.add(strength);
            CompoundTag waterBreathing = new CompoundTag();
            waterBreathing.putShort("id", (short) 13);
            waterBreathing.putByte("lvl", (byte) 0);
            list.add(waterBreathing);
            tag.put("LoliPotion", list);
        }
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        applyLegacyDefaults(stack);
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof Player player) {
            CompoundTag tag = stack.getOrCreateTag();
            if (!hasOwner(stack)) {
                tag.putString(TAG_OWNER, player.getGameProfile().getName());
                tag.putString(TAG_OWNER_UUID, player.getUUID().toString());
            }
            if (settingBool(stack, "loliPickaxeInfiniteBattery", LoliConfig.INFINITE_BATTERY.get())) {
                chargeForgeEnergyInventory(player, stack);
            }
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                com.anotherstar.lolipickaxe.asm.LoliKlassDefense.apply(serverPlayer);
            }
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }


    




    private static void chargeForgeEnergyInventory(Player player, ItemStack battery) {
        if (player == null || battery == null || player.getInventory() == null) return;
        int slots = player.getInventory().getContainerSize();
        for (int slot = 0; slot < slots; slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.isEmpty() || candidate == battery) continue;
            try {
                candidate.getCapability(ForgeCapabilities.ENERGY).ifPresent(storage -> {
                    if (!storage.canReceive()) return;
                    long missing = (long) storage.getMaxEnergyStored() - storage.getEnergyStored();
                    if (missing <= 0L) return;
                    storage.receiveEnergy((int) Math.min(Integer.MAX_VALUE, missing), false);
                });
            } catch (Throwable ignored) {
                 
            }
        }
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 0.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }

    @Override
    public int getEntityLifespan(ItemStack itemStack, Level level) {
        return Integer.MAX_VALUE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                if (settingBool(stack, "loliPickaxeKillRangeEntity", LoliConfig.KILL_RANGE_ENTITY.get())) {
                    int range = settingInt(stack, "loliPickaxeKillRange", LoliConfig.KILL_RANGE.get());
                    int count = killRangeEntity((ServerLevel) level, player, stack, range);
                    player.sendSystemMessage(LegacyTextComponents.translatable("loliPickaxe.killrangeentity", range * 2, count));
                    playSuccess(player);
                }
            } else {
                CompoundTag tag = stack.getTag();
                if (tag == null) {
                    tag = new CompoundTag();
                    tag.putInt(TAG_RANGE, 0);
                    stack.setTag(tag);
                } else if (tag.contains(TAG_RANGE)) {
                    int old = tag.getInt(TAG_RANGE);
                    tag.putInt(TAG_RANGE, old >= LoliConfig.MAX_RANGE.get() ? 0 : old + 1);
                } else {
                    tag.putInt(TAG_RANGE, 1);
                }
                player.sendSystemMessage(LegacyTextComponents.translatable("loliPickaxe.range", 1 + 2 * tag.getInt(TAG_RANGE)));
                playSuccess(player);
            }
             
             
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new RefreshHeldItemPacket(hand));
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (player.level().isClientSide) return false;
        if (player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel level) {
            attackFromPremiumClick(level, serverPlayer, stack, target, serverPlayer.getLookAngle());
             
             
            return true;
        }
        boolean success = killOne(player, stack, target);
        if (settingBool(stack, "loliPickaxeKillFacing", LoliConfig.KILL_FACING.get())) {
            killFacing((ServerLevel) player.level(), player, stack);
            success = true;
        }
        return success;
    }

    public static int killRangeEntity(ServerLevel level, LivingEntity source, ItemStack stack, int range) {
        int safeRange = Math.max(0, range);
        AABB box = new AABB(source.getX() - safeRange, source.getY() - safeRange, source.getZ() - safeRange,
                source.getX() + safeRange, source.getY() + safeRange, source.getZ() + safeRange);
        boolean all = settingBool(stack, "loliPickaxeValidToAllEntity", LoliConfig.VALID_TO_ALL_ENTITY.get());
        List<Entity> targets = new java.util.ArrayList<>(
                level.getEntities(source, box, e -> e != source && (all || isLivingTargetCandidate(e))));
        if (!settingBool(stack, "loliPickaxeValidToAmityEntity", LoliConfig.VALID_TO_AMITY_ENTITY.get())) {
            targets.removeIf(LoliPickaxeItem::isFriendlyEntity);
        }
        for (Entity target : List.copyOf(targets)) {
            try {
                killOne(source, stack, target);
            } catch (Throwable ignored) {
                 
            }
        }
         
         
        return targets.size();
    }

    public static void killFacing(ServerLevel level, LivingEntity source, ItemStack stack) {
        int range = settingInt(stack, "loliPickaxeKillFacingRange", LoliConfig.KILL_FACING_RANGE.get());
        double slope = settingDouble(stack, "loliPickaxeKillFacingSlope", LoliConfig.KILL_FACING_SLOPE.get());
        boolean all = settingBool(stack, "loliPickaxeValidToAllEntity", LoliConfig.VALID_TO_ALL_ENTITY.get());
        Vec3 look = source.getLookAngle().normalize();
        java.util.LinkedHashSet<Entity> targets = new java.util.LinkedHashSet<>();
        for (int dist = 0; dist <= range; dist += 2) {
            AABB box = source.getBoundingBox()
                    .inflate(slope * dist + 2.0D, slope * dist + 0.25D, slope * dist + 2.0D)
                    .move(look.scale(dist));
            targets.addAll(level.getEntities(source, box,
                    e -> e != source && e.distanceTo(source) <= range && (all || isLivingTargetCandidate(e))));
        }
        if (!settingBool(stack, "loliPickaxeValidToAmityEntity", LoliConfig.VALID_TO_AMITY_ENTITY.get())) {
            targets.removeIf(LoliPickaxeItem::isFriendlyEntity);
        }
        for (Entity target : targets) {
            try {
                killOne(source, stack, target);
            } catch (Throwable ignored) {
                 
            }
        }
    }


    public static boolean attackFromPremiumClick(ServerLevel level, ServerPlayer source, ItemStack stack) {
        return attackFromPremiumClick(level, source, stack, null, source.getLookAngle());
    }

    public static boolean attackFromPremiumClick(ServerLevel level, ServerPlayer source, ItemStack stack,
                                                 @Nullable Entity preferred, Vec3 clientLook) {
        if (level == null || source == null || stack == null) return false;
        long gameTime = level.getGameTime();
        Long previous = PREMIUM_CLICK_TICKS.put(source.getUUID(), gameTime);
        if (previous != null && previous.longValue() == gameTime) return false;

        Vec3 direction = clientLook == null ? source.getLookAngle() : clientLook;
        if (!Double.isFinite(direction.x) || !Double.isFinite(direction.y)
                || !Double.isFinite(direction.z) || direction.lengthSqr() < 1.0E-8D) {
            direction = source.getLookAngle();
        }
        direction = direction.normalize();

        boolean success = false;
        Entity precise = findLongRangeViewTarget(level, source, stack, preferred, direction);
        if (precise != null) success = killOne(source, stack, precise);
        if (settingBool(stack, "loliPickaxeKillFacing", LoliConfig.KILL_FACING.get())) {
            killFacing(level, source, stack);
            success = true;
        }
        return success;
    }

    private static Entity findLongRangeViewTarget(ServerLevel level, LivingEntity source, ItemStack stack,
                                                  @Nullable Entity preferred, Vec3 direction) {
        double configuredFacing = settingInt(stack, "loliPickaxeKillFacingRange", LoliConfig.KILL_FACING_RANGE.get());
        double configuredReach = settingDouble(stack, "loliPickaxeBlockReachDistance", LoliConfig.BLOCK_REACH_DISTANCE.get());
        double range = Math.max(5.0D, Math.max(configuredFacing, configuredReach));
        boolean all = settingBool(stack, "loliPickaxeValidToAllEntity", LoliConfig.VALID_TO_ALL_ENTITY.get());

        Entity preferredRoot = normalizeAttackTarget(preferred);
        if (isValidLongRangeCandidate(source, preferredRoot, all)
                && source.distanceToSqr(preferredRoot) <= range * range) {
            return preferredRoot;
        }

        Vec3 start = source.getEyePosition();
        Vec3 end = start.add(direction.scale(range));
        double broadMargin = Math.max(4.0D, Math.min(12.0D, range * 0.08D));
        AABB search = source.getBoundingBox().expandTowards(direction.scale(range)).inflate(broadMargin);

        java.util.LinkedHashSet<Entity> roots = new java.util.LinkedHashSet<>();
        for (Entity raw : level.getEntities(source, search, entity -> entity != source)) {
            Entity candidate = normalizeAttackTarget(raw);
            if (isValidLongRangeCandidate(source, candidate, all)) roots.add(candidate);
        }

        Entity exact = null;
        double exactProjection = Double.MAX_VALUE;
        Entity assisted = null;
        double assistedScore = Double.MAX_VALUE;

        for (Entity candidate : roots) {
            AABB rawBounds = candidate.getBoundingBox();
            double pickInflation = Math.max(0.5D, candidate.getPickRadius());
            AABB bounds = rawBounds.inflate(pickInflation);
            Optional<Vec3> hit = bounds.contains(start) ? Optional.of(start) : bounds.clip(start, end);
            if (hit.isPresent()) {
                double projection = Math.max(0.0D, hit.get().subtract(start).dot(direction));
                if (projection < exactProjection) {
                    exactProjection = projection;
                    exact = candidate;
                }
                continue;
            }

             
             
            Vec3 center = rawBounds.getCenter();
            double projection = center.subtract(start).dot(direction);
            if (projection < 0.0D || projection > range) continue;
            Vec3 closest = start.add(direction.scale(projection));
            double perpendicularSqr = center.distanceToSqr(closest);
            double width = Math.max(0.1D, rawBounds.getXsize());
            double height = Math.max(0.1D, rawBounds.getYsize());
            double depth = Math.max(0.1D, rawBounds.getZsize());
            double modelRadius = Math.max(1.0D,
                    Math.max(candidate.getPickRadius() + 0.5D,
                            Math.max(Math.max(width, depth) * 0.75D, height * 0.35D)));
            double distanceAssist = Math.min(3.5D, 0.5D + projection * 0.02D);
            double allowed = modelRadius + distanceAssist;
            if (perpendicularSqr > allowed * allowed) continue;

            double normalizedMiss = perpendicularSqr / Math.max(0.25D, allowed * allowed);
            double score = normalizedMiss + (projection / range) * 0.025D;
            if (score < assistedScore) {
                assistedScore = score;
                assisted = candidate;
            }
        }
        return exact != null ? exact : assisted;
    }

    @Nullable
    private static Entity normalizeAttackTarget(@Nullable Entity target) {
        if (target instanceof PartEntity<?> part && part.getParent() != null) {
            return part.getParent();
        }
        return target;
    }

    private static boolean isValidLongRangeCandidate(LivingEntity source, @Nullable Entity entity, boolean all) {
        if (entity == null || entity == source || entity.isRemoved()) return false;
        return all || isLivingTargetCandidate(entity);
    }

    private static boolean isLivingTargetCandidate(Entity entity) {
        if (entity instanceof LivingEntity) return true;
        if (entity instanceof PartEntity<?> part) return part.getParent() instanceof LivingEntity;
        return false;
    }

    private static boolean isFriendlyEntity(Entity entity) {
        if (entity instanceof PartEntity<?> part && part.getParent() != null) {
            entity = part.getParent();
        }
        return entity instanceof Player
                || entity instanceof ArmorStand
                || entity instanceof AmbientCreature
                || (entity instanceof Mob && !(entity instanceof Enemy));
    }

    public static boolean killOne(LivingEntity source, ItemStack stack, Entity target) {
        if (source == null || target == null || target == source) return false;

         
         
        if (target instanceof PartEntity<?> part) {
            Entity parent = part.getParent();
            if (parent != null) target = parent;
        }
        if (target == source) return false;

         
         
        if (hasLoliProtection(source)) LoliErasureState.stabilizeProtected(source);

        if (target instanceof LivingEntity living) {
             
             
            if (living instanceof FakePlayer || hasLoliProtection(living)) return true;
            if (LoliErasureState.isErased(living)) {
                LoliErasureState.registerIfErased(living);
                return true;
            }
            if (living instanceof Player player) {
                if (settingBool(stack, "loliPickaxeClearInventory", LoliConfig.CLEAR_INVENTORY.get())) {
                    player.getInventory().clearContent();
                    player.getEnderChestInventory().clearContent();
                }
                if (settingBool(stack, "loliPickaxeDropItems", LoliConfig.DROP_ITEMS.get())) {
                    player.getInventory().dropAll();
                }
                sendSafeLegacyEffects(stack, player);
            }

            boolean compulsory = settingBool(stack, "loliPickaxeCompulsoryRemove", LoliConfig.COMPULSORY_REMOVE.get());
            LoliErasureState.erase(living, ModDamageTypes.loli(living.level(), source), compulsory);
            if (living instanceof Player player) {
                if (settingBool(stack, "loliPickaxeBeyondRedemption", LoliConfig.BEYOND_REDEMPTION.get())) {
                    LoliErasureState.markBeyondRedemption(player);
                }
                if (settingBool(stack, "loliPickaxeReincarnation", LoliConfig.REINCARNATION.get())) {
                    LoliErasureState.markReincarnation(player);
                }
            }
            if (hasLoliProtection(source)) LoliErasureState.stabilizeProtected(source);

            if (living instanceof ServerPlayer serverPlayer
                    && settingBool(stack, "loliPickaxeKickPlayer", LoliConfig.KICK_PLAYER.get())) {
                String message = LegacyLoliSettings.string(stack, "loliPickaxeKickMessage", LoliConfig.KICK_MESSAGE.get());
                serverPlayer.connection.disconnect(LegacyTextComponents.literal(message));
            }
            return true;
        }
        if (settingBool(stack, "loliPickaxeValidToAllEntity", LoliConfig.VALID_TO_ALL_ENTITY.get())) {
            return LoliErasureState.eraseNonLiving(target);
        }
        return false;
    }

    private static void sendSafeLegacyEffects(ItemStack stack, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !LoliConfig.SAFE_ATTACK_SIMULATION.get()) return;
        if (settingBool(stack, "loliPickaxeBlueScreenAttack", LoliConfig.BLUE_SCREEN_ATTACK.get())) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SafeAttackEffectPacket(SafeAttackEffect.BLUE_SCREEN));
        }
        if (settingBool(stack, "loliPickaxeExitAttack", LoliConfig.EXIT_ATTACK.get())) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SafeAttackEffectPacket(SafeAttackEffect.EXIT));
        }
        if (settingBool(stack, "loliPickaxeFailRespondAttack", LoliConfig.FAIL_RESPOND_ATTACK.get())) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SafeAttackEffectPacket(SafeAttackEffect.NOT_RESPONDING));
        }
    }


    public static void retaliateFromAttacker(LivingEntity defender, Entity attacker) {
        if (defender == null || attacker == null || attacker == defender || defender.level().isClientSide
                || !hasLoliProtection(defender) || RETALIATION_GUARD.get()) return;

        ItemStack settings = ItemStack.EMPTY;
        if (defender instanceof Player player) {
            settings = findOwnedPickaxe(player);
            if (settings.isEmpty()) settings = LoliInventoryGuard.rememberedPickaxe(player);
        }
        if (!settingBool(settings, "loliPickaxeThorns", LoliConfig.THORNS.get())) return;

        RETALIATION_GUARD.set(Boolean.TRUE);
        try {
            killOne(defender, settings, attacker);
        } finally {
            RETALIATION_GUARD.set(Boolean.FALSE);
        }
    }

    public static void retaliate(LivingEntity defender, net.minecraft.world.damagesource.DamageSource source) {
        if (defender == null || source == null || defender.level().isClientSide
                || !hasLoliProtection(defender) || RETALIATION_GUARD.get()) return;

        ItemStack settings = ItemStack.EMPTY;
        if (defender instanceof Player player) {
            settings = findOwnedPickaxe(player);
            if (settings.isEmpty()) settings = LoliInventoryGuard.rememberedPickaxe(player);
        }
        if (!settingBool(settings, "loliPickaxeThorns", LoliConfig.THORNS.get())) return;

        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if ((attacker == null || attacker == defender) && direct instanceof Projectile projectile) {
            attacker = projectile.getOwner();
        } else if (attacker == null) {
            attacker = direct;
        }
        if (attacker == null || attacker == defender) return;

        retaliateFromAttacker(defender, attacker);
    }

    public static boolean attackFromLoli(LivingEntity source, Entity target) {
        if (source.level().isClientSide) return false;
        ItemStack settings = source.getMainHandItem();
        boolean success = killOne(source, settings, target);
        if (settingBool(settings, "loliPickaxeKillFacing", LoliConfig.KILL_FACING.get())
                && source.level() instanceof ServerLevel serverLevel) {
            killFacing(serverLevel, source, settings);
            success = true;
        }
        return success;
    }

 
    public static boolean hasLoliProtection(LivingEntity entity) {
        if (entity instanceof LoliEntity loli) return !loli.lolipickaxe$isRemovalAuthorized();
        if (!(entity instanceof Player player)) return false;
        return hasPremiumPlayerProtection(player);
    }


    public static boolean hasPremiumPlayerProtection(Player player) {
        if (player == null) return false;
        try {
            if (!findOwnedPickaxe(player).isEmpty()) return true;
        } catch (Throwable ignored) {
             
        }
        try {
            return !player.level().isClientSide && LoliInventoryGuard.hasTemporaryProtection(player);
        } catch (Throwable ignored) {
            return false;
        }
    }

     
    public static ItemStack findOwnedPickaxe(Player player) {
        if (player == null) return ItemStack.EMPTY;
        try {
            var inventory = player.getInventory();
            if (inventory != null) {
                if (inventory.items != null) {
                    for (ItemStack candidate : inventory.items) {
                        if (isUsableBy(candidate, player)) return candidate;
                    }
                }
                if (inventory.armor != null) {
                    for (ItemStack candidate : inventory.armor) {
                        if (isUsableBy(candidate, player)) return candidate;
                    }
                }
                if (inventory.offhand != null) {
                    for (ItemStack candidate : inventory.offhand) {
                        if (isUsableBy(candidate, player)) return candidate;
                    }
                }
            }
        } catch (Throwable ignored) {
             
        }
        try {
            if (player.containerMenu != null) {
                ItemStack carried = player.containerMenu.getCarried();
                if (isUsableBy(carried, player)) return carried;
            }
        } catch (Throwable ignored) {
             
        }
        return ItemStack.EMPTY;
    }

    public static boolean isUsableBy(ItemStack stack, Player player) {
        return stack != null && player != null && stack.is(ModItems.LOLI_PICKAXE.get())
                && (!hasOwner(stack) || isOwner(stack, player));
    }

    public static boolean settingEnabled(ItemStack stack, String key, boolean global) {
        return settingBool(stack, key, global);
    }

    public static boolean hasOwner(ItemStack stack) {
        return stack.hasTag() && (stack.getTag().contains(TAG_OWNER) || stack.getTag().contains(TAG_OWNER_UUID));
    }

    public static boolean isOwner(ItemStack stack, Player player) {
        if (!stack.hasTag()) return false;
        return stack.getTag().getString(TAG_OWNER).equals(player.getGameProfile().getName())
                || stack.getTag().getString(TAG_OWNER_UUID).equals(player.getUUID().toString());
    }

    public static void playSuccess(Player player) {
         
         
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.playNotifySound(ModSounds.LOLI_SUCCESS.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int firstLegacyLine = tooltip.size();
        tooltip.add(Component.literal("已在GitHub上开源"));
        tooltip.add(Component.translatable("loliPickaxe.curRange", 1 + 2 * getRange(stack)));
        if (settingBool(stack, "loliPickaxeMandatoryDrop", LoliConfig.MANDATORY_DROP.get())) tooltip.add(Component.translatable("loliPickaxe.mandatoryDrop"));
        if (settingBool(stack, "loliPickaxeStopOnLiquid", LoliConfig.STOP_ON_LIQUID.get())) tooltip.add(Component.translatable("loliPickaxe.stopOnLiquid"));
        double distance = settingDouble(stack, "loliPickaxeBlockReachDistance", LoliConfig.BLOCK_REACH_DISTANCE.get());
        if (distance > 0) tooltip.add(Component.translatable("loliPickaxe.blockReachDistance", distance));
        if (settingBool(stack, "loliPickaxeAutoAccept", LoliConfig.AUTO_ACCEPT.get())) tooltip.add(Component.translatable("loliPickaxe.autoAccept"));
        if (settingBool(stack, "loliPickaxeThorns", LoliConfig.THORNS.get())) tooltip.add(Component.translatable("loliPickaxe.thorns"));
        if (settingBool(stack, "loliPickaxeKillRangeEntity", LoliConfig.KILL_RANGE_ENTITY.get())) tooltip.add(Component.translatable("loliPickaxe.killRange", 2 * settingInt(stack, "loliPickaxeKillRange", LoliConfig.KILL_RANGE.get())));
        if (settingBool(stack, "loliPickaxeAutoKillRangeEntity", LoliConfig.AUTO_KILL_RANGE_ENTITY.get())) tooltip.add(Component.translatable("loliPickaxe.autoKillRange", 2 * settingInt(stack, "loliPickaxeAutoKillRange", LoliConfig.AUTO_KILL_RANGE.get())));
        if (settingBool(stack, "loliPickaxeCompulsoryRemove", LoliConfig.COMPULSORY_REMOVE.get())) tooltip.add(Component.translatable("loliPickaxe.compulsoryRemove"));
        if (settingBool(stack, "loliPickaxeValidToAmityEntity", LoliConfig.VALID_TO_AMITY_ENTITY.get())) tooltip.add(Component.translatable("loliPickaxe.validToAmityEntity"));
        if (settingBool(stack, "loliPickaxeValidToAllEntity", LoliConfig.VALID_TO_ALL_ENTITY.get())) tooltip.add(Component.translatable("loliPickaxe.validToAllEntity"));
        if (settingBool(stack, "loliPickaxeClearInventory", LoliConfig.CLEAR_INVENTORY.get())) tooltip.add(Component.translatable("loliPickaxe.clearInventory"));
        if (settingBool(stack, "loliPickaxeDropItems", LoliConfig.DROP_ITEMS.get())) tooltip.add(Component.translatable("loliPickaxe.dropItems"));
        if (settingBool(stack, "loliPickaxeKickPlayer", LoliConfig.KICK_PLAYER.get())) tooltip.add(Component.translatable("loliPickaxe.kickPlayer"));
        if (settingBool(stack, "loliPickaxeReincarnation", LoliConfig.REINCARNATION.get())) tooltip.add(Component.translatable("loliPickaxe.reincarnation"));
        if (settingBool(stack, "loliPickaxeBeyondRedemption", LoliConfig.BEYOND_REDEMPTION.get())) tooltip.add(Component.translatable("loliPickaxe.beyondRedemption"));
        if (settingBool(stack, "loliPickaxeBlueScreenAttack", LoliConfig.BLUE_SCREEN_ATTACK.get())) tooltip.add(Component.translatable("loliPickaxe.blueScreenAttack"));
        if (settingBool(stack, "loliPickaxeExitAttack", LoliConfig.EXIT_ATTACK.get())) tooltip.add(Component.translatable("loliPickaxe.exitAttack"));
        if (settingBool(stack, "loliPickaxeFailRespondAttack", LoliConfig.FAIL_RESPOND_ATTACK.get())) tooltip.add(Component.translatable("loliPickaxe.failRespondAttack"));
        if (settingBool(stack, "loliPickaxeKillFacing", LoliConfig.KILL_FACING.get())) tooltip.add(Component.translatable("loliPickaxe.killFacing", settingInt(stack, "loliPickaxeKillFacingRange", LoliConfig.KILL_FACING_RANGE.get()), settingDouble(stack, "loliPickaxeKillFacingSlope", LoliConfig.KILL_FACING_SLOPE.get())));
        if (settingBool(stack, "loliPickaxeInfiniteBattery", LoliConfig.INFINITE_BATTERY.get())) tooltip.add(Component.translatable("loliPickaxe.infiniteBattery"));
        for (int i = firstLegacyLine; i < tooltip.size(); i++) {
            tooltip.set(i, legacyDescriptionStyle(tooltip.get(i)));
        }
    }

    private static Component legacyDescriptionStyle(Component component) {
        return component.copy()
                .withStyle(ChatFormatting.GRAY)
                .withStyle(style -> style.withBold(false).withItalic(false));
    }

    private static boolean settingBool(ItemStack stack, String key, boolean global) {
        return LegacyLoliSettings.bool(stack, key, global);
    }

    private static int settingInt(ItemStack stack, String key, int global) {
        return LegacyLoliSettings.integer(stack, key, global);
    }

    private static double settingDouble(ItemStack stack, String key, double global) {
        return LegacyLoliSettings.decimal(stack, key, global);
    }

}
