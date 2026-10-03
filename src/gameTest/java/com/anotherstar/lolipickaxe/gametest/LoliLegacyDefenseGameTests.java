package com.anotherstar.lolipickaxe.gametest;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.asm.LoliDefenseHooks;
import com.anotherstar.lolipickaxe.asm.LoliErasureState;
import com.anotherstar.lolipickaxe.config.LegacyLoliSettings;
import com.anotherstar.lolipickaxe.entity.LoliEntity;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import com.anotherstar.lolipickaxe.registry.ModEntities;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

 
@GameTestHolder(LoliPickaxe.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LoliLegacyDefenseGameTests {
    @GameTest(batch = "legacy-defense", template = "loli_arena", timeoutTicks = 60)
    public static void baselineProtectionAndNoGlobalRepair(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        for (int x = (origin.getX() >> 4) - 1; x <= (origin.getX() >> 4) + 1; x++) {
            for (int z = (origin.getZ() >> 4) - 1; z <= (origin.getZ() >> 4) + 1; z++) level.setChunkForced(x, z, true);
        }
        Vec3 point = helper.absoluteVec(new Vec3(5.5, 2, 5.5));
        Player owner = helper.makeMockSurvivalPlayer();
        owner.moveTo(point);
        ItemStack pickaxe = new ItemStack(ModItems.LOLI_PICKAXE.get());
        CompoundTag config = new CompoundTag();
        config.putBoolean("loliPickaxeThorns", false);
        config.putBoolean("loliPickaxeKillFacing", false);
        config.putBoolean("loliPickaxeCompulsoryRemove", false);
        pickaxe.getOrCreateTag().put(LegacyLoliSettings.ROOT, config);
        owner.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        helper.assertTrue(LoliPickaxeItem.hasLoliProtection(owner), "Owned premium pickaxe lost protection");
        owner.setHealth(0.0F);
        helper.assertTrue(owner.getHealth() == 20.0F && owner.getMaxHealth() == 20.0F, "Legacy 20-health contract failed");
        helper.assertFalse(owner.hurt(level.damageSources().generic(), 1000.0F), "Protected owner accepted damage");
        owner.die(level.damageSources().generic());
        owner.kill();
        owner.discard();
        helper.assertTrue(owner.isAlive() && !owner.isRemoved(), "Protected owner entered destructive lifecycle");
        owner.getEntityData().set(LivingEntity.DATA_HEALTH_ID, 0.0F);
        LoliDefenseHooks.maintain(owner);
        helper.assertTrue(owner.getEntityData().get(LivingEntity.DATA_HEALTH_ID) == 20.0F,
                "Living-update maintenance did not repair vanilla health");

         
        owner.getInventory().items.set(1, new ItemStack(Items.DIAMOND, 7));
        owner.getInventory().clearContent();
        owner.getInventory().dropAll();
        int removed = owner.getInventory().clearOrCountMatchingItems(stack -> true, -1, owner.getInventory());
        helper.assertTrue(removed == 0 && owner.getMainHandItem().is(ModItems.LOLI_PICKAXE.get())
                && owner.getInventory().items.get(1).getCount() == 7, "Anti-disarm/clear/drop hooks regressed");
        owner.getInventory().items.set(owner.getInventory().selected, ItemStack.EMPTY);
        LoliErasureState.protect(owner);
        owner.setHealth(9.0F);
        helper.assertTrue(!LoliPickaxeItem.hasLoliProtection(owner) && owner.getHealth() == 9.0F,
                "Protection persisted without a pickaxe/grace state");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SMALL_LOLI_PICKAXE.get()));
        helper.assertFalse(LoliPickaxeItem.hasLoliProtection(owner), "Small pickaxe gained premium immunity");
        owner.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);

         
        Entity arrow = EntityType.ARROW.create(level);
        arrow.moveTo(point.add(0, 1, 2));
        level.addFreshEntity(arrow);
        config.putBoolean("loliPickaxeValidToAllEntity", true);
        LoliPickaxeItem.killOne(owner, pickaxe, arrow);
        helper.assertTrue(arrow.isRemoved(), "Legacy nonliving attack incorrectly required compulsory removal");
        config.putBoolean("loliPickaxeValidToAllEntity", false);

         
        Player victim = helper.makeMockSurvivalPlayer();
        victim.moveTo(point.add(1, 0, 1));
        config.putBoolean("loliPickaxeBeyondRedemption", true);
        config.putBoolean("loliPickaxeReincarnation", true);
        LoliPickaxeItem.killOne(owner, pickaxe, victim);
        helper.assertTrue(victim.dead && LoliErasureState.isBeyondRedemption(victim)
                && LoliErasureState.isReincarnation(victim), "Player punishment skipped the death callback");
        Player clone = helper.makeMockSurvivalPlayer();
        LoliErasureState.copyPlayerPunishments(victim, clone);
        helper.assertTrue(LoliErasureState.isBeyondRedemption(clone) && LoliErasureState.isReincarnation(clone),
                "Player punishment flags did not survive cloning");
        config.putBoolean("loliPickaxeBeyondRedemption", false);
        config.putBoolean("loliPickaxeReincarnation", false);

        LoliEntity loli = ModEntities.LOLI.get().create(level);
        loli.moveTo(point.add(3, 0, 0));
        loli.setNoAi(true);
        level.addFreshEntity(loli);
        loli.setHealth(0.0F);
        loli.die(level.damageSources().generic());
        loli.kill();
        loli.discard();
        helper.assertTrue(loli.isAlive() && !loli.isRemoved() && loli.getHealth() == 20.0F,
                "Own Loli baseline defence failed");

         
        Cow canceled = EntityType.COW.create(level);
        canceled.moveTo(point.add(0, 0, 3));
        level.addFreshEntity(canceled);
        DeathCancel listener = new DeathCancel(canceled);
        MinecraftForge.EVENT_BUS.register(listener);
        try {
            canceled.die(level.damageSources().generic());
            helper.assertTrue(listener.calls == 1 && !canceled.dead, "Ordinary death event changed globally");
            LoliPickaxeItem.killOne(owner, pickaxe, canceled);
            helper.assertTrue(listener.calls == 1 && canceled.dead,
                    "Legacy Loli death did not bypass LivingDeathEvent cancellation");
        } finally { MinecraftForge.EVENT_BUS.unregister(listener); }

         
        LoliEntity rejected = ModEntities.LOLI.get().create(level);
        rejected.moveTo(point.add(4, 0, 3));
        JoinCancel join = new JoinCancel(rejected);
        MinecraftForge.EVENT_BUS.register(join);
        try { helper.assertFalse(level.addFreshEntity(rejected), "Trial admission guard still uncancels spawns"); }
        finally { MinecraftForge.EVENT_BUS.unregister(join); }

         
        AlternateHealthCow alternate = new AlternateHealthCow(level);
        alternate.moveTo(point.add(0, 0, 4));
        level.addFreshEntity(alternate);
        alternate.getPersistentData().putBoolean("LoliDead", true);
        helper.assertTrue(alternate.getHealth() == 42.0F, "Foreign health getter was globally rewritten");
        alternate.getPersistentData().remove("LoliDead");
        alternate.discard();
        helper.runAtTickTime(25, () -> {
            helper.assertTrue(loli.isAlive() && loli.isNoAi(), "Protection overrode NoAI or stalled lifecycle");
            loli.lolipickaxe$setDispersal(true);
            helper.assertTrue(loli.isRemoved(), "Authorized dispersal no longer removes Loli");
            LoliPickaxe.LOGGER.info("Legacy defence regression passed: fixed health, hurt/die/kill/discard, "
                    + "inventory hooks unchanged, small-pickaxe scope, death cancellation, no forced admission, "
                    + "no foreign getter rewriting, NoAI and dispersal");
            helper.succeed();
        });
    }

    public static final class AlternateHealthCow extends Cow {
        AlternateHealthCow(ServerLevel level) { super(EntityType.COW, level); }
        @Override public float getHealth() { return 42.0F; }
    }
    public static final class DeathCancel {
        private final Cow target;
        int calls;
        DeathCancel(Cow target) { this.target = target; }
        @SubscribeEvent public void death(LivingDeathEvent event) {
            if (event.getEntity() == target) { calls++; event.setCanceled(true); }
        }
    }
    public static final class JoinCancel {
        private final Entity target;
        JoinCancel(Entity target) { this.target = target; }
        @SubscribeEvent public void join(EntityJoinLevelEvent event) {
            if (event.getEntity() == target) event.setCanceled(true);
        }
    }
    private LoliLegacyDefenseGameTests() {}
}
