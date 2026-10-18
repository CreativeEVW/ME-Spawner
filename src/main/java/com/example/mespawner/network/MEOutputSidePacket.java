package com.example.mespawner.network;

import com.example.mespawner.MESpawner;
import com.example.mespawner.menu.MESpawnerMenu;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** Serverbound packet toggling an output side on the ME spawner. */
public record MEOutputSidePacket(String side, boolean value) implements CustomPacketPayload {

    public static final Type<MEOutputSidePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MESpawner.MOD_ID, "output_side"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MEOutputSidePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MEOutputSidePacket::side,
            ByteBufCodecs.BOOL, MEOutputSidePacket::value,
            MEOutputSidePacket::new);

    @Override
    public @NotNull Type<MEOutputSidePacket> type() {
        return TYPE;
    }

    public static void handle(MEOutputSidePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof MESpawnerMenu menu) {
                var dir = Direction.byName(packet.side());
                menu.getHost().setOutputSide(dir, packet.value());
            }
        });
    }
}
