package com.anotherstar.lolipickaxe.registry;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, LoliPickaxe.MOD_ID);

    public static final RegistryObject<SoundEvent> LOLI_SUCCESS = SOUNDS.register("lolisuccess",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(LoliPickaxe.MOD_ID, "lolisuccess")));
    public static final RegistryObject<SoundEvent> LOLI_RECORD = SOUNDS.register("lolirecord",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(LoliPickaxe.MOD_ID, "lolirecord")));

    private ModSounds() {}
}
