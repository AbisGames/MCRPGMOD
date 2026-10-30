# Regional boss implementation status

Only implemented and verified behavior is marked complete. Geometry for each boss is an existing prototype. Morvath client QA is sampled singleplayer visual inspection.

| Boss | Entity | Geometry | Textures | Animations | Combat | Phase 2 | Summoning | Loot | GameTests | Client QA |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| frostbound_colossus | complete | prototype | partial | partial | complete | complete | complete | complete | passed | pending |
| nhalkorr_leviathan | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| skollvar_winterfang | partial | prototype | partial | partial | partial | partial | complete | complete | partial | pending |
| hrothgar_drowned_jarl | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| valthor_stormbringer | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| tengri_champion | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| thalassor_deep_tyrant | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| eldaroth_treant | implemented | prototype | implemented | implemented | implemented | implemented | implemented | implemented | passed | pending |
| fallen_king | implemented | prototype | implemented | implemented | implemented | implemented | implemented | implemented | passed | pending |
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
| kragmor_canyon_warlord | implemented | prototype | implemented | implemented | implemented | implemented | implemented | implemented | passed | pending |
| aetherion_sky_guardian | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| sylthera_primordial_bloom | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| nocthar_void_sovereign | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| vermillion_blood_monarch | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |
| lumynara_abyssal_oracle | planned | prototype | planned | planned | planned | planned | planned | planned | planned | pending |

Frostbound now has a registered entity, unique attributes, independent attacks and phase persistence, craftable sigil, Whitecrown biome gate, transactional altar spawn, boss bar, once-only trophy/Frostcrystal rewards and real server GameTest coverage for attack warning, reload, recipe, summon and drops. Texture/animation states compile against the preserved geometry, but in-client clipping/animation QA remains pending; therefore its visual columns remain partial.

Sköllvar has a separate combat controller for locked scent charges, alternating marked pack pounces and attacks on the last observed footprint. GameTests verify charge avoidance, pounce markers, footprint counterplay and the phase-two third-charge feint.

Update: Sköllvar now has a registered wolf entity, its own attributes and movement, persisted phase/summoning state, client renderer, two phase textures, a survival sigil recipe, Pinewall biome altar route and one-time Wintersteel/trophy rewards. Actual altar summoning and loot pass GameTest. In-entity AI/save-reload and client visual QA still need focused coverage.

Eldaroth now has a registered treant entity, native pose renderer and original geometry preserved, a bark/heartwood atlas, three independent attacks (delayed root lattice with open refuge, fixed seed-pod landings, rotating branch sweep with inner refuge), server damage, saved combat timing and phase-two double-bloom behavior. Its Mooncrystal + Elderwood core + ritual core + oak sapling sigil recipe is registered; the Elderwood altar route and one-time trophy/material rewards are covered by the server encounter test. The current full server run passes all 35 required GameTests, including Eldaroth warning/counterplay/damage, phase reload, exact recipe, biome-bound ritual summon and rewards. Client appearance/clipping remains unverified.

The Fallen King now has a registered king entity with its native sword poses, separate phase textures and the original model geometry retained. Its attacks resolve a locked royal thrust followed by a warned phase-two feint/riposte, heralded parallel phantom-cavalry lanes with a sidestep gap, and three marked regalia sectors plus one safe approach sector. The recipe uses Royal Silver, Heartlands core, ritual core and golden apple; the Heartlands altar encounter and unique trophy/material drop are verified. All 35 server GameTests pass, including fixed aim, warning windows, exposed-sector counterplay and phase-two riposte. Client clipping and animation timing still need in-game QA.

Kragmor is wired as a registered canyon warlord with preserved geometry, native rockfall/tether/leap poses and separate base/awakened textures. Its three arena-safe attacks use locked sequential rock landing shadows, a temporary tether web, then a landing circle followed by a separately warned narrow shock line. Positions and pending timers persist through save/reload; phase two shifts rock lanes. The Redrock Iron + Badlands core + ritual core + pointed dripstone recipe, protected Badlands altar route and unique rewards pass server GameTests. All 35 required tests pass. Client QA remains pending.
