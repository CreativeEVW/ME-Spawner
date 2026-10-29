---
navigation:
  parent: index.md
  title: ME Spawner
  icon: me_spawner
item_ids:
- mespawner:me_spawner
---

# ME Spawner

<BlockImage id="me_spawner" scale="8" />

Simulates killing the mob of the spawn egg in the main slot and generates drops from its loot table.

- **Work cycle**: every 10 seconds by default (speed cards halve the cooldown each)
- **Power**: `spawnEnergyCost` per cycle (default 100,000 AE), drawn from the internal buffer first, the grid covers the deficit
- Requires a channel

## Two Slots

| Slot | Accepts | Notes |
|------|---------|-------|
| Main | Spawn eggs / monster disks | Selects the mob; disks need "formed" entries (kill count above the threshold, or formed directly via disk+egg crafting) |
| Weapon | Swords / tools | With Apotheosis installed: simulates the kill with this weapon, generating gems and materials |

## Storage Mode & Output Sides

- **Storage mode button**: Input AE (default, products go into the ME network) / Adjacent containers (products are pushed only to containers on the output sides; nothing is output when they are full)
- **Output sides button**: only visible in adjacent-container mode
- Offline (no network): products can only go to adjacent containers

## Energy Buffer & FE Input

- Built-in 2M AE energy buffer (**private** - the grid does not charge it)
- Accepts direct FE input (1 AE = 2 FE)
- Works offline when the buffer has enough energy

## Special Drops

- Sheep: white wool x2; Warden: echo shard; Wither: nether star
- Ender Dragon: dragon egg (with Draconic Evolution also: dragon heart, draconium dust x64)
- Draconic Evolution: Chaos Guardian -> awakened draconium block, chaos shard, dragon heart
- Allthemodium: Piglich -> piglich heart
- Special drops apply in both normal and probability-card modes and scale with looting

## Acceleration Perception

External tick accelerators do not make the machine work more often: the work rate is locked to the designed interval; accelerated cycles are paid out as a production multiplier (capped at 64x), so total output matches the acceleration while machine overhead stays at the designed rate.

## Upgrade Slots (8)

| Slots | Type | Max | Effect |
|-------|------|-----|--------|
| 1 | Probability card | 1 | Iterate the loot table, drop 1 of every possible item |
| 2-4 | Looting card family | 3 (effective) | Drop count x2/x6/x9 (product of the top 3) |
| 5-8 | Speed card | 4 | Halves the cooldown per card |

## Integration

- With [Apotheosis](apotheosis.md) installed, weapons in the weapon slot generate gems and materials
- When a [Spawner Controller](spawner_controller.md) is active on the network: if this machine's storage mode is "Input AE", all products are handled by the controller; in "Adjacent containers" mode the controller does not interfere

<RecipeFor id="me_spawner" />
