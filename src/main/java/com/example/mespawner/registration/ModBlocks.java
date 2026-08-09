package com.example.mespawner.registration;

import com.example.mespawner.MESpawner;
import com.example.mespawner.block.MESpawnerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MESpawner.MOD_ID);

    public static final DeferredBlock<MESpawnerBlock> ME_SPAWNER = BLOCKS.register(
            "me_spawner",
            () -> new MESpawnerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5, 6)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
}
