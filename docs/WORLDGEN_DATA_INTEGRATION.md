# Author data integration

`SubregionSampler` consumes the exact 120 weighted source definitions; its
850-block jittered Voronoi cells avoid tiny 3–10-block biome patches produced by
blindly scaling the original 2,000–6,000-block cell sizes.

`LandmarkCatalog` loads 70 authored settlements and retains full structure
radii. Four spawn-cluster coordinates are already near zero in the original
source and should **not** be multiplied by 1/600: Crossway, Saltreach,
Millbrook and Stonegate. The authored Millbrook site would overlap Aldermere
after continental scaling; it is remapped to (-4100, -2900) within Heartlands.

`WorldgenJson` loads original records packaged with the mod, rather than
duplicating hundreds of authored subregion definitions in Java.

**Release gate:** These classes still require worldgen biome placement, terrain
noise registration, actual structure files and testing. They do not generate a
world by themselves.
