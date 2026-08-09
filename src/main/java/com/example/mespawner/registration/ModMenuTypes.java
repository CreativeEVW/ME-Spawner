package com.example.mespawner.registration;

import com.example.mespawner.MESpawner;
import com.example.mespawner.menu.MESpawnerMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(BuiltInRegistries.MENU, MESpawner.MOD_ID);

    public static final Supplier<MenuType<MESpawnerMenu>> ME_SPAWNER_MENU =
            MENU_TYPES.register("me_spawner",
                    () -> new MenuType<>(MESpawnerMenu::new, FeatureFlags.DEFAULT_FLAGS));
}
