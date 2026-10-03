package com.anotherstar.lolipickaxe.item;

import com.anotherstar.lolipickaxe.network.ModNetwork;
import com.anotherstar.lolipickaxe.network.RefreshHeldItemPacket;
import com.anotherstar.lolipickaxe.registry.ModSounds;
import com.anotherstar.lolipickaxe.util.LegacyTextComponents;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SmallLoliPickaxeItem extends PickaxeItem {
    public static final String CURRENT_DIGGING_RANGE = "LoliCurrentDiggingRange";

     
    public static final Map<UpgradeType, String> NBT_MAP = new LinkedHashMap<>();
    static {
        NBT_MAP.put(UpgradeType.DODGE, "LoliDodge");
        NBT_MAP.put(UpgradeType.DIGGING_SPEED, "LoliDiggingSpeed");
        NBT_MAP.put(UpgradeType.ATTACK_DAMAGE, "LoliAttackDamage");
        NBT_MAP.put(UpgradeType.ATTACK_SPEED, "LoliAttackSpeed");
        NBT_MAP.put(UpgradeType.FORTUNE, "LoliFortuneLevel");
        NBT_MAP.put(UpgradeType.DIGGING_LEVEL, "LoliDiggingLevel");
        NBT_MAP.put(UpgradeType.DIGGING_RANGE, "LoliDiggingRange");
        NBT_MAP.put(UpgradeType.ANTI_INJURY, "LoliAntiInjury");
        NBT_MAP.put(UpgradeType.BUFF, "LoliBuff");
        NBT_MAP.put(UpgradeType.HIT_RANGE, "LoliHitRange");
        NBT_MAP.put(UpgradeType.BACKPACK, "LoliBackpackPage");
        NBT_MAP.put(UpgradeType.AUTO_FURNACE, "LoliAutoFurnace");
        NBT_MAP.put(UpgradeType.FLY, "LoliFly");
    }

    public SmallLoliPickaxeItem() {
        super(LoliTier.INSTANCE, 0, 0.0F, new Properties().stacksTo(1));
    }

    public static ItemStack getFull() {
        ItemStack stack = new ItemStack(com.anotherstar.lolipickaxe.registry.ModItems.SMALL_LOLI_PICKAXE.get());
        CompoundTag tag = stack.getOrCreateTag();
        for (Map.Entry<UpgradeType, String> entry : NBT_MAP.entrySet()) {
            tag.putInt(entry.getValue(), entry.getKey().maxLevel());
        }
        updateEnchantment(stack);
        return stack;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("LoliDiggingSpeed")) {
             
             
             
            return getTransformValue("LoliDiggingSpeed", tag.getInt("LoliDiggingSpeed"));
        }
        return 1.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("LoliDiggingLevel")) return false;
        int level = getTransformValue("LoliDiggingLevel", tag.getInt("LoliDiggingLevel"));
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return level >= 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return level >= 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return level >= 1;
         
        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot, stack);
        double damage = 0.0D;
        double speed = 0.0D;
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("LoliAttackDamage")) damage = getDoubleTransformValue("LoliAttackDamage", tag.getInt("LoliAttackDamage"));
            if (tag.contains("LoliAttackSpeed")) speed = getTransformValue("LoliAttackSpeed", tag.getInt("LoliAttackSpeed"));
        }
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", damage, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", speed, AttributeModifier.Operation.ADDITION));
        return builder.build();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        updateEnchantment(stack);
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    public static void updateEnchantment(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            stack.setTag(new CompoundTag());
            return;
        }
        if (tag.contains("LoliFortuneLevel")) {
            int level = getTransformValue("LoliFortuneLevel", tag.getInt("LoliFortuneLevel"));
            Map<Enchantment, Integer> enchantments = new HashMap<>();
            enchantments.put(Enchantments.BLOCK_FORTUNE, level);
            enchantments.put(Enchantments.MOB_LOOTING, level);
            EnchantmentHelper.setEnchantments(enchantments, stack);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (player.isShiftKeyDown()) {
                if (tag.contains("LoliHitRange")) {
                    if (!level.isClientSide) {
                        int range = getTransformValue("LoliHitRange", tag.getInt("LoliHitRange")) / 2;
                        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                                player.getBoundingBox().inflate(range + 0.7D, range + 0.1D, range + 0.7D),
                                entity -> entity != player
                                        && !(entity instanceof Player)
                                        && !(entity instanceof ArmorStand)
                                        && !(entity instanceof AmbientCreature)
                                        && !(entity instanceof PathfinderMob && !(entity instanceof Enemy)));
                         
                         
                         
                         
                         
                         
                        float attackStrength = player.getAttackStrengthScale(0.5F);
                        for (LivingEntity target : targets) {
                            hurtAreaTarget(player, stack, target, attackStrength);
                        }
                        playSuccess(player);
                    }
                    player.resetAttackStrengthTicker();
                    if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                         
                         
                        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                                new RefreshHeldItemPacket(hand));
                    }
                    return InteractionResultHolder.consume(stack);
                }
            } else if (!level.isClientSide && tag.contains("LoliDiggingRange")) {
                int maxRange = (getTransformValue("LoliDiggingRange", tag.getInt("LoliDiggingRange")) - 1) / 2 + 1;
                if (tag.contains(CURRENT_DIGGING_RANGE)) {
                    tag.putInt(CURRENT_DIGGING_RANGE, (tag.getInt(CURRENT_DIGGING_RANGE) + 1) % maxRange);
                } else {
                    tag.putInt(CURRENT_DIGGING_RANGE, 1);
                }
                player.sendSystemMessage(LegacyTextComponents.translatable("loliPickaxe.range", 1 + 2 * tag.getInt(CURRENT_DIGGING_RANGE)));
                playSuccess(player);
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    public static int getRange(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(CURRENT_DIGGING_RANGE) ? tag.getInt(CURRENT_DIGGING_RANGE) : 0;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        int firstLegacyLine = tooltip.size();
        for (String levelKey : NBT_MAP.values()) {
            if (!tag.contains(levelKey)) continue;
            int upgradeLevel = tag.getInt(levelKey);
            int value = getTransformValue(levelKey, upgradeLevel);
            if (value == Integer.MIN_VALUE) {
                tooltip.add(Component.translatable("smallLoliPickaxe." + levelKey, getDoubleTransformValue(levelKey, upgradeLevel)));
            } else {
                tooltip.add(Component.translatable("smallLoliPickaxe." + levelKey, value));
            }
        }
        for (int i = firstLegacyLine; i < tooltip.size(); i++) {
            tooltip.set(i, legacyDescriptionStyle(tooltip.get(i)));
        }
    }

    private static Component legacyDescriptionStyle(Component component) {
        return component.copy()
                .withStyle(ChatFormatting.GRAY)
                .withStyle(style -> style.withBold(false).withItalic(false));
    }

    public static int getTransformValue(String key, int level) {
        return switch (key) {
            case "LoliDiggingSpeed" -> 4 << level;
            case "LoliDiggingLevel" -> switch (level) {
                case 0 -> 1;
                case 1 -> 3;
                case 2 -> 7;
                case 3 -> 13;
                case 4 -> 21;
                case 5 -> 32;
                default -> -1;
            };
            case "LoliDiggingRange" -> level * 2 + 3;
            case "LoliAttackSpeed" -> 2 << level;
            case "LoliFortuneLevel" -> 1 << level;
            case "LoliBackpackPage" -> 2 << level;
            case "LoliBuff" -> level + 1;
            case "LoliHitRange" -> level * 10 + 6;
            case "LoliAutoFurnace", "LoliFly" -> level;
            default -> Integer.MIN_VALUE;
        };
    }

    public static double getDoubleTransformValue(String key, int level) {
        return switch (key) {
            case "LoliDodge", "LoliAntiInjury" -> (level + 1) / 10.0D;
            case "LoliAttackDamage" -> 4.0D + Math.pow(2.0D, Math.pow(2.0D, level));
            default -> Integer.MIN_VALUE;
        };
    }


    private static boolean hurtAreaTarget(Player player, ItemStack stack, LivingEntity target, float attackStrength) {
        if (target == null || target == player || !target.isAlive()) return false;
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        damage += EnchantmentHelper.getDamageBonus(stack, target.getMobType());
        float strength = Math.max(0.0F, Math.min(1.0F, attackStrength));
        damage *= 0.2F + strength * strength * 0.8F;
        if (!Float.isFinite(damage) || damage <= 0.0F) return false;
        return target.hurt(player.damageSources().playerAttack(player), damage);
    }

    private static void playSuccess(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.playNotifySound(ModSounds.LOLI_SUCCESS.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
