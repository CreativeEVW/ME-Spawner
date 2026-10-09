package com.example.mespawner.util;

import com.example.mespawner.MESpawner;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import appeng.api.stacks.AEItemKey;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores learned extra drops per entity in the world folder:
 * {@code <world>/mespawner/extra_drops/<namespace>__<path>.json}
 */
public class ExtraDropsStorage {

    private static final Gson GSON = new Gson();

    public static Path getFile(ServerLevel level, ResourceLocation entityId) {
        return level.getServer().getWorldPath(LevelResource.ROOT)
                .resolve("mespawner")
                .resolve("extra_drops")
                .resolve(entityId.getNamespace() + "__" + entityId.getPath() + ".json");
    }

    public static Map<AEItemKey, Long> load(ServerLevel level, ResourceLocation entityId) {
        var result = new HashMap<AEItemKey, Long>();
        var file = getFile(level, entityId);
        try {
            if (!Files.exists(file)) return result;
            var json = JsonParser.parseString(Files.readString(file));
            if (!json.isJsonObject()) return result;
            var entries = json.getAsJsonObject().getAsJsonArray("entries");
            boolean stale = false;
            for (var e : entries) {
                var obj = e.getAsJsonObject();
                var id = ResourceLocation.parse(obj.get("item").getAsString());
                long count = obj.get("count").getAsLong();
                // Note: ITEM.get() returns the default (AIR) for unknown ids on NeoForge,
                // which would yield a null AEItemKey — must check containsKey first.
                if (!BuiltInRegistries.ITEM.containsKey(id)) {
                    stale = true; // item from a removed mod — drop it
                    continue;
                }
                var key = AEItemKey.of(BuiltInRegistries.ITEM.get(id));
                if (key == null || count <= 0) {
                    stale = true; // bad entry — drop it
                    continue;
                }
                result.put(key, count);
            }
            if (stale) {
                // Rewrite the file without entries whose items no longer exist
                save(level, entityId, result);
            }
        } catch (Exception e) {
            MESpawner.LOGGER.warn("Failed to load extra drops for {}", entityId, e);
        }
        return result;
    }

    public static void save(ServerLevel level, ResourceLocation entityId, Map<AEItemKey, Long> entries) {
        var file = getFile(level, entityId);
        try {
            Files.createDirectories(file.getParent());
            var arr = new JsonArray();
            for (var entry : entries.entrySet()) {
                var obj = new JsonObject();
                var itemId = BuiltInRegistries.ITEM.getKey(entry.getKey().getItem());
                obj.addProperty("item", itemId.toString());
                obj.addProperty("count", entry.getValue());
                arr.add(obj);
            }
            var root = new JsonObject();
            root.add("entries", arr);
            Files.writeString(file, GSON.toJson(root));
        } catch (Exception e) {
            MESpawner.LOGGER.warn("Failed to save extra drops for {}", entityId, e);
        }
    }
}
