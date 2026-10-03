package com.anotherstar.lolipickaxe.event;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.config.LegacyLoliSettings;
import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.inventory.PagedPickaxeStorage;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import com.anotherstar.lolipickaxe.registry.ModEnchantments;
import com.anotherstar.lolipickaxe.registry.ModItems;
import com.anotherstar.lolipickaxe.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

 
@Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID)
public final class LegacyMiningEvents {
    private static boolean breaking;

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (breaking || event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack tool = event.getItemStack();
        if (!tool.is(ModItems.LOLI_PICKAXE.get()) || player.getAbilities().instabuild) return;
        event.setCanceled(true);
        breakCube(player, tool, event.getPos());
    }

    private static void breakCube(ServerPlayer player, ItemStack tool, BlockPos origin) {
        ServerLevel level = player.serverLevel();
        int range = Math.max(0, Math.min(LoliPickaxeItem.getRange(tool), LoliConfig.MAX_RANGE.get()));
        boolean mandatoryDrop = LegacyLoliSettings.bool(tool, "loliPickaxeMandatoryDrop", LoliConfig.MANDATORY_DROP.get());
        boolean autoAccept = LegacyLoliSettings.bool(tool, "loliPickaxeAutoAccept", LoliConfig.AUTO_ACCEPT.get());
        boolean autoFurnace = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.AUTO_FURNACE.get(), tool) > 0;
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool);
        int silk = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool);
        List<ItemStack> drops = new ArrayList<>();
        float smeltingExperience = 0.0F;

        breaking = true;
        try {
            for (int dx = -range; dx <= range; dx++) {
                for (int dy = -range; dy <= range; dy++) {
                    for (int dz = -range; dz <= range; dz++) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(pos);
                        if (state.isAir()) continue;
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        List<ItemStack> local = new ArrayList<>(Block.getDrops(state, level, pos, blockEntity, player, tool));
                        if (autoFurnace) {
                            for (int i = 0; i < local.size(); i++) {
                                Smelted smelted = smelt(level, local.get(i), fortune);
                                if (smelted != null) {
                                    local.set(i, smelted.stack());
                                    smeltingExperience += smelted.experience();
                                }
                            }
                        }
                        local.removeIf(ItemStack::isEmpty);
                        if (local.isEmpty() && mandatoryDrop) {
                            ItemStack forced = new ItemStack(state.getBlock());
                            if (!forced.isEmpty()) local.add(forced);
                        }
                        drops.addAll(local);
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        } finally {
            breaking = false;
        }

        drops.removeIf(stack -> stack.isEmpty() || blacklisted(tool, stack));
        for (ItemStack drop : drops) {
            ItemStack remainder = autoAccept ? PagedPickaxeStorage.insert(tool, drop) : drop;
            if (!remainder.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, origin.getX() + 0.5D, origin.getY() + 0.5D,
                        origin.getZ() + 0.5D, remainder));
            }
        }
        if ((int) smeltingExperience > 0) ExperienceOrb.award(level, Vec3.atCenterOf(origin), (int) smeltingExperience);
        player.playNotifySound(ModSounds.LOLI_SUCCESS.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static Smelted smelt(ServerLevel level, ItemStack source, int fortune) {
        if (source.isEmpty()) return null;
        SimpleContainer input = new SimpleContainer(source.copy());
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
        if (recipe.isEmpty()) return null;
        ItemStack result = recipe.get().getResultItem(level.registryAccess()).copy();
        if (result.isEmpty()) return null;
        int multiplier = 1;
        if (fortune > 0) {
            multiplier = level.random.nextInt(fortune + 2);
            if (multiplier == 0) multiplier = 1;
        }
        int outputCount = result.getCount() * source.getCount() * multiplier;
        result.setCount(outputCount);
        float experience = recipe.get().getExperience() * source.getCount() * multiplier;
        return new Smelted(result, experience);
    }

    private static boolean blacklisted(ItemStack tool, ItemStack drop) {
        CompoundTag tag = tool.getTag();
        if (tag == null || !tag.contains("Blacklist", Tag.TAG_LIST)) return false;
        ListTag list = tag.getList("Blacklist", Tag.TAG_COMPOUND);
        String id = BuiltInRegistries.ITEM.getKey(drop.getItem()).toString();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag black = list.getCompound(i);
            if (!black.contains("Name") || !black.contains("Damage")) continue;
            if (id.equals(black.getString("Name")) && drop.getDamageValue() == black.getInt("Damage")) return true;
        }
        return false;
    }

    private record Smelted(ItemStack stack, float experience) {}
    private LegacyMiningEvents() {}
}
