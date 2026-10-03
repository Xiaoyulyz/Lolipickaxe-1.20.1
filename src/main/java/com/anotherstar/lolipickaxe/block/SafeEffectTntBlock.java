package com.anotherstar.lolipickaxe.block;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.network.ModNetwork;
import com.anotherstar.lolipickaxe.network.SafeAttackEffect;
import com.anotherstar.lolipickaxe.network.SafeAttackEffectPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;







public final class SafeEffectTntBlock extends TntBlock {
    private static final Map<ServerLevel, List<PendingEffect>> PENDING = new WeakHashMap<>();

    private final SafeAttackEffect effect;

    public SafeEffectTntBlock(SafeAttackEffect effect) {
        super(BlockBehaviour.Properties.of().instabreak().ignitedByLava().sound(net.minecraft.world.level.block.SoundType.GRASS));
        this.effect = effect;
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos,
                             @Nullable Direction face, @Nullable LivingEntity igniter) {
        if (level instanceof ServerLevel serverLevel) {
            prime(serverLevel, pos, igniter, -1);
        }
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        PrimedTnt probe = new PrimedTnt(serverLevel,
                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                explosion.getIndirectSourceEntity());
        int baseFuse = probe.getFuse();
        int shortenedFuse = serverLevel.random.nextInt(Math.max(1, baseFuse / 4)) + Math.max(1, baseFuse / 8);
        prime(serverLevel, pos, explosion.getIndirectSourceEntity(), shortenedFuse);
    }

    private void prime(ServerLevel level, BlockPos pos, @Nullable LivingEntity igniter, int fuseOverride) {
        PrimedTnt primed = new PrimedTnt(level,
                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, igniter);
        if (fuseOverride >= 0) primed.setFuse(fuseOverride);
        level.addFreshEntity(primed);
        level.playSound(null, primed.getX(), primed.getY(), primed.getZ(),
                SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);

        PENDING.computeIfAbsent(level, ignored -> new ArrayList<>()).add(
                new PendingEffect(level.getGameTime() + primed.getFuse(),
                        primed.getX(), primed.getY(), primed.getZ(), effect));
    }

    private static void trigger(ServerLevel level, PendingEffect pending) {
        if (!LoliConfig.ENABLE_BUFF_ATTACK_TNT.get() || !LoliConfig.SAFE_ATTACK_SIMULATION.get()) return;
        double radius = LoliConfig.SAFE_ATTACK_RADIUS.get();
        double radiusSquared = radius * radius;
        for (ServerPlayer target : level.players()) {
            if (target.distanceToSqr(pending.x, pending.y, pending.z) < radiusSquared) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target),
                        new SafeAttackEffectPacket(pending.effect));
            }
        }
    }

    private record PendingEffect(long triggerTick, double x, double y, double z, SafeAttackEffect effect) {}

    @Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID)
    public static final class FuseEvents {
        @SubscribeEvent
        public static void levelTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
            List<PendingEffect> pending = PENDING.get(level);
            if (pending == null || pending.isEmpty()) return;

            long now = level.getGameTime();
            Iterator<PendingEffect> iterator = pending.iterator();
            while (iterator.hasNext()) {
                PendingEffect effect = iterator.next();
                if (now >= effect.triggerTick) {
                    trigger(level, effect);
                    iterator.remove();
                }
            }
            if (pending.isEmpty()) PENDING.remove(level);
        }

        private FuseEvents() {}
    }
}
