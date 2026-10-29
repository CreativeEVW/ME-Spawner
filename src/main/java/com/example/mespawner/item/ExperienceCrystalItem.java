package com.example.mespawner.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Experience crystal: right-click consumes one crystal and grants the same experience as a
 * bottle o' enchanting; shift-right-click absorbs the whole held stack at once.
 */
public class ExperienceCrystalItem extends Item {

    public ExperienceCrystalItem(Properties properties) {
        super(properties);
    }

    /** Same amount as vanilla's experience bottle (3 + rand(5) + rand(5) = 3..11 XP). */
    private static int bottleXp(Level level) {
        return 3 + level.random.nextInt(5) + level.random.nextInt(5);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                // Absorb the entire held stack at once
                player.giveExperiencePoints(bottleXp(level) * stack.getCount());
                stack.shrink(stack.getCount());
            } else {
                player.giveExperiencePoints(bottleXp(level));
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
