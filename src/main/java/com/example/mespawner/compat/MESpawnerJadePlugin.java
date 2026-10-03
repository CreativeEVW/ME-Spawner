package com.example.mespawner.compat;

import com.example.mespawner.MESpawner;
import com.example.mespawner.block.MESpawnerBlock;
import com.example.mespawner.blockentity.MESpawnerBlockEntity;
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
    }

    @Override
    public void registerClient(IWailaClientRegistration reg) {
        reg.registerBlockComponent(new InfoProvider(), MESpawnerBlock.class);
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
}
