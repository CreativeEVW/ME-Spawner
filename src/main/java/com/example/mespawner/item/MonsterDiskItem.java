package com.example.mespawner.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.TreeMap;

public class MonsterDiskItem extends Item {

    private final int capacity;

    public MonsterDiskItem(Properties properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    // ===== Data access =====

    public Map<ResourceLocation, Integer> getEntries(ItemStack stack) {
        var map = new TreeMap<ResourceLocation, Integer>();
        var customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var tag = customData.copyTag();
        if (tag.contains("entries", CompoundTag.TAG_COMPOUND)) {
            var entries = tag.getCompound("entries");
            for (String key : entries.getAllKeys()) {
                map.put(ResourceLocation.parse(key), entries.getInt(key));
            }
        }
        return map;
    }

    private void setEntries(ItemStack stack, Map<ResourceLocation, Integer> entries) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var e = new CompoundTag();
        for (var entry : entries.entrySet()) {
            e.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("entries", e);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public int getFillState(ItemStack stack) {
        int count = getEntries(stack).size();
        if (count == 0) return 0;
        return count >= capacity ? 2 : 1;
    }

    /** Write an entity entry with the given kill count (used by the disk+egg crafting recipe). */
    public void writeEntityEntry(ItemStack stack, ResourceLocation entityKey, int killCount) {
        var entries = new TreeMap<>(getEntries(stack));
        entries.put(entityKey, killCount);
        setEntries(stack, entries);
    }

    /** Compute the "formed" kill count threshold: n = floor(1000 / maxHealth). */
    public static int getFormedKillCount(net.minecraft.world.entity.EntityType<?> type, Level level) {
        double maxHealth = 20.0;
        var entity = type.create(level);
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            maxHealth = living.getMaxHealth();
        }
        return (int) Math.floor(1000.0 / maxHealth);
    }

    /** Get the entity types that are "formed" (kill count exceeds threshold). */
    public java.util.List<net.minecraft.world.entity.EntityType<?>> getFormedEntityTypes(ItemStack stack, Level level) {
        var formed = new java.util.ArrayList<net.minecraft.world.entity.EntityType<?>>();
        for (var entry : getEntries(stack).entrySet()) {
            // ENTITY_TYPE.get() returns the default (Pig) for unknown ids — check containsKey first
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entry.getKey())) continue;
            var type = BuiltInRegistries.ENTITY_TYPE.get(entry.getKey());
            if (entry.getValue() > getFormedKillCount(type, level)) {
                formed.add(type);
            }
        }
        return formed;
    }

    /** Attempt to store an entity type into the disk. Returns 0=stored, 1=already present, 2=full. */
    public int tryStoreEntity(ItemStack stack, ResourceLocation entityKey) {
        var entries = new TreeMap<>(getEntries(stack));
        if (entries.containsKey(entityKey)) return 1;
        if (entries.size() >= capacity) return 2;
        entries.put(entityKey, 0);
        setEntries(stack, entries);
        return 0;
    }

    /** Sync the player's kill stats into the disk, never lowering existing values (set, not add). */
    public void recordStats(ItemStack stack, Player player) {
        if (player instanceof ServerPlayer sp) {
            var updated = new TreeMap<ResourceLocation, Integer>();
            for (var entry : getEntries(stack).entrySet()) {
                // ENTITY_TYPE.get() returns the default (Pig) for unknown ids — check containsKey first
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entry.getKey())) continue;
                var type = BuiltInRegistries.ENTITY_TYPE.get(entry.getKey());
                // Keep the disk's value when the player's stats are lower (e.g. entries formed via crafting).
                updated.put(entry.getKey(), Math.max(entry.getValue(), sp.getStats().getValue(Stats.ENTITY_KILLED.get(type))));
            }
            setEntries(stack, updated);
            sp.displayClientMessage(Component.translatable("item.mespawner.monster_disk.recorded"), true);
        }
    }

    // ===== Right-click air =====
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            recordStats(stack, player);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        } else {
            if (level.isClientSide()) {
                openListScreen(stack, getEntries(stack));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
    }

    private void openListScreen(ItemStack stack, Map<ResourceLocation, Integer> entries) {
        // Client-only class: only loaded on the client (guarded by isClientSide above)
        com.example.mespawner.client.ClientEvents.openMonsterDiskScreen(entries, capacity);
    }
}
