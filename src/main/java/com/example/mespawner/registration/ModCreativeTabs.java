package com.example.mespawner.registration;

import com.example.mespawner.MESpawner;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, MESpawner.MOD_ID);

    public static final Supplier<CreativeModeTab> MESPAWNER_TAB = CREATIVE_TABS.register(
            "mespawner_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mespawner"))
                    .icon(() -> new ItemStack(ModItems.ME_SPAWNER.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.ME_SPAWNER.get());
                        output.accept(ModItems.PROBABILITY_CARD.get());
                        output.accept(ModItems.LOOTING_CARD.get());
                        output.accept(ModItems.COMPRESSED_LOOTING_CARD.get());
                        output.accept(ModItems.ULTIMATE_LOOTING_CARD.get());
                    })
                    .build()
    );
}
