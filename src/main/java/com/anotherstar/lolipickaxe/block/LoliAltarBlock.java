package com.anotherstar.lolipickaxe.block;

import com.anotherstar.lolipickaxe.entity.LoliEntity;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import com.anotherstar.lolipickaxe.item.SmallLoliPickaxeItem;
import com.anotherstar.lolipickaxe.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

 
public final class LoliAltarBlock extends Block {
    private static final int RADIUS = 31;
     
    private static final String[] LEGACY_PATTERN = {
            "###.###.###.###.###.###.###.###.###.###.###.###.###.###.###.###",
            "##...#...#...#...#...#...#...#...#...#...#...#...#...#...#...##",
            "#.###.....###.....###.....###.....###.....###.....###.....###.#",
            "..##.......#.......#.......#.......#.......#.......#.......##..",
            "#.#.###.###.........###.###.........###.###.........###.###.#.#",
            "##..##...#...........#...#...........#...#...........#...##..##",
            "#...#.###.............###.............###.............###.#...#",
            "......##...............#...............#...............##......",
            "#...#.#.###.###.###.###.................###.###.###.###.#.#...#",
            "##..##..##...#...#...#...................#...#...#...##..##..##",
            "#.#.#...#.###.....###.....................###.....###.#...#.#.#",
            "..##......##.......#.......................#.......##......##..",
            "#.#.....#.#.###.###.........................###.###.#.#.....#.#",
            "##......##..##...#...........................#...##..##......##",
            "#.......#...#.###.............................###.#...#.......#",
            "..............##...............................##..............",
            "#.......#...#.#.###.###.###.###.###.###.###.###.#.#...#.......#",
            "##......##..##..##...#...#...#...#...#...#...##..##..##......##",
            "#.#.....#.#.#...#.###.....###.....###.....###.#...#.#.#.....#.#",
            "..##......##......##.......#.......#.......##......##......##..",
            "#.#.#...#.#.....#.#.###.###.........###.###.#.#.....#.#...#.#.#",
            "##..##..##......##..##...#...........#...##..##......##..##..##",
            "#...#.#.#.......#...#.###.............###.#...#.......#.#.#...#",
            "......##..............##...............##..............##......",
            "#...#.#.........#...#.#.###.###.###.###.#.#...#.........#.#...#",
            "##..##..........##..##..##...#...#...##..##..##..........##..##",
            "#.#.#...........#.#.#...#.###.....###.#...#.#.#...........#.#.#",
            "..##..............##......##.......##......##..............##..",
            "#.#.............#.#.....#.#.###.###.#.#.....#.#.............#.#",
            "##..............##......##..##...##..##......##..............##",
            "#...............#.......#...#.###.#...#.......#...............#",
            "..............................###..............................",
            "#...............#.......#...#.###.#...#.......#...............#",
            "##..............##......##..##...##..##......##..............##",
            "#.#.............#.#.....#.#.###.###.#.#.....#.#.............#.#",
            "..##..............##......##.......##......##..............##..",
            "#.#.#...........#.#.#...#.###.....###.#...#.#.#...........#.#.#",
            "##..##..........##..##..##...#...#...##..##..##..........##..##",
            "#...#.#.........#...#.#.###.###.###.###.#.#...#.........#.#...#",
            "......##..............##...............##..............##......",
            "#...#.#.#.......#...#.###.............###.#...#.......#.#.#...#",
            "##..##..##......##..##...#...........#...##..##......##..##..##",
            "#.#.#...#.#.....#.#.###.###.........###.###.#.#.....#.#...#.#.#",
            "..##......##......##.......#.......#.......##......##......##..",
            "#.#.....#.#.#...#.###.....###.....###.....###.#...#.#.#.....#.#",
            "##......##..##..##...#...#...#...#...#...#...##..##..##......##",
            "#.......#...#.#.###.###.###.###.###.###.###.###.#.#...#.......#",
            "..............##...............................##..............",
            "#.......#...#.###.............................###.#...#.......#",
            "##......##..##...#...........................#...##..##......##",
            "#.#.....#.#.###.###.........................###.###.#.#.....#.#",
            "..##......##.......#.......................#.......##......##..",
            "#.#.#...#.###.....###.....................###.....###.#...#.#.#",
            "##..##..##...#...#...#...................#...#...#...##..##..##",
            "#...#.#.###.###.###.###.................###.###.###.###.#.#...#",
            "......##...............#...............#...............##......",
            "#...#.###.............###.............###.............###.#...#",
            "##..##...#...........#...#...........#...#...........#...##..##",
            "#.#.###.###.........###.###.........###.###.........###.###.#.#",
            "..##.......#.......#.......#.......#.......#.......#.......##..",
            "#.###.....###.....###.....###.....###.....###.....###.....###.#",
            "##...#...#...#...#...#...#...#...#...#...#...#...#...#...#...##",
            "###.###.###.###.###.###.###.###.###.###.###.###.###.###.###.###"
    };

    public LoliAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
        if (LEGACY_PATTERN.length != 63) throw new IllegalStateException("Legacy altar pattern must have 63 columns");
        for (String column : LEGACY_PATTERN) {
            if (column.length() != 63) throw new IllegalStateException("Legacy altar pattern must be 63 x 63");
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) {
                if (player.getAbilities().instabuild && player.isCrouching()) placeLegacyPattern(level, pos);
            } else if (stack.getItem() instanceof LoliPickaxeItem
                    || stack.getItem() instanceof SmallLoliPickaxeItem) {
                if (matchesLegacyPattern(level, pos)) {
                    clearPattern(level, pos);
                    LoliEntity loli = ModEntities.LOLI.get().create(level);
                    if (loli != null) {
                        loli.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
                        level.addFreshEntity(loli);
                    }
                }
            }
        }
         
        return InteractionResult.SUCCESS;
    }

    private void placeLegacyPattern(Level level, BlockPos center) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                BlockPos target = center.offset(dx, 0, dz);
                level.setBlock(target, patternAt(dx, dz) ? defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private boolean matchesLegacyPattern(Level level, BlockPos center) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                BlockState actual = level.getBlockState(center.offset(dx, 0, dz));
                if (patternAt(dx, dz)) {
                    if (!actual.is(this)) return false;
                } else if (!actual.isAir()) return false;
            }
        }
        return true;
    }

    private void clearPattern(Level level, BlockPos center) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                level.setBlock(center.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static boolean patternAt(int dx, int dz) {
        return LEGACY_PATTERN[dx + RADIUS].charAt(dz + RADIUS) == '#';
    }
}
