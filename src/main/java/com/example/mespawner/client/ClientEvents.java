package com.example.mespawner.client;

import appeng.client.gui.style.StyleManager;
import com.example.mespawner.MESpawner;
import com.example.mespawner.item.MonsterDiskItem;
import com.example.mespawner.menu.MESpawnerMenu;
import com.example.mespawner.registration.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MESpawner.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        var prop = ResourceLocation.fromNamespaceAndPath(MESpawner.MOD_ID, "fill_state");
        for (var disk : new MonsterDiskItem[]{
                ModItems.MONSTER_DISK_1K.get(),
                ModItems.MONSTER_DISK_4K.get(),
                ModItems.MONSTER_DISK_16K.get(),
                ModItems.MONSTER_DISK_64K.get(),
                ModItems.MONSTER_DISK_256K.get()}) {
            ItemProperties.register(disk, prop, (stack, level, entity, seed) -> disk.getFillState(stack));
        }
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MESpawnerMenu.TYPE, ClientEvents::createMESpawnerScreen);
    }

    private static MESpawnerScreen createMESpawnerScreen(
            MESpawnerMenu menu, Inventory inv, Component title) {
        var style = StyleManager.loadStyleDoc("/screens/me_spawner.json");
        return new MESpawnerScreen(menu, inv, title, style);
    }

    /** Opens the monster disk list screen. Called from common code but only on the client. */
    public static void openMonsterDiskScreen(
            java.util.Map<ResourceLocation, Integer> entries, int capacity) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new MonsterDiskScreen(
                Component.translatable("item.mespawner.monster_disk"), entries, capacity));
    }
}
