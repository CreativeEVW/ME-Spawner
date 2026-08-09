package com.example.mespawner.registration;

import com.example.mespawner.MESpawner;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MESpawner.MOD_ID);

    public static final DeferredItem<BlockItem> ME_SPAWNER = ITEMS.register("me_spawner",
            () -> new BlockItem(ModBlocks.ME_SPAWNER.get(), new Item.Properties()));

    // Upgrade cards
    public static final DeferredItem<Item> PROBABILITY_CARD = ITEMS.register("probability_card",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> LOOTING_CARD = ITEMS.register("looting_card",
            () -> new Item(new Item.Properties().stacksTo(64)));

}
