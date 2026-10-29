package com.example.mespawner.menu;

import appeng.api.config.YesNo;
import appeng.api.util.IConfigManager;
import appeng.core.definitions.AEItems;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import com.example.mespawner.MESpawner;
import com.example.mespawner.api.MESettings;
import com.example.mespawner.blockentity.SpawnerControllerBlockEntity;
import com.example.mespawner.menu.helper.DirectionSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import java.util.ArrayList;

/** Menu for the spawner controller — direct copy of AE2's storage bus UI plus spawner-style output controls. */
public class SpawnerControllerMenu extends UpgradeableMenu<SpawnerControllerBlockEntity> {

    public static final MenuType<SpawnerControllerMenu> TYPE = MenuTypeBuilder
            .create(SpawnerControllerMenu::new, SpawnerControllerBlockEntity.class)
            .withMenuTitle(host -> Component.translatable("block.mespawner.spawner_controller"))
            .buildUnregistered(ResourceLocation.fromNamespaceAndPath(MESpawner.MOD_ID, "spawner_controller"));

    @GuiSync(20)
    public int status;
    @GuiSync(21)
    public long powerUsage;
    @GuiSync(22)
    public YesNo meExport = YesNo.YES;
    @GuiSync(23)
    public DirectionSet outputSides = new DirectionSet(new ArrayList<>());
    @GuiSync(24)
    public boolean hasConflict;

    public SpawnerControllerMenu(int id, Inventory playerInventory, SpawnerControllerBlockEntity host) {
        super(TYPE, id, playerInventory, host);
        registerClientAction("clear", this::clear);
    }

    @Override
    protected void setupConfig() {
        // Storage-bus style: 2 base rows of 9, capacity card expands 5 more rows
        addExpandableConfigSlots(getHost().getConfig(), 2, 9, 5);
    }

    @Override
    public boolean isSlotEnabled(int index) {
        // Any capacity card unlocks ALL config slots
        return getUpgrades().getInstalledUpgrades(AEItems.CAPACITY_CARD) > 0;
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        this.meExport = this.getHost().getConfigManager().getSetting(MESettings.ME_EXPORT);
        this.outputSides.clear();
        this.outputSides.addAll(this.getHost().getOutputSides());
    }

    public java.util.List<net.minecraft.core.Direction> getOutputSides() {
        return outputSides.sides();
    }

    /** Clears all marked items (storage-bus style). */
    public void clear() {
        if (isClientSide()) {
            sendClientAction("clear");
            return;
        }
        getHost().getConfig().clear();
        broadcastChanges();
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            status = getHost().getStatus();
            powerUsage = getHost().getPowerUsagePerTick();
            hasConflict = getHost().hasConflict();
        }
        super.broadcastChanges();
    }
}
