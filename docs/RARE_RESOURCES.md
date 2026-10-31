# Rare resource development

Independent additions on `biome-bosses`; the existing 27 regional ore/node registrations
and protected worldgen behavior remain intact. Native decoration tests are opt-in so
focused registry/mining/safety tests run before expensive native generation.

| Resource | Acquisition / actual biome | Y | Required pickaxe |
| --- | --- | --- | --- |
| Infernium | Morvath, deterministic 25% per encounter | boss reward | none |
| Voidsteel | registered material; Nocthar drop pending | planned | none |
| Celestium | registered material; Aetherion drop pending | planned | none |
| Bloodcrystal | registered material; Vermillion drop pending | planned | none |
| Soulstone | Umbral Chasm | -48..-1 | Umbrite |
| Dragonite | End islands | 8..70 | existing Netherite |
| Abyssium | deep-ocean biomes / selected deep regional ocean biomes | -48..0 | Tideglass |
| Runestone | Greyspine Mountains | -32..80 | Runic Iron |
| Astralite | natural Aetheris floating terrain, not authored sanctuary islands | 256..368 | Skycrystal |
| Sunforged | Sunscorch Desert | 16..64 | Sunglass |
| Moonshard | Elderwood | -32..32 | Mooncrystal |
| Primordial | outer End islands beyond radius 1000 | 20..60 | existing Netherite |

Each regional pickaxe uses three existing matching regional materials and two sticks.
No recipe needs its own gated rare resource. Dragonite/Primordial use already reachable
vanilla equipment. Pickaxe durability/repair use the iron tool material baseline.

Small veins (2–4 blocks maximum per decorating chunk), one attempt with a rarity filter
(1/8..1/32 depending on resource). Feature scans only its permitted vertical range for
natural stone/endstone; no writes outside the current chunk. Actual Y/biome/dimension
are checked for every block. Protected site reservations, any native structure-reference
chunk, player provenance, block entities and artificial blocks are skipped. Workers use
only the already-loaded protection cache, never SavedData I/O.

Server block-break cancellation enforces exact pickaxe identity. Tool-conditioned loot
also rejects empty tools/explosion bypasses. Rare deposits do not enter regional renewal.
A dedicated progression creative tab exposes resources/tools/ritual items for builders.

Implemented GameTests: all registrations, native tool/loot tables and cycle-free recipes;
real survival FakePlayer block breaking with wrong/correct pickaxes; positive live feature
placement versus provenance/inventory/artificial-block protection. Optional native test
observes naturally decorated RPG and End chunks, checks actual biome/Y/dimension, small
vein bounds and no deposit leakage into Heartlands. No manual ore placement in that test.

Focused CI run41066796125 at `4d4c6fe`: **31/31 required tests passed**, including
the three new resource tests. Native run41068590614 at `27f951a` also passed **35/35 tests**. All eight resources
were observed in naturally decorated seed-0 RPG/End chunks (not manually placed):
Soulstone 5 eligible chunks, Abyssium 6, Runestone 5, Astralite 4, Sunforged 10,
Moonshard 9, Dragonite 25, Primordial 33. This is functional coverage, not a
statistical distribution claim across seeds. Voidsteel/Celestium/Bloodcrystal are
materials only; no unfinished bosses are registered or claimed to drop them yet.
