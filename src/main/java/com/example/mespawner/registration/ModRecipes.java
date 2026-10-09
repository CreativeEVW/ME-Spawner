package com.example.mespawner.registration;

import com.example.mespawner.MESpawner;
import com.example.mespawner.recipe.DiskEggRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, MESpawner.MOD_ID);

    // The recipe itself uses RecipeType.CRAFTING (see DiskEggRecipe#getType);
    // only the serializer is custom.
    public static final Supplier<RecipeSerializer<DiskEggRecipe>> DISK_EGG_SERIALIZER = RECIPE_SERIALIZERS.register(
            "disk_egg_crafting",
            () -> new SimpleCraftingRecipeSerializer<>(DiskEggRecipe::new));
}
