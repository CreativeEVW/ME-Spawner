package com.example.mespawner.client;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import com.example.mespawner.menu.MESpawnerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MESpawnerScreen extends UpgradeableScreen<MESpawnerMenu> {

    public MESpawnerScreen(MESpawnerMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        setTextContent("status", Component.translatable(switch (menu.status) {
            case 1 -> "gui.mespawner.status_working";
            case 2 -> "gui.mespawner.status_no_energy";
            default -> "gui.mespawner.status_idle";
        }));

        setTextContent("power_usage",
                Component.translatable("gui.mespawner.power_usage", menu.powerUsage));
    }
}
