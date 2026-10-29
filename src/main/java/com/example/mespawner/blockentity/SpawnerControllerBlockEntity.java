package com.example.mespawner.blockentity;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import appeng.api.config.Setting;
import appeng.api.config.YesNo;
import appeng.api.networking.*;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.blockentity.powersink.IExternalPowerSink;
import appeng.core.definitions.AEItems;
import appeng.helpers.ForgeEnergyAdapter;
import appeng.me.energy.StoredEnergyAmount;
import appeng.me.helpers.MachineSource;
import appeng.util.ConfigInventory;
import appeng.util.ConfigManager;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.example.mespawner.api.MESettings;
import com.example.mespawner.block.SpawnerControllerBlock;
import com.example.mespawner.menu.SpawnerControllerMenu;
import com.example.mespawner.registration.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Spawner controller: one per AE2 network (single-instance suppression via
 * IPassiveEnergyGenerator, same as AE2's Crystal Resonance Generator). All spawners on the
 * network are filtered by this machine's marked items. Intercepted items are converted to
 * experience crystals when a void card is installed. Storage-bus style config slots.
 */
public class SpawnerControllerBlockEntity extends AENetworkedBlockEntity
        implements IAEPowerStorage, IExternalPowerSink, IGridTickable, MenuProvider, IUpgradeableObject, IConfigurableObject, InternalInventoryHost {

    public static final int MAX_UPGRADE_SLOTS = 6;

    private final StoredEnergyAmount stored;
    private final IUpgradeInventory upgrades =
            UpgradeInventories.forMachine(com.example.mespawner.registration.ModBlocks.SPAWNER_CONTROLLER,
                    MAX_UPGRADE_SLOTS, this::onUpgradesChanged);

    /** Marked items (storage-bus style config, 7 rows of 9, 2 base + capacity-card expansion). */
    private final ConfigInventory config = ConfigInventory.configTypes(63)
            .changeListener(this::saveChanges)
            .build();

    /** Storage mode: ME_EXPORT = YES inserts products into the ME network, NO pushes them to adjacent containers. */
    private final ConfigManager configManager = new ConfigManager(this::onConfigChanged);

    /** Sides products are pushed to when in adjacent-container mode. */
    private final Set<Direction> outputSides = EnumSet.noneOf(Direction.class);

    /** AE2-style FE adapter: external cables can feed the internal AE buffer (1 AE = 2 FE). */
    private final IEnergyStorage forgeEnergyAdapter = new ForgeEnergyAdapter(this);

    public SpawnerControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IAEPowerStorage.class, this)
                .addService(IGridTickable.class, this)
                // Machine discovery: required for IGrid.getMachines(SpawnerControllerBlockEntity.class)
                .addService(SpawnerControllerBlockEntity.class, this);
        var b = (SpawnerControllerBlock) state.getBlock();
        stored = new StoredEnergyAmount(0, b.getMaxPower(), t -> setChanged());
        this.configManager.registerSetting(MESettings.ME_EXPORT, YesNo.YES);
        this.outputSides.addAll(List.of(Direction.values()));
    }

    // ===== Config inventory =====
    public ConfigInventory getConfig() {
        return config;
    }

    public boolean isMarked(AEItemKey key) {
        // Match against the STORED marks only (empty slots' filters allow everything).
        for (int i = 0; i < config.size(); i++) {
            var stack = config.getStack(i);
            if (stack != null && stack.what() instanceof AEItemKey configured
                    && configured.getItem() == key.getItem()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasInverterCard() {
        return upgrades.isInstalled(AEItems.INVERTER_CARD);
    }

    public boolean hasVoidCard() {
        return upgrades.isInstalled(AEItems.VOID_CARD);
    }

    /** Top-3 looting multipliers among installed looting cards (spawner semantics). */
    public long getLootingMultiplier() {
        var multipliers = new ArrayList<Long>();
        for (int i = 0; i < upgrades.getInstalledUpgrades(ModItems.ULTIMATE_LOOTING_CARD); i++) multipliers.add(9L);
        for (int i = 0; i < upgrades.getInstalledUpgrades(ModItems.COMPRESSED_LOOTING_CARD); i++) multipliers.add(6L);
        for (int i = 0; i < upgrades.getInstalledUpgrades(ModItems.LOOTING_CARD); i++) multipliers.add(2L);
        multipliers.sort(Collections.reverseOrder());
        long mult = 1;
        for (int i = 0; i < Math.min(3, multipliers.size()); i++) {
            mult *= multipliers.get(i);
        }
        return mult;
    }

    // ===== Filtering: the controller only DELETES products; passing products keep the spawner's own output routing =====

    public boolean isConfigEmpty() {
        for (int i = 0; i < config.size(); i++) {
            if (config.getStack(i) != null) return false;
        }
        return true;
    }

    /**
     * Takeover: handles a spawner product.
     * Base whitelist: empty filter allows everything, otherwise only marked items pass.
     * Inverter card flips it: empty filter destroys everything, otherwise matching items are destroyed.
     * Destroyed products become experience crystals (1:1) when a void card is installed;
     * passing products follow this machine's output routing. All amounts are multiplied by
     * this machine's looting multiplier.
     */
    public void handleProduct(AEItemKey key, long amount) {
        long boosted = amount * getLootingMultiplier();

        boolean marked = isMarked(key);
        // Base whitelist: empty filter allows everything
        boolean allow = isConfigEmpty() || marked;
        if (hasInverterCard()) {
            // Inverter flips the base semantics: empty filter → destroy everything
            allow = !allow;
        }

        if (!allow) {
            if (hasVoidCard()) {
                // 1 item = 1 experience crystal
                insertOrExport(AEItemKey.of(ModItems.EXPERIENCE_CRYSTAL.get()), boosted, new MachineSource(this));
            }
            return;
        }

        insertOrExport(key, boosted, new MachineSource(this));
    }

    /** Controller's own output routing: same storage-mode / adjacent-container rules as the spawner. */
    private void insertOrExport(AEItemKey key, long amount, MachineSource src) {
        var grid = getMainNode().getGrid();
        boolean gridActive = grid != null && getMainNode().isActive();
        if (gridActive && configManager.getSetting(MESettings.ME_EXPORT) == YesNo.YES) {
            grid.getStorageService().getInventory().insert(key, amount, Actionable.MODULATE, src);
        } else if (!outputSides.isEmpty() && level != null) {
            exportToAdjacent(key, amount);
        }
    }

    /** Push as much of the given key into adjacent containers on the output sides (strict, drops overflow). */
    private void exportToAdjacent(AEItemKey key, long amount) {
        long remaining = amount;
        int maxStack = key.getItem().getDefaultMaxStackSize();
        for (Direction dir : outputSides) {
            if (remaining <= 0) break;
            var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    worldPosition.relative(dir), dir.getOpposite());
            if (handler == null) continue;
            while (remaining > 0) {
                int batch = (int) Math.min(remaining, maxStack);
                var leftover = key.toStack(batch);
                for (int i = 0; i < handler.getSlots() && !leftover.isEmpty(); i++) {
                    leftover = handler.insertItem(i, leftover, false);
                }
                int inserted = batch - leftover.getCount();
                if (inserted <= 0) break;
                remaining -= inserted;
            }
        }
    }

    // ===== Single instance per network: the controller with the lowest position wins =====

    /** Whether another controller with a lower position exists on this network (this one is suppressed). */
    public boolean hasConflict() {
        var grid = getMainNode().getGrid();
        if (grid == null || !getMainNode().isActive()) return false;
        for (var other : grid.getMachines(SpawnerControllerBlockEntity.class)) {
            if (other != this && other.worldPosition.compareTo(worldPosition) < 0) {
                return true;
            }
        }
        return false;
    }

    /** Whether this is THE active controller on its network. */
    public boolean isActiveController() {
        return getMainNode().getGrid() != null && getMainNode().isActive() && !hasConflict();
    }

    private void updateOnlineState() {
        if (level != null && !level.isClientSide()) {
            SpawnerControllerBlock.setOnline(level, worldPosition, getBlockState(),
                    getMainNode().isActive() && isActiveController());
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(20, 20, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        updateOnlineState();
        return TickRateModulation.SLOWER;
    }

    // ===== Config manager / output sides =====
    @Override
    public IConfigManager getConfigManager() {
        return configManager;
    }

    private void onConfigChanged(IConfigManager manager, Setting<?> setting) {
        saveChanges();
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
    }

    public Set<Direction> getOutputSides() {
        return outputSides;
    }

    public void setOutputSide(Direction side, boolean value) {
        if (value) {
            this.outputSides.add(side);
        } else {
            this.outputSides.remove(side);
        }
        onConfigChanged(null, null);
    }

    // ===== Upgrades / inventory host =====
    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    private void onUpgradesChanged() {
        setChanged();
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        setChanged();
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        setChanged();
    }

    @Override
    public boolean isClientSide() {
        return level != null && level.isClientSide();
    }

    @Override
    protected IManagedGridNode createMainNode() {
        return GridHelper.createManagedNode(this, new IGridNodeListener<>() {
            @Override public void onSaveChanges(SpawnerControllerBlockEntity o, IGridNode n) { o.setChanged(); }
            @Override public void onStateChanged(SpawnerControllerBlockEntity o, IGridNode n, State s) {
                o.updateOnlineState();
            }
        }).setExposedOnSides(EnumSet.allOf(Direction.class));
    }

    @Override public AECableType getCableConnectionType(Direction d) { return AECableType.SMART; }

    // ===== IAEPowerStorage =====
    @Override public double injectAEPower(double a, Actionable m) {
        double i = stored.insert(a, m == Actionable.MODULATE); return a - i;
    }
    @Override public double extractAEPower(double a, Actionable m, PowerMultiplier pm) {
        double r = pm.multiply(a); double e = stored.extract(r, m == Actionable.MODULATE); return pm.divide(e);
    }
    @Override public double getAEMaxPower() { return stored.getMaximum(); }
    @Override public double getAECurrentPower() { return stored.getAmount(); }
    @Override public boolean isAEPublicPowerStorage() { return false; } // private buffer, fed by FE input
    @Override public appeng.api.config.AccessRestriction getPowerFlow() { return appeng.api.config.AccessRestriction.READ_WRITE; }

    // ===== IExternalPowerSink (copied from AE2's AEBasePoweredBlockEntity) =====
    @Override
    public double injectExternalPower(PowerUnit input, double amount, Actionable mode) {
        return PowerUnit.AE.convertTo(input, injectAEPower(input.convertTo(PowerUnit.AE, amount), mode));
    }

    @Override
    public double getExternalPowerDemand(PowerUnit externalUnit, double maxPowerRequired) {
        return PowerUnit.AE.convertTo(externalUnit,
                Math.max(0.0, getAEMaxPower() - getAECurrentPower()));
    }

    /** FE capability endpoint. Null-side queries (e.g. Jade) get null; side access gets the adapter. */
    public IEnergyStorage getEnergyStorage(Direction side) {
        if (side == null) return null;
        return forgeEnergyAdapter;
    }

    // ===== GUI sync =====
    public int getStatus() {
        return 0;
    }

    public long getPowerUsagePerTick() {
        return 0;
    }

    // ===== Memory card: save/load the marked items (same mechanism as the storage bus) =====
    @Override
    public void importSettings(appeng.util.SettingsFrom mode, net.minecraft.core.component.DataComponentMap input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        var list = input.get(appeng.api.ids.AEComponents.EXPORTED_CONFIG_INV);
        if (list != null) {
            this.config.readFromList(list);
        }
    }

    @Override
    public void exportSettings(appeng.util.SettingsFrom mode, net.minecraft.core.component.DataComponentMap.Builder output, @Nullable Player player) {
        super.exportSettings(mode, output, player);
        if (mode == appeng.util.SettingsFrom.MEMORY_CARD) {
            output.set(appeng.api.ids.AEComponents.EXPORTED_CONFIG_INV, this.config.toList());
        }
    }

    // ===== NBT =====
    @Override public void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r); t.putDouble("pwr", stored.getAmount());
        upgrades.writeToNBT(t, "upgrades", r);
        config.writeToChildTag(t, "config", r);
        configManager.writeToNBT(t, r);
        var sides = new ListTag();
        for (var side : this.outputSides) {
            sides.add(StringTag.valueOf(side.getName()));
        }
        t.put("output_side", sides);
    }
    @Override public void loadTag(CompoundTag t, HolderLookup.Provider r) {
        super.loadTag(t, r); stored.setStored(t.getDouble("pwr"));
        upgrades.readFromNBT(t, "upgrades", r);
        config.readFromChildTag(t, "config", r);
        configManager.readFromNBT(t, r);
        this.outputSides.clear();
        if (t.contains("output_side")) {
            for (var name : t.getList("output_side", CompoundTag.TAG_STRING)) {
                this.outputSides.add(Direction.byName(name.getAsString()));
            }
        } else {
            this.outputSides.addAll(List.of(Direction.values()));
        }
    }

    // ===== Menu =====
    @Override public Component getDisplayName() { return Component.translatable("block.mespawner.spawner_controller"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new SpawnerControllerMenu(id, inv, this); }
}
