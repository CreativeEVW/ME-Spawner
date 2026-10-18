package com.example.mespawner.client;

import appeng.api.config.YesNo;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import com.example.mespawner.api.MESettings;
import com.example.mespawner.client.button.ActionEPPButton;
import com.example.mespawner.client.button.MEIcon;
import com.example.mespawner.client.button.MEServerSettingToggleButton;
import com.example.mespawner.client.gui.subgui.OutputSideConfig;
import com.example.mespawner.menu.MESpawnerMenu;
import com.example.mespawner.registration.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class MESpawnerScreen extends UpgradeableScreen<MESpawnerMenu> {

    private final MEServerSettingToggleButton<YesNo> exportBtn;
    private final ActionEPPButton outputSideBtn;

    public MESpawnerScreen(MESpawnerMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.exportBtn = new MEServerSettingToggleButton<>(MESettings.ME_EXPORT, YesNo.NO);
        this.addToLeftToolbar(exportBtn);

        this.outputSideBtn = new ActionEPPButton(b -> this.openOutputConfig(), MEIcon.OUTPUT_SIDES.getBlitter());
        this.outputSideBtn.setMessage(Component.translatable("gui.mespawner.set_output_sides.open"));
        this.addToLeftToolbar(this.outputSideBtn);
    }

    private void openOutputConfig() {
        if (this.getMenu().getHost() != null) {
            switchToScreen(new OutputSideConfig<>(
                    this,
                    new ItemStack(ModItems.ME_SPAWNER.get()),
                    this.getMenu().getHost(),
                    this.getMenu().getOutputSides(),
                    OutputSideConfig::sendToggle));
        }
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

        this.exportBtn.set(menu.meExport);
        // Output direction button only shows when products go to adjacent containers
        this.outputSideBtn.setVisibility(this.exportBtn.getCurrentValue() == YesNo.NO);
    }
}
