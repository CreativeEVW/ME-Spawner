package com.example.mespawner.block;

import com.example.mespawner.blockentity.MESpawnerBlockEntity;
import com.example.mespawner.registration.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class MESpawnerBlock extends BaseEntityBlock {

    public static final BooleanProperty ONLINE = BooleanProperty.create("online");
    public static final MapCodec<MESpawnerBlock> CODEC = simpleCodec(MESpawnerBlock::new);

    public static final double MAX_POWER = 2_000_000;
    public static final double CHARGE_RATE = 200_000;

    public MESpawnerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ONLINE, false));
    }

    public double getMaxPower() { return MAX_POWER; }
    public double getChargeRate() { return CHARGE_RATE; }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof MESpawnerBlockEntity be) {
                net.minecraft.world.Containers.dropContents(level, pos, be.eggSlot);
                net.minecraft.world.Containers.dropContents(level, pos, be.cardSlots);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(ONLINE);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MESpawnerBlockEntity(ModBlockEntities.ME_SPAWNER_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return null; // AE2 IGridTickable handles it
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hit) {
        // Wrench is handled by AE2's WrenchHook
        if (!level.isClientSide()) {
            MenuProvider mp = state.getMenuProvider(level, pos);
            if (mp != null) player.openMenu(mp, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            MenuProvider mp = state.getMenuProvider(level, pos);
            if (mp != null) player.openMenu(mp, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState s) { return true; }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MESpawnerBlockEntity be) {
            double f = be.getAECurrentPower() / be.getAEMaxPower();
            return Mth.floor(f * 14) + (be.getAECurrentPower() > 0 ? 1 : 0);
        }
        return 0;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, BlockPos from, boolean moving) {
    }

    public static void setOnline(Level level, BlockPos pos, BlockState state, boolean on) {
        if (!level.isClientSide() && state.getValue(ONLINE) != on)
            level.setBlock(pos, state.setValue(ONLINE, on), Block.UPDATE_ALL);
    }
}
