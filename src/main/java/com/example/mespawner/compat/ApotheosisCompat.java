package com.example.mespawner.compat;

import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Apotheosis integration. All Apotheosis class references live in this class.
 *
 * IMPORTANT: callers must check ModList.get().isLoaded("apotheosis") BEFORE
 * invoking any method here. Loading this class itself fails with
 * NoClassDefFoundError when Apotheosis/Placebo are absent (JVM verification
 * resolves the referenced types' class hierarchies), so no isLoaded() helper
 * is provided here — the guard must live outside this class.
 */
public class ApotheosisCompat {

    public static boolean isGem(ItemStack stack) {
        return stack.getItem() instanceof GemItem;
    }

    public static boolean isGemItem(appeng.api.stacks.AEItemKey key) {
        return key.getItem() instanceof GemItem;
    }

    /**
     * Generate gems and materials following the installed weapon's rarity.
     * Gem purity: 74% = weapon level, 24% = +1, 2% = +2 (overflow → double gems).
     * Materials: 74% = weapon level, 24% = +1, 2% = +2 (overflow → double godforged pearls).
     */
    public static void generateBonus(ServerLevel level, ItemStack weapon, Consumer<ItemStack> output) {
        var rarityHolder = AffixHelper.getRarity(weapon);
        if (rarityHolder == null || rarityHolder.getId() == null) return;

        var rarities = RarityRegistry.getSortedRarities();
        int rarityIndex = -1;
        var rarityId = rarityHolder.getId();
        for (int i = 0; i < rarities.size(); i++) {
            if (RarityRegistry.INSTANCE.getKey(rarities.get(i)).equals(rarityId)) {
                rarityIndex = i;
                break;
            }
        }
        if (rarityIndex < 0) return;

        var rand = level.random;

        // ===== Gems =====
        int gemRoll = rand.nextInt(100);
        int gemLevel = rarityIndex + (gemRoll < 74 ? 0 : gemRoll < 98 ? 1 : 2);
        int gemCount = 1;
        if (gemLevel > 5) {
            gemLevel = 5;
            gemCount = 2; // level overflow → double gems
        }
        var purity = Purity.values()[gemLevel];
        var gem = randomGem();
        if (gem != null) {
            for (int i = 0; i < gemCount; i++) {
                output.accept(GemRegistry.createGemStack(gem, purity));
            }
        }

        // ===== Materials =====
        int matRoll = rand.nextInt(100);
        int matLevel = rarityIndex + (matRoll < 74 ? 0 : matRoll < 98 ? 1 : 2);
        if (matLevel > 4) {
            // Material level overflow → double godforged pearls (the mythic material)
            var pearl = RarityRegistry.getSortedRarities().get(4).getMaterial();
            output.accept(new ItemStack(pearl, 2));
        } else {
            var rarity = rarities.get(matLevel);
            output.accept(new ItemStack(rarity.getMaterial()));
        }
    }

    private static Gem randomGem() {
        var gems = new ArrayList<Gem>(GemRegistry.INSTANCE.getValues());
        if (gems.isEmpty()) return null;
        return gems.get(net.minecraft.util.RandomSource.create().nextInt(gems.size()));
    }
}
