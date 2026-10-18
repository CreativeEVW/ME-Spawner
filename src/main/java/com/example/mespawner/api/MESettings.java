package com.example.mespawner.api;

import appeng.api.config.Setting;
import appeng.api.config.YesNo;
import com.google.common.base.Preconditions;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom AE2-style settings used by the config manager
 * (copied from AppliedGenerators' AGSettings pattern).
 */
public final class MESettings {

    private static final Map<String, Setting<?>> SETTINGS = new HashMap<>();

    /** YES = insert products into the ME network, NO = push them into adjacent containers. */
    public static final Setting<YesNo> ME_EXPORT = register("me_export", YesNo.YES, YesNo.NO);

    private MESettings() {
        // NO-OP
    }

    @SuppressWarnings("SameParameterValue")
    @SafeVarargs
    private static synchronized <T extends Enum<T>> Setting<T> register(String name, T firstOption, T... moreOptions) {
        Preconditions.checkState(!SETTINGS.containsKey(name));
        Setting<T> setting = new Setting<>(name, firstOption.getDeclaringClass(), EnumSet.of(firstOption, moreOptions));
        SETTINGS.put(name, setting);
        return setting;
    }

    public static Setting<?> getOrThrow(String name) {
        var setting = SETTINGS.get(name);
        if (setting == null) {
            throw new IllegalArgumentException("Unknown setting '" + name + "'");
        }
        return setting;
    }
}
