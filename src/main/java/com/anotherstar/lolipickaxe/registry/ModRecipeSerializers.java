package com.anotherstar.lolipickaxe.registry;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.recipe.LoliPickaxeUpgradeRecipe;
import com.anotherstar.lolipickaxe.recipe.SmallLoliUpgradeRecipe;
import com.anotherstar.lolipickaxe.recipe.SuperpositionRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, LoliPickaxe.MOD_ID);

    public static final RegistryObject<RecipeSerializer<SuperpositionRecipe>> SUPERPOSITION = SERIALIZERS.register(
            "loli_superposition", () -> new SimpleCraftingRecipeSerializer<>(SuperpositionRecipe::new));
    public static final RegistryObject<RecipeSerializer<SmallLoliUpgradeRecipe>> SMALL_LOLI_UPGRADE = SERIALIZERS.register(
            "small_loli_pickaxe_up", () -> new SimpleCraftingRecipeSerializer<>(SmallLoliUpgradeRecipe::new));
    public static final RegistryObject<RecipeSerializer<LoliPickaxeUpgradeRecipe>> LOLI_PICKAXE_UPGRADE = SERIALIZERS.register(
            "loli_pickaxe", () -> new SimpleCraftingRecipeSerializer<>(LoliPickaxeUpgradeRecipe::new));

    private ModRecipeSerializers() {}
}
