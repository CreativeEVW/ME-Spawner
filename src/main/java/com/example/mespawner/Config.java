package com.example.mespawner;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = MESpawner.MOD_ID)
public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Energy (AE units)
    public static final ModConfigSpec.IntValue SPAWN_ENERGY_COST;
    public static final ModConfigSpec.DoubleValue DEFAULT_MAX_ENERGY;
    public static final ModConfigSpec.DoubleValue DEFAULT_CHARGE_RATE;

    // Spawning
    public static final ModConfigSpec.IntValue SPAWN_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue MAX_SPAWN_COUNT;
    public static final ModConfigSpec.IntValue SPAWN_RANGE;

    // Machine
    public static final ModConfigSpec.BooleanValue REQUIRE_REDSTONE;

    static {
        BUILDER.push("Energy");
        SPAWN_ENERGY_COST = BUILDER
                .comment("AE energy cost per spawn operation")
                .defineInRange("spawnEnergyCost", 100000, 0, Integer.MAX_VALUE);
        DEFAULT_MAX_ENERGY = BUILDER
                .comment("Maximum AE energy storage of the ME Spawner (1000k AE)")
                .defineInRange("maxEnergyStorage", 2000000.0, 0.0, Double.MAX_VALUE);
        DEFAULT_CHARGE_RATE = BUILDER
                .comment("Charge rate of the ME Spawner (AE/t)")
                .defineInRange("chargeRate", 200000.0, 0.0, Double.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("Spawning");
        SPAWN_COOLDOWN_TICKS = BUILDER
                .comment("Cooldown in ticks between spawn operations (20 ticks = 1 second)")
                .defineInRange("spawnCooldownTicks", 100, 1, Integer.MAX_VALUE);
        MAX_SPAWN_COUNT = BUILDER
                .comment("Maximum number of mobs that can exist within spawn range before pausing")
                .defineInRange("maxSpawnCount", 10, 1, 100);
        SPAWN_RANGE = BUILDER
                .comment("Range (in blocks) to check for existing mobs and spawn new ones")
                .defineInRange("spawnRange", 4, 1, 16);
        BUILDER.pop();

        BUILDER.push("Machine");
        REQUIRE_REDSTONE = BUILDER
                .comment("If true, the ME Spawner requires a redstone signal to operate")
                .define("requireRedstone", false);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    // Runtime values
    public static int spawnEnergyCost;
    public static double defaultMaxEnergy;
    public static double defaultChargeRate;
    public static int spawnCooldownTicks;
    public static int maxSpawnCount;
    public static int spawnRange;
    public static boolean requireRedstone;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        spawnEnergyCost = SPAWN_ENERGY_COST.get();
        defaultMaxEnergy = DEFAULT_MAX_ENERGY.get();
        defaultChargeRate = DEFAULT_CHARGE_RATE.get();
        spawnCooldownTicks = SPAWN_COOLDOWN_TICKS.get();
        maxSpawnCount = MAX_SPAWN_COUNT.get();
        spawnRange = SPAWN_RANGE.get();
        requireRedstone = REQUIRE_REDSTONE.get();
    }
}
