package com.anotherstar.lolipickaxe.item;

import com.anotherstar.lolipickaxe.client.ClientCardScreens;
import com.anotherstar.lolipickaxe.util.LegacyCardNameCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

public final class CardItem extends Item {
    public enum Kind { LOCAL, ALBUM, ONLINE }

    private static final String LEGACY_SUMMON = "#U53ec#U5524#U796d#U575b#U6446#U653e#U65b9#U5f0f.png";
    private static final String LEGACY_GROUP = "#U5c0f#U83ab#U5973#U513f";

    private final Kind kind;

    public CardItem(Kind kind) {
        super(new Properties().stacksTo(64));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if ((kind == Kind.LOCAL || kind == Kind.ALBUM) && !stack.hasTag()) return 1;
        return 64;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component original = super.getName(stack);
        String raw = original.getString();
        if (!LegacyCardNameCodec.containsEncodedText(raw)) return original;
        return Component.literal(LegacyCardNameCodec.decode(raw)).withStyle(original.getStyle());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    ClientCardScreens.open(kind, hand, stack.copy(), player.isShiftKeyDown()));
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide) {
            if (kind == Kind.LOCAL && (!stack.hasTag() || !stack.getTag().contains("picture"))) {
                stack.getOrCreateTag().putString("picture",
                        level.random.nextBoolean() ? "gk_head_portrait.png" : LEGACY_SUMMON);
            } else if (kind == Kind.ALBUM && (!stack.hasTag() || !stack.getTag().contains("PictureGroup"))) {
                stack.getOrCreateTag().putString("PictureGroup", LEGACY_GROUP);
            }

             
             
            if (stack.hasCustomHoverName()) {
                String rawName = stack.getHoverName().getString();
                if (LegacyCardNameCodec.containsEncodedText(rawName)) {
                    stack.setHoverName(Component.literal(LegacyCardNameCodec.decode(rawName)));
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!stack.hasTag()) return;
        if (kind == Kind.LOCAL && stack.getTag().contains("picture")) {
            addDecodedLine(tooltip, stack.getTag().getString("picture"));
        } else if (kind == Kind.ALBUM && stack.getTag().contains("PictureGroup")) {
            addDecodedLine(tooltip, stack.getTag().getString("PictureGroup"));
        } else if (kind == Kind.ONLINE && stack.getTag().contains("ImageUrl")) {
            String url = stack.getTag().getString("ImageUrl");
            if (!url.isBlank()) tooltip.add(Component.literal(url).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void addDecodedLine(List<Component> tooltip, String raw) {
        String visible = LegacyCardNameCodec.decode(raw);
        if (!visible.isBlank()) tooltip.add(Component.literal(visible).withStyle(ChatFormatting.DARK_GRAY));
    }
}
