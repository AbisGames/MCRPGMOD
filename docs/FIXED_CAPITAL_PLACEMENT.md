# Fixed capital castles

Exactly 22 authored capital locations are recorded in `fixed_capital_locations.json`. Each castle MUST be placed at its unique settlement centre and NEVER via random spread, random jigsaw structure sets or repeated chunk noise sampling. The template naming convention is `pipican:capitals/<city_id>_royal_citadel`, with Sylvaran using an independently generated elven-palace NBT. Treat names as intended targets: model files currently exist as downloadable separate artifacts, NOT yet in this branch.

A worldgen implementation must use a once-per-world placed/handled state and terrain-derived Y; large structures should be broken into chunks or jigsaw pieces. Preserve protection zones; do not claim this manifest alone installs castle placement. Twenty-one additional furnished capital templates were generated externally on 2026-10-27 with 160x76x160 geometries and 12 rooms each; their zip package has not yet been imported into GitHub and was not singleplayer-tested.
