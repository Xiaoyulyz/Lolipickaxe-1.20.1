package com.anotherstar.lolipickaxe.gametest;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.entity.LoliEntity;
import com.anotherstar.lolipickaxe.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

 
@GameTestHolder(LoliPickaxe.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LoliEntityGameTests {
    @GameTest(template = "loli_arena", timeoutTicks = 360)
    public static void navigationAndLifecycle(GameTestHelper helper) {
        boolean originalAttack = LoliConfig.LOLI_ATTACK.get();
        boolean originalTeleport = LoliConfig.LOLI_TELEPORT.get();
        double originalSpeed = LoliConfig.LOLI_SPEED.get();
        helper.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, helper.getLevel().getServer());
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
         
        for (int x = (origin.getX() >> 4) - 1; x <= (origin.getX() >> 4) + 1; x++) {
            for (int z = (origin.getZ() >> 4) - 1; z <= (origin.getZ() >> 4) + 1; z++) {
                helper.getLevel().setChunkForced(x, z, true);
            }
        }
        LoliConfig.LOLI_ATTACK.set(false);
        LoliConfig.LOLI_TELEPORT.set(true);  
        LoliConfig.LOLI_SPEED.set(1.0D);
        helper.assertFalse(ForgeRegistries.ITEMS.containsKey(new ResourceLocation(LoliPickaxe.MOD_ID, "replica_notice")),
                "Removed notice item is still registered");

        helper.assertBlockPresent(Blocks.STONE, new BlockPos(2, 1, 8));
        LoliEntity loli = helper.spawn(ModEntities.LOLI.get(), 2.5F, 2.0F, 8.5F);
        Cow control = helper.spawnWithNoFreeWill(EntityType.COW, 10.5F, 2.0F, 4.5F);
        Vec3 start = loli.position();
        Vec3[] previous = {start};
        Cow[] target = {null};
        Vec3[] paused = {null};
        int[] attackAnimationTicks = {0};
        helper.onEachTick(() -> {
            if (loli.isRemoved()) return;
            Vec3 now = loli.position();
            if (loli.swinging) attackAnimationTicks[0]++;
            helper.assertTrue(now.distanceToSqr(previous[0]) < 0.64D,
                    "Ordinary walking/chasing snapped more than 0.8 blocks in one tick");
            previous[0] = now;
        });
        helper.runAtTickTime(20, () -> {
            BlockPos destination = helper.absolutePos(new BlockPos(12, 2, 8));
            LoliPickaxe.LOGGER.info("Loli path diagnostic: pos={}, blockPos={}, tickCount={}, onGround={}, noAI={}, floor={}, goal={}, registered={}",
                    loli.position(), loli.blockPosition(), loli.tickCount, loli.onGround(), loli.isNoAi(),
                    helper.getLevel().getBlockState(loli.blockPosition().below()), destination,
                    helper.getLevel().getEntity(loli.getUUID()) == loli);
            LoliPickaxe.LOGGER.info("Control tickCount={}, Loli entity-ticking={}, control entity-ticking={}, forced chunks={}",
                    control.tickCount, helper.getLevel().isPositionEntityTicking(loli.blockPosition()),
                    helper.getLevel().isPositionEntityTicking(control.blockPosition()), helper.getLevel().getForcedChunks().size());
            control.discard();
            helper.assertTrue(loli.getNavigation().moveTo(destination.getX() + 0.5D,
                    destination.getY(), destination.getZ() + 0.5D, 1.0D), "Could not create an open-ground path");
        });
        helper.runAtTickTime(45, () -> {
            helper.assertTrue(loli.tickCount > 20, "Spawned Loli is absent from the vanilla tick list");
            helper.assertTrue(loli.position().distanceToSqr(start) > 4.0D, "Loli did not walk along its path");
            helper.assertTrue(loli.getTarget() == null, "Attack-disabled Loli acquired a target");
            LoliConfig.LOLI_SPEED.set(32.0D);
        });
        helper.runAtTickTime(50, () -> {
            helper.assertTrue(Math.abs(loli.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.6D) < 0.00001D,
                    "Old high-speed config was not safely clamped");
            LoliConfig.LOLI_SPEED.set(1.0D);
            LoliConfig.LOLI_ATTACK.set(true);
            target[0] = helper.spawnWithNoFreeWill(EntityType.COW, 2.5F, 2.0F, 8.5F);
            loli.setTarget(target[0]);
        });
        helper.runAtTickTime(160, () -> {
            helper.assertTrue(target[0].isRemoved() || !target[0].isAlive(), "Loli did not pursue and attack its target");
            helper.assertTrue(attackAnimationTicks[0] > 0, "Loli's melee goal never performed a swing");
            LoliConfig.LOLI_ATTACK.set(false);
            loli.setNoAi(true);
            loli.getNavigation().stop();
            loli.setDeltaMovement(Vec3.ZERO);
        });
        helper.runAtTickTime(170, () -> paused[0] = loli.position());
        helper.runAtTickTime(195, () -> {
            helper.assertTrue(loli.isNoAi(), "Health/defence maintenance overwrote NoAI");
            helper.assertTrue(loli.position().distanceToSqr(paused[0]) < 0.01D, "NoAI Loli moved unexpectedly");
            loli.setNoAi(false);
            loli.getRandom().setSeed(42L);
            paused[0] = loli.position();
            loli.goalSelector.getAvailableGoals().forEach(goal -> {
                if (goal.getGoal() instanceof RandomStrollGoal stroll) stroll.trigger();
            });
        });
        helper.runAtTickTime(295, () -> {
            helper.assertTrue(loli.position().distanceToSqr(paused[0]) > 0.25D, "Idle stroll goal did not move the Loli");
            loli.lolipickaxe$setDispersal(true);
        });
        helper.runAtTickTime(305, () -> {
            helper.assertTrue(helper.getLevel().getEntity(loli.getUUID()) == null,
                    "Dispersed Loli remains in the server entity manager");
            LoliConfig.LOLI_ATTACK.set(originalAttack);
            LoliConfig.LOLI_TELEPORT.set(originalTeleport);
            LoliConfig.LOLI_SPEED.set(originalSpeed);
            helper.succeed();
        });
    }

    private LoliEntityGameTests() {}
}
