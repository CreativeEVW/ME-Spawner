package com.example.mespawner.client;

import appeng.api.config.ActionItems;
import appeng.api.config.YesNo;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import com.example.mespawner.api.MESettings;
import com.example.mespawner.client.button.ActionEPPButton;
import com.example.mespawner.client.button.MEIcon;
import com.example.mespawner.client.button.MEServerSettingToggleButton;
import com.example.mespawner.client.gui.subgui.OutputSideConfig;
import com.example.mespawner.menu.SpawnerControllerMenu;
import com.example.mespawner.registration.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Screen for the spawner controller — direct copy of AE2's storage bus screen plus spawner-style output controls. */
public class SpawnerControllerScreen extends UpgradeableScreen<SpawnerControllerMenu> {

    private final MEServerSettingToggleButton<YesNo> exportBtn;
    private final ActionEPPButton outputSideBtn;

    public SpawnerControllerScreen(SpawnerControllerMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.addToLeftToolbar(new ActionButton(ActionItems.CLOSE, b -> this.menu.clear()));

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
                    new ItemStack(ModItems.SPAWNER_CONTROLLER.get()),
                    this.getMenu().getHost(),
                    this.getMenu().getOutputSides(),
                    OutputSideConfig::sendToggle));
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.exportBtn.set(menu.meExport);
        // Output direction button only shows when products go to adjacent containers
        this.outputSideBtn.setVisibility(this.exportBtn.getCurrentValue() == YesNo.NO);

        // Conflict state, shown red in the GUI
        if (menu.hasConflict) {
            setTextContent("conflict", Component.translatable("gui.mespawner.controller_conflict")
                    .withStyle(ChatFormatting.RED));
        } else {
            setTextContent("conflict", Component.empty());
        }
    }
}
