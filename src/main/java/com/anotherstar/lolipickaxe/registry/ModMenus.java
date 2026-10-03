package com.anotherstar.lolipickaxe.registry;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.menu.LoliPickaxeMenu;
import com.anotherstar.lolipickaxe.menu.LoliBlacklistMenu;
import com.anotherstar.lolipickaxe.menu.PasswordWorkbenchMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, LoliPickaxe.MOD_ID);
    public static final RegistryObject<MenuType<LoliPickaxeMenu>> LOLI_PICKAXE = MENUS.register("loli_pickaxe",
            () -> IForgeMenuType.create(LoliPickaxeMenu::fromNetwork));
    public static final RegistryObject<MenuType<LoliBlacklistMenu>> LOLI_BLACKLIST = MENUS.register("loli_blacklist",
            () -> IForgeMenuType.create(LoliBlacklistMenu::fromNetwork));
    public static final RegistryObject<MenuType<PasswordWorkbenchMenu>> PASSWORD_WORKBENCH = MENUS.register("password_workbench",
            () -> IForgeMenuType.create(PasswordWorkbenchMenu::fromNetwork));

    private ModMenus() {}
}
