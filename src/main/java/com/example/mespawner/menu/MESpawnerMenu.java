package com.example.mespawner.menu;

import appeng.core.definitions.AEItems;
import com.example.mespawner.blockentity.MESpawnerBlockEntity;
import com.example.mespawner.registration.ModItems;
import com.example.mespawner.registration.ModMenuTypes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

public class MESpawnerMenu extends AbstractContainerMenu {

    private final MESpawnerBlockEntity blockEntity;

    public MESpawnerMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.ME_SPAWNER_MENU.get(), containerId);
        this.blockEntity = null;
        addSlots(playerInventory);
    }

    public MESpawnerMenu(int containerId, Inventory playerInventory, MESpawnerBlockEntity be) {
        super(ModMenuTypes.ME_SPAWNER_MENU.get(), containerId);
        this.blockEntity = be;
        addSlots(playerInventory);
    }

    private void addSlots(Inventory playerInventory) {
        // Spawn egg slot
        addSlot(new Slot(blockEntity != null ? blockEntity.eggSlot : new SimpleContainer(1),
                0, 80, 47) {
            @Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof SpawnEggItem; }
            @Override public int getMaxStackSize() { return 1; }
        });

        // 8 upgrade/card slots
        var cardInv = blockEntity != null ? blockEntity.cardSlots : new SimpleContainer(8);
        for (int i = 0; i < 8; i++) {
            final int slot = i;
            addSlot(new Slot(cardInv, i, 185, 7 + i * 18) {
                @Override public boolean mayPlace(ItemStack s) {
                    if (slot == 0) return s.getItem() == ModItems.PROBABILITY_CARD.get();
                    if (slot <= 3) return s.getItem() == ModItems.LOOTING_CARD.get();
                    return s.getItem() == AEItems.SPEED_CARD.asItem();
                }
                @Override public int getMaxStackSize() { return 1; }
            });
        }

        // Player inventory
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(playerInventory, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++)
            addSlot(new Slot(playerInventory, c, 8 + c * 18, 142));
    }

    public MESpawnerBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        var slot = this.slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        var stack = slot.getItem();
        if (idx <= 8) {
            // Machine → player
            var copy = stack.copy();
            if (!this.moveItemStackTo(stack, 9, 45, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
            return ItemStack.EMPTY; // fully handled
        }

        // Player → machine
        var item = stack.getItem();
        if (item instanceof SpawnEggItem) {
            return moveOrFail(stack, slot, 0, 1, false);
        }
        if (item == ModItems.PROBABILITY_CARD.get()) {
            return moveOrFail(stack, slot, 1, 2, false);
        }
        if (item == ModItems.LOOTING_CARD.get()) {
            return moveOrFail(stack, slot, 2, 5, false);
        }
        if (item == AEItems.SPEED_CARD.asItem()) {
            return moveOrFail(stack, slot, 5, 9, false);
        }
        return ItemStack.EMPTY;
    }

    private ItemStack moveOrFail(ItemStack stack, Slot from, int start, int end, boolean rev) {
        var before = stack.copy();
        if (!this.moveItemStackTo(stack, start, end, rev)) return ItemStack.EMPTY;
        from.onQuickCraft(stack, before);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        if (blockEntity != null)
            return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                    p, com.example.mespawner.registration.ModBlocks.ME_SPAWNER.get());
        return true;
    }
}
