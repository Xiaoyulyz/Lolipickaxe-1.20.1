package com.anotherstar.lolipickaxe.registry;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.item.AddonItem;
import com.anotherstar.lolipickaxe.item.CardItem;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import com.anotherstar.lolipickaxe.item.BugEntityClearItem;
import com.anotherstar.lolipickaxe.item.LoliDispersalItem;
import com.anotherstar.lolipickaxe.item.LegacyBlockItem;
import com.anotherstar.lolipickaxe.item.SmallLoliPickaxeItem;
import com.anotherstar.lolipickaxe.item.UpgradeType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LoliPickaxe.MOD_ID);

    public static final RegistryObject<Item> LOLI_PICKAXE = ITEMS.register("loli_pickaxe", LoliPickaxeItem::new);
    public static final RegistryObject<Item> SMALL_LOLI_PICKAXE = ITEMS.register("small_loli_pickaxe", SmallLoliPickaxeItem::new);
    public static final RegistryObject<Item> LOLI_SPAWN_EGG = ITEMS.register("loli_spawn_egg", () ->
            new ForgeSpawnEggItem(ModEntities.LOLI, 0xFFFFFF, 0x000000, new Item.Properties()));

    public static final RegistryObject<Item> LOLI_COAL_ADDON = addon(UpgradeType.DODGE);
    public static final RegistryObject<Item> LOLI_IRON_ADDON = addon(UpgradeType.DIGGING_SPEED);
    public static final RegistryObject<Item> LOLI_GOLD_ADDON = addon(UpgradeType.ATTACK_DAMAGE);
    public static final RegistryObject<Item> LOLI_REDSTONE_ADDON = addon(UpgradeType.ATTACK_SPEED);
    public static final RegistryObject<Item> LOLI_LAPIS_ADDON = addon(UpgradeType.FORTUNE);
    public static final RegistryObject<Item> LOLI_DIAMOND_ADDON = addon(UpgradeType.DIGGING_LEVEL);
    public static final RegistryObject<Item> LOLI_EMERALD_ADDON = addon(UpgradeType.DIGGING_RANGE);
    public static final RegistryObject<Item> LOLI_OBSIDIAN_ADDON = addon(UpgradeType.ANTI_INJURY);
    public static final RegistryObject<Item> LOLI_GLOW_ADDON = addon(UpgradeType.BUFF);
    public static final RegistryObject<Item> LOLI_QUARTZ_ADDON = addon(UpgradeType.HIT_RANGE);
    public static final RegistryObject<Item> LOLI_NETHER_STAR_ADDON = addon(UpgradeType.BACKPACK);
    public static final RegistryObject<Item> LOLI_AUTO_FURNACE_ADDON = addon(UpgradeType.AUTO_FURNACE);
    public static final RegistryObject<Item> LOLI_FLY_ADDON = addon(UpgradeType.FLY);
    public static final RegistryObject<Item> LOLI_ENTITY_SOUL_ADDON = addon(UpgradeType.ENTITY_SOUL);

    public static final RegistryObject<Item> LOLI_DISPERSAL = ITEMS.register("loli_dispersal", LoliDispersalItem::new);
    public static final RegistryObject<Item> BUG_ENTITY_CLEAR = ITEMS.register("bug_entity_clear", BugEntityClearItem::new);
    public static final RegistryObject<Item> LOLI_CARD = ITEMS.register("loli_card", () -> new CardItem(CardItem.Kind.LOCAL));
    public static final RegistryObject<Item> LOLI_CARD_ALBUM = ITEMS.register("loli_card_album", () -> new CardItem(CardItem.Kind.ALBUM));
    public static final RegistryObject<Item> LOLI_CARD_ONLINE = ITEMS.register("loli_card_online", () -> new CardItem(CardItem.Kind.ONLINE));
    public static final RegistryObject<Item> LOLI_RECORD = ITEMS.register("loli_record", () ->
            new RecordItem(1, ModSounds.LOLI_RECORD, new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 1243));

    public static final RegistryObject<Item> LOLI_BLUE_SCREEN_TNT = blockItem("loli_blue_screen_tnt", ModBlocks.LOLI_BLUE_SCREEN_TNT, LegacyBlockItem.Tooltip.BUFF_ATTACK_TNT);
    public static final RegistryObject<Item> LOLI_EXIT_TNT = blockItem("loli_exit_tnt", ModBlocks.LOLI_EXIT_TNT, LegacyBlockItem.Tooltip.BUFF_ATTACK_TNT);
    public static final RegistryObject<Item> LOLI_FAIL_RESPOND_TNT = blockItem("loli_fail_respond_tnt", ModBlocks.LOLI_FAIL_RESPOND_TNT, LegacyBlockItem.Tooltip.BUFF_ATTACK_TNT);
    public static final RegistryObject<Item> LOLI_ALTAR = blockItem("loli_altar", ModBlocks.LOLI_ALTAR, LegacyBlockItem.Tooltip.ALTAR);
    public static final RegistryObject<Item> PASSWORD_WORK_BENCH = blockItem("password_work_bench", ModBlocks.PASSWORD_WORK_BENCH, LegacyBlockItem.Tooltip.NONE);

    private static RegistryObject<Item> addon(UpgradeType type) {
        return ITEMS.register(type.id(), () -> new AddonItem(type));
    }

    private static RegistryObject<Item> blockItem(String id, RegistryObject<? extends net.minecraft.world.level.block.Block> block, LegacyBlockItem.Tooltip tooltip) {
        return ITEMS.register(id, () -> new LegacyBlockItem(block.get(), new Item.Properties(), tooltip));
    }

    private ModItems() {}
}
