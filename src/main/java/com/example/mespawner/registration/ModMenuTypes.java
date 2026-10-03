package com.example.mespawner.registration;

import com.example.mespawner.MESpawner;
import com.example.mespawner.menu.MESpawnerMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(BuiltInRegistries.MENU, MESpawner.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MESpawnerMenu>> ME_SPAWNER =
            MENU_TYPES.register("me_spawner", () -> MESpawnerMenu.TYPE);
}
