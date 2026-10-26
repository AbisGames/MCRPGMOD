# Quest, NPC, trainer and economy architecture

This branch builds on the latest `main` source tree. All authored Java sources include explanatory comments.

## What is implemented
- Validated quest, NPC and trainer definitions with fixed one-quest rule for ordinary quest NPCs.
- Multiple-stage trainer questlines; during training the ClassHost interface demands classless play, disabled class powers and mastery, then restores the previous class. Permanent class unlock is awarded only at the end.
- Two dojo class slots, with quick swap logic; equipment of the slots is dojo-only.
- Per-player quest objectives, completion, coins, unlocked recipes/classes and training progress.
- Generic objective events: kill, deliver, visit, talk, target hits and clear structure.
- Vendor listings with prices/stock, recipe catalog types for cooking, brewing, forging and upgrades.
- NPC definitions may reference profession, structure tag, behavior profile and shop.
- Regular quests can have class restrictions; training quest progression is classless.
- No class mastery or skill points granted by quest rewards.

## IMPORTANT: not yet a finished playable NeoForge feature
These are Java *domain* services. Missing are actual NPC entities, models/animations,
AI/pathfinding, structure marker placement, biome-based shop selection, datapack codecs,
NeoForge saved data/attachments and packets, GUI screens, exact class-system adapter
(`ClassService`), event listeners, inventory transactions, and in-game testing.
The actual generated structures will be authored in a separate future branch.
No NPCs/shops spawn in-game just because the definitions exist.

Do not expose `QuestService.progress` or merchant methods to unverified client packets.
Never let the client claim its own kill, inventory delivery, structure visit, reward,
trainer completion, dojo access, or currency balance. Those are server-authoritative.
A shop purchase is not complete until balance, stock, inventory transfer and
persistence are guaranteed to succeed together; otherwise rollback.

## Critical existing code alignment
Current main contains an *older* first-login class selection and vanilla XP stat
purchase path. The class team must replace first-login selection with trainers, add
persistent unlocked classes and two slot APIs, and honor the training suppression
flag in `ClassEffectApplier`, `MasteryService` and server skill execution.
Never delete class progression or skill mastery while entering training.

The stat system under `com.pipican.stats` is not yet wired to NeoForge.
Trainer work should not silently treat stat XP or class XP as skill mastery.

## Recommended next integration steps
1. Implement a player attachment Codec, login/death sync and transactional reward handling.
2. Implement NPC types/renderer, daily schedules, structure-local navigation and spawn markers.
3. Register datapack codecs for quests, trainer questlines, shops, recipes and NPC profiles.
4. Bridge trainer suppression and dojo changes to class manager; disable first-join class dialog.
5. Implement UI for NPC dialogue, quest journal, shop and cooking/brewing books.
6. Add server-side quest event attribution and structure lookup for worldgen cave clearing.
7. Add GameTests for reconnect, duplicate rewards, training cancel, class swap, item and coin duplication.
