package com.example.mespawner.menu;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MESpawnerScreen extends UpgradeableScreen<MESpawnerMenu> {

    public MESpawnerScreen(MESpawnerMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }
}
