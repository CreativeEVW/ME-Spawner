package com.example.mespawner.menu;

import com.example.mespawner.MESpawner;
import com.example.mespawner.blockentity.MESpawnerBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class MESpawnerScreen extends AbstractContainerScreen<MESpawnerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MESpawner.MOD_ID, "textures/gui/me_spawner.png");

    public MESpawnerScreen(MESpawnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 211;
        this.imageHeight = 178;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        MESpawnerBlockEntity be = this.menu.getBlockEntity();
        if (be != null) {
            g.drawString(this.font,
                    Component.translatable("gui.mespawner.energy",
                            String.format("%.0f", be.getAECurrentPower()),
                            String.format("%.0f", be.getAEMaxPower())),
                    88, 20, 0x404040, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        if (this.hoveredSlot != null) {
            this.renderTooltip(g, mx, my);
        }
    }
}
