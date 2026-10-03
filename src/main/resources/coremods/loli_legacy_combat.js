var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');

var LIVING = 'net/minecraft/world/entity/LivingEntity';
var ENTITY = 'net/minecraft/world/entity/Entity';
var DAMAGE_SOURCE = 'net/minecraft/world/damagesource/DamageSource';
var REMOVAL_REASON = 'net/minecraft/world/entity/Entity$RemovalReason';
var SYNCED_DATA = 'net/minecraft/network/syncher/SynchedEntityData';
var DATA_ACCESSOR = 'net/minecraft/network/syncher/EntityDataAccessor';
var SERVER_CONNECTION = 'net/minecraft/server/network/ServerGamePacketListenerImpl';
var DEFENSE = 'com/anotherstar/lolipickaxe/asm/LoliDefenseHooks';
var UNSAFE_BRIDGE = 'com/anotherstar/lolipickaxe/asm/LoliUnsafeBridge';

function filterSetHealth(method) {
    var prefix = new InsnList();
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new VarInsnNode(Opcodes.FLOAD, 1));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DEFENSE, 'filterSetHealth', '(L' + LIVING + ';F)F', false));
    prefix.add(new VarInsnNode(Opcodes.FSTORE, 1));
    method.instructions.insert(prefix);
    return method;
}

function overrideHealthGetter(method) {
    var prefix = new InsnList();
    var continueLabel = new LabelNode();
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DEFENSE, 'shouldOverrideHealth', '(L' + LIVING + ';)Z', false));
    prefix.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DEFENSE, 'forcedHealth', '(L' + LIVING + ';)F', false));
    prefix.add(new InsnNode(Opcodes.FRETURN));
    prefix.add(continueLabel);
    method.instructions.insert(prefix);
    return method;
}

function overrideMaxHealthGetter(method) {
    var prefix = new InsnList();
    var continueLabel = new LabelNode();
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DEFENSE, 'shouldOverrideMaxHealth', '(L' + LIVING + ';)Z', false));
    prefix.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DEFENSE, 'forcedMaxHealth', '(L' + LIVING + ';)F', false));
    prefix.add(new InsnNode(Opcodes.FRETURN));
    prefix.add(continueLabel);
    method.instructions.insert(prefix);
    return method;
}

function guardDamage(method) {
    var prefix = new InsnList();
    var continueLabel = new LabelNode();
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 1));
    prefix.add(new VarInsnNode(Opcodes.FLOAD, 2));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DEFENSE, 'shouldBlockDamage',
        '(L' + LIVING + ';L' + DAMAGE_SOURCE + ';F)Z', false));
    prefix.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
    prefix.add(new InsnNode(Opcodes.ICONST_0));
    prefix.add(new InsnNode(Opcodes.IRETURN));
    prefix.add(continueLabel);
    method.instructions.insert(prefix);
    return method;
}

function guardDeath(method) {
    var prefix = new InsnList();
    var continueLabel = new LabelNode();
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 1));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DEFENSE, 'shouldBlockDeath',
        '(L' + LIVING + ';L' + DAMAGE_SOURCE + ';)Z', false));
    prefix.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
    prefix.add(new InsnNode(Opcodes.RETURN));
    prefix.add(continueLabel);
    method.instructions.insert(prefix);
    return method;
}

function guardVoidMethod(method, ownerDesc, hookName) {
    var prefix = new InsnList();
    var continueLabel = new LabelNode();
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DEFENSE, hookName, '(L' + ownerDesc + ';)Z', false));
    prefix.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
    prefix.add(new InsnNode(Opcodes.RETURN));
    prefix.add(continueLabel);
    method.instructions.insert(prefix);
    return method;
}

function guardRemoval(method) {
    var prefix = new InsnList();
    var continueLabel = new LabelNode();
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 1));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DEFENSE, 'shouldBlockRemoval',
        '(L' + ENTITY + ';L' + REMOVAL_REASON + ';)Z', false));
    prefix.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
    prefix.add(new InsnNode(Opcodes.RETURN));
    prefix.add(continueLabel);
    method.instructions.insert(prefix);
    return method;
}

function bypassLoliDeathEvent(method) {
    var prefix = new InsnList();
    var vanilla = new LabelNode();
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 1));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DEFENSE, 'shouldBypassDeathEvent',
        '(L' + LIVING + ';L' + DAMAGE_SOURCE + ';)Z', false));
    prefix.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
    prefix.add(new InsnNode(Opcodes.ICONST_0));
    prefix.add(new InsnNode(Opcodes.IRETURN));
    prefix.add(vanilla);
    method.instructions.insert(prefix);
    return method;
}



function filterSynchedSet(method) {
    var prefix = new InsnList();
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 1));
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 2));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, UNSAFE_BRIDGE, 'filterSynchedValue',
        '(L' + SYNCED_DATA + ';L' + DATA_ACCESSOR + ';Ljava/lang/Object;)Ljava/lang/Object;', false));
    prefix.add(new VarInsnNode(Opcodes.ASTORE, 2));
    method.instructions.insert(prefix);
    return method;
}

function overrideSynchedGet(method) {
    var prefix = new InsnList();
    var vanilla = new LabelNode();
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 1));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, UNSAFE_BRIDGE, 'shouldOverrideSynchedValue',
        '(L' + SYNCED_DATA + ';L' + DATA_ACCESSOR + ';)Z', false));
    prefix.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 1));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, UNSAFE_BRIDGE, 'forcedSynchedValue',
        '(L' + SYNCED_DATA + ';L' + DATA_ACCESSOR + ';)Ljava/lang/Object;', false));
    prefix.add(new InsnNode(Opcodes.ARETURN));
    prefix.add(vanilla);
    method.instructions.insert(prefix);
    return method;
}

function guardDisconnect(method) {
    var prefix = new InsnList();
    var continueLabel = new LabelNode();
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DEFENSE, 'shouldBlockDisconnect',
        '(L' + SERVER_CONNECTION + ';)Z', false));
    prefix.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
    prefix.add(new InsnNode(Opcodes.RETURN));
    prefix.add(continueLabel);
    method.instructions.insert(prefix);
    return method;
}

function guardLivingTick(method) {
    var prefix = new InsnList();
    var continueLabel = new LabelNode();
    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DEFENSE, 'shouldCancelLivingTick',
        '(L' + LIVING + ';)Z', false));
    prefix.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
    prefix.add(new InsnNode(Opcodes.RETURN));
    prefix.add(continueLabel);
    method.instructions.insert(prefix);

    var nodes = method.instructions.toArray();
    for (var i = 0; i < nodes.length; i++) {
        if (nodes[i].getOpcode() === Opcodes.RETURN) {
            var tail = new InsnList();
            tail.add(new VarInsnNode(Opcodes.ALOAD, 0));
            tail.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DEFENSE, 'afterLivingTick',
                '(L' + LIVING + ';)V', false));
            method.instructions.insertBefore(nodes[i], tail);
        }
    }
    return method;
}

function initializeCoreMod() {
    return {
        'legacy_synched_health_get': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.network.syncher.SynchedEntityData',
                'methodName': ASMAPI.mapMethod('m_135370_'),
                'methodDesc': '(Lnet/minecraft/network/syncher/EntityDataAccessor;)Ljava/lang/Object;'},
            'transformer': overrideSynchedGet
        },
        'legacy_synched_health_set': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.network.syncher.SynchedEntityData',
                'methodName': ASMAPI.mapMethod('m_135381_'),
                'methodDesc': '(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V'},
            'transformer': filterSynchedSet
        },
        'legacy_synched_health_force_set': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.network.syncher.SynchedEntityData',
                'methodName': ASMAPI.mapMethod('m_276349_'),
                'methodDesc': '(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V'},
            'transformer': filterSynchedSet
        },
        'legacy_living_tick': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.LivingEntity',
                'methodName': ASMAPI.mapMethod('m_8119_'), 'methodDesc': '()V'},
            'transformer': guardLivingTick
        },
        'legacy_disconnect_guard': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.server.network.ServerGamePacketListenerImpl',
                'methodName': ASMAPI.mapMethod('m_9942_'),
                'methodDesc': '(Lnet/minecraft/network/chat/Component;)V'},
            'transformer': guardDisconnect
        },
        'legacy_health_write': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.LivingEntity',
                'methodName': ASMAPI.mapMethod('m_21153_'), 'methodDesc': '(F)V'},
            'transformer': filterSetHealth
        },
        'legacy_health': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.LivingEntity',
                'methodName': ASMAPI.mapMethod('m_21223_'), 'methodDesc': '()F'},
            'transformer': overrideHealthGetter
        },
        'legacy_max_health': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.LivingEntity',
                'methodName': ASMAPI.mapMethod('m_21233_'), 'methodDesc': '()F'},
            'transformer': overrideMaxHealthGetter
        },
        'legacy_living_hurt': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.LivingEntity',
                'methodName': ASMAPI.mapMethod('m_6469_'), 'methodDesc': '(Lnet/minecraft/world/damagesource/DamageSource;F)Z'},
            'transformer': guardDamage
        },
        'legacy_living_die': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.LivingEntity',
                'methodName': ASMAPI.mapMethod('m_6667_'), 'methodDesc': '(Lnet/minecraft/world/damagesource/DamageSource;)V'},
            'transformer': guardDeath
        },
        'legacy_player_hurt': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.player.Player',
                'methodName': ASMAPI.mapMethod('m_6469_'), 'methodDesc': '(Lnet/minecraft/world/damagesource/DamageSource;F)Z'},
            'transformer': guardDamage
        },
        'legacy_player_die': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.player.Player',
                'methodName': ASMAPI.mapMethod('m_6667_'), 'methodDesc': '(Lnet/minecraft/world/damagesource/DamageSource;)V'},
            'transformer': guardDeath
        },
        'legacy_remove': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.Entity',
                'methodName': ASMAPI.mapMethod('m_142687_'), 'methodDesc': '(Lnet/minecraft/world/entity/Entity$RemovalReason;)V'},
            'transformer': guardRemoval
        },
        'legacy_set_removed': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.Entity',
                'methodName': ASMAPI.mapMethod('m_142467_'), 'methodDesc': '(Lnet/minecraft/world/entity/Entity$RemovalReason;)V'},
            'transformer': guardRemoval
        },
        'legacy_kill': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.Entity',
                'methodName': ASMAPI.mapMethod('m_6074_'), 'methodDesc': '()V'},
            'transformer': function(method) { return guardVoidMethod(method, ENTITY, 'shouldCancelProtectedKill'); }
        },
        'legacy_discard': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.Entity',
                'methodName': ASMAPI.mapMethod('m_146870_'), 'methodDesc': '()V'},
            'transformer': function(method) { return guardVoidMethod(method, ENTITY, 'shouldCancelLoliRemoval'); }
        },
        'legacy_loli_death_event': {
            'target': {'type': 'METHOD', 'class': 'net.minecraftforge.common.ForgeHooks',
                'methodName': 'onLivingDeath', 'methodDesc': '(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Z'},
            'transformer': bypassLoliDeathEvent
        }
    };
}
