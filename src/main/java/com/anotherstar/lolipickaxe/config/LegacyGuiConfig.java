package com.anotherstar.lolipickaxe.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.List;

 
public final class LegacyGuiConfig {
    public enum Type { BOOLEAN, INT, DOUBLE, STRING }
    public record Option(String key, String comment, Type type, double min, double max) {}

    public static final List<Option> OPTIONS = List.of(
            bool("loliPickaxeMandatoryDrop", "强制掉落方块"),
            bool("loliPickaxeStopOnLiquid", "显示流体边框"),
            decimal("loliPickaxeBlockReachDistance", "挖掘距离", 0, 20),
            bool("loliPickaxeAutoAccept", "自动收纳进储藏室"),
            bool("loliPickaxeThorns", "反伤"),
            bool("loliPickaxeKillRangeEntity", "潜行右键杀死周围实体"),
            integer("loliPickaxeKillRange", "潜行右键杀死周围实体的范围", 0, 100),
            bool("loliPickaxeAutoKillRangeEntity", "自动杀死周围实体"),
            integer("loliPickaxeAutoKillRange", "自动杀死周围实体的范围", 0, 10),
            bool("loliPickaxeCompulsoryRemove", "强制清除生物"),
            bool("loliPickaxeValidToAmityEntity", "范围攻击对非怪物有效"),
            bool("loliPickaxeValidToAllEntity", "对全部实体有效"),
            bool("loliPickaxeClearInventory", "清空玩家背包"),
            bool("loliPickaxeDropItems", "缴械"),
            bool("loliPickaxeKickPlayer", "踢出玩家"),
            string("loliPickaxeKickMessage", "踢出玩家消息"),
            bool("loliPickaxeReincarnation", "伊邪那美(需同时开启踢出玩家)"),
            bool("loliPickaxeBeyondRedemption", "灵魂超度"),
            bool("loliPickaxeBlueScreenAttack", "蓝屏打击"),
            bool("loliPickaxeExitAttack", "蹦溃打击"),
            bool("loliPickaxeFailRespondAttack", "未响应打击"),
            bool("loliPickaxeKillFacing", "左键范围攻击"),
            integer("loliPickaxeKillFacingRange", "范围攻击范围", 0, 200),
            decimal("loliPickaxeKillFacingSlope", "范围攻击斜率", 0, 1),
            bool("loliPickaxeInfiniteBattery", "超级电池"),
            bool("loliPickaxeInvisible", "视觉迷惑"),
            bool("loliPickaxeShowInvisible", "显示隐身生物")
    );

    private static Option bool(String key, String comment) { return new Option(key, comment, Type.BOOLEAN, 0, 1); }
    private static Option integer(String key, String comment, int min, int max) { return new Option(key, comment, Type.INT, min, max); }
    private static Option decimal(String key, String comment, double min, double max) { return new Option(key, comment, Type.DOUBLE, min, max); }
    private static Option string(String key, String comment) { return new Option(key, comment, Type.STRING, 0, 0); }

    public static boolean defaultBoolean(String key) {
        return switch (key) {
            case "loliPickaxeMandatoryDrop" -> LoliConfig.MANDATORY_DROP.get();
            case "loliPickaxeStopOnLiquid" -> LoliConfig.STOP_ON_LIQUID.get();
            case "loliPickaxeAutoAccept" -> LoliConfig.AUTO_ACCEPT.get();
            case "loliPickaxeThorns" -> LoliConfig.THORNS.get();
            case "loliPickaxeKillRangeEntity" -> LoliConfig.KILL_RANGE_ENTITY.get();
            case "loliPickaxeAutoKillRangeEntity" -> LoliConfig.AUTO_KILL_RANGE_ENTITY.get();
            case "loliPickaxeCompulsoryRemove" -> LoliConfig.COMPULSORY_REMOVE.get();
            case "loliPickaxeValidToAmityEntity" -> LoliConfig.VALID_TO_AMITY_ENTITY.get();
            case "loliPickaxeValidToAllEntity" -> LoliConfig.VALID_TO_ALL_ENTITY.get();
            case "loliPickaxeClearInventory" -> LoliConfig.CLEAR_INVENTORY.get();
            case "loliPickaxeDropItems" -> LoliConfig.DROP_ITEMS.get();
            case "loliPickaxeKickPlayer" -> LoliConfig.KICK_PLAYER.get();
            case "loliPickaxeReincarnation" -> LoliConfig.REINCARNATION.get();
            case "loliPickaxeBeyondRedemption" -> LoliConfig.BEYOND_REDEMPTION.get();
            case "loliPickaxeBlueScreenAttack" -> LoliConfig.BLUE_SCREEN_ATTACK.get();
            case "loliPickaxeExitAttack" -> LoliConfig.EXIT_ATTACK.get();
            case "loliPickaxeFailRespondAttack" -> LoliConfig.FAIL_RESPOND_ATTACK.get();
            case "loliPickaxeKillFacing" -> LoliConfig.KILL_FACING.get();
            case "loliPickaxeInfiniteBattery" -> LoliConfig.INFINITE_BATTERY.get();
            case "loliPickaxeInvisible" -> LoliConfig.INVISIBLE.get();
            case "loliPickaxeShowInvisible" -> LoliConfig.SHOW_INVISIBLE.get();
            default -> false;
        };
    }

    public static int defaultInt(String key) {
        return switch (key) {
            case "loliPickaxeKillRange" -> LoliConfig.KILL_RANGE.get();
            case "loliPickaxeAutoKillRange" -> LoliConfig.AUTO_KILL_RANGE.get();
            case "loliPickaxeKillFacingRange" -> LoliConfig.KILL_FACING_RANGE.get();
            default -> 0;
        };
    }

    public static double defaultDouble(String key) {
        return switch (key) {
            case "loliPickaxeBlockReachDistance" -> LoliConfig.BLOCK_REACH_DISTANCE.get();
            case "loliPickaxeKillFacingSlope" -> LoliConfig.KILL_FACING_SLOPE.get();
            default -> 0;
        };
    }

    public static String defaultString(String key) {
        return "loliPickaxeKickMessage".equals(key) ? LoliConfig.KICK_MESSAGE.get() : "";
    }

    public static CompoundTag sanitize(CompoundTag input) {
        CompoundTag output = new CompoundTag();
        for (Option option : OPTIONS) {
            if (!input.contains(option.key())) continue;
            switch (option.type()) {
                case BOOLEAN -> output.putBoolean(option.key(), input.getBoolean(option.key()));
                case INT -> output.putInt(option.key(), Mth.clamp(input.getInt(option.key()), (int) option.min(), (int) option.max()));
                case DOUBLE -> output.putDouble(option.key(), Mth.clamp(input.getDouble(option.key()), option.min(), option.max()));
                case STRING -> output.putString(option.key(), input.getString(option.key()).substring(0, Math.min(100, input.getString(option.key()).length())));
            }
        }
        return output;
    }

    private LegacyGuiConfig() {}
}
