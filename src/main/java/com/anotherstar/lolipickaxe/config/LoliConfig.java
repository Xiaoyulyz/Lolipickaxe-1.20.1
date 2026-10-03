package com.anotherstar.lolipickaxe.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class LoliConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue MAX_RANGE;
    public static final ForgeConfigSpec.IntValue STORAGE_PAGES;
    public static final ForgeConfigSpec.BooleanValue CANCEL_STACK_LIMIT;
    public static final ForgeConfigSpec.IntValue SLOT_STACK_LIMIT;

    public static final ForgeConfigSpec.BooleanValue MANDATORY_DROP;
    public static final ForgeConfigSpec.BooleanValue STOP_ON_LIQUID;
    public static final ForgeConfigSpec.DoubleValue BLOCK_REACH_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue AUTO_ACCEPT;
    public static final ForgeConfigSpec.BooleanValue THORNS;
    public static final ForgeConfigSpec.BooleanValue KILL_RANGE_ENTITY;
    public static final ForgeConfigSpec.IntValue KILL_RANGE;
    public static final ForgeConfigSpec.BooleanValue AUTO_KILL_RANGE_ENTITY;
    public static final ForgeConfigSpec.IntValue AUTO_KILL_RANGE;
    public static final ForgeConfigSpec.IntValue DROP_PROTECT_TIME;
    public static final ForgeConfigSpec.IntValue EFFECT_DURATION;
    public static final ForgeConfigSpec.BooleanValue FIND_OWNER;
    public static final ForgeConfigSpec.IntValue FIND_OWNER_RANGE;
    public static final ForgeConfigSpec.BooleanValue COMPULSORY_REMOVE;
    public static final ForgeConfigSpec.BooleanValue FORBID_ON_LIVING_UPDATE;
    public static final ForgeConfigSpec.BooleanValue VALID_TO_AMITY_ENTITY;
    public static final ForgeConfigSpec.BooleanValue VALID_TO_ALL_ENTITY;
    public static final ForgeConfigSpec.BooleanValue CLEAR_INVENTORY;
    public static final ForgeConfigSpec.BooleanValue DROP_ITEMS;
    public static final ForgeConfigSpec.BooleanValue KICK_PLAYER;
    public static final ForgeConfigSpec.ConfigValue<String> KICK_MESSAGE;
    public static final ForgeConfigSpec.BooleanValue REINCARNATION;
    public static final ForgeConfigSpec.BooleanValue BEYOND_REDEMPTION;
    public static final ForgeConfigSpec.BooleanValue BLUE_SCREEN_ATTACK;
    public static final ForgeConfigSpec.BooleanValue EXIT_ATTACK;
    public static final ForgeConfigSpec.BooleanValue FAIL_RESPOND_ATTACK;
    public static final ForgeConfigSpec.BooleanValue KILL_FACING;
    public static final ForgeConfigSpec.IntValue KILL_FACING_RANGE;
    public static final ForgeConfigSpec.DoubleValue KILL_FACING_SLOPE;
    public static final ForgeConfigSpec.BooleanValue INFINITE_BATTERY;
    public static final ForgeConfigSpec.BooleanValue INVISIBLE;
    public static final ForgeConfigSpec.BooleanValue SHOW_INVISIBLE;
    public static final ForgeConfigSpec.BooleanValue SPACE_FOLDING;
    public static final ForgeConfigSpec.DoubleValue MAX_TELEPORT_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BUFF_ATTACK_TNT;
    public static final ForgeConfigSpec.DoubleValue LOLI_SPEED;
    public static final ForgeConfigSpec.BooleanValue LOLI_ATTACK;
    public static final ForgeConfigSpec.BooleanValue LOLI_TELEPORT;
    public static final ForgeConfigSpec.DoubleValue CARD_DROP_PROBABILITY;
    public static final ForgeConfigSpec.DoubleValue CARD_ALBUM_DROP_PROBABILITY;
    public static final ForgeConfigSpec.DoubleValue RECORD_DROP_PROBABILITY;
    public static final ForgeConfigSpec.DoubleValue ENTITY_SOUL_DROP_PROBABILITY;
    public static final ForgeConfigSpec.BooleanValue TRIGGER_BREAK_EVENT;

    public static final ForgeConfigSpec.BooleanValue SAFE_ATTACK_SIMULATION;
    public static final ForgeConfigSpec.IntValue SAFE_ATTACK_RADIUS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");
        MAX_RANGE = builder.comment("最大采掘范围（对应旧版 loliPickaxeMaxRange）")
                .defineInRange("loliPickaxeMaxRange", 5, 0, 64);
        STORAGE_PAGES = builder.comment("储藏室最大页数")
                .defineInRange("loliPickaxeMaxPage", 100, 1, 1000);
        CANCEL_STACK_LIMIT = builder.comment("储藏室取消物品堆叠限制")
                .define("loliPickaxeCancelStackLimit", true);
        SLOT_STACK_LIMIT = builder.comment("储藏室最大堆叠数")
                .defineInRange("loliPickaxeSlotStackLimit", 2_000_000_000, 64, Integer.MAX_VALUE);

        MANDATORY_DROP = builder.define("loliPickaxeMandatoryDrop", false);
        STOP_ON_LIQUID = builder.define("loliPickaxeStopOnLiquid", false);
        BLOCK_REACH_DISTANCE = builder.defineInRange("loliPickaxeBlockReachDistance", 0.0D, 0.0D, 20.0D);
        AUTO_ACCEPT = builder.define("loliPickaxeAutoAccept", true);
        THORNS = builder.define("loliPickaxeThorns", true);
        KILL_RANGE_ENTITY = builder.define("loliPickaxeKillRangeEntity", true);
        KILL_RANGE = builder.defineInRange("loliPickaxeKillRange", 50, 0, 100);
        AUTO_KILL_RANGE_ENTITY = builder.define("loliPickaxeAutoKillRangeEntity", false);
        AUTO_KILL_RANGE = builder.defineInRange("loliPickaxeAutoKillRange", 5, 0, 10);
        DROP_PROTECT_TIME = builder.defineInRange("loliPickaxeDropProtectTime", 200, 0, 60_000);
        EFFECT_DURATION = builder.defineInRange("loliPickaxeDuration", 200, 0, Integer.MAX_VALUE);
        FIND_OWNER = builder.define("loliPickaxeFindOwner", true);
        FIND_OWNER_RANGE = builder.defineInRange("loliPickaxeFindOwnerRange", 50, 0, Integer.MAX_VALUE);
        COMPULSORY_REMOVE = builder.define("loliPickaxeCompulsoryRemove", true);
        FORBID_ON_LIVING_UPDATE = builder.define("loliPickaxeForbidOnLivingUpdate", false);
        VALID_TO_AMITY_ENTITY = builder.define("loliPickaxeValidToAmityEntity", true);
        VALID_TO_ALL_ENTITY = builder.define("loliPickaxeValidToAllEntity", false);
        CLEAR_INVENTORY = builder.define("loliPickaxeClearInventory", false);
        DROP_ITEMS = builder.define("loliPickaxeDropItems", false);
        KICK_PLAYER = builder.define("loliPickaxeKickPlayer", false);
        KICK_MESSAGE = builder.define("loliPickaxeKickMessage", "你被氪金萝莉踢出了服务器");
        REINCARNATION = builder.define("loliPickaxeReincarnation", false);
        BEYOND_REDEMPTION = builder.define("loliPickaxeBeyondRedemption", false);
        BLUE_SCREEN_ATTACK = builder.define("loliPickaxeBlueScreenAttack", false);
        EXIT_ATTACK = builder.define("loliPickaxeExitAttack", false);
        FAIL_RESPOND_ATTACK = builder.define("loliPickaxeFailRespondAttack", false);
        KILL_FACING = builder.define("loliPickaxeKillFacing", true);
        KILL_FACING_RANGE = builder.defineInRange("loliPickaxeKillFacingRange", 50, 0, 200);
        KILL_FACING_SLOPE = builder.defineInRange("loliPickaxeKillFacingSlope", 0.1D, 0.0D, 1.0D);
        INFINITE_BATTERY = builder.define("loliPickaxeInfiniteBattery", true);
        INVISIBLE = builder.define("loliPickaxeInvisible", false);
        SHOW_INVISIBLE = builder.define("loliPickaxeShowInvisible", true);
        SPACE_FOLDING = builder.define("loliPickaxeSpaceFolding", true);
        MAX_TELEPORT_DISTANCE = builder.defineInRange("loliPickaxeMaxTeleportDistance", 512.0D, 0.0D, Double.MAX_VALUE);
        ENABLE_BUFF_ATTACK_TNT = builder.define("loliEnableBuffAttackTNT", false);
        LOLI_SPEED = builder.comment("萝莉移动速度倍率；旧配置仍兼容，实际倍率限制在 0.25 到 2，避免过快抖动或零速卡死")
                .defineInRange("loliSpeed", 1.0D, 0.0D, 32.0D);
        LOLI_ATTACK = builder.define("loliAttack", true);
        LOLI_TELEPORT = builder.comment("仅在追击受阻至少 3 秒后尝试安全瞬移，冷却 5 秒；不会每 tick 贴脸瞬移")
                .define("loliTeleport", false);
        CARD_DROP_PROBABILITY = builder.defineInRange("loliCardDropProbability", 0.1D, 0.0D, 1.0D);
        CARD_ALBUM_DROP_PROBABILITY = builder.defineInRange("loliCardAlbumDropProbability", 0.01D, 0.0D, 1.0D);
        RECORD_DROP_PROBABILITY = builder.defineInRange("loliRecordDropProbability", 0.001D, 0.0D, 1.0D);
        ENTITY_SOUL_DROP_PROBABILITY = builder.defineInRange("entitySoulDropProbability", 0.01D, 0.0D, 1.0D);
        TRIGGER_BREAK_EVENT = builder.define("loliPickaxeTriggerBreakEvent", true);

        builder.comment("以下两项只约束危险旧效果的安全模拟，不改变物品 Tooltip 文案。不要执行旧版外部程序/强退/死循环代码。");
        SAFE_ATTACK_SIMULATION = builder.define("safeAttackSimulation", true);
        SAFE_ATTACK_RADIUS = builder.defineInRange("safeAttackRadius", 5, 1, 256);
        builder.pop();
        SPEC = builder.build();
    }

    private LoliConfig() {}
}
