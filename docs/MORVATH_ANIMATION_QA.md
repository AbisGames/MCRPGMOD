# Morvath animation and model QA — 2026-10-30

Minecraft 26.1.2 / NeoForge 26.1.2.124, native renderer, AMD Radeon OpenGL 3.3.
Only the approved isolated Superflat world in `run-client-boss` was used.

## Completed corrections

- Added a centered grip, pommel, crossguard and stepped tapered blade; adjusted
  the wrist attachment and resting pitch. The grip stays inside the gauntlet.
- Separated the overlapping hip cubes and torso/leg front planes at runtime,
  keeping the earlier cuff/boot shell separation. All 27 original geometry files
  remain byte-identical after Git newline normalization.
- Reduced the armed-hand walking swing, clamped head tracking and added subtle
  breathing. The inspected standing and walking blade poses clear the flat floor.
- Smoothed windup/recovery and partial-tick presentation. Basic/cleave downswing
  reaches its strike pose at the server windup boundary; no combat timing changed.
- Distinguished meteor's casting hand from its sword hand and eruption's raised
  preparation from its downward release. Both return toward the resting pose.
- Corrected reversed arm roll in phase change, meteor and death. Phase change
  now opens the arms instead of intersecting the hands inside the chest.
- Kept the animated death model above its nominal floor plane by measuring the
  posed cube corners after vanilla's sideways roll. The sword no longer vanishes
  underground on the test platform. This is visual placement, not a hitbox change.
- Expanded frustum culling bounds for raised weapons/crests and the fallen body.

## Native-client inspection

The test-only render-state modifier feeds the actual production model and layers.
Held frames permit comparison; normal-speed loops check movement and return poses.
It does not replace the renderer with a separate mock model or alter server damage.

| Pose | Inspected result |
| --- | --- |
| Idle | Front/side full-body silhouette, breathing, eyes and stable armor surfaces |
| Walk | Opposed leg swing, restrained sword hand, flat-floor clearance |
| Basic | Raised preparation and lowered strike/recovery, sword remains attached |
| Cleave | Side/back preparation, lateral sword sweep and return toward rest |
| Meteor Rain | Side/front casting arm, separate weapon silhouette and recovery |
| Volcanic Eruption | Front/side preparation, downward release and recovery |
| Summoning | Real altar spawn and preview: raised arms/head settle toward rest |
| Hurt | Red damage overlay, brief head/body recoil and neutral return |
| Phase transition | Brighter phase-two sheet, arms open clear of chest, recover |
| Death | Front/side roll, visible body/weapon above flat floor, real cleanup afterward |

External evidence is under `outputs/morvath-animation-qa/final-*.png`; the final death
captures include the floor correction. Earlier `before-*`/non-final captures document
the intermediate defects and are not the final acceptance images.

The actual altar still consumed one sigil (4 to 3). Real AI was resumed after preview;
the real death control removed the boss/bar and produced the normal loot/XP flow.
The world was saved and quit. Build, package verification and all 35 required server
GameTests passed after the production changes. No new registry IDs were introduced.

This is sampled visual QA in one integrated singleplayer client with visual immunity,
not a claim of every frame/terrain angle, measured FPS, a balanced survival kill or a
real multiplayer session. Death clearance uses the entity's nominal floor plane;
nearby raised terrain can still occlude any large model normally.
