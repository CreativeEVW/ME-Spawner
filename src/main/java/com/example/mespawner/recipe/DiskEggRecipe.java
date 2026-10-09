package com.example.mespawner.recipe;

import com.example.mespawner.item.MonsterDiskItem;
import com.example.mespawner.registration.ModItems;
import com.example.mespawner.registration.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Crafting recipe: a not-full monster disk + a spawn egg writes the egg's entity
 * into the disk directly as "formed".
 */
public class DiskEggRecipe implements CraftingRecipe {

    /** Sentinel kill count guaranteeing the formed state (always &gt; n, max n = 1000). */
    public static final int FORMED_KILLS = 1_000_000;

    private final CraftingBookCategory category;

    public DiskEggRecipe(CraftingBookCategory category) {
        this.category = category;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack disk = null;
        ItemStack egg = null;
        for (var stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof MonsterDiskItem) {
                if (disk != null) return false; // only one disk
                disk = stack;
            } else if (stack.getItem() instanceof SpawnEggItem) {
                if (egg != null) return false; // only one egg
                egg = stack;
            } else {
                return false; // only disk + egg allowed
            }
        }
        if (disk == null || egg == null) return false;

        var diskItem = (MonsterDiskItem) disk.getItem();
        return diskItem.getEntries(disk).size() < diskItem.getCapacity();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack disk = null;
        ItemStack egg = null;
        for (var stack : input.items()) {
            if (stack.getItem() instanceof MonsterDiskItem) disk = stack;
            else if (stack.getItem() instanceof SpawnEggItem) egg = stack;
        }
        if (disk == null || egg == null) return ItemStack.EMPTY;

        var type = (net.minecraft.world.entity.EntityType<?>) ((SpawnEggItem) egg.getItem()).getType(egg);
        if (type == null) return ItemStack.EMPTY;

        var result = disk.copy();
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        ((MonsterDiskItem) disk.getItem()).writeEntityEntry(result, key, FORMED_KILLS);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ModItems.MONSTER_DISK_1K.get());
    }

    @Override
    public boolean isSpecial() {
        return true; // don't show in recipe book
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.DISK_EGG_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        // Must be RecipeType.CRAFTING: the crafting menu only looks up recipes in this bucket.
        return RecipeType.CRAFTING;
    }
}
