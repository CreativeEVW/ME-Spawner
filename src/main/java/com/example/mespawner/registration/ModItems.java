package com.example.mespawner.registration;

import com.example.mespawner.MESpawner;
import com.example.mespawner.item.MonsterDiskItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MESpawner.MOD_ID);

    public static final DeferredItem<BlockItem> ME_SPAWNER = ITEMS.register("me_spawner",
            () -> new BlockItem(ModBlocks.ME_SPAWNER.get(), new Item.Properties()));

    // Upgrade cards — must be AE2 UpgradeCardItem for native upgrade slots
    public static final DeferredItem<Item> PROBABILITY_CARD = ITEMS.register("probability_card",
            () -> appeng.api.upgrades.Upgrades.createUpgradeCardItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> LOOTING_CARD = ITEMS.register("looting_card",
            () -> appeng.api.upgrades.Upgrades.createUpgradeCardItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> COMPRESSED_LOOTING_CARD = ITEMS.register("compressed_looting_card",
            () -> appeng.api.upgrades.Upgrades.createUpgradeCardItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ULTIMATE_LOOTING_CARD = ITEMS.register("ultimate_looting_card",
            () -> appeng.api.upgrades.Upgrades.createUpgradeCardItem(new Item.Properties().stacksTo(64)));

    // Spawner cells
    public static final DeferredItem<Item> SPAWNER_CELL_1K = ITEMS.register("spawner_cell_1k",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SPAWNER_CELL_4K = ITEMS.register("spawner_cell_4k",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SPAWNER_CELL_16K = ITEMS.register("spawner_cell_16k",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SPAWNER_CELL_64K = ITEMS.register("spawner_cell_64k",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SPAWNER_CELL_256K = ITEMS.register("spawner_cell_256k",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // Monster disks
    public static final DeferredItem<MonsterDiskItem> MONSTER_DISK_1K = ITEMS.register("monster_disk_1k",
            () -> new MonsterDiskItem(new Item.Properties().stacksTo(1), 1));
    public static final DeferredItem<MonsterDiskItem> MONSTER_DISK_4K = ITEMS.register("monster_disk_4k",
            () -> new MonsterDiskItem(new Item.Properties().stacksTo(1), 4));
    public static final DeferredItem<MonsterDiskItem> MONSTER_DISK_16K = ITEMS.register("monster_disk_16k",
            () -> new MonsterDiskItem(new Item.Properties().stacksTo(1), 16));
    public static final DeferredItem<MonsterDiskItem> MONSTER_DISK_64K = ITEMS.register("monster_disk_64k",
            () -> new MonsterDiskItem(new Item.Properties().stacksTo(1), 64));
    public static final DeferredItem<MonsterDiskItem> MONSTER_DISK_256K = ITEMS.register("monster_disk_256k",
            () -> new MonsterDiskItem(new Item.Properties().stacksTo(1), 256));
}
