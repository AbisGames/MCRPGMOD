# Pipican

An RPG mod for **Minecraft 26.1.2** on **NeoForge** (mod id `pipican`, package `com.pipican`).

## Features so far

- **Data-driven class system** – every class is one JSON file (stats, special stat + resource bar, starting kit,
  skill tree, special skill tree, passives, special passives, base health, weakness, equipment affinities,
  trinket slots). No Java needed to add a class.
- **Stats** of any number, bought with vanilla XP; **class levels** from separate class XP.
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
