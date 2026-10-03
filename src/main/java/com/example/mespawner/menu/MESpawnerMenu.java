package com.example.mespawner.menu;

import appeng.menu.SlotSemantics;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.AppEngSlot;
import com.example.mespawner.MESpawner;
import com.example.mespawner.blockentity.MESpawnerBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

public class MESpawnerMenu extends UpgradeableMenu<MESpawnerBlockEntity> {

    public static final MenuType<MESpawnerMenu> TYPE = MenuTypeBuilder
            .create(MESpawnerMenu::new, MESpawnerBlockEntity.class)
            .withMenuTitle(host -> Component.translatable("block.mespawner.me_spawner"))
            .buildUnregistered(ResourceLocation.fromNamespaceAndPath(MESpawner.MOD_ID, "me_spawner"));

    public MESpawnerMenu(int id, Inventory playerInventory, MESpawnerBlockEntity host) {
        super(TYPE, id, playerInventory, host);
    }

    @Override
    protected void setupInventorySlots() {
        addSlot(new AppEngSlot(getHost().eggSlot, 0) {
            @Override public boolean mayPlace(ItemStack s) {
                return s.getItem() instanceof SpawnEggItem
                        || s.getItem() instanceof com.example.mespawner.item.MonsterDiskItem;
            }
            @Override public int getMaxStackSize() { return 1; }
        }, SlotSemantics.STORAGE);
    }
}
