"""Gear sprites (docs/PIXEL_ART.md), built so their shapes stay clean:

* Weapons and tools are complete hand-drawn FILL grids per style (blade, guard and grip designed together), on
  vanilla's layout: corner to corner on the x+y=15 diagonal, tip top right, pommel bottom left. Outlines are added
  afterwards (two-tone, like vanilla). Shading runs along straight 45-degree lines.
* Armor icons are SHAPE MASKS: only the left half is drawn and mirrored, so every piece is symmetric; shading is
  computed per part as a lit volume (ore_art.shade_mask), so nothing is shaded by hand.

Fill legend: 1-5 ore metal (dark -> light), a-d trim, w x y grip/haft, g G glow.
Mask legend: letters are parts - each connected run of one letter is shaded as one volume.
  M metal (ore)    T trim    o dark interior    R ridge (light; mirrors to r, dark)    v rivet    G gem
"""

E = '................'


def _pad(rows):
    rows = list(rows) + [E] * (16 - len(rows))
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    return rows


# ================================================================ swords: blade + guard + grip, per style
SWORD = {
    # Longsword: a straight 3 px blade with a fuller, a square-capped crossguard.
    'knight': _pad([
        E,
        '.............54.',
        '............542.',
        '...........542..',
        '..........542...',
        '.........532....',
        '..dc....532.....',
        '.cdc...532......',
        '..cbc.532.......',
        '...bbc32........',
        '....bbc.........',
        '....yxbc........',
        '...yx.bab.......',
        '..yx...aa.......',
        '.cb.............',
        '.ba.............',
    ]),
    # Estoc: a long narrow blade, a short guard.
    'slim': _pad([
        E,
        '..............4.',
        '.............53.',
        '............53..',
        '...........53...',
        '..........53....',
        '.........53.....',
        '........53......',
        '.......53.......',
        '...dc.53........',
        '....bc3.........',
        '....ybc.........',
        '...yx.ba........',
        '..yx............',
        '.cb.............',
        '.ba.............',
    ]),
    # Greatsword: a broad ridged blade and a long crossguard.
    'brutal': _pad([
        '............455.',
        '...........45532',
        '..........45532.',
        '.........45532..',
        '........45532...',
        '.......45532....',
        '.dc...45532.....',
        '..cc.45532......',
        '...cc5532.......',
        '....bc32........',
        '....ybc.........',
        '...yx.bc........',
        '..yx...bb.......',
        '.dcb....aa......',
        '.cba............',
        '..a.............',
    ]),
    # Falchion: the edge bellies out toward a clipped tip; the guard sweeps down.
    'cruel': _pad([
        E,
        '.............55.',
        '...........4542.',
        '..........4542..',
        '.........4542...',
        '........4542....',
        '........542.....',
        '..c....542......',
        '..dc..542.......',
        '...cc532........',
        '....bc2.........',
        '....ybc.........',
        '...yx.bb........',
        '..yx...ba.......',
        '.cb....a........',
        '.ba.............',
    ]),
    # Runeblade: a glowing rune channel and a ring guard holding a gem.
    'arcane': _pad([
        E,
        '.............54.',
        '............5G2.',
        '...........5g2..',
        '..........5G2...',
        '.........5g2....',
        '........5G2.....',
        '..dc...5g2......',
        '..cdc.532.......',
        '...cGGc2........',
        '...bgGbc........',
        '....ybbc........',
        '...yx..ba.......',
        '..yx............',
        '.cb.............',
        '.ba.............',
    ]),
}


# ================================================================ hafted gear: a 2 px haft on the diagonal
def haft(top_row):
    rows = [['.'] * 16 for _ in range(16)]
    for r in range(top_row, 15):
        x = 15 - r
        rows[r][x - 1], rows[r][x] = 'y', 'x'
    rows[14][0], rows[14][1] = 'c', 'b'
    rows[15][0], rows[15][1] = 'b', 'a'
    return [''.join(r) for r in rows]


AXE = {
    # Bearded axe: a tall bit hanging below the eye, the edge facing up-left.
    'knight': (4, _pad([
        E,
        '......55........',
        '.....5543.......',
        '.....554332.....',
        '.....5543321....',
        '.....5432.......',
        '.....543........',
        '......52........',
    ])),
    # Hatchet: a small quick head.
    'slim': (5, _pad([
        E,
        E,
        '........55......',
        '.......55432....',
        '.......54321....',
        '........52......',
    ])),
    # Labrys: a double bit.
    'brutal': (4, _pad([
        '.....55.........',
        '....5543........',
        '....55432...4...',
        '....554332.443..',
        '....5543321432..',
        '.....5432..321..',
        '.....543....21..',
        '......52........',
    ])),
    # Crescent axe: a thin moon of steel with a hooked beard.
    'cruel': (4, _pad([
        '.....55.........',
        '....554.........',
        '....54..........',
        '....54...32.....',
        '....5433321.....',
        '....54..........',
        '....543.........',
        '.....543........',
        '......43........',
    ])),
    # Moon axe with a glowing rune.
    'arcane': (4, _pad([
        '.....55.........',
        '....554.........',
        '....5G..........',
        '....5g...32.....',
        '....5gG3321.....',
        '....5g..........',
        '....543.........',
        '.....53.........',
    ])),
}

SPEAR = {
    # Leaf blade over a socket ring.
    'knight': (6, _pad([
        '..............4.',
        '............4553',
        '...........45532',
        '..........45532.',
        '..........4532..',
        '.........dc22...',
        '.........cb.....',
    ])),
    # Needle point.
    'slim': (6, _pad([
        '...............4',
        '..............53',
        '.............53.',
        '............53..',
        '...........53...',
        '..........dc....',
        '.........cb.....',
    ])),
    # Boar spear: a broad head and a crossbar.
    'brutal': (7, _pad([
        '...........455..',
        '..........45554.',
        '..........455432',
        '...........45321',
        '..........4c432.',
        '........dcbc21..',
        '........cb.b....',
        '...........a....',
    ])),
    # Barbed head: two hooks swept back.
    'cruel': (6, _pad([
        '..............4.',
        '.............453',
        '............4532',
        '..........44532.',
        '.........4.432..',
        '.........4c2.3..',
        '.........cb..2..',
    ])),
    # Ritual spear: a glowing core in the head.
    'arcane': (6, _pad([
        '..............4.',
        '............4553',
        '...........45G32',
        '..........45g32.',
        '..........4532..',
        '.........dG22...',
        '.........cb.....',
    ])),
}

TOOL = {
    'pickaxe': (5, _pad([
        E,
        '.....4555c......',
        '....45555dc4....',
        '...4543..cb554..',
        '..432...cb..453.',
        '..32.........43.',
        '..2...........3.',
        '..............2.',
    ])),
    'shovel': (6, _pad([
        '...........455..',
        '..........45554.',
        '.........455543.',
        '.........455432.',
        '..........44321.',
        '.........dc22...',
        '.........cb.....',
    ])),
    'hoe': (4, _pad([
        E,
        '.....4555dc.....',
        '....455543cb....',
        '....432..cb.....',
        '....32..........',
    ])),
}
# Where higher-tier gear sets a gem.
GEM_AT = {'pickaxe': (10, 2), 'shovel': (10, 5), 'hoe': (10, 1)}


# ================================================================ armor icons as half masks (cols 0-7, mirrored)
def mirror(half):
    swap = {'R': 'r', 'r': 'R'}
    return [h + ''.join(swap.get(c, c) for c in reversed(h)) for h in half]


HELM = {
    # Hood: a peaked cowl over a dark face, falling onto the shoulders.
    'light': ['........', '........', '.......M', '......MM', '.....MMM', '....MMMM', '...MMMoo', '..MMMooo',
              '..MMoooo', '..MMoooo', '..MMMooo', '..MMMMoo', '.MMMM...', '........', '........', '........'],
    # Sallet: a rounded bowl, a brow band, eye openings either side of a nasal, cheek guards.
    'medium': ['........', '........', '........', '.....MMM', '...MMMMM', '..MMMMMM', '..MMMMMM', '..TTTTTT',
               '..MMoooR', '..MMoooR', '..MMoooR', '..MMoooo', '...M....', '........', '........', '........'],
    # Great helm: a flat top, a visor slit split by a bar, breathing holes, a riveted band.
    'heavy': ['........', '........', '....MMMM', '...MMMMM', '..MMMMMM', '..MMMMMM', '..MMMMMR', '..MooooR',
              '..MMMMMR', '..MMMoMR', '..MMMMMR', '..TTvTTT', '...MMMMM', '........', '........', '........'],
}
CHEST = {
    # Brigandine: riveted plates under cloth, small shoulder caps, a V neck and a belt.
    'light': ['........', '........', '.MMM....', 'MMMMM...', 'MMMMMMoo', '.MMMMMMo', '...MMMMM', '...MvMMM',
              '...MMMMM', '...MMvMM', '...TTTTv', '...MMMMM', '....MMMM', '.....MMM', '........', '........'],
    # Cuirass: round pauldrons, a breastplate with a ridge, a belt.
    'medium': ['........', '........', '.MMM....', 'MMMMM...', 'MMMMMMoo', 'MMMMMMMR', 'TTTTMMMR', '...MMMMR',
               '...MMMMR', '...MMMMR', '...TTTTv', '...MMMMM', '....MMMM', '.....MMM', '........', '........'],
    # Fluted plate: layered pauldrons with trimmed lames, a gorget, a ridged breastplate.
    'heavy': ['........', '.MMM....', 'MMMMM...', 'MMMMMTTT', 'TTTTTMMR', 'MMMMMMMR', 'TTTTMMMR', '...MMMMR',
              '...MMMMR', '...TTTTv', '...MMMMM', '...MMMMM', '....MMMM', '.....MMM', '........', '........'],
}
LEGS = {
    'light': ['........', '........', '...TTTTv', '...MMMMM', '...MMMMM', '...MMMMo', '...MMMM.', '...MMMM.',
              '...MMMM.', '...MMMM.', '...MMMM.', '...MMMM.', '...MMMM.', '........', '........', '........'],
    'medium': ['........', '........', '...TTTTv', '...MMMMM', '...MMMMM', '...MMMMo', '...MMMM.', '...MMMM.',
               '...TTTT.', '...MMMM.', '...MMMM.', '...MMMM.', '...MMMM.', '........', '........', '........'],
    'heavy': ['........', '........', '..TTTTTv', '..MMMMMM', '..MMMMMM', '..MMMMMo', '..MMMMM.', '..MMMMM.',
              '..TTvTT.', '..MMMMM.', '..MMMMM.', '..MMMMM.', '..MMMMM.', '........', '........', '........'],
}
# Boots are not mirrored: one boot drawn twice, both toes forward (right).
BOOT = {
    'light': ['........', '........', '........', '........', '.TTT....', '.MMM....', '.MMM....', '.MMM....',
              '.MMMM...', '.MMMMM..', '.MMMMMM.', '.TTTTTT.', '........', '........', '........', '........'],
    'medium': ['........', '........', '........', '.TTT....', '.MMM....', '.MMM....', '.MMM....', '.MMMM...',
               '.MMMMM..', '.MMMMMM.', '.TTTTTT.', '........', '........', '........', '........', '........'],
    'heavy': ['........', '........', '........', 'TTTT....', 'MMMM....', 'MMMM....', 'MTTM....', 'MMMMM...',
              'MMMMMM..', 'MMMMMMM.', 'TTTTTTT.', '........', '........', '........', '........', '........'],
}

# Theme marks for the left half (x 0-7), mirrored. Each is one connected shape touching the piece.
CREST = {
    'earth': {},
    'frost': {(5, 1): 'T', (5, 2): 'T', (7, 0): 'T', (7, 1): 'T', (7, 2): 'T'},                # a crown of spikes
    'fire': {(7, 0): 'G', (6, 1): 'G', (7, 1): 'G', (7, 2): 'G'},                               # a burning plume
    'sea': {(7, 0): 'T', (7, 1): 'T', (7, 2): 'T'},                                              # a fin ridge
    'holy': {(0, 3): 'T', (1, 3): 'T', (0, 4): 'T', (1, 4): 'T', (1, 5): 'T', (2, 5): 'T'},     # wings
    'shadow': {(1, 1): 'T', (1, 2): 'T', (2, 2): 'T', (2, 3): 'T', (2, 4): 'T'},                # horns
    'nature': {(4, 1): 'G', (4, 2): 'G', (3, 1): 'G'},                                           # sprigs
    'arcane': {(7, 1): 'G', (6, 2): 'T', (7, 2): 'G'},                                           # circlet gem
}
EMBLEM = {   # on the breastplate, left half (mirrored)
    'earth': {(7, 6): 'T', (6, 7): 'T', (7, 7): 'T', (5, 8): 'T', (6, 8): 'T', (7, 8): 'T'},    # mountain
    'frost': {(7, 5): 'T', (7, 6): 'T', (6, 6): 'T', (7, 7): 'T', (5, 6): 'T'},                 # cross star
    'fire': {(7, 5): 'G', (7, 6): 'G', (6, 6): 'G', (6, 7): 'G', (7, 7): 'G'},                  # flame
    'sea': {(5, 7): 'T', (6, 6): 'T', (7, 6): 'T', (7, 7): 'T'},                                # wave crest
    'holy': {(7, 5): 'T', (6, 6): 'T', (7, 6): 'G', (7, 7): 'T', (5, 6): 'T'},                  # sun
    'shadow': {(6, 5): 'T', (7, 5): 'T', (6, 6): 'G', (7, 6): 'T', (7, 7): 'T'},                # skull
    'nature': {(7, 5): 'G', (6, 6): 'G', (7, 6): 'G', (7, 7): 'G'},                             # leaf
    'arcane': {(7, 5): 'T', (6, 6): 'T', (7, 6): 'G', (7, 7): 'T'},                             # rune diamond
}
