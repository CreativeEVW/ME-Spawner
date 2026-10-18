package com.example.mespawner.menu;

import appeng.api.config.YesNo;
import appeng.api.util.IConfigManager;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.AppEngSlot;
import com.example.mespawner.MESpawner;
import com.example.mespawner.api.MESettings;
import com.example.mespawner.blockentity.MESpawnerBlockEntity;
import com.example.mespawner.menu.helper.DirectionSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;

import java.util.ArrayList;

public class MESpawnerMenu extends UpgradeableMenu<MESpawnerBlockEntity> {

    public static final SlotSemantic WEAPON = SlotSemantics.register("WEAPON", false);

    public static final MenuType<MESpawnerMenu> TYPE = MenuTypeBuilder
            .create(MESpawnerMenu::new, MESpawnerBlockEntity.class)
            .withMenuTitle(host -> Component.translatable("block.mespawner.me_spawner"))
            .buildUnregistered(ResourceLocation.fromNamespaceAndPath(MESpawner.MOD_ID, "me_spawner"));

    @GuiSync(20)
    public int status;
    @GuiSync(21)
    public long powerUsage;
    @GuiSync(22)
    public YesNo meExport = YesNo.YES;
    @GuiSync(23)
    public DirectionSet outputSides = new DirectionSet(new ArrayList<>());

    public MESpawnerMenu(int id, Inventory playerInventory, MESpawnerBlockEntity host) {
        super(TYPE, id, playerInventory, host);
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        this.meExport = this.getHost().getConfigManager().getSetting(MESettings.ME_EXPORT);
        this.outputSides.clear();
        this.outputSides.addAll(this.getHost().getOutputSides());
    }

    public java.util.List<net.minecraft.core.Direction> getOutputSides() {
        return outputSides.sides();
    }

    @Override
    protected void setupInventorySlots() {
        // Spawn egg slot (left of center)
        addSlot(new AppEngSlot(getHost().eggSlot, 0) {
            @Override public boolean mayPlace(ItemStack s) {
                return s.getItem() instanceof SpawnEggItem
                        || s.getItem() instanceof com.example.mespawner.item.MonsterDiskItem;
            }
            @Override public int getMaxStackSize() { return 1; }
        }, SlotSemantics.STORAGE);

        // Weapon slot (right of center) — swords and tools only
        addSlot(new AppEngSlot(getHost().weaponSlot, 0) {
            @Override public boolean mayPlace(ItemStack s) {
                return s.getItem() instanceof SwordItem
                        || s.getItem() instanceof DiggerItem;
            }
            @Override public int getMaxStackSize() { return 1; }
        }, WEAPON);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            status = getHost().getStatus();
            powerUsage = getHost().getPowerUsagePerTick();
        }
        super.broadcastChanges();
    }
}
