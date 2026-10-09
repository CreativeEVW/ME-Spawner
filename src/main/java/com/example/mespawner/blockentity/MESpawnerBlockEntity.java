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
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.me.energy.StoredEnergyAmount;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
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
        implements IAEPowerStorage, IGridTickable, MenuProvider, IUpgradeableObject, InternalInventoryHost {

    private static final int BASE_COOLDOWN = 200; // 10s
    public static final int MAX_UPGRADE_SLOTS = 8;

    private final StoredEnergyAmount stored;
    public final AppEngInternalInventory eggSlot = new AppEngInternalInventory(this, 1);
    public final AppEngInternalInventory weaponSlot = new AppEngInternalInventory(this, 1);
    private final IUpgradeInventory upgrades =
            UpgradeInventories.forMachine(com.example.mespawner.registration.ModBlocks.ME_SPAWNER,
                    MAX_UPGRADE_SLOTS, this::onUpgradesChanged);

    public MESpawnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IAEPowerStorage.class, this)
                .addService(IGridTickable.class, this);
        var b = (MESpawnerBlock) state.getBlock();
        stored = new StoredEnergyAmount(0, b.getMaxPower(), t -> setChanged());
        eggSlot.setMaxStackSize(0, 1);
        weaponSlot.setMaxStackSize(0, 1);
    }

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
            @Override public void onSaveChanges(MESpawnerBlockEntity o, IGridNode n) { o.setChanged(); }
            @Override public void onStateChanged(MESpawnerBlockEntity o, IGridNode n, State s) {
                if (o.level != null && !o.level.isClientSide())
                    MESpawnerBlock.setOnline(o.level, o.worldPosition,
                            o.getBlockState(), n.isActive());
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
        int speedCount = (int) upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
        int interval = getEffectiveCooldown(speedCount);
        if (tickCounter < interval) return;
        tickCounter = 0;

        if (!getMainNode().isActive()) return;

        var grid = getMainNode().getGrid();
        if (grid == null) return;

        var types = collectTypes(lv);
        if (types.isEmpty()) return;

        // Draw power (per entity type)
        var energyGrid = grid.getEnergyService();
        long totalCost = (long) Config.spawnEnergyCost * types.size();
        var extracted = energyGrid.extractAEPower(totalCost, Actionable.MODULATE,
                appeng.api.config.PowerMultiplier.CONFIG);
        if (Math.abs(extracted - totalCost) > 1) return;

        long lootingMult = getLootingMultiplier();
        boolean hasProbability = upgrades.isInstalled(ModItems.PROBABILITY_CARD);
        var src = new MachineSource(this);
        var inv = grid.getStorageService().getInventory();

        // Process each entity type in parallel
        for (var type : types) {
            processEntityType(lv, type, hasProbability, lootingMult, src, inv);
        }

        // Apotheosis integration: with a weapon installed, generate gems/materials
        // following the weapon's rarity
        var weapon = weaponSlot.getStackInSlot(0);
        if (!weapon.isEmpty() && hasApotheosis()) {
            com.example.mespawner.compat.ApotheosisCompat.generateBonus(lv, weapon, stack -> {
                var key = AEItemKey.of(stack);
                if (key != null) inv.insert(key, stack.getCount(), Actionable.MODULATE, src);
            });
        }

        setChanged();
    }

    /** Collect the entity types currently selected for processing (egg or formed disk mobs). */
    public java.util.List<EntityType<?>> collectTypes(ServerLevel lv) {
        var types = new java.util.ArrayList<EntityType<?>>();
        var item = eggSlot.getStackInSlot(0);
        if (item.isEmpty()) return types;
        if (item.getItem() instanceof SpawnEggItem eggItem) {
            var t = (EntityType<?>) eggItem.getType(item);
            if (t != null) types.add(t);
        } else if (item.getItem() instanceof com.example.mespawner.item.MonsterDiskItem disk) {
            types.addAll(disk.getFormedEntityTypes(item, lv));
        }
        return types;
    }

    /** 0=idle, 1=working, 2=missing energy. */
    public int getStatus() {
        if (!(level instanceof ServerLevel lv)) return 0;
        var types = collectTypes(lv);
        if (types.isEmpty()) return 0; // nothing to process → idle

        var grid = getMainNode().getGrid();
        if (grid == null || !getMainNode().isActive()) return 2;

        long required = (long) Config.spawnEnergyCost * types.size();
        double extracted = grid.getEnergyService().extractAEPower(required, Actionable.SIMULATE,
                appeng.api.config.PowerMultiplier.CONFIG);
        if (Math.abs(extracted - required) > 1) return 2;

        return 1;
    }

    /** Current power usage in AE per tick. */
    public long getPowerUsagePerTick() {
        if (!(level instanceof ServerLevel lv)) return 0;
        var types = collectTypes(lv);
        if (types.isEmpty()) return 0;
        int speedCount = (int) upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
        int interval = getEffectiveCooldown(speedCount);
        long totalCost = (long) Config.spawnEnergyCost * types.size();
        return Math.max(1, totalCost / interval);
    }

    /**
     * Build loot params. If a weapon is installed, simulate the player killing the mob
     * with that weapon so kill conditions and looting enchantments apply.
     */
    private net.minecraft.world.level.storage.loot.LootParams buildLootParams(ServerLevel lv, net.minecraft.world.entity.Entity tmpEntity) {
        var builder = new net.minecraft.world.level.storage.loot.LootParams.Builder(lv)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                        worldPosition.getCenter())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, tmpEntity);

        var weapon = weaponSlot.getStackInSlot(0);
        if (!weapon.isEmpty()) {
            var fakePlayer = new net.neoforged.neoforge.common.util.FakePlayer(lv,
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "MESpawner"));
            fakePlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);

            var lootingHolder = lv.registryAccess().holderOrThrow(
                    net.minecraft.world.item.enchantment.Enchantments.LOOTING);
            int lootingLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                    lootingHolder, weapon);

            builder.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,
                            lv.damageSources().playerAttack(fakePlayer))
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER, fakePlayer)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ATTACKING_ENTITY, fakePlayer)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DIRECT_ATTACKING_ENTITY, fakePlayer)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, weapon)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ENCHANTMENT_LEVEL, lootingLevel)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ENCHANTMENT_ACTIVE, lootingLevel > 0);
        } else {
            builder.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,
                    lv.damageSources().generic());
        }

        return builder.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
    }

    /**
     * Apotheosis is optional. Never touch ApotheosisCompat (not even an isLoaded()
     * helper inside it) unless this returns true — loading that class can itself
     * fail with NoClassDefFoundError when Apotheosis/Placebo are absent.
     */
    private static boolean hasApotheosis() {
        return net.neoforged.fml.ModList.get().isLoaded("apotheosis");
    }

    private void processEntityType(ServerLevel lv, EntityType<?> type, boolean hasProbability,
                                   long lootingMult, MachineSource src, appeng.api.storage.MEStorage inv) {
        var lootTable = lv.getServer().reloadableRegistries().getLootTable(type.getDefaultLootTable());
        if (lootTable == LootTable.EMPTY) return;

        var tmpEntity = type.create(lv);
        if (tmpEntity == null) return;
        var params = buildLootParams(lv, tmpEntity);

        if (hasProbability) {
            // Probability card: products come ONLY from the loot table (static expansion
            // + learned extra drops). No boss bonus or other operations.
            var expanded = staticallyExpandLootTable(lv, lootTable, params, new java.util.HashMap<>());

            // Produce from static expansion (skip Apotheosis gems — handled by weapon bonus)
            for (var entry : expanded.entrySet()) {
                if (hasApotheosis()
                        && com.example.mespawner.compat.ApotheosisCompat.isGemItem(entry.getKey())) {
                    continue;
                }
                inv.insert(entry.getKey(), entry.getValue() * lootingMult, Actionable.MODULATE, src);
            }

            // Produce learned extra drops (from previous normal rolls)
            var entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            var extras = com.example.mespawner.util.ExtraDropsStorage.load(lv, entityId);
            // Apotheosis gems are never produced by the probability card, and stale
            // entries from older versions are purged from the saved data
            if (hasApotheosis()) {
                boolean purged = extras.keySet().removeIf(com.example.mespawner.compat.ApotheosisCompat::isGemItem);
                if (purged) {
                    com.example.mespawner.util.ExtraDropsStorage.save(lv, entityId, extras);
                }
            }
            for (var entry : extras.entrySet()) {
                if (entry.getKey() == null) continue; // defensive: never insert a null key
                inv.insert(entry.getKey(), entry.getValue() * lootingMult, Actionable.MODULATE, src);
            }

            // Learn: roll the normal production logic once (no production) and save
            // any items not already covered by static expansion or previous extras
            var discovered = new java.util.HashMap<AEItemKey, Long>();
            for (var drop : lootTable.getRandomItems(params)) {
                // Apotheosis gems are never saved to the world folder
                if (hasApotheosis()
                        && com.example.mespawner.compat.ApotheosisCompat.isGem(drop)) {
                    continue;
                }
                var key = AEItemKey.of(drop);
                if (key != null) {
                    discovered.merge(key, (long) Math.max(1, drop.getCount()), Math::max);
                }
            }
            boolean changed = false;
            for (var entry : discovered.entrySet()) {
                if (expanded.containsKey(entry.getKey())) continue; // already produced statically
                if (!extras.containsKey(entry.getKey())) {
                    extras.put(entry.getKey(), entry.getValue());
                    changed = true;
                }
            }
            if (changed) {
                com.example.mespawner.util.ExtraDropsStorage.save(lv, entityId, extras);
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

        // Bonus drops for special bosses — manually-added fake loot table entries,
        // applied in both probability and normal production (affected by looting multiplier)
        if (type == EntityType.ENDER_DRAGON) {
            inv.insert(AEItemKey.of(net.minecraft.world.item.Items.DRAGON_EGG), lootingMult, Actionable.MODULATE, src);
        } else if (type == EntityType.WITHER) {
            inv.insert(AEItemKey.of(net.minecraft.world.item.Items.NETHER_STAR), lootingMult, Actionable.MODULATE, src);
        }
    }

    /**
     * Statically expand the loot table, enumerating every possible drop.
     * Known entry types are expanded directly (ignoring conditions), while
     * dynamic/custom entries (e.g. from other mods) are executed normally.
     */
    private java.util.Map<AEItemKey, Long> staticallyExpandLootTable(ServerLevel lv, LootTable table,
                                                                      net.minecraft.world.level.storage.loot.LootParams params,
                                                                      java.util.Map<AEItemKey, Long> result) {
        var context = new net.minecraft.world.level.storage.loot.LootContext.Builder(params)
                .create(java.util.Optional.empty());
        try {
            var poolsField = LootTable.class.getDeclaredField("pools");
            poolsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var pools = (java.util.List<net.minecraft.world.level.storage.loot.LootPool>) poolsField.get(table);
            for (var pool : pools) {
                var entriesField = net.minecraft.world.level.storage.loot.LootPool.class.getDeclaredField("entries");
                entriesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                var entries = (java.util.List<net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer>) entriesField.get(pool);
                for (var entry : entries) {
                    expandEntry(lv, entry, params, context, result);
                }
            }
        } catch (Exception e) {
            // Fallback: normal rolls
            for (int i = 0; i < 100; i++)
                for (var drop : table.getRandomItems(params))
                    result.merge(AEItemKey.of(drop), (long) drop.getCount(), Math::max);
        }
        result.remove(null);
        return result;
    }

    private void expandEntry(ServerLevel lv,
                             net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer entry,
                             net.minecraft.world.level.storage.loot.LootParams params,
                             net.minecraft.world.level.storage.loot.LootContext context,
                             java.util.Map<AEItemKey, Long> result) {
        try {
            if (entry instanceof net.minecraft.world.level.storage.loot.entries.LootItem lootItem) {
                var f = net.minecraft.world.level.storage.loot.entries.LootItem.class.getDeclaredField("item");
                f.setAccessible(true);
                var item = (net.minecraft.core.Holder<net.minecraft.world.item.Item>) f.get(lootItem);
                result.merge(AEItemKey.of(item.value()), computeMaxCount(lootItem), Math::max);
            } else if (entry instanceof net.minecraft.world.level.storage.loot.entries.TagEntry tagEntry) {
                var f = net.minecraft.world.level.storage.loot.entries.TagEntry.class.getDeclaredField("tag");
                f.setAccessible(true);
                var tag = (net.minecraft.tags.TagKey<net.minecraft.world.item.Item>) f.get(tagEntry);
                long count = computeMaxCount(tagEntry);
                for (var h : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                    result.merge(AEItemKey.of(h.value()), count, Math::max);
                }
            } else if (entry instanceof net.minecraft.world.level.storage.loot.entries.NestedLootTable nested) {
                var f = net.minecraft.world.level.storage.loot.entries.NestedLootTable.class.getDeclaredField("contents");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                var contents = (com.mojang.datafixers.util.Either<
                        net.minecraft.resources.ResourceKey<LootTable>, LootTable>) f.get(nested);
                contents.ifLeft(key -> {
                    var sub = lv.getServer().reloadableRegistries().getLootTable(key);
                    if (sub != LootTable.EMPTY) staticallyExpandLootTable(lv, sub, params, result);
                });
                contents.ifRight(sub -> staticallyExpandLootTable(lv, sub, params, result));
            } else if (entry instanceof net.minecraft.world.level.storage.loot.entries.CompositeEntryBase composite) {
                var f = net.minecraft.world.level.storage.loot.entries.CompositeEntryBase.class.getDeclaredField("children");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                var children = (java.util.List<net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer>) f.get(composite);
                for (var child : children) {
                    expandEntry(lv, child, params, context, result);
                }
            } else if (entry instanceof net.minecraft.world.level.storage.loot.entries.EmptyLootItem) {
                // No drops
            } else {
                // Dynamic / mod-custom entries: execute them normally against the context
                executeEntryNormally(entry, context, result);
            }
        } catch (Exception ignored) {
            // Reflection failed for this entry: execute it normally as fallback
            executeEntryNormally(entry, context, result);
        }
    }

    private void executeEntryNormally(
            net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer entry,
            net.minecraft.world.level.storage.loot.LootContext context,
            java.util.Map<AEItemKey, Long> result) {
        try {
            entry.expand(context, poolEntry -> poolEntry.createItemStack(stack -> {
                var key = AEItemKey.of(stack);
                if (key != null) result.merge(key, (long) Math.max(1, stack.getCount()), Math::max);
            }, context));
        } catch (Exception ignored) {
            // Give up on this entry
        }
    }

    /** Compute the maximum possible count for an entry by inspecting SetItemCountFunction. */
    private long computeMaxCount(net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer singleton) {
        try {
            var f = net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer.class.getDeclaredField("functions");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            var functions = (java.util.List<net.minecraft.world.level.storage.loot.functions.LootItemFunction>) f.get(singleton);
            for (var fn : functions) {
                if (fn instanceof net.minecraft.world.level.storage.loot.functions.SetItemCountFunction countFn) {
                    var vf = net.minecraft.world.level.storage.loot.functions.SetItemCountFunction.class.getDeclaredField("value");
                    vf.setAccessible(true);
                    var provider = (net.minecraft.world.level.storage.loot.providers.number.NumberProvider) vf.get(countFn);
                    return maxCountOf(provider);
                }
            }
        } catch (Exception ignored) {}
        return 1;
    }

    private long maxCountOf(net.minecraft.world.level.storage.loot.providers.number.NumberProvider provider) {
        if (provider instanceof net.minecraft.world.level.storage.loot.providers.number.ConstantValue c) {
            return Math.max(1, (long) c.value());
        }
        if (provider instanceof net.minecraft.world.level.storage.loot.providers.number.UniformGenerator u) {
            return maxCountOf(u.max());
        }
        return 1; // Unknown providers: default to 1
    }

    private long getLootingMultiplier() {
        // Collect all installed looting card multipliers and keep only the top 3
        // (looting slots are a shared pool of 3 across all looting card tiers)
        var multipliers = new java.util.ArrayList<Long>();
        for (int i = 0; i < upgrades.getInstalledUpgrades(ModItems.ULTIMATE_LOOTING_CARD); i++) multipliers.add(9L);
        for (int i = 0; i < upgrades.getInstalledUpgrades(ModItems.COMPRESSED_LOOTING_CARD); i++) multipliers.add(6L);
        for (int i = 0; i < upgrades.getInstalledUpgrades(ModItems.LOOTING_CARD); i++) multipliers.add(2L);
        multipliers.sort(java.util.Collections.reverseOrder());
        long mult = 1;
        for (int i = 0; i < Math.min(3, multipliers.size()); i++) {
            mult *= multipliers.get(i);
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

    // ===== NBT =====
    @Override public void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r); t.putDouble("pwr", stored.getAmount());
        var eggTag = new CompoundTag();
        eggSlot.writeToNBT(t, "egg", r);
        weaponSlot.writeToNBT(t, "weapon", r);
        upgrades.writeToNBT(t, "upgrades", r);
    }
    @Override public void loadTag(CompoundTag t, HolderLookup.Provider r) {
        super.loadTag(t, r); stored.setStored(t.getDouble("pwr"));
        eggSlot.readFromNBT(t, "egg", r);
        weaponSlot.readFromNBT(t, "weapon", r);
        upgrades.readFromNBT(t, "upgrades", r);
    }

    // ===== Menu =====
    @Override public Component getDisplayName() { return Component.translatable("block.mespawner.me_spawner"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new MESpawnerMenu(id, inv, this); }
}
