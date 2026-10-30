# Regional boss design catalog

`assets/pipican/geo/bosses/combat_designs.json` declares all 27 regional designs.
`BossDesigns` validates immutable metadata against the preserved boss manifest/models.
Only Morvath is functional; all client-QA flags are false. Other entries remain planned
and cannot be activated by changing a flag alone: the loader rejects a functional
binding without an implemented entity/ability registration.

Every entry includes region, health/damage, 5+2+1+1 sigil ingredients, biome-height
restrictions, three distinct mechanics and explicit counterplay/warning/recovery/cooldown,
once-only half-health changes, individual trophy/reward specification, model and ten
animation-state mappings, and an arena that requires individual validation.

Planned trophies and sigil IDs are explicitly labeled as planned, not resolved or
registered as working items. Existing dungeon ingredients are checked against the
actual item registry. Nocthar/Aetherion/Vermillion reference their registered rare
materials at 20%/25%/25%; they do not drop them until their encounters are implemented.

Examples are behaviorally different: circuit-junction puzzles for Durmagon, stance
counterplay for the Fallen King, current anchors for Nhalkorr, color-ordered spore
rings for Sylthera and paired spatial seams for Nocthar. They are design specifications,
not renamed Morvath attack handlers. Each needs a separate tested implementation batch.

Morvath's registered health/base damage now read the shared design. Ability damage
scales from the native attack-damage attribute; default values stay unchanged. The
native clock/animation bindings remain authoritative and are checked against metadata.

The runtime loader rejects malformed or misleading designs before exposing them: sigil
IDs must be unique, Y ranges and all four ingredient counts valid (9 ingredients total),
ability implementation markers must agree with whether the boss is playable, and the
ten animation slots must be complete and may not claim planned assets are implemented.
Reward counts/XP/chances and trophy registration must also match the functional state.
GameTest validates all 27 regions/models, unique mechanics, real ingredient/reward IDs,
one functional binding and Morvath timing/default stats. The tightened loader passed
36/36 required server GameTests on 2026-10-30.
