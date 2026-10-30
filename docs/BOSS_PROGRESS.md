# Regional boss implementation status

Only implemented and verified behavior is marked complete. Geometry for each boss is an existing prototype. Morvath client QA is sampled singleplayer visual inspection.

| Boss | Entity | Geometry | Textures | Animations | Combat | Phase 2 | Summoning | Loot | GameTests | Client QA |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| frostbound_colossus | partial | prototype | partial | partial | partial | partial | planned | planned | partial | pending |
| nhalkorr_leviathan | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| skollvar_winterfang | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| hrothgar_drowned_jarl | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| valthor_stormbringer | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| tengri_champion | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| thalassor_deep_tyrant | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| eldaroth_treant | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| fallen_king | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| durmagon_runic_titan | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| longwei_jade_dragon | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| morgatha_swamp_queen | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| aurelios_sunken_emperor | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| morvath_ashen_king | complete | prototype | complete | complete | complete | complete | complete | complete | passed | sampled |
| lianhua_lotus_serpent | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| akhmar_sand_emperor | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| netharis_river_guardian | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| zahira_crimson_corsair | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| xaltek_emerald_devourer | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| corallith_reef_colossus | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| azhar_golden_behemoth | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| kragmor_canyon_warlord | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| aetherion_sky_guardian | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| sylthera_primordial_bloom | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| nocthar_void_sovereign | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| vermillion_blood_monarch | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| lumynara_abyssal_oracle | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |

Current infrastructure work: summoning resolves an explicitly implemented boss binding; Morvath remains the only binding. Its existing altar and save format are unchanged. An unregistered Frostbound entity now owns the fissure, wheel and alternating lance attacks, selects targets, moves and persists combat/phase state. GameTest exercises its attack and phase reload using a fixture entity type. Registry, rendering, phase transition animation, summoning, rewards, full encounter and client QA remain incomplete.
