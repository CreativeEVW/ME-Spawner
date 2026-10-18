package com.example.mespawner.client.button;

import appeng.client.gui.style.Blitter;
import net.minecraft.client.gui.components.Button;

/** EPPButton with a fixed blitter icon (copied from AppliedGenerators' ActionEPPButton). */
public class ActionEPPButton extends EPPButton {

    private final Blitter icon;

    public ActionEPPButton(Button.OnPress onPress, Blitter icon) {
        super(onPress);
        this.icon = icon;
    }

    protected Blitter getBlitterIcon() {
        return this.icon;
    }
}
