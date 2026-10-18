package com.example.mespawner.menu.helper;

import appeng.menu.guisync.PacketWritable;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** GuiSync-able list of directions (copied from AppliedGenerators' DirectionSet). */
public record DirectionSet(List<Direction> sides) implements PacketWritable {

    @SuppressWarnings("unused")
    public DirectionSet(RegistryFriendlyByteBuf buf) {
        this(fromBytes(buf));
    }

    private static List<Direction> fromBytes(RegistryFriendlyByteBuf buf) {
        List<Direction> fields = new ArrayList<>();
        int size = buf.readByte();
        while (size > 0) {
            size--;
            fields.add(Direction.from3DDataValue(buf.readByte()));
        }
        return fields;
    }

    public void writeToPacket(RegistryFriendlyByteBuf buf) {
        buf.writeByte(this.sides.size());

        for (Direction side : this.sides) {
            buf.writeByte(side.get3DDataValue());
        }
    }

    public void addAll(Collection<Direction> sides) {
        this.sides.addAll(sides);
    }

    public void clear() {
        this.sides.clear();
    }
}
