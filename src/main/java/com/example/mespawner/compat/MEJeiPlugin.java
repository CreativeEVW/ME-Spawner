package com.example.mespawner.compat;

import appeng.menu.SlotSemantics;
import com.example.mespawner.MESpawner;
import com.example.mespawner.client.SpawnerControllerScreen;
import com.example.mespawner.registration.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI integration: ghost-drag items from JEI into the spawner controller's config slots
 * (same behavior as AE2's storage bus). Optional — only loaded when JEI is present.
 */
@JeiPlugin
public class MEJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(MESpawner.MOD_ID, "jei");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(SpawnerControllerScreen.class, new ConfigGhostHandler());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Info page for the experience crystal
        registration.addIngredientInfo(ModItems.EXPERIENCE_CRYSTAL.get(),
                Component.translatable("jei.mespawner.experience_crystal_info"));
    }

    /** Ghost-drag targets = the controller's CONFIG slots. */
    static class ConfigGhostHandler implements IGhostIngredientHandler<SpawnerControllerScreen> {

        @Override
        public <I> List<Target<I>> getTargetsTyped(SpawnerControllerScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
            var stack = ingredient.getItemStack().orElse(null);
            if (stack == null || stack.isEmpty()) {
                return List.of();
            }
            var menu = gui.getMenu();
            var targets = new ArrayList<Target<I>>();
            for (var slot : menu.getSlots(SlotSemantics.CONFIG)) {
                Rect2i area = new Rect2i(slot.x + gui.getGuiLeft(), slot.y + gui.getGuiTop(), 16, 16);
                int index = slot.index;
                targets.add(new Target<I>() {
                    @Override
                    public Rect2i getArea() {
                        return area;
                    }

                    @Override
                    public void accept(I ignored) {
                        menu.setFilter(index, stack);
                    }
                });
            }
            return targets;
        }

        @Override
        public void onComplete() {
        }
    }
}
