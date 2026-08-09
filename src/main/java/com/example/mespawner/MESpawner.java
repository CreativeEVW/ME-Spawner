package com.example.mespawner;

import appeng.api.AECapabilities;
import appeng.blockentity.AEBaseBlockEntity;
import com.example.mespawner.menu.MESpawnerScreen;
import com.example.mespawner.registration.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        AEBaseBlockEntity.registerBlockEntityItem(
                ModBlockEntities.ME_SPAWNER_BLOCK_ENTITY.get(),
                ModItems.ME_SPAWNER.get());
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.ME_SPAWNER_BLOCK_ENTITY.get(),
                (be, ctx) -> be);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ME_SPAWNER_MENU.get(), MESpawnerScreen::new);
    }
}
