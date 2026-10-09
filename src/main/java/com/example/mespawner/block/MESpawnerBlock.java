package com.example.mespawner.block;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.locator.MenuLocators;
import com.example.mespawner.blockentity.MESpawnerBlockEntity;
import com.example.mespawner.menu.MESpawnerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class MESpawnerBlock extends AEBaseEntityBlock<MESpawnerBlockEntity> {

    public static final BooleanProperty ONLINE = BooleanProperty.create("online");

    public static final double MAX_POWER = 2_000_000;
    public static final double CHARGE_RATE = 200_000;

    public MESpawnerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ONLINE, false));
    }

    public double getMaxPower() { return MAX_POWER; }
    public double getChargeRate() { return CHARGE_RATE; }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof MESpawnerBlockEntity be) {
                for (var stack : be.eggSlot) {
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
                for (var stack : be.weaponSlot) {
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
                be.getUpgrades().forEach(stack ->
                        net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack));
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(ONLINE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        var be = getBlockEntity(level, pos);
        if (be == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            appeng.menu.MenuOpener.open(MESpawnerMenu.TYPE, player, MenuLocators.forBlockEntity(be));
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
