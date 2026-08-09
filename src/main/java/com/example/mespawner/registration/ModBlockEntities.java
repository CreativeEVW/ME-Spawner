package com.example.mespawner.registration;

import com.example.mespawner.MESpawner;
import com.example.mespawner.blockentity.MESpawnerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MESpawner.MOD_ID);

    public static final Supplier<BlockEntityType<MESpawnerBlockEntity>> ME_SPAWNER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("me_spawner", () -> {
                var ref = new AtomicReference<BlockEntityType<MESpawnerBlockEntity>>();
                var type = BlockEntityType.Builder.of(
                        (pos, state) -> new MESpawnerBlockEntity(ref.get(), pos, state),
                        ModBlocks.ME_SPAWNER.get()
                ).build(null);
                ref.set(type);
                return type;
            });
}
