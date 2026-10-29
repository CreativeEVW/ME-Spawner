---
navigation:
  parent: index.md
  title: Spawner Controller
  icon: spawner_controller
item_ids:
- mespawner:spawner_controller
---

# Spawner Controller

<BlockImage id="spawner_controller" scale="8" />

**Only one controller per AE network can be active**: with multiple controllers on the same network, the one with the lowest position wins; the others turn off - their texture dims, Jade shows a red "Controller Conflict", and the GUI shows the same message in red.

## Function

Takes over the products of all ME spawners on the network whose storage mode is "Input AE":

| State | Behavior |
|-------|----------|
| Empty filter + no inverter card | No filtering, all products pass through the controller's own output routing |
| Empty filter + inverter card | **Destroy everything** (no production) |
| Filter set + no inverter card (whitelist) | Non-matching products are **destroyed** |
| Inverter card installed (blacklist) | Matching products are **destroyed** (empty filter lets everything through) |
| Destroyed + overflow destruction card | Destroyed amounts become [experience crystals](experience_crystal.md) at 1:1, output by the controller; without the card they vanish |
| Looting cards | All amounts are multiplied by the controller's looting multiplier (product of the top 3) |

Spawners in "Adjacent containers" mode are **not** affected by the controller.

## GUI (storage-bus style)

- Config slots: 9 columns, 2 base rows; a capacity card unlocks all rows (7 rows, 63 slots total)
- Clear button (x): clears all marks
- JEI ghost-dragging supported; the memory card saves/loads the marks (shift-right-click to save, right-click to load)

## Upgrade Slots (6)

| Slot | Card | Max | Effect |
|------|------|-----|--------|
| 1 | Inverter card | 1 | Switches to blacklist mode |
| 2 | Capacity card | 1 | Unlocks all config slots |
| 3 | Overflow destruction card | 1 | Destroyed products become experience crystals |
| 4-6 | Looting card family | 3 (effective) | x2/x6/x9, product of the top 3 |

## Power

Same as the ME spawner: 2M AE private buffer + FE input (1 AE = 2 FE).

<RecipeFor id="spawner_controller" />
