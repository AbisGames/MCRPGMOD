# Region-aware RPG topography

`pipican:regional_elevation` is added to the vanilla Overworld
`final_density` field only in `pipican:rpg_overworld`. The map uses
custom 50k region lookup to raise Greyspine, Jade and Whitecrown terrain and
depress ocean regions, with noise-based erosion and a 500-block transition
margin near macroregion edges.

The contribution fades out below Y=32 to preserve caves and vanilla deep
ores. It is intentionally tapered above typical mountain crests and is
seed-independent, matching the deterministic macro-region map.

No full physical or visual regression test has yet verified sensible
terrain/chunk borders or FPS. Existing saves do not retroactively change.
