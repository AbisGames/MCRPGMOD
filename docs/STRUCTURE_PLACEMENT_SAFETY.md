# Structure placement safety gate (incremental)

The 22 capital coordinates and all castle templates are unchanged. The capital
placement service now rejects a tile if its bounding box exceeds vertical world
limits, sampled surface heights differ by more than 12 blocks from the shared
foundation, or sampled contents contain non-natural blocks/block entities. It
clears natural overburden inside an accepted tile before placing the NBT.

## Important limitations / blockers
- This is a conservative first-stage guard, **not** collision-proof generation.
  Sampling in the footprint does not guarantee all occupied blocks are checked.
- No chunk-generation-stage reservation or cancellation of vanilla structures
  is implemented. Existing vanilla structures may still appear inside capital
  sites, and conflicts may prevent the castle from completing.
- Only one piece is checked at a time; full 63-piece capital complexes need
  a complete union footprint and protected-volume plan before construction.
- Rejecting highly uneven plots may leave some capitals unbuilt. Terrain must
  be prepared in the noise/surface generation phase, *before* the city service
  starts, rather than forcing placement or flattening uncontrolled areas.
- No gameplay test of castle interiors, slope transitions, foundations, or
  other natural blocks has been performed. Clearing terrain before a failed
  template placement does not currently roll back the world.
- Future village, mine, and dungeon placement must share a spatial reservation
  index and use three-dimensional envelopes. Underground structures require
  deliberate excavation but protected entrances and cave intersections.

## Release criteria
1. Reserve exact per-capital volumes at chunk-generation time; exclude vanilla
   structure starts and mod-generated candidates in these areas.
2. Fit natural terrain to approved palace/city footprint using smoothly blended
   height fields and an authored foundation level.
3. Check a complete structural occupancy mask before changing any blocks;
   make placement atomic or support rollback.
4. Place and inspect all 22 complete 63-piece capitals across multiple seeds,
   reloads, and chunk request orders; verify air-filled rooms and entrances.
5. Verify the safety gate does not destroy player modifications or pre-existing
   structures and that deferred tiles report actionable diagnostics.
