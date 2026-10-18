package com.example.mespawner.network;

import appeng.api.config.Setting;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.menu.AEBaseMenu;
import appeng.util.EnumCycler;
import com.example.mespawner.MESpawner;
import com.example.mespawner.api.MESettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Serverbound packet cycling a custom config-manager setting
 * (copied from AppliedGenerators' AGConfigButtonPacket, without glodium).
 */
public record MEConfigButtonPacket(Setting<?> option, boolean rotationDirection) implements CustomPacketPayload {

    public static final Type<MEConfigButtonPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MESpawner.MOD_ID, "config_button"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MEConfigButtonPacket> STREAM_CODEC =
            StreamCodec.ofMember(MEConfigButtonPacket::write, MEConfigButtonPacket::decode);

    @Override
    public @NotNull Type<MEConfigButtonPacket> type() {
        return TYPE;
    }

    public static MEConfigButtonPacket decode(RegistryFriendlyByteBuf stream) {
        var option = MESettings.getOrThrow(stream.readUtf());
        var rotationDirection = stream.readBoolean();
        return new MEConfigButtonPacket(option, rotationDirection);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeUtf(option.getName());
        data.writeBoolean(rotationDirection);
    }

    public static void handle(MEConfigButtonPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player && player.containerMenu instanceof AEBaseMenu baseMenu) {
                if (baseMenu.getTarget() instanceof IConfigurableObject configurableObject) {
                    var cm = configurableObject.getConfigManager();
                    if (cm.hasSetting(packet.option())) {
                        cycleSetting(cm, packet.option(), packet.rotationDirection());
                    }
                }
            }
        });
    }

    private static <T extends Enum<T>> void cycleSetting(IConfigManager cm, Setting<T> setting, boolean backwards) {
        var currentValue = cm.getSetting(setting);
        var nextValue = EnumCycler.rotateEnum(currentValue, backwards, setting.getValues());
        cm.putSetting(setting, nextValue);
    }
}
