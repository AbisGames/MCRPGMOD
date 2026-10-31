# Regional boss design catalog

`assets/pipican/geo/bosses/combat_designs.json` declares all 27 regional designs.
`BossDesigns` validates immutable metadata against the preserved boss manifest/models.
All 27 designs now have functional entity and ability registrations. Their
`client_qa_complete` flags remain false because the current idle/phase-entry render pass
does not verify each boss's attacks, movement, intended-biome textures, or full animation
sequences.

Every entry includes region, health/damage, 5+2+1+1 sigil ingredients, biome-height
restrictions, three distinct mechanics and explicit counterplay/warning/recovery/cooldown,
once-only half-health changes, individual trophy/reward specification, model and ten
animation-state mappings, and an arena that requires individual validation.

All 27 sigils and trophies resolve to registered items, and the altar/loot tests verify
their recipes and encounter reward routes. Existing dungeon ingredients are checked
against the actual item registry. Nocthar/Aetherion/Vermillion use their registered rare
materials at the configured 20%/25%/25% chances.

Examples are behaviorally different: circuit-junction puzzles for Durmagon, stance
counterplay for the Fallen King, current anchors for Nhalkorr, color-ordered spore
rings for Sylthera and paired spatial seams for Nocthar. These mechanics have individual
combat implementations and GameTest coverage; this does not replace client animation QA.

Morvath's registered health/base damage now read the shared design. Ability damage
scales from the native attack-damage attribute; default values stay unchanged. The
native clock/animation bindings remain authoritative and are checked against metadata.

The runtime loader rejects malformed or misleading designs before exposing them: sigil
IDs must be unique, Y ranges and all four ingredient counts valid (9 ingredients total),
ability implementation markers must agree with whether the boss is playable, and the
ten animation slots must be complete and may not claim planned assets are implemented.
Reward counts/XP/chances and trophy registration must also match the functional state.
GameTest validates all 27 regions/models, unique mechanics, real ingredient/reward IDs,
functional bindings and combat/reward behavior. The latest complete server suite passed
72/72 required GameTests on 2026-10-31.
