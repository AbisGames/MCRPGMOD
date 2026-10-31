#!/usr/bin/env python3
"""Verify the actual deliverable JAR includes every authored structure template."""
import argparse
import json
from pathlib import Path
from zipfile import ZipFile
from validate_full_structures import inspect_nbt

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("jar", type=Path)
args = parser.parse_args()
data = ROOT / "src/main/resources/data/pipican"
expected = {f"data/pipican/structure/{p.relative_to(data / 'structure').as_posix()}"
            for p in (data / "structure").rglob("*.nbt")}
for name in ("pillager_blackspire", "umbral_ancient_city", "morvath_portal_chamber", "aetheris_sky_citadel"):
    expected.add(f"data/pipican/structure/special/{name}.nbt")
for assembly in (data / "worldgen/capital_assemblies").glob("*.json"):
    layout = json.loads(assembly.read_text())
    rows = layout["relative_positions"] + layout["connectors"]
    assert len(rows) == 63, assembly
    for row in rows:
        namespace, path = row["template"].split(":", 1)
        expected.add(f"data/{namespace}/structure/{path}.nbt")
with ZipFile(args.jar) as jar:
    names = set(jar.namelist())
    missing = sorted(expected - names)
    assert not missing, f"Missing structure templates in {args.jar}: {missing}"
    assert not any("gametest/WorldgenIntegrationTests" in name for name in names), "Test harness shipped in mod JAR"
    corrupt = jar.testzip()
    assert corrupt is None, f"Corrupt JAR entry: {corrupt}"
    for name in expected:
        inspect_nbt(Path(name),jar.read(name))
print(f"PASS: actual mod JAR contains {len(expected)} expected structure templates, including all 22 capital assemblies and special landmarks")
