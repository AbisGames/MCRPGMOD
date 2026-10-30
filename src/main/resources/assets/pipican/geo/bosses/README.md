# Biome boss geometry prototypes

27 distinct articulated Bedrock-style `*.geo.json` starting models, with box UVs
and separate bones. The geometry files remain byte-identical to the initial
`biome-bosses` prototypes; see `boss_manifest.json` for region/identifier metadata.

Morvath now has a native Minecraft renderer, basic idle/walk/head animation,
entity registration and two original draft basalt textures. The manifest scale
was baked into these prototypes; the renderer does not multiply it a second time.
The other 26 bosses remain unregistered prototypes. None is claimed as finished
art or a completed playable fight at this framework checkpoint.

See `docs/BIOME_BOSSES.md` for implemented server features, tests and remaining work.
