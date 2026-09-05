package com.example.mespawner.blockentity;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.*;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.me.energy.StoredEnergyAmount;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import com.example.mespawner.Config;
import com.example.mespawner.block.MESpawnerBlock;
import com.example.mespawner.menu.MESpawnerMenu;
import com.example.mespawner.registration.ModBlockEntities;
import com.example.mespawner.registration.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class MESpawnerBlockEntity extends AENetworkedBlockEntity
        implements IAEPowerStorage, IGridTickable, MenuProvider {

    private static final int BASE_COOLDOWN = 200; // 10s

    private final StoredEnergyAmount stored;
    public final SimpleContainer eggSlot = new SimpleContainer(1);
    public final SimpleContainer cardSlots = new SimpleContainer(8);
    private ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ZOMBIE);
    private EntityType<?> cachedEntity = EntityType.ZOMBIE;

    public MESpawnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IAEPowerStorage.class, this)
                .addService(IGridTickable.class, this);
        var b = (MESpawnerBlock) state.getBlock();
        stored = new StoredEnergyAmount(0, b.getMaxPower(), t -> setChanged());
        eggSlot.addListener(c -> setChanged());
        cardSlots.addListener(c -> setChanged());
    }

    @Override
    protected IManagedGridNode createMainNode() {
        return GridHelper.createManagedNode(this, new IGridNodeListener<>() {
            @Override public void onSaveChanges(MESpawnerBlockEntity o, IGridNode n) { o.setChanged(); }
            @Override public void onStateChanged(MESpawnerBlockEntity o, IGridNode n, State s) {
                if (o.level != null && !o.level.isClientSide())
                    MESpawnerBlock.setOnline(o.level, o.worldPosition,
                            o.getBlockState(), n.isActive());
            }
        }).setExposedOnSides(EnumSet.allOf(Direction.class));
    }

    @Override public AECableType getCableConnectionType(Direction d) { return AECableType.COVERED; }

    // ===== IAEPowerStorage =====
    @Override public double injectAEPower(double a, Actionable m) {
        double i = stored.insert(a, m == Actionable.MODULATE); return a - i;
    }
    @Override public double extractAEPower(double a, Actionable m, PowerMultiplier pm) {
        double r = pm.multiply(a); double e = stored.extract(r, m == Actionable.MODULATE); return pm.divide(e);
    }
    @Override public double getAEMaxPower() { return stored.getMaximum(); }
    @Override public double getAECurrentPower() { return stored.getAmount(); }
    @Override public boolean isAEPublicPowerStorage() { return true; }
    @Override public appeng.api.config.AccessRestriction getPowerFlow() { return appeng.api.config.AccessRestriction.READ_WRITE; }

    // ===== IGridTickable =====
    @Override public TickingRequest getTickingRequest(IGridNode n) { return new TickingRequest(1, 20, false); }
    @Override public TickRateModulation tickingRequest(IGridNode n, int t) {
        if (getLevel() instanceof ServerLevel sl) process(sl, t);
        return TickRateModulation.IDLE;
    }

    private int tickCounter;

    private void process(ServerLevel lv, int ticksSinceLastCall) {
        tickCounter += ticksSinceLastCall;
        int speedCount = 0;
        for (int i = 4; i < 8; i++) if (!cardSlots.getItem(i).isEmpty()) speedCount++;
        int interval = getEffectiveCooldown(speedCount);
        if (tickCounter < interval) return;
        tickCounter = 0;

        if (!getMainNode().isActive()) return;

        var grid = getMainNode().getGrid();
        if (grid == null) return;

        var item = eggSlot.getItem(0);
        if (item.isEmpty()) return;

        // Collect entity types to process
        var types = new java.util.ArrayList<EntityType<?>>();
        if (item.getItem() instanceof SpawnEggItem eggItem) {
            var t = (EntityType<?>) eggItem.getType(item);
            if (t != null) types.add(t);
        } else if (item.getItem() instanceof com.example.mespawner.item.MonsterDiskItem disk) {
            types.addAll(disk.getFormedEntityTypes(item, lv));
        }
        if (types.isEmpty()) return;

        // Draw power (per entity type)
        var energyGrid = grid.getEnergyService();
        long totalCost = (long) Config.spawnEnergyCost * types.size();
        var extracted = energyGrid.extractAEPower(totalCost, Actionable.MODULATE,
                appeng.api.config.PowerMultiplier.CONFIG);
        if (Math.abs(extracted - totalCost) > 1) return;

        long lootingMult = getLootingMultiplier();
        boolean hasProbability = !cardSlots.getItem(0).isEmpty();
        var src = new MachineSource(this);
        var inv = grid.getStorageService().getInventory();

        // Process each entity type in parallel
        for (var type : types) {
            processEntityType(lv, type, hasProbability, lootingMult, src, inv);
        }

        setChanged();
    }

    private void processEntityType(ServerLevel lv, EntityType<?> type, boolean hasProbability,
                                   long lootingMult, MachineSource src, appeng.api.storage.MEStorage inv) {
        var lootTable = lv.getServer().reloadableRegistries().getLootTable(type.getDefaultLootTable());
        if (lootTable == LootTable.EMPTY) return;

        var tmpEntity = type.create(lv);
        if (tmpEntity == null) return;
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(lv)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                        worldPosition.getCenter())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, tmpEntity)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,
                        lv.damageSources().generic())
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);

        if (hasProbability) {
            for (var key : getLootTableKeys(lootTable, params)) {
                inv.insert(key, lootingMult, Actionable.MODULATE, src);
            }
        } else {
            for (var drop : lootTable.getRandomItems(params)) {
                var key = AEItemKey.of(drop);
                if (key == null) continue;
                long amt = drop.getCount() * lootingMult;
                if (amt < 1) amt = 1;
                inv.insert(key, amt, Actionable.MODULATE, src);
            }
        }
    }

    private void collectKeys(net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer entry,
                              java.util.Set<AEItemKey> keys) {
        if (entry instanceof net.minecraft.world.level.storage.loot.entries.LootItem lootItem) {
            try {
                var f = net.minecraft.world.level.storage.loot.entries.LootItem.class.getDeclaredField("item");
                f.setAccessible(true);
                var item = (net.minecraft.core.Holder<net.minecraft.world.item.Item>) f.get(lootItem);
                keys.add(AEItemKey.of(item.value()));
            } catch (Exception ignored) {}
        } else if (entry instanceof net.minecraft.world.level.storage.loot.entries.TagEntry tagEntry) {
            try {
                var f = net.minecraft.world.level.storage.loot.entries.TagEntry.class.getDeclaredField("tag");
                f.setAccessible(true);
                var tag = (net.minecraft.tags.TagKey<net.minecraft.world.item.Item>) f.get(tagEntry);
                BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(h ->
                    keys.add(AEItemKey.of(h.value())));
            } catch (Exception ignored) {}
        } else if (entry instanceof net.minecraft.world.level.storage.loot.entries.CompositeEntryBase composite) {
            try {
                var f = net.minecraft.world.level.storage.loot.entries.CompositeEntryBase.class.getDeclaredField("children");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                var children = (java.util.List<net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer>) f.get(composite);
                for (var child : children) collectKeys(child, keys);
            } catch (Exception ignored) {}
        }
    }

    private java.util.Set<AEItemKey> getLootTableKeys(LootTable table, net.minecraft.world.level.storage.loot.LootParams params) {
        var keys = new java.util.HashSet<AEItemKey>();
        try {
            var poolsField = LootTable.class.getDeclaredField("pools");
            poolsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var pools = (java.util.List<net.minecraft.world.level.storage.loot.LootPool>) poolsField.get(table);
            System.out.println("[MESpawner] loot pools count: " + pools.size());
            for (var pool : pools) {
                var entriesField = net.minecraft.world.level.storage.loot.LootPool.class.getDeclaredField("entries");
                entriesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                var entries = (java.util.List<net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer>) entriesField.get(pool);
                for (var entry : entries) {
                    collectKeys(entry, keys);
                }
            }
        } catch (Exception e) {
            System.out.println("[MESpawner] Loot reflection failed: " + e);
            // Fallback: roll randomly
            for (int i = 0; i < 30; i++)
                for (var drop : table.getRandomItems(params))
                    keys.add(AEItemKey.of(drop));
        }
        keys.remove(null);
        return keys;
    }

    private long getLootingMultiplier() {
        long mult = 1;
        for (int i = 1; i < 4; i++) {
            var item = cardSlots.getItem(i).getItem();
            if (item == ModItems.LOOTING_CARD.get()) mult *= 2;
            else if (item == ModItems.COMPRESSED_LOOTING_CARD.get()) mult *= 6;
            else if (item == ModItems.ULTIMATE_LOOTING_CARD.get()) mult *= 9;
        }
        return mult;
    }

    private int getEffectiveCooldown(int speedCards) {
        int base = BASE_COOLDOWN;
        for (int i = 0; i < speedCards; i++) base = Math.max(1, base / 2);
        return base;
    }

    private void insertIntoNetwork(IStorageService storage, ItemStack drop, long amount, MachineSource src) {
        var key = AEItemKey.of(drop);
        if (key == null) return;
        storage.getInventory().insert(key, amount, Actionable.MODULATE, src);
    }

    // ===== Entity =====
    public EntityType<?> getEntityType() {
        if (cachedEntity == null) cachedEntity = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(EntityType.ZOMBIE);
        return cachedEntity;
    }

    // ===== NBT =====
    @Override public void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r); t.putDouble("pwr", stored.getAmount()); t.putString("ent", entityId.toString());
        var eggTag = new CompoundTag();
        net.minecraft.world.ContainerHelper.saveAllItems(eggTag, eggSlot.getItems(), r);
        t.put("egg", eggTag);
        var cardTag = new CompoundTag();
        net.minecraft.world.ContainerHelper.saveAllItems(cardTag, cardSlots.getItems(), r);
        t.put("cards", cardTag);
    }
    @Override public void loadTag(CompoundTag t, HolderLookup.Provider r) {
        super.loadTag(t, r); stored.setStored(t.getDouble("pwr"));
        if (t.contains("ent")) { entityId = ResourceLocation.parse(t.getString("ent")); cachedEntity = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(EntityType.ZOMBIE); }
        if (t.contains("egg")) net.minecraft.world.ContainerHelper.loadAllItems(t.getCompound("egg"), eggSlot.getItems(), r);
        if (t.contains("cards")) net.minecraft.world.ContainerHelper.loadAllItems(t.getCompound("cards"), cardSlots.getItems(), r);
    }

    // ===== Menu =====
    @Override public Component getDisplayName() { return Component.translatable("block.mespawner.me_spawner"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new MESpawnerMenu(id, inv, this); }
}
