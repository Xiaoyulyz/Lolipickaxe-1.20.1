package com.anotherstar.lolipickaxe.item;

import java.util.Arrays;
import java.util.Optional;

public enum UpgradeType {
    DODGE("loli_coal_addon", "item.loliCoalAddon.name", "LoliDodge", 9),
    DIGGING_SPEED("loli_iron_addon", "item.loliIronAddon.name", "LoliDiggingSpeed", 9),
    ATTACK_DAMAGE("loli_gold_addon", "item.loliGoldAddon.name", "LoliAttackDamage", 6),
    ATTACK_SPEED("loli_redstone_addon", "item.loliRedstoneAddon.name", "LoliAttackSpeed", 3),
    FORTUNE("loli_lapis_addon", "item.loliLapisAddon.name", "LoliFortuneLevel", 5),
    DIGGING_LEVEL("loli_diamond_addon", "item.loliDiamondAddon.name", "LoliDiggingLevel", 5),
    DIGGING_RANGE("loli_emerald_addon", "item.loliEmeraldAddon.name", "LoliDiggingRange", 4),
    ANTI_INJURY("loli_obsidian_addon", "item.loliObsidianAddon.name", "LoliAntiInjury", 9),
    BUFF("loli_glow_addon", "item.loliGlowAddon.name", "LoliBuff", 2),
    HIT_RANGE("loli_quartz_addon", "item.loliQuartzAddon.name", "LoliHitRange", 2),
    BACKPACK("loli_nether_star_addon", "item.loliNetherStarAddon.name", "LoliBackpackPage", 4),
    AUTO_FURNACE("loli_auto_furnace_addon", "item.loliAutoFurnaceAddon.name", "LoliAutoFurnace", 0),
    FLY("loli_fly_addon", "item.loliFlyAddon.name", "LoliFly", 0),
    ENTITY_SOUL("loli_entity_soul_addon", "item.loliEntitySoulAddon.name", "LoliEntitySoul", 6);

    private final String id;
    private final String nameKey;
    private final String nbtKey;
    private final int maxLevel;

    UpgradeType(String id, String nameKey, String nbtKey, int maxLevel) {
        this.id = id;
        this.nameKey = nameKey;
        this.nbtKey = nbtKey;
        this.maxLevel = maxLevel;
    }

    public String id() { return id; }
    public String nameKey() { return nameKey; }
    public String nbtKey() { return nbtKey; }
    public int maxLevel() { return maxLevel; }

    public static Optional<UpgradeType> byId(String id) {
        return Arrays.stream(values()).filter(type -> type.id.equals(id)).findFirst();
    }
}
