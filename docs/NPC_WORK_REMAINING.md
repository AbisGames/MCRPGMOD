# Integration blockers / release gate

The quest and NPC system is **not yet fully playable**. For the user-requested definition of done, these are mandatory:

- NPC entity registration, AI navigation, trade interactions, rendering, texture UV mapping and in-world spawn rules
- Dialogue and quest journal screens
- NeoForge player attachment codec for accepted/completed quests, money, recipes, class unlocks and training state; login/death sync
- Verified objectives tied to actual game events and real-world structure instances
- Server transaction management for money + item transfer, inventory validation, duplication protection and mining shop progression
- Data codecs and datapack loaders for quest/NPC/trainer/store/recipes; biome-scoped merchants
- Special workstation, brewing/cooking recipe books, blacksmith upgrade service
- Class suppression integration in class effect applier / mastery / skills during training, dojo slot/keybind networking
- Minecraft 26.1.2 / Java 25 Gradle compilation and dedicated server + client GameTests
- Worldgen structure/NPC location integration once authored separately

There is no basis to claim this branch is merge-ready as a playable release before the checklist passes.
