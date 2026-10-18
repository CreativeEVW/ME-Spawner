package com.example.mespawner.client.button;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Blitter-based icon button (copied from AppliedGenerators' EPPButton). */
public abstract class EPPButton extends IconButton {

    public EPPButton(Button.OnPress onPress) {
        super(onPress);
    }

    abstract Blitter getBlitterIcon();

    protected final Icon getIcon() {
        return null;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (this.visible) {
            Item item = this.getItemOverlay();
            Blitter blitter = this.getBlitterIcon();
            if (this.isHalfSize()) {
                this.width = 8;
                this.height = 8;
            }

            int yOffset = this.isHovered() ? 1 : 0;
            if (this.isHalfSize()) {
                if (!this.isDisableBackground()) {
                    Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(this.getX(), this.getY()).zOffset(10).blit(guiGraphics);
                }

                if (item != null) {
                    guiGraphics.renderItem(new ItemStack(item), this.getX(), this.getY(), 0, 20);
                } else if (blitter != null) {
                    if (!this.active) {
                        blitter.opacity(0.5F);
                    }

                    blitter.dest(this.getX(), this.getY()).zOffset(20).blit(guiGraphics);
                }
            } else {
                if (!this.isDisableBackground()) {
                    Icon bgIcon = this.isHovered() ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER : (this.isFocused() ? Icon.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND);
                    bgIcon.getBlitter().dest(this.getX() - 1, this.getY() + yOffset, 18, 20).zOffset(2).blit(guiGraphics);
                }

                if (item != null) {
                    guiGraphics.renderItem(new ItemStack(item), this.getX(), this.getY() + 1 + yOffset, 0, 3);
                } else if (blitter != null) {
                    blitter.dest(this.getX(), this.getY() + 1 + yOffset).zOffset(3).blit(guiGraphics);
                }
            }
        }
    }
}
