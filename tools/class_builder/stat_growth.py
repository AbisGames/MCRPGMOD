"""Suggests each class's stat growth (spec section 2, "Growth" column) from its strengths and weaknesses.

Every class gets the same budget: the eight growths always average 1, so no class gets stronger overall - only
what it is good at grows faster and what it is bad at grows slower. Signals, per stat:
  - starting stats (the designer's intent; the main signal),
  - what the passives and special passives talk about ("+15% magic damage" is an Intelligence strength),
  - the weakness text ("-10% max health" is a Vitality weakness),
  - base health and heavy-armor affinities (Vitality / Endurance).
The stats are ranked and given the tiers in TIERS; a class whose signals are flat (a jack of all trades) gets tiers
pulled toward 1.

    python3 tools/class_builder/stat_growth.py class_specs           # print the table
    python3 tools/class_builder/stat_growth.py class_specs --write   # also write the Growth column into the specs
"""
import os
import re
import statistics
import sys

sys.path.insert(0, os.path.dirname(__file__))
import parse_specs  # noqa: E402

CORE = ['vitality', 'strength', 'dexterity', 'agility', 'intelligence', 'wisdom', 'endurance', 'luck']
TIERS = [1.5, 1.25, 1.25, 1.0, 1.0, 0.75, 0.75, 0.5]  # averages exactly 1

# What a line talks about -> which stat it is a strength (in passives) or weakness (in the weakness) of.
KEYWORDS = {
    'vitality': r'max health|\bhearts?\b|armou?r\b|damage taken|take(?:s)? (?:\d+% )?less damage|toughness',
    'strength': r'melee|attack damage|\bswords?\b|\baxes?\b|\bmaces?\b|unarmed|knockback|heavy hits?|charged hits?|cleave|sweep',
    'dexterity': r'attack speed|\bbows?\b|crossbows?|arrows?|ranged|projectile|draw|reload|firearms?|off-hand|parry',
    'agility': r'movement speed|\bspeed\b|sprint|dodge|jump|fall damage|stealth|sneak|evade|climb|glide',
    'intelligence': r'magic damage|\bspells?\b|enchant|arcane|explosion|fire damage|lightning|mana|aether',
    'wisdom': r'heal(?:s|ing)?\b|regeneration and instant health|aura|song|chant|prayer|holy|undead|curse|potion',
    'endurance': r'regen|hunger|food|natural regeneration|resist|immune|poison|drown|freez|burn',
    'luck': r'\bcrit|critical|chance|loot|luck|drops?\b|rare|fortune',
}


def _hits(text, stat):
    return len(re.findall(KEYWORDS[stat], text.lower()))


def growth(c):
    """-> ({stat: growth}, {stat: score}, why) for one parsed class spec."""
    base = [c['stats'].get(s, 0) for s in CORE]
    total = sum(base) or 1
    good = ' '.join(c.get('passives', []) + c.get('special_passives', []) + [c['special'].get('per_point') or ''])
    bad = (c.get('weakness') or {}).get('does') or ''
    heavy = sum(1 for k, v in (c.get('affinity') or {}).items() if 'heavy' in k and v in ('proficient', 'mastery'))
    # The class's two biggest starting stats are what it is built around: a weakness about them ("-10% magic damage
    # without your book") is a condition on the class's core, not a weak stat.
    core = sorted(CORE, key=lambda s: -base[CORE.index(s)])[:2]
    score = {}
    for i, s in enumerate(CORE):
        v = base[i] / total * 10                       # share of the starting points, 0-10 (the main signal)
        v += 0.4 * min(2, _hits(good, s))              # passives lean on it
        if s not in core:
            v -= 1.0 * min(1, _hits(bad, s))           # the weakness hits it
        if s == 'vitality':
            v += (c["health"] - 26) / 12               # 20 health -0.5, 34 health +0.67
        if s in ('vitality', 'endurance'):
            v += 0.2 * heavy
        score[s] = v
    order = sorted(CORE, key=lambda s: (-score[s], -base[CORE.index(s)], CORE.index(s)))
    # Flat profiles get flatter tiers: spread 1 when the scores vary a lot, less when they don't.
    vals = list(score.values())
    spread = min(1.0, statistics.pstdev(vals) / 2.0)
    out = {}
    for rank, s in enumerate(order):
        out[s] = round(round((1 + (TIERS[rank] - 1) * spread) * 20) / 20, 2)
    return out, score, order


def main(argv):
    d = argv[1] if len(argv) > 1 else 'class_specs'
    write = '--write' in argv
    rows = []
    for name in sorted(os.listdir(d)):
        if not name.endswith('.md') or name.startswith('_') or name in ('README.md', 'CLASSES.md', 'BUILD_REPORT.md', 'STAT_GROWTH.md'):
            continue
        path = os.path.join(d, name)
        c = parse_specs.parse(path)
        g, score, order = growth(c)
        rows.append((c, g, order))
        if write:
            _write(path, g)
    for c, g, order in rows:
        print(f"{c['id']:20} " + ' '.join(f'{g[s]:.2f}' for s in CORE) + f"   best {order[0]}, worst {order[-1]}")
    if write:
        _report(d, rows)


def _write(path, g):
    """Sets the Growth column of the spec's section 2 table (adds the column if missing)."""
    text = open(path, encoding='utf-8').read()
    names = {s.capitalize(): s for s in CORE}

    def row(m):
        label, points = m.group(1).strip(), m.group(2).strip()
        s = names.get(label)
        if s is None:
            return m.group(0)
        return f"| {m.group(1)}| {points} | x{g[s]:g} |"
    text = re.sub(r'^\| ((?:Vitality|Strength|Dexterity|Agility|Intelligence|Wisdom|Endurance|Luck)\s*)\| *(-?\d+) *\|(?: *x?[\d.]+ *\|)?$',
                  row, text, flags=re.M)
    text = re.sub(r'^\| Stat(\s*)\| Points \|(?: Growth \|)?$', r'| Stat\1| Points | Growth |', text, flags=re.M)
    text = re.sub(r'^\|-+\|-+\|(?:-+\|)?$', '|--------------|--------|--------|', text, count=1, flags=re.M)
    text = re.sub(r'^\| (Special stat \(section 3\)) \| *(-?\d+) *\|(?: *x?[\d.]+ *\|)?$', r'| \1 | \2 | |', text, flags=re.M)
    open(path, 'w', encoding='utf-8').write(text)


def _report(d, rows):
    lines = ['# Stat growth by class', '',
             'Generated by `tools/class_builder/stat_growth.py` from each class\'s starting stats, passives, weakness, base',
             'health and armor. Each class\'s eight growths average 1: no class gets stronger overall, it just gets more out',
             'of what it is good at and less out of what it is bad at. To change one, edit the Growth column in that',
             'class\'s section 2 (the generator only rewrites it when run with `--write`).', '',
             '| Class | ' + ' | '.join(s.capitalize()[:4] for s in CORE) + ' | Best | Worst |',
             '|---|' + '---|' * (len(CORE) + 2)]
    for c, g, order in rows:
        cells = []
        for s in CORE:
            v = g[s]
            cells.append(f'**{v:g}**' if v >= 1.25 else (f'_{v:g}_' if v <= 0.75 else f'{v:g}'))
        lines.append(f"| {c['name']} | " + ' | '.join(cells) + f' | {order[0].capitalize()} | {order[-1].capitalize()} |')
    open(os.path.join(d, 'STAT_GROWTH.md'), 'w', encoding='utf-8').write('\n'.join(lines) + '\n')


if __name__ == '__main__':
    main(sys.argv)
