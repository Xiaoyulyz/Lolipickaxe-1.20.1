package com.anotherstar.lolipickaxe.damage;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

 
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> LOLI = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation(LoliPickaxe.MOD_ID, "loli"));

    public static DamageSource loli(Level level, @Nullable Entity source) {
        Holder<DamageType> type = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(LOLI);
        return new DamageSource(type, source, source);
    }

    private ModDamageTypes() {}
}
