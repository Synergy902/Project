package com.synergy902.projectrose.menu;

import com.synergy902.projectrose.ProjectRose;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class RoseMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ProjectRose.MOD_ID);

    public static final RegistryObject<MenuType<AdminLoadoutMenu>> ADMIN_LOADOUT = MENUS.register(
            "admin_loadout",
            () -> IForgeMenuType.create(AdminLoadoutMenu::new)
    );

    private RoseMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}

