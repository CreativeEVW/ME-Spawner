package com.example.mespawner.client.button;

import appeng.api.config.Setting;
import com.example.mespawner.network.MEConfigButtonPacket;
import net.neoforged.neoforge.network.PacketDistributor;

/** Serverbound variant of the setting toggle button (copied from AppliedGenerators' AGServerSettingToggleButton). */
public class MEServerSettingToggleButton<T extends Enum<T>> extends MESettingToggleButton<T> {

    public MEServerSettingToggleButton(Setting<T> setting, T val) {
        super(setting, val, MEServerSettingToggleButton::sendToServer);
    }

    private static <T extends Enum<T>> void sendToServer(MESettingToggleButton<T> button, boolean backwards) {
        PacketDistributor.sendToServer(new MEConfigButtonPacket(button.getSetting(), backwards));
    }
}
