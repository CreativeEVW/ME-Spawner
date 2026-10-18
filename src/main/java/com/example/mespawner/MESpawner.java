package com.example.mespawner;

import appeng.api.AECapabilities;
import appeng.api.upgrades.Upgrades;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.core.definitions.AEItems;
import com.example.mespawner.item.MonsterDiskItem;
import com.example.mespawner.registration.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
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
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::onRegisterPayloads);

        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
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

    /** Drop cached loot-table expansions when the resource manager reloads (tables are replaced). */
    private void onAddReloadListener(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        event.addListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager ->
                com.example.mespawner.blockentity.MESpawnerBlockEntity.clearExpansionCache());
    }

    /** Release session caches when the server stops. */
    private void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        com.example.mespawner.blockentity.MESpawnerBlockEntity.clearExpansionCache();
        com.example.mespawner.util.ExtraDropsStorage.clearCache();
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.ME_SPAWNER_BLOCK_ENTITY.get(),
                (be, ctx) -> be);
        // FE input via AE2's ForgeEnergyAdapter (same pattern as AE2's charger/inscriber)
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ME_SPAWNER_BLOCK_ENTITY.get(),
                (be, side) -> be.getEnergyStorage(side));
    }

    /** Register serverbound payloads (config-button cycling and output-side toggles). */
    private void onRegisterPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(MESpawner.MOD_ID);
        registrar.playToServer(
                com.example.mespawner.network.MEConfigButtonPacket.TYPE,
                com.example.mespawner.network.MEConfigButtonPacket.STREAM_CODEC,
                com.example.mespawner.network.MEConfigButtonPacket::handle);
        registrar.playToServer(
                com.example.mespawner.network.MEOutputSidePacket.TYPE,
                com.example.mespawner.network.MEOutputSidePacket.STREAM_CODEC,
                com.example.mespawner.network.MEOutputSidePacket::handle);
    }
}
