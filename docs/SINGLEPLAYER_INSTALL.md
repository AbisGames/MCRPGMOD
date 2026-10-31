# Singleplayer test build

Download the GitHub Actions artifact produced by the Worldgen validation workflow.
Inside it is the compiled pipican mod JAR (not a separately installed Minecraft game).
Install it in a clean Minecraft 26.1.2 NeoForge 26.1.2.124 mods folder with Java 25.
Create a disposable new world with world type Pipican RPG World; do not reuse an existing survival save.

Commands:
/rpgsites
/rpgsites locate sylvaran
/rpgsites locate durnhold
/rpgsites progress

Warning: this is an experimental solo QA build. Do not use for long-term worlds.
Large-city placement can temporarily pause the game while templates are assembled.
All currently generated NBT models are procedural authored kits, not finalized photo-real concept-art replicas.