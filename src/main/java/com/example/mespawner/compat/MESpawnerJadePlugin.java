package com.example.mespawner.compat;

import com.example.mespawner.MESpawner;
import com.example.mespawner.block.MESpawnerBlock;
import com.example.mespawner.block.SpawnerControllerBlock;
import com.example.mespawner.blockentity.MESpawnerBlockEntity;
import com.example.mespawner.blockentity.SpawnerControllerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class MESpawnerJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration reg) {
        reg.registerBlockDataProvider(new StatusProvider(), MESpawnerBlockEntity.class);
        reg.registerBlockDataProvider(new ControllerConflictProvider(), SpawnerControllerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration reg) {
        reg.registerBlockComponent(new InfoProvider(), MESpawnerBlock.class);
        reg.registerBlockComponent(new ControllerConflictComponent(), SpawnerControllerBlock.class);
    }

    static class StatusProvider implements IServerDataProvider<BlockAccessor> {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor a) {
            if (a.getBlockEntity() instanceof MESpawnerBlockEntity be) {
                var egg = be.eggSlot.getStackInSlot(0);
                if (!egg.isEmpty()) tag.putString("slotItem", egg.getHoverName().getString());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(MESpawner.MOD_ID, "status");
        }
    }

    static class InfoProvider implements IBlockComponentProvider {
        @Override
        public void appendTooltip(ITooltip t, BlockAccessor a, IPluginConfig c) {
            var d = a.getServerData();
            if (d.contains("slotItem")) {
                t.add(Component.literal("§a" + d.getString("slotItem")));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(MESpawner.MOD_ID, "info");
        }
    }

    /** Server data: whether this controller is suppressed by another controller on the network. */
    static class ControllerConflictProvider implements IServerDataProvider<BlockAccessor> {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor a) {
            if (a.getBlockEntity() instanceof SpawnerControllerBlockEntity be && be.hasConflict()) {
                tag.putBoolean("controllerConflict", true);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(MESpawner.MOD_ID, "controller_conflict");
        }
    }

    /** Client display: red conflict line for suppressed controllers. */
    static class ControllerConflictComponent implements IBlockComponentProvider {
        @Override
        public void appendTooltip(ITooltip t, BlockAccessor a, IPluginConfig c) {
            var d = a.getServerData();
            if (d.contains("controllerConflict")) {
                t.add(Component.translatable("gui.mespawner.controller_conflict").withStyle(ChatFormatting.RED));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(MESpawner.MOD_ID, "controller_conflict");
        }
    }
}
