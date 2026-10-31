# Pipican

An RPG mod for **Minecraft 26.1.2** on **NeoForge** (mod id `pipican`, package `com.pipican`).

## Features so far

- **Data-driven class system** – every class is one JSON file (stats, special stat + resource bar, starting kit,
  skill tree, special skill tree, passives, special passives, base health, weakness, equipment affinities,
  trinket slots). No Java needed to add a class.
- **Path of Exile-style passive trees**: one shared tree with per-class start points and a class-only special tree,
  split across files, with a pan/zoom/search screen.
- **Dark Souls-style level-up screen**: stat points are spent freely in any stat; each class scales differently
  (S-E grades from its `stat_growth`), with a before/after preview of what the points change.
- **Skill mastery**: skills (weapons, movement, blocking, later abilities) level up by use through 5 tiers; each tier
  gives a bonus and a passive-tree point.
- **Stats** of any number (eight built in); kill XP gives both **class levels** and **stat points**.
- **84 classes** (4 novice, 16 expert, 64 master) built from the plain-language specs in `class_specs/`;
  see `class_specs/BUILD_REPORT.md` for what isn't in the game yet.
- **Equipment affinities** (mastery → forbidden) for every weapon, armor piece and trinket.
- **Per-class trinket slots** (rings, amulet, shoulderpad, ...).

See **[docs/CLASS_SYSTEM.md](docs/CLASS_SYSTEM.md)** for how it all works, and
[docs/templates/pipican_template_pack](docs/templates/pipican_template_pack) for a copy-paste class template.

## Development

Requires Java 25 for the game (Gradle downloads it automatically).

```
./gradlew build                 # build the mod jar
./gradlew runClient             # dev client
./gradlew runGameTestServer     # run the automated GameTests
```

This is a collaborative repository: work on a feature branch and open a pull request.
