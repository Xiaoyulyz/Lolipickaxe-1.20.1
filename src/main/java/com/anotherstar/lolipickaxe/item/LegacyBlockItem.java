package com.anotherstar.lolipickaxe.item;

import com.anotherstar.lolipickaxe.config.LoliConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

public final class LegacyBlockItem extends BlockItem {
    public enum Tooltip { NONE, ALTAR, BUFF_ATTACK_TNT }
    private final Tooltip tooltipType;

    public LegacyBlockItem(Block block, Properties properties, Tooltip tooltipType) {
        super(block, properties);
        this.tooltipType = tooltipType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (tooltipType == Tooltip.ALTAR) tooltip.add(Component.translatable("loliAltar.use"));
        else if (tooltipType == Tooltip.BUFF_ATTACK_TNT) tooltip.add(Component.translatable(
                LoliConfig.ENABLE_BUFF_ATTACK_TNT.get() ? "buffAttackTNT.enable" : "buffAttackTNT.disable"));
    }
}
