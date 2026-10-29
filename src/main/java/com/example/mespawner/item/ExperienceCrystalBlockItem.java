package com.example.mespawner.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Experience crystal block: cannot be placed, only used. Right-click grants one experience
 * LEVEL; shift-right-click consumes the whole held stack (one level per block).
 */
public class ExperienceCrystalBlockItem extends Item {

    public ExperienceCrystalBlockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                // One level per block for the entire held stack
                player.giveExperienceLevels(stack.getCount());
                stack.shrink(stack.getCount());
            } else {
                player.giveExperienceLevels(1);
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
