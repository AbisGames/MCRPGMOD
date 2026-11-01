"""Build pipican class data from class_specs/*.md.

usage (from the repo root):
    python3 tools/class_builder/build.py class_specs src/main/resources class_specs/BUILD_REPORT.md
Writes data/pipican/pipican/{class,stat,skill,skill_tree} and data/pipican/tags/{damage_type,item}.
"""
import collections
import json
import math
import os
import re

# The translators use thousands of distinct patterns; the default cache (512) recompiles them over and over.
re._MAXCACHE = 1_000_000
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import abilities as ab  # noqa: E402
import bonus  # noqa: E402
import triggers  # noqa: E402
import named  # noqa: E402
import effects as fx  # noqa: E402
import parse_specs  # noqa: E402

NS = 'pipican'
NOT_YET = {'text': ' (not in game yet)', 'color': 'dark_gray', 'italic': True}
PARTLY = {'text': ' (partly not in game yet)', 'color': 'dark_gray', 'italic': True}

GEAR_TAGS = {
    'swords': '#pipican:weapons/swords', 'axes': '#pipican:weapons/axes', 'spears': '#pipican:weapons/spears',
    'maces': '#pipican:weapons/maces', 'tridents': '#pipican:weapons/tridents', 'bows': '#pipican:weapons/bows',
    'crossbows': '#pipican:weapons/crossbows', 'shields': '#pipican:weapons/shields',
    'light armor': '#pipican:armor/light', 'medium armor': '#pipican:armor/medium', 'heavy armor': '#pipican:armor/heavy',
}
TIER_RANK = ['forbidden', 'clumsy', 'neutral', 'proficient', 'mastery']

# Hand-mapped "Specific items / exceptions" (free text in 6 specs). Rules go before the category rules.
EXCEPTIONS = {
    'druid': lambda aff: [{'items': ['minecraft:' + i for i in
                                     ('wooden_sword', 'stone_sword', 'wooden_axe', 'stone_axe', 'wooden_spear', 'stone_spear')
                                     if TIER_RANK.index(aff.get(_cat(i), 'neutral')) < TIER_RANK.index('proficient')],
                           'tier': 'proficient'}],
    'gunslinger': lambda aff: [],  # crossbows already proficient in the table (firearms don't exist yet)
    'magician': lambda aff: [{'items': ['minecraft:blaze_rod', 'minecraft:stick'], 'tier': 'proficient'}],
    'witch': lambda aff: [{'items': ['minecraft:stick', 'minecraft:blaze_rod'], 'tier': 'proficient'}],
    'priest': lambda aff: [{'items': ['minecraft:wooden_shovel', 'minecraft:stone_shovel'], 'tier': 'neutral'}],
    'wizard': lambda aff: [{'items': ['minecraft:book', 'minecraft:enchanted_book'], 'tier': 'proficient'}],
}
EXCEPTION_NOTES = {
    'gunslinger': 'Firearms don\'t exist yet; crossbows stand in (already proficient in the table).',
    'wizard': 'Book/enchanted book counts as proficient in any hand (no off-hand-only rule exists).',
    'priest': 'Shovels are neutral (they don\'t become maces: there is no "counts as" rule).',
    'magician': 'Stick/blaze rod are proficient (they don\'t become spears: there is no "counts as" rule).',
    'witch': 'Stick/blaze rod are proficient (there is no "staff" category).',
}


def _cat(item):
    for k in ('sword', 'axe', 'spear'):
        if item.endswith('_' + k):
            return k + 's'
    return None


# Icon ids in the specs that aren't real item ids.
ICON_FIX = {'zombie_spawner_egg': 'zombie_spawn_egg', 'smithing_template': 'coast_armor_trim_smithing_template'}

UNSTACKABLE = re.compile(r'(?:_sword|_axe|_pickaxe|_shovel|_hoe|_spear|_helmet|_chestplate|_leggings|_boots|^minecraft:(?:bow|crossbow|'
                         r'trident|shield|mace|potion|splash_potion|lingering_potion|enchanted_book|writable_book|written_book|saddle|'
                         r'totem_of_undying|elytra|flint_and_steel|shears|fishing_rod|spyglass|compass|clock|map|goat_horn|bundle)|_bucket)$')

# Kit items the specs describe in words.
POTIONS = {'resistance': 'turtle_master', 'poison': 'poison', 'weakness': 'weakness', 'healing': 'healing',
           'invisibility': 'invisibility'}


def kit_entry(cid, n, item, extra, notes):
    if item is None:
        notes.append(f'kit line not understood: "{extra}"')
        return None
    if item in ('splash_potion', 'potion'):
        m = re.match(r'of (\w+)', extra)
        potion = POTIONS.get(m.group(1)) if m else None
        if m and m.group(1) == 'resistance':
            notes.append('kit: "splash potion of resistance" doesn\'t exist in vanilla; gave Turtle Master instead')
        if potion:
            return {'id': 'minecraft:' + item, 'count': n, 'components': {'minecraft:potion_contents': {'potion': 'minecraft:' + potion}}}
    if item == 'random' and 'potion' in extra:
        notes.append('kit: "random potion" isn\'t possible; gave awkward potions')
        return {'id': 'minecraft:potion', 'count': n, 'components': {'minecraft:potion_contents': {'potion': 'minecraft:awkward'}}}
    if item == 'enchanted_book':
        notes.append('kit: "random level-1 enchanted book" isn\'t possible; gave a plain book')
        return {'id': 'minecraft:book', 'count': n}
    if item == 'smithing_template':
        notes.append('kit: "any trim" template -> coast armor trim')
        return {'id': 'minecraft:coast_armor_trim_smithing_template', 'count': n}
    if item == 'pistol':
        notes.append('kit: pistol (planned) -> crossbow')
        return {'id': 'minecraft:crossbow', 'count': n}
    if item == 'bullets':
        notes.append('kit: bullets (planned) -> arrows')
        return {'id': 'minecraft:arrow', 'count': n}
    e = {'id': 'minecraft:' + item}
    if n != 1:
        e['count'] = n
    return e


# ------------------------------------------------------------------ skill triggers

def map_trigger(text):
    """-> (trigger json, exact?)"""
    t = text.lower()
    wmap = [('sword', '#pipican:weapons/swords'), ('axe', '#pipican:weapons/axes'), ('mace', '#pipican:weapons/maces'),
            ('spear', '#pipican:weapons/spears'), ('staff', ['minecraft:blaze_rod', 'minecraft:stick'])]
    m = re.match(r'^hitting mobs with (\w+?)s?\b(.*)$', t)
    if m:
        for k, tag in wmap:
            if m.group(1).startswith(k):
                return {'type': 'pipican:melee_hit', 'items': tag}, m.group(2).strip() in ('',) or k == 'spear'
    if t.startswith('fighting with your staff'):
        return {'type': 'pipican:melee_hit', 'items': ['minecraft:blaze_rod', 'minecraft:stick']}, True
    if 'arrows from a bow' in t:
        return {'type': 'pipican:ranged_hit', 'items': '#pipican:weapons/bows'}, True
    if 'crossbow bolts' in t:
        return {'type': 'pipican:ranged_hit', 'items': '#pipican:weapons/crossbows'}, True
    if 'firearms' in t and t.startswith('hitting'):
        return {'type': 'pipican:ranged_hit', 'items': '#pipican:weapons/crossbows'}, False
    if t.startswith('blocking attacks'):
        return {'type': 'pipican:block'}, True
    if t == 'sprinting':
        return {'type': 'pipican:move', 'mode': 'sprint', 'per_blocks': 10}, True
    m = re.match(r'^taking damage while wearing (?:at least )?(\d) (?:pieces of )?heavy armor(?: pieces)?$', t)
    if m:
        return {'type': 'pipican:hurt_wearing', 'items': '#pipican:armor/heavy', 'min_pieces': int(m.group(1))}, True
    # ---- approximations
    if t.startswith('swimming'):
        return {'type': 'pipican:move', 'mode': 'swim', 'per_blocks': 10}, False
    if 'travelling' in t or 'discovering' in t or 'chunks' in t:
        return {'type': 'pipican:move', 'mode': 'walk', 'per_blocks': 50}, False
    if 'jump' in t or 'fall' in t:
        return {'type': 'pipican:jump'}, False
    if t.startswith(('taking', 'absorbing', 'surviving')) or 'damage near allies' in t:
        return {'type': 'pipican:hurt_wearing', 'items': '#pipican:armor/any', 'min_pieces': 1}, False
    if t.startswith('blocking'):
        return {'type': 'pipican:block'}, False
    if 'bow' in t and 'hit' in t:
        return {'type': 'pipican:ranged_hit', 'items': '#pipican:weapons/bows'}, False
    if t.startswith(('hitting', 'landing', 'fighting', 'damaging', 'making mobs', 'poisoning', 'cursing', 'setting mobs')):
        return {'type': 'pipican:melee_hit'}, False
    return {'type': 'pipican:kill'}, False


THRESHOLDS = {'normal': None, 'fast': [50, 200, 600, 1500, 3500], 'slow': [200, 800, 2400, 6000, 14000]}


def skill_icon(trigger, fallback):
    items = trigger.get('items')
    icons = {'#pipican:weapons/swords': 'minecraft:iron_sword', '#pipican:weapons/axes': 'minecraft:iron_axe',
             '#pipican:weapons/maces': 'minecraft:mace', '#pipican:weapons/spears': 'minecraft:trident',
             '#pipican:weapons/bows': 'minecraft:bow', '#pipican:weapons/crossbows': 'minecraft:crossbow',
             '#pipican:armor/heavy': 'minecraft:iron_chestplate'}
    if isinstance(items, str) and items in icons:
        return icons[items]
    return {'pipican:block': 'minecraft:shield', 'pipican:move': 'minecraft:feather', 'pipican:jump': 'minecraft:rabbit_foot'}.get(
        trigger['type'], fallback)


def slug(s):
    return re.sub(r'[^a-z0-9]+', '_', s.lower().replace("'", '')).strip('_')


def with_marker(text, unsupported, any_effects):
    if not unsupported:
        return text
    return {'text': text, 'extra': [PARTLY if any_effects else NOT_YET]}


# ------------------------------------------------------------------ abilities

ABILITY_IN_NOTES = re.compile(r'\*\*([^*]+)\*\*\s*\(((?:[^()]|\([^)]*\))*)\)')
# "unlocks Recall Weapon ability", "unlocks Holy Radiance; ...", "unlocks Turn Undead (undead flee for 5s)"
UNLOCKS = re.compile(r'(?:^|[;,]\s*)unlocks (?:the )?(?!an? )([A-Z][\w\' -]+?)(?: ability)?(?:\s*\([^)]*\))?(?=\s*(?:[;,]|$))', re.I)


def spec_abilities(c):
    """[(name, description)] from section 12's ability notes, plus abilities that nodes unlock."""
    out = {}
    for line in c['notes'].splitlines():
        if re.search(r'abilit|talent', line, re.I):
            for m in ABILITY_IN_NOTES.finditer(line):
                if not m.group(1).lower().startswith(('power tier', 'co-design', 'decided')):
                    out.setdefault(m.group(1).strip(), m.group(2).strip())
    for tree in c['trees'].values():
        for n in tree['nodes']:
            for m in UNLOCKS.finditer(n['gives']):
                out.setdefault(m.group(1).strip(), '')
    return out


def ability_json(name, description, d):
    j = {'name': name}
    if description:
        j['description'] = description
    j['icon'] = 'minecraft:' + d.get('icon', 'book')
    if d.get('cost'):
        j['cost'] = d['cost']
    if d.get('spend_all'):
        j['spend_all'] = True
    j['cooldown_ticks'] = int(round(d.get('cooldown', 10) * 20))
    if d.get('rest'):
        j['uses_per_rest'] = d['rest']
    if d.get('duration'):
        j['duration_ticks'] = int(round(d['duration'] * 20))
    if d.get('toggle'):
        j['toggle'] = True
        j['drain_per_second'] = d.get('drain', 0)
    if d.get('pulse'):
        j['pulse_ticks'] = d['pulse']
    if d.get('anchored'):
        j['anchored'] = True
    for k in ('requires', 'auto', 'end_when'):
        if d.get(k):
            j[k] = d[k]
    for k in ('actions', 'end_actions', 'pulse_actions', 'effects'):
        if d.get(k):
            j[k] = d[k]
    return j


# ------------------------------------------------------------------ tree layout

QUARTER_ANGLE = {'power': -90, 'faith': 0, 'trickery': 90, 'war': 180}
QUARTER_ORDER = ['power', 'faith', 'trickery', 'war']


def quarter_of(where):
    w = (where or '').lower()
    # The first quarter named is the class's own ("War quarter, leaning toward the Trickery quarter").
    found = [(w.find(q + ' quarter'), q) for q in QUARTER_ANGLE if q + ' quarter' in w]
    if found:
        return min(found)[1]
    for q, words in {'power': ('top',), 'faith': ('right',), 'trickery': ('bottom',), 'war': ('left',)}.items():
        if any(x in w for x in words):
            return q
    return None


def layout_starts(classes):
    """class id -> (angle degrees, radius) for the shared trees."""
    names = {c['name'].lower(): c['id'] for c in classes}
    tier = {}
    for c in classes:
        r = re.search(r'ring (\d)', c['trees']['general']['where'] or '')
        tier[c['id']] = int(r.group(1)) if r else 3
    pos = {}
    by_q = collections.defaultdict(list)
    for c in classes:
        q = quarter_of(c['trees']['general']['where']) or 'war'
        by_q[(q, tier[c['id']])].append(c)
    for (q, ring), cs in by_q.items():
        if ring == 1:
            for c in cs:
                pos[c['id']] = (QUARTER_ANGLE[q], 300)
    for (q, ring), cs in by_q.items():
        if ring != 2:
            continue
        base = QUARTER_ANGLE[q]
        def lean(c):
            w = c['trees']['general']['where'].lower()
            m = re.search(r'leaning toward the (\w+) quarter', w)
            if not m:
                return 0
            d = (QUARTER_ANGLE[m.group(1)] - base + 540) % 360 - 180
            return 1 if d > 0 else -1
        cs = sorted(cs, key=lambda c: (lean(c), c['id']))
        n = len(cs)
        for i, c in enumerate(cs):
            pos[c['id']] = (base + (i - (n - 1) / 2) * 20, 750)
    masters = collections.defaultdict(list)
    for c in classes:
        if tier[c['id']] != 3:
            continue
        m = re.search(r'just beyond the ([\w -]+?)(?:[,.)]|$)', c['trees']['general']['where'] or '')
        anchor = names.get(m.group(1).strip().lower()) if m else None
        if anchor not in pos:
            q = quarter_of(c['trees']['general']['where']) or 'war'
            anchor = None
            masters[('q', q)].append(c)
        else:
            masters[anchor].append(c)
    for anchor, cs in masters.items():
        base = QUARTER_ANGLE[anchor[1]] if isinstance(anchor, tuple) else pos[anchor][0]
        cs = sorted(cs, key=lambda c: c['id'])
        n = len(cs)
        for i, c in enumerate(cs):
            pos[c['id']] = (base + (i - (n - 1) / 2) * 5, 1250 if i % 2 == 0 else 1600)
    # Masters anchored near a quarter edge can land on a neighbour's region: push each ring apart.
    for radius, sep in ((1250, 8.0), (1600, 6.5)):
        ring = sorted((cid for cid, (a, r) in pos.items() if r == radius), key=lambda cid: pos[cid][0])
        ang = [pos[cid][0] for cid in ring]
        for _ in range(200):
            moved = False
            for i in range(len(ang)):
                j = (i + 1) % len(ang)
                gap = (ang[j] - ang[i]) % 360 if len(ang) > 1 else 360
                if gap < sep:
                    push = (sep - gap) / 2
                    ang[i] -= push
                    ang[j] += push
                    moved = True
            if not moved:
                break
        for cid, a in zip(ring, ang):
            pos[cid] = (a, radius)
    return pos


def place_region(nodes, start_id, angle, radius, step=70, spread=55):
    """Nodes radiate outward from the start, layer by layer (BFS depth), spread sideways."""
    ids = {n['id'] for n in nodes}
    adj = collections.defaultdict(set)
    for n in nodes:
        for l in n['links']:
            if l in ids:
                adj[n['id']].add(l)
                adj[l].add(n['id'])
    depth = {start_id: 0}
    queue = [start_id]
    while queue:
        cur = queue.pop(0)
        for nb in sorted(adj[cur]):
            if nb not in depth:
                depth[nb] = depth[cur] + 1
                queue.append(nb)
    for n in nodes:
        depth.setdefault(n['id'], max(depth.values()) + 1)
    layers = collections.defaultdict(list)
    for n in nodes:
        layers[depth[n['id']]].append(n['id'])
    a = math.radians(angle)
    radial = (math.cos(a), math.sin(a))
    lateral = (-math.sin(a), math.cos(a))
    out = {}
    for d, layer in layers.items():
        k = len(layer)
        for i, nid in enumerate(layer):
            r = radius + d * step
            off = (i - (k - 1) / 2) * spread
            out[nid] = (round(radial[0] * r + lateral[0] * off), round(radial[1] * r + lateral[1] * off))
    return out


# ------------------------------------------------------------------ build

def main(spec_dir, res_dir, report_path):
    classes = parse_specs.parse_dir(spec_dir)
    data = os.path.join(res_dir, 'data', NS, NS)
    for sub in ('class', 'skill', 'skill_tree', 'ability'):
        shutil.rmtree(os.path.join(data, sub), ignore_errors=True)
    for f in os.listdir(os.path.join(data, 'stat')) if os.path.isdir(os.path.join(data, 'stat')) else []:
        if f not in {s + '.json' for s in fx.CORE_STATS}:
            os.remove(os.path.join(data, 'stat', f))

    def write(rel, obj):
        p = os.path.join(res_dir, 'data', rel)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, 'w', encoding='utf-8') as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write('\n')

    report = collections.defaultdict(list)       # category -> [(class, where, text)]
    notes = collections.defaultdict(list)        # class -> [note]
    counts = collections.Counter()

    def tr(cid, where, text, ctx, category=None):
        eff, uns = fx.translate(text, ctx)
        if eff and re.search(r'\(max [^)]*\)', text or ''):
            notes[cid].append(f'{where}: cap not enforced (scaling has no maximum): "{text}"')
        counts['phrases'] += 1
        if not uns:
            counts['full'] += 1
        elif eff:
            counts['partial'] += 1
        for u in uns:
            report[category or classify(u)].append((cid, where, text if len(uns) == 1 and not eff else u))
        return eff, uns

    # Shared skill definitions (by name) and who trains them.
    skill_defs = {}
    skill_users = collections.defaultdict(list)
    for c in classes:
        for s in c['skills']:
            if 'ref' not in s:
                key = slug(s['name']) if s['shared'] else c['id'] + '_' + slug(s['name'])
                if key in skill_defs:
                    notes[c['id']].append(f'skill "{s["name"]}" defined twice; using the first definition')
                    continue
                skill_defs[key] = (c, s)
            key = slug(s['name']) if ('ref' in s or s.get('shared')) else c['id'] + '_' + slug(s['name'])
            skill_users[key].append(c['id'])

    # Damage-type and armor tags used by the translations / triggers.
    for tag, values in fx.DAMAGE_TYPE_TAGS.items():
        write(f'{NS}/tags/damage_type/{tag}.json', {'values': values})
    # Biome groups used by "in natural biomes" / "in snowy biomes" conditions.
    write(f'{NS}/tags/worldgen/biome/natural.json', {'values': [
        '#minecraft:is_forest', '#minecraft:is_jungle', '#minecraft:is_taiga', '#minecraft:is_savanna', 'minecraft:plains',
        'minecraft:sunflower_plains', 'minecraft:meadow', 'minecraft:cherry_grove', 'minecraft:swamp', 'minecraft:mangrove_swamp']})
    write(f'{NS}/tags/worldgen/biome/cold.json', {'values': [
        'minecraft:snowy_plains', 'minecraft:ice_spikes', 'minecraft:snowy_taiga', 'minecraft:snowy_beach', 'minecraft:snowy_slopes',
        'minecraft:frozen_peaks', 'minecraft:jagged_peaks', 'minecraft:grove', 'minecraft:frozen_river', 'minecraft:frozen_ocean',
        'minecraft:deep_frozen_ocean']})
    write(f'{NS}/tags/item/weapons/metal.json', {'values': [f'minecraft:{m}_{w}' for m in ('iron', 'diamond', 'netherite')
                                                            for w in ('sword', 'axe', 'pickaxe', 'shovel', 'hoe', 'spear')]})
    write(f'{NS}/tags/item/weapons/axes_maces.json', {'values': ['#pipican:weapons/axes', '#pipican:weapons/maces']})
    write(f'{NS}/tags/item/weapons/polearms.json', {'values': ['#pipican:weapons/spears', '#pipican:weapons/tridents']})
    write(f'{NS}/tags/item/weapons/staff.json', {'values': ['minecraft:blaze_rod', 'minecraft:stick']})
    write(f'{NS}/tags/item/food/cooked_meat.json', {'values': ['minecraft:cooked_beef', 'minecraft:cooked_porkchop', 'minecraft:cooked_mutton',
                                                           'minecraft:cooked_chicken', 'minecraft:cooked_rabbit']})
    write(f'{NS}/tags/item/armor/any.json', {'values': ['#pipican:armor/light', '#pipican:armor/medium', '#pipican:armor/heavy']})

    # Abilities unlocked by a skill tier ("Tier III: unlocks Recall Weapon ability") belong to that tier, not the class.
    tier_granted = set()
    for key, (owner, sk) in skill_defs.items():
        for t_text in sk['tiers']:
            for m in UNLOCKS.finditer(t_text or ''):
                if (owner['id'], m.group(1).strip()) in ab.A:
                    tier_granted.add(f'{NS}:{owner["id"]}/{slug(m.group(1).strip())}')

    starts = layout_starts(classes)
    first_general = True
    first_stat = True
    general_ids = {}
    stat_ids = {}

    for c in classes:
        if c['icon'] in ICON_FIX:
            notes[c['id']].append(f'icon "{c["icon"]}" isn\'t an item id; used {ICON_FIX[c["icon"]]}')
            c['icon'] = ICON_FIX[c['icon']]
    for c in classes:
        cid = c['id']
        sp = c['special']
        sid = f'{NS}:{cid}_{slug(sp["name"])}'
        res = c['resource']
        ctx = {'special_name': sp['name'], 'special_id': sid, 'resource_name': res['name'] if res else None, 'class_id': cid}
        named.learn(cid, c['passives'] + c['special_passives'], ctx)
        # Every ability is also a named state: "+1 armor while in Divine Ecstasy", "when Divine Ecstasy ends".
        for name in list(spec_abilities(c)) + ab.EXTRA.get(cid, []):
            if (cid, name) in ab.A:
                named.NAMED[cid].setdefault(name.lower(), ({'type': 'pipican:ability_active', 'ability': f'{NS}:{cid}/{slug(name)}'},
                                                           ab.A[(cid, name)].get('effects', [])))
        color = (0xFF << 24) | int(res['color'], 16) if res and res['color'] else -1
        if color >= 1 << 31:
            color -= 1 << 32

        # Special stat: per-point attribute bonuses go on the stat; anything else becomes a scaling special passive.
        per_point, extra_special = [], []
        eff, _ = tr(cid, 'special stat per point', sp['per_point'], dict(ctx, implicit_per_point=True))
        for e in eff:
            scal = e.get('scaling', [])
            if e['type'] == 'pipican:attribute' and len(scal) == 1 and scal[0]['source'] == 'special_stat' and not e['amount']:
                b = {'attribute': e['attribute'], 'amount': scal[0]['per']}
                if e.get('operation'):
                    b['operation'] = e['operation']
                per_point.append(b)
            else:
                extra_special.append(e)
        stat = {'name': sp['name'], 'description': sp['description'], 'max': sp['max'], 'exclusive': True, 'color': color}
        if per_point:
            stat['per_point'] = per_point
        write(f'{NS}/{NS}/stat/{cid}_{slug(sp["name"])}.json', stat)

        passives = []
        for p in c['passives']:
            hand = next((v for (k_cid, k), v in ab.PASSIVE_EFFECTS.items() if k_cid == cid and p.startswith(k)), None)
            if hand is not None:
                passives += hand
                counts['phrases'] += 1
                counts['full'] += 1
            else:
                passives += tr(cid, 'passive', p, dict(ctx, passive=True))[0]
        special_passives = list(extra_special)
        for p in c['special_passives']:
            special_passives += tr(cid, 'special passive', p, dict(ctx, passive=True))[0]
        if cid in ab.WEAKNESS_EFFECTS:
            weff, wuns = ab.WEAKNESS_EFFECTS[cid], []
            counts['phrases'] += 1
            counts['full'] += 1
        else:
            weff, wuns = tr(cid, 'weakness', c['weakness']['does'], dict(ctx, weakness=True))

        base_stats = {f'{NS}:{k}': v for k, v in c['stats'].items() if k != 'SPECIAL' and v}
        if c['stats'].get('SPECIAL'):
            base_stats[sid] = c['stats']['SPECIAL']

        special_stat = {'stat': sid}
        if res:
            special_stat['resource'] = {
                'name': res['name'], 'base_max': res['base_max'], 'max_per_point': res['max_per_point'],
                'regen_per_second': res['regen_per_second'], 'regen_per_point': res['regen_per_point'],
                'color': color, 'starts_full': res['starts_full'],
            }
            fills, out_of_combat = bonus.fill_rules(res['starts_note'])
            if out_of_combat:
                special_stat['resource']['drains_in_combat'] = False
            passives += fills
            drain_only = res['regen_per_second'] < 0 and not fills

        kit = []
        for n, i, x in c['kit']:
            entry = kit_entry(cid, n, i, x, notes[cid])
            if not entry:
                continue
            # Items that don't stack can't be given as "count": 2; give separate stacks instead.
            count = entry.get('count', 1)
            if count > 1 and UNSTACKABLE.search(entry['id']):
                kit.extend([{k: v for k, v in entry.items() if k != 'count'} for _ in range(count)])
            else:
                kit.append(entry)

        aff = c['affinity']
        rules = EXCEPTIONS.get(cid, lambda a: [])(aff) if c['affinity_exceptions'].lower() != 'none' else []
        if cid in EXCEPTION_NOTES:
            notes[cid].append('gear: ' + EXCEPTION_NOTES[cid])
        rules = [r for r in rules if r['items']]
        for gear, tier in aff.items():
            if tier != 'neutral' and gear in GEAR_TAGS:
                rules.append({'items': GEAR_TAGS[gear], 'tier': tier})

        # --- abilities
        listed = spec_abilities(c)
        for extra in ab.EXTRA.get(cid, []):
            listed.setdefault(extra, '')
        ability_ids = {}
        for name, desc in listed.items():
            d = ab.A.get((cid, name))
            if (cid, name) in ab.NOT_ABILITIES:
                continue
            if d is None:
                report['Abilities not in game yet'].append((cid, f'ability {name}', desc or 'unlocked by a tree node'))
                continue
            ability_ids[name.lower()] = f'{NS}:{cid}/{slug(name)}'
            if any(a.get('abilities') == 'FORMS' for a in d.get('actions', [])):
                forms = [f'{NS}:{cid}/{slug(n)}' for (k_cid, n) in ab.A if k_cid == cid and n.endswith(' Form')]
                d = dict(d, actions=[dict(a, abilities=forms) if a.get('abilities') == 'FORMS' else a for a in d['actions']])
            write(f'{NS}/{NS}/ability/{cid}/{slug(name)}.json', ability_json(name, desc or d.get('description'), d))
            counts['abilities'] += 1
            if d.get('approx'):
                notes[cid].append(f'ability {name}: {d["approx"]}')
        node_granted = set()

        # --- trees
        trees = c['trees']
        def build_nodes(kind, tree_nodes, positions):
            out = []
            for n in tree_nodes:
                node = {'id': n['id'], 'name': n['name']}
                eff, uns = ([], [])
                grants = [ability_ids[m.group(1).strip().lower()] for m in UNLOCKS.finditer(n['gives'])
                          if m.group(1).strip().lower() in ability_ids]
                hand = ab.NODE_EFFECTS.get((cid, n['id']))
                if hand is not None:
                    grants += [ability_ids[g.lower()] for g in hand[1] if g.lower() in ability_ids]
                node_granted.update(grants)
                # "unlocks X ability" is handled by grants; translate whatever else the node gives.
                rest = UNLOCKS.sub('', n['gives']).strip(' ;,') if grants else n['gives']
                if hand is not None:
                    eff, uns, rest = list(hand[0]), [], ''
                    counts['phrases'] += 1
                    counts['full'] += 1
                if rest:
                    eff, uns = tr(cid, f'{kind} tree: {n["name"]}', rest, dict(ctx, keystone=n['size'] == 'keystone', node_name=n['name']))
                elif grants:
                    counts['phrases'] += 1
                    counts['full'] += 1
                if n['gives']:
                    node['description'] = with_marker(n['gives'], uns, bool(eff) or bool(grants))
                node['type'] = n['size'] if n['size'] in ('small', 'notable', 'keystone', 'start') else 'small'
                node['x'], node['y'] = positions[n['id']]
                if n['links']:
                    node['links'] = n['links']
                if node['type'] != 'start':
                    if n['cost'] != 1:
                        node['cost'] = n['cost']
                    if n['level'] != 1:
                        node['required_level'] = n['level']
                if eff:
                    node['effects'] = eff
                if grants:
                    node['grants'] = grants
                out.append(node)
            return out

        def start_of(tree_nodes):
            st = [n for n in tree_nodes if n['size'] == 'start']
            return st[0]['id'] if st else tree_nodes[0]['id']

        angle, radius = starts.get(cid, (0, 1250))
        gnodes = trees['general']['nodes']
        for n in gnodes:
            if n['id'] in general_ids:
                notes[cid].append(f'general-tree node id "{n["id"]}" also used by {general_ids[n["id"]]}')
            general_ids[n['id']] = cid
        gfile = {'tree': f'{NS}:general', 'nodes': build_nodes('general', gnodes, place_region(gnodes, start_of(gnodes), angle, radius))}
        if first_general:
            gfile = {'tree': f'{NS}:general', 'name': 'Passive Tree', 'nodes': gfile['nodes']}
            first_general = False
        write(f'{NS}/{NS}/skill_tree/general/{cid}.json', gfile)

        snodes = trees['special']['nodes']
        write(f'{NS}/{NS}/skill_tree/special/{cid}.json',
              {'name': f'{c["name"]} Tree', 'nodes': build_nodes('special', snodes, place_region(snodes, start_of(snodes), 90, 0, 70, 70))})

        tnodes = trees['stat']['nodes']
        for n in tnodes:
            if n['id'] in stat_ids:
                notes[cid].append(f'stat-tree node id "{n["id"]}" also used by {stat_ids[n["id"]]}')
            stat_ids[n['id']] = cid
        # Stat trees are no longer shipped: stat points are spent freely in the Dark-Souls-style level-up screen
        # (Stats tab), and each class's stat_growth decides how much it gets out of each point. The spec's stat-tree
        # section is still parsed and link-checked so old specs keep validating.

        for kind, tn in (('general', gnodes), ('special', snodes), ('stat', tnodes)):
            ids = {n['id'] for n in tn}
            for n in tn:
                for l in n['links']:
                    if l not in ids:
                        notes[cid].append(f'{kind} tree: node "{n["id"]}" links to "{l}", which isn\'t in this class\'s tree')

        rpg = {
            'name': c['name'], 'description': c['description'], 'icon': 'minecraft:' + c['icon'],
            'selectable': c['selectable'], 'base_health': c['health'], 'base_stats': base_stats,
            **({'stat_growth': {(sid if k == 'SPECIAL' else f'{NS}:{k}'): v for k, v in c['growth'].items()}} if c.get('growth') else {}),
            'special_stat': special_stat, 'starting_kit': kit,
            'skill_tree': {'tree': f'{NS}:general', 'start': start_of(gnodes)},
            'special_skill_tree': {'tree': f'{NS}:special/{cid}', 'start': start_of(snodes)},
            'progression': {'max_level': c['max_level'], 'starting_skill_points': c['free_general'],
                            'starting_special_skill_points': c['free_special']},
        }
        for e in passives + special_passives:
            extra = named.EXTRA.get(cid, {}).get(e.get('name')) if e.get('type') == 'pipican:on' else None
            if extra:
                acts = e['actions']
                end = len(acts) - 1 if acts and acts[-1]['type'] in ('pipican:shrug', 'pipican:cheat_death') else len(acts)
                e['actions'] = acts[:end] + extra + acts[end:]
        if passives:
            rpg['passives'] = passives
        if special_passives:
            rpg['special_passives'] = special_passives
        rpg['weakness'] = {'description': with_marker(c['weakness']['read'], wuns, bool(weff)), 'effects': weff}
        rpg['affinities'] = {'default': 'neutral', 'rules': rules}
        slots = {f'{NS}:{k}': v for k, v in c['trinkets'].items() if v}
        if slots:
            rpg['trinket_slots'] = slots
        own = [a for a in ability_ids.values() if a not in node_granted and a not in tier_granted]
        if own:
            rpg['abilities'] = own
        for patch in named.PATCHES.get(cid, []):
            patch(rpg)
        # A draining bar is only a problem when nothing in the class fills it.
        if res and drain_only and not re.search(r'"type": "pipican:resource", "amount": [1-9]', json.dumps(rpg)):
            report['Resource bar that fills from use (only the drain works)'].append(
                (cid, 'resource', f'{res["name"]} drains {abs(res["regen_per_second"])}/s; filling it from fighting needs the active-skill system'))
        write(f'{NS}/{NS}/class/{cid}.json', rpg)

    # --- skills
    for key, (owner, s) in sorted(skill_defs.items()):
        ctx = {'special_name': owner['special']['name'], 'special_id': f'{NS}:{owner["id"]}_{slug(owner["special"]["name"])}',
               'resource_name': owner['resource']['name'] if owner['resource'] else None, 'class_id': owner['id']}
        mapped = triggers.map_text(s['trigger'], ctx)
        trig, exact = mapped if mapped else map_trigger(s['trigger'])
        if mapped and not exact:
            # Some of the text was read: keep what was, list the rest.
            pass
        if not exact:
            report['Skill trigger approximated'].append((owner['id'], s['name'], f'"{s["trigger"]}" -> {trig["type"]}' +
                                                         (f' {trig.get("items")}' if trig.get('items') else '')))
        tiers = []
        for t_text in s['tiers']:
            grants = [f'{NS}:{owner["id"]}/{slug(m.group(1).strip())}' for m in UNLOCKS.finditer(t_text or '')
                      if (owner['id'], m.group(1).strip()) in ab.A]
            rest = UNLOCKS.sub('', t_text or '').strip(' ;,') if grants else (t_text or '')
            eff, uns = tr(owner['id'], f'skill {s["name"]}', rest, ctx) if rest else ([], [])
            if grants and not rest:
                counts['phrases'] += 1
                counts['full'] += 1
            tier = {'description': with_marker(t_text or '', uns, bool(eff) or bool(grants)), 'effects': eff}
            if grants:
                tier['grants'] = grants
            tiers.append(tier)
        skill = {'name': s['name'], 'description': 'Trained by ' + s['trigger'] + '.',
                 'icon': skill_icon(trig, 'minecraft:' + owner['icon']), 'trigger': trig}
        if THRESHOLDS.get(s['speed'].lower()):
            skill['thresholds'] = THRESHOLDS[s['speed'].lower()]
        skill['points_to'] = s['points_to']
        users = sorted(set(skill_users[key]))
        skill['classes'] = [f'{NS}:{u}' for u in users]
        skill['tiers'] = tiers
        write(f'{NS}/{NS}/skill/{key}.json', skill)
    for key, users in skill_users.items():
        if key not in skill_defs:
            for u in users:
                notes[u].append(f'references shared skill "{key}" that no file defines')

    for tag, values in sorted({**fx.combat.MOB_TAGS, **fx.combat.GENERATED}.items()):
        write(f'{NS}/tags/entity_type/{tag}.json', {'values': values})
    for tag, values in sorted(fx.cond.GROUND_TAGS.items()):
        write(f'{NS}/tags/block/{tag}.json', {'values': values})

    write_report(report_path, classes, report, notes, counts, skill_defs)
    print(f'{len(classes)} classes, {len(skill_defs)} skills, {counts["abilities"]} abilities; phrases {counts["phrases"]}: '
          f'{counts["full"]} full, {counts["partial"]} partial')


CATEGORIES = [
    ('Resource bar: filling, refills, spending', r'regen|refill|fills?\b|max \w+(?: \w+)?$|\bmax (?!health)\w+|drains|costs? \d+|spend'),
    ('Active abilities (named abilities, cooldowns, auras, summons)', r'unlock|ability|cooldown|aura|song|chant|smite|berserk|minion|eidolon|curse|decoy|blink|stance|ecstasy|execute|grimoire|temple|form\b|sigil|gadget|premonition|optimization|prayer|invocation|succor|ward\b|bound weapon|favored|staff|servant|summon|toggle|right-click|spell'),
    ('Bonus against a kind of mob / marked target', r'against|\bvs\b|undead|prey|mark|unclean|illager|animals|arthropod|golem|to mobs|boss'),
    ('Conditional bonus (while / when / where)', r'\bwhile\b|\bwhen\b|below|above|night|daylight|underground|in water|outdoors|nether|\bend\b|wet|mount|sneak|sprint|full health|dark|light|rain|biome|forest|village|holding|wearing|without|with no|in combat|first hit|for \d+s'),
    ('Critical hits', r'crit'),
    ('Chance on hit / proc effects', r'chance|% to |on hit|your hits|hits give|inflict|bleed|poison|ignite|set on fire|knock'),
    ('Immunities', r'immune|ignore|can\'t be|cannot be|never'),
    ('Potions, XP, loot, trading, crafting, food', r'potion|experience|\bxp\b|loot|trade|villager|craft|brew|enchant|drop|barter|tame|harvest|crop|hunger|food|durability|repair|anvil'),
    ('Per-weapon attack speed, reload, draw speed', r'attack speed|reload|draw'),
]


def classify(text):
    t = text.lower()
    for name, pat in CATEGORIES:
        if re.search(pat, t):
            return name
    return 'Other mechanics'


def write_report(path, classes, report, notes, counts, skill_defs):
    L = ['# Class build report', '',
         'Generated from `class_specs/*.md` by the class builder. Everything listed here is **not in the game yet**:',
         'tree nodes and skill tiers still show their text in-game, marked "(not in game yet)" or "(partly not in game yet)".', '',
         f'- Classes built: **{len(classes)}**, skills: **{len(skill_defs)}**, active abilities: **{counts["abilities"]}** '
         f'(of {counts["abilities"] + len(report.get("Abilities not in game yet", []))} named in the specs)',
         f'- Bonus phrases: **{counts["phrases"]}**. Fully working: **{counts["full"]}**, partly working: **{counts["partial"]}**, '
         f'not yet: **{counts["phrases"] - counts["full"] - counts["partial"]}**', '',
         'Passives and weaknesses have no tooltip of their own, so their missing parts are only listed here.', '',
         '## Missing features, biggest first', '']
    order = sorted(report.items(), key=lambda kv: -len(kv[1]))
    L.append('| Feature | Lines |')
    L.append('|---|---|')
    for name, items in order:
        L.append(f'| [{name}](#{slug(name).replace("_", "-")}) | {len(items)} |')
    L.append('')
    for name, items in order:
        L.append(f'## {name}')
        L.append('')
        by_class = collections.defaultdict(list)
        for cid, where, text in items:
            by_class[cid].append((where, text))
        for cid in sorted(by_class):
            L.append(f'- **{cid}**')
            for where, text in by_class[cid]:
                L.append(f'  - {where}: {text}')
        L.append('')
    L.append('## Other notes per class')
    L.append('')
    for cid in sorted(notes):
        for n in sorted(set(notes[cid])):
            L.append(f'- **{cid}**: {n}')
    L.append('')
    with open(path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(L))


if __name__ == '__main__':
    main(*sys.argv[1:4])
