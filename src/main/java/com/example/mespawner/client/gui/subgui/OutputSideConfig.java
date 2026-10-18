package com.example.mespawner.client.gui.subgui;

import appeng.api.config.ActionItems;
import appeng.api.orientation.RelativeSide;
import appeng.api.parts.IPart;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import com.example.mespawner.client.button.OutputButton;
import com.example.mespawner.network.MEOutputSidePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/** Direction chooser sub-screen (copied from AppliedGenerators' OutputSideConfig). */
public class OutputSideConfig<C extends AEBaseMenu, P extends AEBaseScreen<C>> extends AESubScreen<C, P> {

    private final EnumMap<Direction, OutputButton> buttons = new EnumMap<>(Direction.class);

    public OutputSideConfig(P parent, ItemStack icon, AEBaseBlockEntity host, List<Direction> selectedSides,
                            BiConsumer<Direction, Boolean> setter) {
        super(parent, "/screens/set_output_sides.json");
        TabButton button = new TabButton(Icon.BACK, icon.getHoverName(), (btn) -> this.returnToParent());
        this.widgets.add("return", button);
        ActionButton clear = new ActionButton(ActionItems.S_CLOSE, (b) -> {
            for (OutputButton btn : this.buttons.values()) {
                btn.setOn(false);
            }

            for (Direction side : Direction.values()) {
                setter.accept(side, false);
            }
        });
        clear.setHalfSize(true);
        clear.setDisableBackground(true);
        clear.setMessage(Component.translatable("gui.mespawner.set_output_sides.clear"));
        this.widgets.add("clear", clear);

        for (Direction side : Direction.values()) {
            OutputButton btn = new OutputButton((b) -> {
                ((OutputButton) b).flip();
                setter.accept(side, ((OutputButton) b).isOn());
            });
            if (host.getLevel() != null) {
                btn.setDisplay(this.getDisplayIcon(host, host.getLevel(), side));
            }

            this.buttons.put(side, btn);
        }

        for (Direction side : selectedSides) {
            this.buttons.get(side).setOn(true);
        }

        for (RelativeSide relative : RelativeSide.values()) {
            Direction side = host.getOrientation().getSide(relative);
            this.widgets.add(relative.name().toLowerCase(Locale.ROOT), this.buttons.get(side));
        }
    }

    /** Sends a side toggle to the server via our own payload. */
    public static void sendToggle(Direction side, boolean value) {
        PacketDistributor.sendToServer(new MEOutputSidePacket(side.getName(), value));
    }

    private ItemLike getDisplayIcon(AEBaseBlockEntity host, Level world, Direction side) {
        BlockPos pos = host.getBlockPos().relative(side);
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof CableBusBlockEntity cable) {
            IPart part = cable.getPart(side.getOpposite());
            if (part != null) {
                return part.getPartItem();
            }
        }

        return world.getBlockState(pos).getBlock();
    }

    protected void init() {
        super.init();
        this.setSlotsHidden(SlotSemantics.TOOLBOX, true);
    }
}
