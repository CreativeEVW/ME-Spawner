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

When connected to an ME network, processes a spawn egg's loot table every 10 seconds and inserts the generated drops directly into ME storage.

**High energy consumption.** May not work for special spawn eggs.

## Usage

1. Connect the ME Spawner to your ME network via ME cables
2. Place a spawn egg in the center slot to select the entity type
3. The machine will automatically generate drops from the loot table and store them in the network

## Upgrade Slots (8 slots)

| Slot | Type | Max | Effect |
|------|------|-----|--------|
| 1 | Probability Card | 1 | Resolves the entire loot table, inserting 1 of every possible item |
| 2-4 | Looting Card | 3 | Doubles drop quantity per card (×2, ×4, ×8) |
| 5-8 | Acceleration Card | 4 | Halves the processing interval per card |

<RecipeFor id="me_spawner" />
