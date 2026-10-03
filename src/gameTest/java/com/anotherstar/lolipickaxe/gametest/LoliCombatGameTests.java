package com.anotherstar.lolipickaxe.gametest;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.asm.LoliErasureState;
import com.anotherstar.lolipickaxe.config.LegacyLoliSettings;
import com.anotherstar.lolipickaxe.config.LegacyGuiConfig;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import com.anotherstar.lolipickaxe.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

 
@GameTestHolder(LoliPickaxe.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LoliCombatGameTests {
    @GameTest(batch = "combat", template = "loli_arena", timeoutTicks = 80)
    public static void deathLootAndCallbacks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        for (int x = (origin.getX() >> 4) - 1; x <= (origin.getX() >> 4) + 1; x++) {
            for (int z = (origin.getZ() >> 4) - 1; z <= (origin.getZ() >> 4) + 1; z++) {
                level.setChunkForced(x, z, true);
            }
        }
        Player attacker = helper.makeMockSurvivalPlayer();
        ItemStack weapon = new ItemStack(ModItems.LOLI_PICKAXE.get());
        CompoundTag config = new CompoundTag();
        config.putBoolean("loliPickaxeCompulsoryRemove", false);
         
        config.putBoolean("loliPickaxeStrongSpawnBan", true);
        helper.assertFalse(LegacyGuiConfig.sanitize(config).contains("loliPickaxeStrongSpawnBan"),
                "Deleted spawn-ban setting is still accepted by the settings packet sanitizer");
        config.putBoolean("loliPickaxeKillFacing", false);
        weapon.getOrCreateTag().put(LegacyLoliSettings.ROOT, config);
        attacker.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        Vec3 point = helper.absoluteVec(new Vec3(5.5, 2, 5.5));
        AABB box = new AABB(point, point).inflate(2);
        boolean oldMobLoot = level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
        level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(true, level.getServer());
        CallbackCow[] custom = {null};
        try {
             
            assertLoot(helper, attacker, weapon, EntityType.COW, Items.BEEF, point, box);
            assertLoot(helper, attacker, weapon, EntityType.PIG, Items.PORKCHOP, point, box);
            assertLoot(helper, attacker, weapon, EntityType.SHEEP, Items.WHITE_WOOL, point, box);
            assertLoot(helper, attacker, weapon, EntityType.CHICKEN, Items.CHICKEN, point, box);

             
            config.putBoolean("loliPickaxeValidToAmityEntity", true);
            config.putInt("loliPickaxeKillFacingRange", 8);
            attacker.moveTo(point.add(0, 0, -3));
            attacker.setYRot(0);
            attacker.setXRot(0);
            Cow ranged = EntityType.COW.create(level);
            ranged.moveTo(point);
            level.addFreshEntity(ranged);
            LoliPickaxeItem.killRangeEntity(level, attacker, weapon, 5);
            helper.assertTrue(count(level, box, Items.BEEF) > 0, "Range attack lost death loot");
            cleanDrops(level, box);
            Cow facing = EntityType.COW.create(level);
            facing.moveTo(point);
            level.addFreshEntity(facing);
            LoliPickaxeItem.killFacing(level, attacker, weapon);
            helper.assertTrue(count(level, box, Items.BEEF) > 0, "Facing attack lost death loot");
            cleanDrops(level, box);

            DelayedCow delayed = new DelayedCow(level);
            delayed.moveTo(point);
            level.addFreshEntity(delayed);
            LoliPickaxeItem.killOne(attacker, weapon, delayed);
            LoliPickaxeItem.killOne(attacker, weapon, delayed);
            helper.assertTrue(delayed.deathCalls == 1 && count(level, box, Items.EMERALD) == 1,
                    "Delayed custom death was invoked twice before the vanilla dead flag was set");
            cleanDrops(level, box);
            delayed.setHealth(delayed.getMaxHealth());
            LoliPickaxeItem.killOne(attacker, weapon, delayed);
            helper.assertTrue(delayed.deathCalls == 2 && count(level, box, Items.EMERALD) == 1,
                    "A revived boss phase was permanently blocked by the previous death guard");
            cleanDrops(level, box);

            custom[0] = new CallbackCow(level);
            custom[0].moveTo(point);
            level.addFreshEntity(custom[0]);
            double maxHealth = custom[0].getMaxHealth();
            LoliPickaxeItem.killOne(attacker, weapon, custom[0]);
            helper.assertTrue(custom[0].deathCalls == 1 && custom[0].lootCalls == 1,
                    "Attack bypassed the subclass death/loot overrides");
            helper.assertTrue(custom[0].playerCredit && custom[0].observedAttacker == attacker,
                    "Fresh-spawn kill did not retain player loot credit");
            helper.assertTrue(custom[0].getMaxHealth() == maxHealth,
                    "Attack changed maximum health instead of current health");
            helper.assertTrue(count(level, box, Items.DIAMOND) == 1, "Custom death reward is missing");
            int reward = count(level, box, Items.DIAMOND);
            LoliPickaxeItem.killOne(attacker, weapon, custom[0]);
            helper.assertTrue(custom[0].deathCalls == 1 && count(level, box, Items.DIAMOND) == reward,
                    "Repeated hit duplicated death callbacks or drops");
            cleanDrops(level, box);

             
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false, level.getServer());
            Cow noLoot = EntityType.COW.create(level);
            noLoot.moveTo(point);
            level.addFreshEntity(noLoot);
            LoliPickaxeItem.killOne(attacker, weapon, noLoot);
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, box).isEmpty(),
                    "Attack ignored doMobLoot=false");
            helper.assertTrue(level.getEntitiesOfClass(ExperienceOrb.class, box).isEmpty(),
                    "Attack ignored doMobLoot=false for experience");
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(true, level.getServer());

             
            Cow canceled = EntityType.COW.create(level);
            canceled.moveTo(point);
            level.addFreshEntity(canceled);
            CancelDrops listener = new CancelDrops(canceled);
            MinecraftForge.EVENT_BUS.register(listener);
            try {
                LoliPickaxeItem.killOne(attacker, weapon, canceled);
                helper.assertTrue(listener.calls == 1, "Forge LivingDropsEvent did not run exactly once");
                helper.assertTrue(count(level, box, Items.BEEF) == 0,
                        "Attack uncanceled drops or reran a base loot fallback");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
            }
            cleanDrops(level, box);

             
            config.putBoolean("loliPickaxeCompulsoryRemove", true);
            Cow forced = EntityType.COW.create(level);
            forced.moveTo(point);
            level.addFreshEntity(forced);
            LoliPickaxeItem.killOne(attacker, weapon, forced);
            helper.assertTrue(count(level, box, Items.BEEF) > 0,
                    "Compulsory attack removed the mob before loot settlement");
            helper.assertFalse(forced.isRemoved(), "Compulsory attack skipped the death animation window");
            cleanDrops(level, box);
            Cow replacement = EntityType.COW.create(level);
            replacement.moveTo(point.add(4, 0, 0));
            helper.assertTrue(level.addFreshEntity(replacement), "Same-type respawn was still blocked");
            helper.runAtTickTime(35, () -> {
                helper.assertTrue(level.getEntity(forced.getUUID()) == null,
                        "Compulsory corpse did not leave the entity manager");
                helper.assertTrue(custom[0].deathCalls == 1 && custom[0].lootCalls == 1,
                        "A tick watchdog repeated custom death/loot callbacks");
                helper.assertTrue(level.getEntity(replacement.getUUID()) == replacement && replacement.isAlive(),
                        "The removed spawn-ban sweep still removed a same-type replacement");
                replacement.discard();
                LoliPickaxe.LOGGER.info("Combat regression passed: four vanilla loot tables, XP, fresh-spawn credit, "
                        + "virtual custom callbacks, no duplicate drops, doMobLoot, Forge cancellation, delayed removal");
                helper.succeed();
            });
        } finally {
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(oldMobLoot, level.getServer());
        }
    }

    private static <T extends LivingEntity> void assertLoot(GameTestHelper helper, Player attacker,
            ItemStack weapon, EntityType<T> type, Item expected, Vec3 point, AABB box) {
        T target = type.create(helper.getLevel());
        target.moveTo(point);
        helper.getLevel().addFreshEntity(target);
        double maximum = target.getMaxHealth();
        LoliPickaxeItem.killOne(attacker, weapon, target);
        helper.assertTrue(count(helper.getLevel(), box, expected) > 0, "Missing guaranteed loot for " + type);
        helper.assertFalse(helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, box).isEmpty(),
                "Missing player kill XP for " + type);
        helper.assertTrue(target.getMaxHealth() == maximum, "Max-health mutation for " + type);
        helper.assertFalse(LoliErasureState.isErased(target), "Normal death was marked for structural erasure");
        int before = count(helper.getLevel(), box, expected);
        LoliPickaxeItem.killOne(attacker, weapon, target);
        helper.assertTrue(count(helper.getLevel(), box, expected) == before, "Duplicate loot for " + type);
        cleanDrops(helper.getLevel(), box);
    }

    private static int count(ServerLevel level, AABB box, Item item) {
        return level.getEntitiesOfClass(ItemEntity.class, box).stream()
                .filter(drop -> drop.getItem().is(item)).mapToInt(drop -> drop.getItem().getCount()).sum();
    }

    private static void cleanDrops(ServerLevel level, AABB box) {
        level.getEntitiesOfClass(ItemEntity.class, box).forEach(Entity::discard);
        level.getEntitiesOfClass(ExperienceOrb.class, box).forEach(Entity::discard);
    }

    public static final class CallbackCow extends Cow {
        int deathCalls;
        int lootCalls;
        boolean playerCredit;
        Entity observedAttacker;

        CallbackCow(ServerLevel level) { super(EntityType.COW, level); }

        @Override public void die(DamageSource source) {
            deathCalls++;
            super.die(source);
        }

        @Override protected void dropFromLootTable(DamageSource source, boolean killedByPlayer) {
            lootCalls++;
            playerCredit = killedByPlayer;
            observedAttacker = source.getEntity();
            super.dropFromLootTable(source, killedByPlayer);
            spawnAtLocation(Items.DIAMOND);
        }
    }

    public static final class CancelDrops {
        final LivingEntity target;
        int calls;
        CancelDrops(LivingEntity target) { this.target = target; }
        @SubscribeEvent public void onDrops(LivingDropsEvent event) {
            if (event.getEntity() == target) {
                calls++;
                event.setCanceled(true);
            }
        }
    }

     
    public static final class DelayedCow extends Cow {
        int deathCalls;
        DelayedCow(ServerLevel level) { super(EntityType.COW, level); }
        @Override public void die(DamageSource source) {
            deathCalls++;
            spawnAtLocation(Items.EMERALD);
        }
    }

    private LoliCombatGameTests() {}
}
