package com.anotherstar.lolipickaxe.registry;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.entity.LoliEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LoliPickaxe.MOD_ID);

    public static final RegistryObject<EntityType<LoliEntity>> LOLI = ENTITIES.register("loli", () ->
            EntityType.Builder.of(LoliEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.5F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build(LoliPickaxe.MOD_ID + ":loli"));

    @Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Attributes {
        @SubscribeEvent
        public static void register(EntityAttributeCreationEvent event) {
            event.put(LOLI.get(), LoliEntity.createAttributes().build());
        }
    }

    private ModEntities() {}
}
