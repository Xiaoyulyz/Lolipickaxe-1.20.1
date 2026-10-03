package com.anotherstar.lolipickaxe.event;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.asm.LoliInventoryGuard;
import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.inventory.PagedPickaxeStorage;
import com.anotherstar.lolipickaxe.item.SmallLoliPickaxeItem;
import com.anotherstar.lolipickaxe.registry.ModItems;
import com.anotherstar.lolipickaxe.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID)
public final class CommonEvents {
    private static final ThreadLocal<Boolean> AREA_BREAK = ThreadLocal.withInitial(() -> false);

     
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (AREA_BREAK.get() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        ItemStack tool = player.getMainHandItem();
        if (!tool.is(ModItems.SMALL_LOLI_PICKAXE.get())) return;
        int radius = SmallLoliPickaxeItem.getRange(tool);
        CompoundTag tag = tool.getTag();
        boolean autoFurnace = tag != null && tag.contains("LoliAutoFurnace")
                && SmallLoliPickaxeItem.getTransformValue("LoliAutoFurnace", tag.getInt("LoliAutoFurnace")) == 0;
        boolean hasInventory = tag != null && tag.contains("LoliBackpackPage");
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool);
        ServerLevel level = player.serverLevel();
        BlockPos origin = event.getPos();
        float furnaceExperience = 0.0F;
        List<ItemStack> drops = new ArrayList<>();

        event.setCanceled(true);
        AREA_BREAK.set(true);
        try {
            for (BlockPos mutable : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius),
                    origin.offset(radius, radius, radius))) {
                BlockPos pos = mutable.immutable();
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.getDestroySpeed(level, pos) <= 0.0F) continue;
                if (state.getCollisionShape(level, pos).isEmpty()) continue;
                if (!((SmallLoliPickaxeItem) tool.getItem()).isCorrectToolForDrops(tool, state)) continue;
                BlockEntity blockEntity = level.getBlockEntity(pos);
                List<ItemStack> local = new ArrayList<>(Block.getDrops(state, level, pos, blockEntity, player, tool.copy()));
                if (autoFurnace) {
                    for (int i = 0; i < local.size(); i++) {
                        Smelted result = smelt(level, local.get(i), fortune);
                        if (result != null) {
                            local.set(i, result.stack());
                            furnaceExperience += result.experience();
                        }
                    }
                }
                local.removeIf(ItemStack::isEmpty);
                drops.addAll(local);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        } finally {
            AREA_BREAK.set(false);
        }

        drops.removeIf(drop -> drop.isEmpty() || blacklisted(tool, drop));
        for (ItemStack drop : drops) {
            ItemStack remainder = hasInventory ? PagedPickaxeStorage.insert(tool, drop) : drop;
            if (!remainder.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, origin.getX() + 0.5D, origin.getY() + 0.5D,
                        origin.getZ() + 0.5D, remainder));
            }
        }
        if ((int) furnaceExperience > 0) ExperienceOrb.award(level, Vec3.atCenterOf(origin), (int) furnaceExperience);
        player.playNotifySound(ModSounds.LOLI_SUCCESS.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static Smelted smelt(ServerLevel level, ItemStack source, int fortune) {
        if (source.isEmpty()) return null;
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(source.copy()), level);
        if (recipe.isEmpty()) return null;
        ItemStack output = recipe.get().getResultItem(level.registryAccess()).copy();
        if (output.isEmpty()) return null;
        int multiplier = 1;
        if (fortune > 0) {
            multiplier = level.random.nextInt(fortune + 2);
            if (multiplier == 0) multiplier = 1;
        }
        output.setCount(output.getCount() * source.getCount() * multiplier);
        return new Smelted(output, recipe.get().getExperience() * source.getCount() * multiplier);
    }

    private static boolean blacklisted(ItemStack tool, ItemStack drop) {
        CompoundTag tag = tool.getTag();
        if (tag == null || !tag.contains("Blacklist", Tag.TAG_LIST)) return false;
        ListTag list = tag.getList("Blacklist", Tag.TAG_COMPOUND);
        String id = BuiltInRegistries.ITEM.getKey(drop.getItem()).toString();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag black = list.getCompound(i);
            if (black.contains("Name") && black.contains("Damage")
                    && id.equals(black.getString("Name")) && drop.getDamageValue() == black.getInt("Damage")) return true;
        }
        return false;
    }

     
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemEntity itemEntity = event.getEntity();
        ItemStack stack = itemEntity.getItem();
        if (!stack.is(ModItems.LOLI_PICKAXE.get())) return;

        Player player = event.getPlayer();
        int protectMs = LoliConfig.DROP_PROTECT_TIME.get();
        long now = System.currentTimeMillis();
        boolean allowDrop = protectMs <= 0;

        if (protectMs > 0) {
            CompoundTag tag = stack.getOrCreateTag();
            if (tag.contains("preDropTime")) {
                long previous = tag.getLong("preDropTime");
                allowDrop = now - previous < protectMs;
            }
            tag.putLong("preDropTime", now);
        }

        if (!allowDrop) {
             
             
             
            event.setCanceled(true);
            ItemStack restored = stack.copy();
            int selected = player.getInventory().selected;
            if (player.getInventory().getItem(selected).isEmpty()) {
                player.getInventory().setItem(selected, restored);
            } else if (!player.addItem(restored)) {
                player.containerMenu.setCarried(restored);
            }
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            return;
        }

         
         
         
        itemEntity.setPos(player.getX(),
                player.getY() - 0.30000001192092896D + player.getEyeHeight(),
                player.getZ());
        itemEntity.setInvulnerable(true);
        itemEntity.setPickUpDelay(0);
        LoliInventoryGuard.markIntentionalDrop(player);
    }

    private record Smelted(ItemStack stack, float experience) {}
    private CommonEvents() {}
}
