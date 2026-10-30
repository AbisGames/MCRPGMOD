# Blacksmith upgrade backend

`BlacksmithUpgrades.execute(ServerPlayer, Request)` is a server-only inventory API.
No NPC entity, screen, class-system file or custom network handler is introduced.
The later NPC interface must derive the player from its authenticated server context,
quote server costs, and submit the slot, full expected item snapshot, expected revision,
a fresh request UUID and the selected active profile.

Profiles live in `data/pipican/blacksmith_profiles.json`. Ashlands is the first active
profile: Cinderite / Ashen King's Heart / Infernium. Unfinished boss materials are not
used in active upgrade profiles. More profiles use the same backend once their rewards
and equipment progression are playable.

Supports vanilla/modded armor with an armor attribute, weapons and tools. The first
upgrade binds the equipment to its profile; it cannot switch regions afterward.
Non-equipment, stacked equipment, unknown profiles, stale snapshots/revisions,
insufficient materials and capped requests are rejected without changing gear/materials.

Each tier has +1..+10: Regional, Elite, Legendary, Mythic. At +10 an evolution consumes
materials and advances tier at +0; total progression points are retained, so evolution
does not weaken equipment. The item ID/appearance stays intact. Set-group metadata
is stored for later set-bonus integration; this backend does not invent class passives
or implement an NPC interface/set effect.

Costs use existing main-inventory materials, excluding the upgraded slot:
- Upgrade to n: (2+n) regional materials times 1/2/4/8 by tier.
- From +6: 1/2/3/4 boss hearts by tier.
- From +8: 1/2/4/8 Infernium by tier.
- Evolution: 32 regional +2 boss +2 rare, times the current tier multiplier.

Armor bonus: 0.05 per total progression point, up to +2 armor per piece.
Weapon/tool attack bonus: 0.2 per point, up to +8. Existing native/class modifiers
remain, and only the unique `pipican:blacksmith/*` modifier is replaced each time.
The native equipment slot group is retained. These are initial balance numbers.

Transactions run only on the owning server thread. Inventory material plans and output
are prepared before mutation. Runtime failure restores involved snapshots. All unrelated
data components, enchantments, custom names, durability, trims and contributor custom
data remain on the copy. Request nonces block duplicate handling; monotonic item revisions
persist through evolution/reload so old requests cannot become valid when a level resets.

GameTests cover exact material spending, repeat/stale requests, insufficient ingredients,
invalid items, early/maximum evolution, all forty upgrade levels and three evolutions,
metadata/set groups and preservation of enchantments/name/damage/custom data/other modifiers.
Compilation passes; runtime results are recorded after CI.
