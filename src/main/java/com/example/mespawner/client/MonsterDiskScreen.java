package com.example.mespawner.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonsterDiskScreen extends Screen {

    private final Map<ResourceLocation, Integer> entries;
    private final int capacity;
    private final Map<ResourceLocation, Integer> formedCounts = new HashMap<>();
    private final Map<ResourceLocation, Float> maxHealths = new HashMap<>();
    private final List<ResourceLocation> orderedKeys;

    private int scrollOffset = 0;

    public MonsterDiskScreen(Component title, Map<ResourceLocation, Integer> entries, int capacity) {
        super(title);
        this.entries = entries;
        this.capacity = capacity;
        this.orderedKeys = new ArrayList<>(entries.keySet());
        computeEntityInfo();
    }

    private void computeEntityInfo() {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (var key : entries.keySet()) {
            var type = BuiltInRegistries.ENTITY_TYPE.get(key);
            if (type == null) continue;
            double maxHealth = 20.0;
            var entity = type.create(level);
            if (entity instanceof LivingEntity living) {
                maxHealth = living.getMaxHealth();
            }
            maxHealths.put(key, (float) maxHealth);
            formedCounts.put(key, (int) Math.floor(1000.0 / maxHealth));
        }
    }

    private int getVisibleCount() {
        return Math.max(1, (this.height - 60) / 28);
    }

    private int getMaxScroll() {
        return Math.max(0, entries.size() - getVisibleCount());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollOffset = (int) Math.max(0, Math.min(this.scrollOffset - scrollY, getMaxScroll()));
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        g.drawCenteredString(this.font, this.title, cx, 16, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.literal(entries.size() + " / " + capacity), cx, 30, 0xAAAAAA);

        int visible = getVisibleCount();
        int y = 50;
        for (int i = 0; i < visible && (i + scrollOffset) < orderedKeys.size(); i++) {
            var key = orderedKeys.get(i + scrollOffset);
            var type = BuiltInRegistries.ENTITY_TYPE.get(key);
            if (type == null) continue;

            var egg = SpawnEggItem.byId(type);
            if (egg != null) {
                g.renderItem(new ItemStack(egg), cx - 80, y - 4);
            }

            String name = type.getDescription().getString();
            g.drawString(this.font, name, cx - 58, y, 0xFFFFFF, false);

            int kills = entries.get(key);
            int n = formedCounts.getOrDefault(key, Integer.MAX_VALUE);
            int color = kills > n ? 0x55FF55 : 0xFFFF55; // green if formed, yellow otherwise
            g.drawString(this.font, Component.literal(kills + "/" + n), cx + 60, y, color, false);

            // Second line: gray max health
            float health = maxHealths.getOrDefault(key, 20.0f);
            g.drawString(this.font, Component.literal("生命值: " + health), cx - 58, y + 12, 0xAAAAAA, false);

            y += 28;
        }

        drawScrollbar(g);
    }

    private void drawScrollbar(GuiGraphics g) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return;

        int trackTop = 50;
        int trackBottom = this.height - 20;
        int trackHeight = trackBottom - trackTop;
        int trackX = this.width - 8;

        g.fill(trackX, trackTop, trackX + 4, trackBottom, 0x33FFFFFF);

        float ratio = (float) scrollOffset / maxScroll;
        int thumbHeight = Math.max(20, (int) (trackHeight * ((float) getVisibleCount() / entries.size())));
        int thumbTop = trackTop + (int) ((trackHeight - thumbHeight) * ratio);
        g.fill(trackX, thumbTop, trackX + 4, thumbTop + thumbHeight, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
