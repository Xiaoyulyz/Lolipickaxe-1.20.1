package com.anotherstar.lolipickaxe.block;

import com.anotherstar.lolipickaxe.menu.PasswordWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

 
public final class PasswordWorkbenchBlock extends Block {
    private static final Component TITLE = Component.translatable("container.crafting");

    public PasswordWorkbenchBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new PasswordWorkbenchMenu(containerId, inventory, level, pos),
                    TITLE);
            NetworkHooks.openScreen(serverPlayer, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
