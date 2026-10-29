---
navigation:
  parent: index.md
  title: Apotheosis
  icon: me_spawner
item_ids:
- mespawner:me_spawner
---

# Apotheosis Integration

When Apotheosis is installed, the ME Spawner's weapon slot accepts weapons: the machine simulates the player killing the mob with that weapon, and additionally generates gems and materials based on the weapon's rarity.

- Gems: 74% at the weapon's level, 24% at +1 level, 2% at +2 levels (overflow gives two gems)
- Materials follow the same rarity roll (overflow gives two godforged pearls)
- The probability card never produces Apotheosis gems; stale gem entries in learned drops are purged automatically
