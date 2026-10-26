# Profession services added without finalized classes

The new domain services are modular Java APIs with explanatory comments.
They do **not** register in-game NPC entities or game screens.

- `ProfessionService`: blacksmith equipment forging/upgrades, cook food and alchemist potion services. Requires authoritative inventory transaction.
- `MinerSupplyService`: individual miner's ore delivery threshold unlock, snapshot/restore.
- `MerchantRegion`: biome + structure-tag shop catalogue routing, deterministic priority.
- `NpcRoutine`: structure marker schedule and local wandering radius; requires entity navigation bridge.
- `RewardTransaction`: boundary for fully atomic coin/item/recipe operations.
- Class definitions and IDs are not hard-coded in these services.

## Release status
This is a **partial implementation only**. No integration tests or NeoForge runtime compilation have been completed. Multiplayer NPC AI, GUI, renderer, inventory bridge, player attachment, real worldgen binding and trainer UI remain outstanding. Do not merge as a completed playable feature.
