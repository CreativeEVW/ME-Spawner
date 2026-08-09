package com.example.mespawner.compat;

import appeng.core.definitions.AEItems;
import com.example.mespawner.MESpawner;
import com.example.mespawner.block.MESpawnerBlock;
import com.example.mespawner.blockentity.MESpawnerBlockEntity;
import com.example.mespawner.registration.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
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
                var node = be.getMainNode();
                int status;
                if (node != null && node.isActive()) {
                    status = 2;
                } else if (node != null && node.isReady() && node.hasGridBooted()) {
                    status = 1; // grid booted but no channel → missing channel
                } else {
                    status = 0; // no grid or grid unpowered → offline
                }
                tag.putInt("grid", status);

                var egg = be.eggSlot.getItem(0);
                if (!egg.isEmpty()) tag.putString("eggName", egg.getHoverName().getString());
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
            if (d.contains("grid")) {
                int s = d.getInt("grid");
                t.add(Component.translatable(s == 0 ? "jade.mespawner.offline"
                        : s == 1 ? "jade.mespawner.missing_channel"
                        : "jade.mespawner.online"));
            }
            if (d.contains("eggName")) {
                t.add(Component.literal("§a" + d.getString("eggName")));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(MESpawner.MOD_ID, "info");
        }
    }
}
