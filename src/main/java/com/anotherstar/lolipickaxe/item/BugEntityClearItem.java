package com.anotherstar.lolipickaxe.item;

import com.anotherstar.lolipickaxe.asm.LoliStructuralPurge;
import com.anotherstar.lolipickaxe.client.ClientPacketHandlers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

 
public final class BugEntityClearItem extends Item {
    public BugEntityClearItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) {
            player.swing(hand, true);
             
             
             
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientPacketHandlers.suppressBugEntity(target));
            return InteractionResult.SUCCESS;
        }

         
         
         
        if (player.level() instanceof ServerLevel serverLevel) {
            LoliStructuralPurge.purge(serverLevel, target, List.of());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("不要对正常实体使用!").withStyle(ChatFormatting.RED));
    }
}
