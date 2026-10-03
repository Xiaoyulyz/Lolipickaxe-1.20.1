package com.anotherstar.lolipickaxe.registry;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.block.SafeEffectTntBlock;
import com.anotherstar.lolipickaxe.block.LoliAltarBlock;
import com.anotherstar.lolipickaxe.block.PasswordWorkbenchBlock;
import com.anotherstar.lolipickaxe.network.SafeAttackEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LoliPickaxe.MOD_ID);

    public static final RegistryObject<Block> LOLI_BLUE_SCREEN_TNT = BLOCKS.register("loli_blue_screen_tnt",
            () -> new SafeEffectTntBlock(SafeAttackEffect.BLUE_SCREEN));
    public static final RegistryObject<Block> LOLI_EXIT_TNT = BLOCKS.register("loli_exit_tnt",
            () -> new SafeEffectTntBlock(SafeAttackEffect.EXIT));
    public static final RegistryObject<Block> LOLI_FAIL_RESPOND_TNT = BLOCKS.register("loli_fail_respond_tnt",
            () -> new SafeEffectTntBlock(SafeAttackEffect.NOT_RESPONDING));

    public static final RegistryObject<Block> LOLI_ALTAR = BLOCKS.register("loli_altar", () ->
            new LoliAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(50.0F, 1200.0F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Block> PASSWORD_WORK_BENCH = BLOCKS.register("password_work_bench", () ->
            new PasswordWorkbenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));

    private ModBlocks() {}
}
