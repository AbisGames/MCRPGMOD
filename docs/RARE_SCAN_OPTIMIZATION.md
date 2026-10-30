# Rare deposit scan optimization

The anchor scan reuses a mutable block position and checks natural host stone
before consulting biome and player-protection data. Air and other non-host blocks
no longer incur those lookups or allocate a new position for every height. Scan
order, random draws, host predicates and placement rules are unchanged.

On seed 0 the native-generation GameTest found all eight deposits with the same
first-hit chunk and block counts as the previous checkpoint. All 36 required
focused plus native-generation tests passed. This is not a benchmark or proof of
identical distributions across all seeds.
