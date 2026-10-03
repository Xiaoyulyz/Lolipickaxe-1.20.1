var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');

var INVENTORY = 'net/minecraft/world/entity/player/Inventory';
var PLAYER = 'net/minecraft/world/entity/player/Player';
var GUARD = 'com/anotherstar/lolipickaxe/asm/LoliInventoryGuard';

function guardPrefix(returnZero) {
    var list = new InsnList();
    var continueLabel = new LabelNode();
    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
    list.add(new FieldInsnNode(Opcodes.GETFIELD, INVENTORY, ASMAPI.mapField('f_35978_'), 'L' + PLAYER + ';'));
    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, GUARD, 'shouldBlockInventoryMutation', '(L' + PLAYER + ';)Z', false));
    list.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
    if (returnZero) {
        list.add(new InsnNode(Opcodes.ICONST_0));
        list.add(new InsnNode(Opcodes.IRETURN));
    } else {
        list.add(new InsnNode(Opcodes.RETURN));
    }
    list.add(continueLabel);
    return list;
}

function prependGuard(method, returnZero) {
    method.instructions.insert(guardPrefix(returnZero));
    return method;
}

function initializeCoreMod() {
    return {
        'loli_clear_or_count_matching_items': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.player.Inventory',
                'methodName': ASMAPI.mapMethod('m_36022_'),
                'methodDesc': '(Ljava/util/function/Predicate;ILnet/minecraft/world/Container;)I'},
            'transformer': function(method) { return prependGuard(method, true); }
        },
        'loli_drop_all_inventory': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.player.Inventory',
                'methodName': ASMAPI.mapMethod('m_36071_'), 'methodDesc': '()V'},
            'transformer': function(method) { return prependGuard(method, false); }
        },
        'loli_clear_inventory_content': {
            'target': {'type': 'METHOD', 'class': 'net.minecraft.world.entity.player.Inventory',
                'methodName': ASMAPI.mapMethod('m_6211_'), 'methodDesc': '()V'},
            'transformer': function(method) { return prependGuard(method, false); }
        }
    };
}
