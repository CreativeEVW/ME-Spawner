package com.example.mespawner;

import appeng.api.AECapabilities;
import appeng.api.upgrades.Upgrades;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.core.definitions.AEItems;
import com.example.mespawner.item.MonsterDiskItem;
import com.example.mespawner.menu.MESpawnerScreen;
import com.example.mespawner.registration.*;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MESpawner.MOD_ID)
public class MESpawner {

    public static final String MOD_ID = "mespawner";
    public static final Logger LOGGER = LoggerFactory.getLogger(MESpawner.class);

    public MESpawner(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::clientSetup);

        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        var stack = event.getItemStack();
        if (!(stack.getItem() instanceof MonsterDiskItem disk)) return;
        var player = event.getEntity();
        if (!player.isShiftKeyDown()) return;

        var target = event.getTarget();
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());

        int result = disk.tryStoreEntity(stack, key);
        if (result == 0) {
            if (!event.getLevel().isClientSide()) {
                player.displayClientMessage(Component.translatable("item.mespawner.monster_disk.stored",
                        target.getType().getDescription().getString()), true);
            }
        } else if (result == 2) {
            if (!event.getLevel().isClientSide()) {
                player.displayClientMessage(Component.translatable("item.mespawner.monster_disk.full"), true);
            }
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
    }

    private void clientSetup(FMLClientSetupEvent event) {
        var prop = ResourceLocation.fromNamespaceAndPath(MOD_ID, "fill_state");
        for (var disk : new MonsterDiskItem[]{
                ModItems.MONSTER_DISK_1K.get(),
                ModItems.MONSTER_DISK_4K.get(),
                ModItems.MONSTER_DISK_16K.get(),
                ModItems.MONSTER_DISK_64K.get(),
                ModItems.MONSTER_DISK_256K.get()}) {
            ItemProperties.register(disk, prop, (stack, level, entity, seed) -> disk.getFillState(stack));
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        var block = ModBlocks.ME_SPAWNER.get();
        var beType = ModBlockEntities.ME_SPAWNER_BLOCK_ENTITY.get();

        AEBaseBlockEntity.registerBlockEntityItem(beType, ModItems.ME_SPAWNER.get());
        block.setBlockEntity(
                com.example.mespawner.blockentity.MESpawnerBlockEntity.class,
                beType, null, null);

        // Register upgrade cards with AE2's native upgrade system
        Upgrades.add(ModItems.PROBABILITY_CARD.get(), block, 1);
        Upgrades.add(ModItems.LOOTING_CARD.get(), block, 3);
        Upgrades.add(ModItems.COMPRESSED_LOOTING_CARD.get(), block, 3);
        Upgrades.add(ModItems.ULTIMATE_LOOTING_CARD.get(), block, 3);
        Upgrades.add(AEItems.SPEED_CARD, block, 4);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.ME_SPAWNER_BLOCK_ENTITY.get(),
                (be, ctx) -> be);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(com.example.mespawner.menu.MESpawnerMenu.TYPE, MESpawner::createMESpawnerScreen);
    }

    private static MESpawnerScreen createMESpawnerScreen(
            com.example.mespawner.menu.MESpawnerMenu menu,
            net.minecraft.world.entity.player.Inventory inv,
            net.minecraft.network.chat.Component title) {
        var style = appeng.client.gui.style.StyleManager.loadStyleDoc("/screens/me_spawner.json");
        return new MESpawnerScreen(menu, inv, title, style);
    }
}
