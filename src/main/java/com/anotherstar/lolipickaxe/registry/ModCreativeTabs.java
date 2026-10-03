package com.anotherstar.lolipickaxe.registry;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.item.AddonItem;
import com.anotherstar.lolipickaxe.item.CardItem;
import com.anotherstar.lolipickaxe.item.SmallLoliPickaxeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LoliPickaxe.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("loli", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.loli"))
            .icon(() -> ModItems.LOLI_PICKAXE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.LOLI_PICKAXE.get().getDefaultInstance());
                output.accept(new ItemStack(ModItems.SMALL_LOLI_PICKAXE.get()));
                output.accept(SmallLoliPickaxeItem.getFull());
                output.accept(ModItems.LOLI_DISPERSAL.get());
                output.accept(ModItems.BUG_ENTITY_CLEAR.get());
                output.accept(ModItems.LOLI_SPAWN_EGG.get());

                ItemStack portrait = new ItemStack(ModItems.LOLI_CARD.get());
                portrait.getOrCreateTag().putString("picture", "gk_head_portrait.png");
                output.accept(portrait);
                ItemStack altarCard = new ItemStack(ModItems.LOLI_CARD.get());
                altarCard.getOrCreateTag().putString("picture", "#U53ec#U5524#U796d#U575b#U6446#U653e#U65b9#U5f0f.png");
                output.accept(altarCard);
                ItemStack album = new ItemStack(ModItems.LOLI_CARD_ALBUM.get());
                album.getOrCreateTag().putString("PictureGroup", "#U5c0f#U83ab#U5973#U513f");
                output.accept(album);

                output.accept(ModItems.LOLI_CARD_ONLINE.get());
                for (String url : new String[] {
                        "https://bigimg.cheerfun.dev/get/https://i.pximg.net/img-original/img/2017/03/18/03/44/39/61965296_p0.png",
                        "https://bigimg.cheerfun.dev/get/https://i.pximg.net/img-original/img/2015/10/23/18/05/06/53170539_p0.jpg",
                        "https://bigimg.cheerfun.dev/get/https://i.pximg.net/img-original/img/2015/09/27/07/15/20/52735806_p0.jpg"}) {
                    ItemStack online = new ItemStack(ModItems.LOLI_CARD_ONLINE.get());
                    online.getOrCreateTag().putString("ImageUrl", url);
                    output.accept(online);
                }
                output.accept(ModItems.LOLI_RECORD.get());
                output.accept(ModItems.LOLI_BLUE_SCREEN_TNT.get());
                output.accept(ModItems.LOLI_EXIT_TNT.get());
                output.accept(ModItems.LOLI_FAIL_RESPOND_TNT.get());
                output.accept(ModItems.LOLI_ALTAR.get());
                output.accept(ModItems.PASSWORD_WORK_BENCH.get());

            }).build());

    public static final RegistryObject<CreativeModeTab> RECIPE = TABS.register("loli_recipe", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.loliRecipe"))
            .icon(() -> ((AddonItem) ModItems.LOLI_ENTITY_SOUL_ADDON.get()).withLevel(((AddonItem) ModItems.LOLI_ENTITY_SOUL_ADDON.get()).type().maxLevel(), 1))
            .displayItems((parameters, output) -> ModItems.ITEMS.getEntries().forEach(entry -> {
                if (entry.get() instanceof AddonItem addon) {
                    for (int level = 0; level <= addon.type().maxLevel(); level++) output.accept(addon.withLevel(level, 1));
                }
            })).build());

    private ModCreativeTabs() {}
}
