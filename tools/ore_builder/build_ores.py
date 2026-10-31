"""Generates everything the ores need from catalog.py.

    python3 tools/ore_builder/build_ores.py [src/main/resources]

Writes (all under the resources root):
  pipican/ores.json                      what Java registers (blocks, items, colours) - read at startup
  assets/pipican/...                     blockstates, block/item models, item definitions (tinted), textures, lang
  data/pipican/loot_table/blocks/...     ore drops (raw/gem with Fortune, the ore with Silk Touch)
  data/pipican/recipe/...                smelting, blasting and storage-block recipes
  data/pipican/worldgen/...              one configured + placed feature per spawning ore
  data/pipican/neoforge/biome_modifier/  adds each ore to its biomes
  data/{minecraft,c,pipican}/tags/...    mining level, c:ores / c:ingots / c:gems ..., pipican tier tags
  gear (gear.py)                         armor, tools and weapons of every set, and their set bonuses
Generated files are rewritten on every run; files it no longer produces are removed (tracked in ores.json).
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import catalog  # noqa: E402
import textures  # noqa: E402
import gear  # noqa: E402
import special_uses  # noqa: E402

NS = 'pipican'
RARITY_EN = {1: 'Common', 2: 'Frequent', 3: 'Medium', 4: 'Rare', 5: 'Very Rare', 6: 'Epic', 7: 'Legendary', 8: 'Mythic', 9: 'Unique'}
RARITY_TR = {1: 'Yaygın', 2: 'Sık', 3: 'Orta', 4: 'Nadir', 5: 'Çok nadir', 6: 'Epik', 7: 'Efsanevi', 8: 'Mitik', 9: 'Benzersiz'}
# Ores whose blocks give off light.
GLOW = {'night_jade': 7, 'sunstone': 10, 'lacrima': 6, 'hihiirokane': 12, 'warpstone': 6, 'tiberium': 7, 'energon': 8,
        'thunderstone': 5, 'prismatic_shard': 9, 'promethium': 11, 'luminite': 10, 'chaos_emerald': 8, 'phazon': 9,
        'dilithium': 5, 'hextech': 7, 'eco': 6, 'element_zero': 4}
BASES = {'stone': 'minecraft:block/stone', 'deepslate': 'minecraft:block/deepslate', 'nether': 'minecraft:block/netherrack',
         'end': 'minecraft:block/end_stone'}


def variants(o):
    """Ore block variants: (block id, base). None when the ore isn't mined at all."""
    if o['use'] in ('alloy', 'relic'):
        return []
    dim = catalog.PLACES[o['place']][0] if o['place'] else 'overworld'
    i = o['id']
    if dim == 'nether':
        return [(f'nether_{i}_ore', 'nether')]
    if dim == 'end':
        return [(f'end_{i}_ore', 'end')]
    return [(f'{i}_ore', 'stone'), (f'deepslate_{i}_ore', 'deepslate')]


def items_of(o):
    """The ore's own items: (item id, kind) - kind 'raw', 'ingot' or 'gem'."""
    i = o['id']
    if o['form'] == 'gem':
        return [(i, 'gem')]
    out = [(f'{i}_ingot', 'ingot')]
    if variants(o):
        out.insert(0, (f'raw_{i}', 'raw'))
    return out


def storage_blocks(o):
    i = o['id']
    out = [(f'{i}_block', 'gem' if o['form'] == 'gem' else 'metal')]
    if o['form'] == 'metal' and variants(o):
        out.append((f'raw_{i}_block', 'raw'))
    return out


class Out:
    def __init__(self, root):
        self.root = root
        self.written = []

    def json(self, rel, data):
        path = os.path.join(self.root, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write('\n')
        self.written.append(rel)


def custom(root, kind, name):
    """A hand-made texture (assets/pipican/textures/<kind>/<name>.png), used as-is instead of the tinted one."""
    return os.path.exists(os.path.join(root, 'assets', NS, 'textures', kind, name + '.png'))


def color(o):
    return 0xFF000000 | o['color']


def signed(c):
    """ARGB int as Java's signed int (item tints take that)."""
    return c - (1 << 32) if c >= 1 << 31 else c


def build(root):
    out = Out(root)
    textures.write_all(os.path.join(root, 'assets', NS, 'textures'))
    runtime = []
    lang_en, lang_tr = {}, {}
    tags = {}  # (registry, tag path) -> [entries]

    def tag(reg, path, *entries):
        tags.setdefault((reg, path), []).extend(entries)

    # Shared parent models: a base cube with a tinted overlay, and a tinted cube.
    out.json(f'assets/{NS}/models/block/tinted_ore.json', {
        'parent': 'minecraft:block/block',
        'textures': {'particle': '#base'},
        'elements': [
            {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {f: {'texture': '#base', 'cullface': f} for f in
                                                              ('down', 'up', 'north', 'south', 'west', 'east')}},
            {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {f: {'texture': '#overlay', 'cullface': f, 'tintindex': 0} for f in
                                                              ('down', 'up', 'north', 'south', 'west', 'east')}},
        ]})
    out.json(f'assets/{NS}/models/block/tinted_cube.json', {
        'parent': 'minecraft:block/block',
        'textures': {'particle': '#all'},
        'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {f: {'texture': '#all', 'cullface': f, 'tintindex': 0} for f in
                                                                     ('down', 'up', 'north', 'south', 'west', 'east')}}]})

    for o in catalog.ORES:
        i, tier, c = o['id'], o['tier'], color(o)
        tint = [{'type': 'minecraft:constant', 'value': signed(c)}]
        entry = {'id': i, 'tier': tier, 'form': o['form'], 'use': o['use'], 'color': c, 'light': GLOW.get(i, 0),
                 'ores': [], 'items': [], 'storage': []}
        drop_item = f'{NS}:raw_{i}' if o['form'] == 'metal' else f'{NS}:{i}'
        lang_en[f'{NS}.ore.{i}.note'] = o['note']
        # --- ore blocks
        for block, base in variants(o):
            entry['ores'].append({'id': block, 'base': base})
            overlay = f'{NS}:block/ore_overlay_{"gem" if o["form"] == "gem" else "metal"}'
            if custom(root, 'block', block):
                out.json(f'assets/{NS}/models/block/{block}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{block}'}})
                block_tint = []
            else:
                out.json(f'assets/{NS}/models/block/{block}.json', {'parent': f'{NS}:block/tinted_ore',
                                                                    'textures': {'base': BASES[base], 'overlay': overlay}})
                block_tint = tint
            entry['ores'][-1]['tinted'] = bool(block_tint)
            out.json(f'assets/{NS}/blockstates/{block}.json', {'variants': {'': {'model': f'{NS}:block/{block}'}}})
            out.json(f'assets/{NS}/items/{block}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{block}', 'tints': block_tint}})
            # Limited (T8/T9) ores are about scarcity: Fortune doesn't multiply them.
            out.json(f'data/{NS}/loot_table/blocks/{block}.json', ore_loot(block, drop_item, o['form'], fortune=o['use'] != 'limited'))
            prefix = {'stone': '', 'deepslate': 'Deepslate ', 'nether': 'Nether ', 'end': 'End '}[base]
            prefix_tr = {'stone': '', 'deepslate': 'Derin Kayrak ', 'nether': 'Nether ', 'end': 'End '}[base]
            lang_en[f'block.{NS}.{block}'] = f'{prefix}{o["en"]} Ore'
            lang_tr[f'block.{NS}.{block}'] = f'{prefix_tr}{o["tr"]} Cevheri'
            tag('block', 'minecraft:mineable/pickaxe', f'{NS}:{block}')
            for t in mining_tags(tier):
                tag('block', t, f'{NS}:{block}')
            tag('block', f'{NS}:ores/tier_{tier}', f'{NS}:{block}')
            tag('block', 'c:ores', f'{NS}:{block}')
            tag('block', f'c:ores/{i}', f'{NS}:{block}')
            tag('item', 'c:ores', f'{NS}:{block}')
            tag('item', f'c:ores/{i}', f'{NS}:{block}')
            tag('block', {'stone': 'c:ores_in_ground/stone', 'deepslate': 'c:ores_in_ground/deepslate',
                          'nether': 'c:ores_in_ground/netherrack', 'end': f'{NS}:ores_in_ground/end_stone'}[base], f'{NS}:{block}')
        # --- items
        for item, kind in items_of(o):
            entry['items'].append({'id': item, 'kind': kind})
            tex = f'{NS}:item/' + {'raw': 'raw_ore', 'ingot': 'ingot', 'gem': 'gem'}[kind]
            item_tint = tint
            # Hand-made art wins: the item's own texture, or a metal's material icon for its ingot.
            for own in ([item] + ([i] if kind == 'ingot' else [])):
                if custom(root, 'item', own):
                    tex, item_tint = f'{NS}:item/{own}', []
                    break
            out.json(f'assets/{NS}/models/item/{item}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': tex}})
            out.json(f'assets/{NS}/items/{item}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{item}', 'tints': item_tint}})
            lang_en[f'item.{NS}.{item}'] = {'raw': f'Raw {o["en"]}', 'ingot': f'{o["en"]} Ingot', 'gem': o['en']}[kind]
            lang_tr[f'item.{NS}.{item}'] = {'raw': f'Ham {o["tr"]}', 'ingot': f'{o["tr"]} Külçesi', 'gem': o['tr']}[kind]
            if kind == 'raw':
                tag('item', 'c:raw_materials', f'{NS}:{item}')
                tag('item', f'c:raw_materials/{i}', f'{NS}:{item}')
            elif kind == 'ingot':
                tag('item', 'c:ingots', f'{NS}:{item}')
                tag('item', f'c:ingots/{i}', f'{NS}:{item}')
            else:
                tag('item', 'c:gems', f'{NS}:{item}')
                tag('item', f'c:gems/{i}', f'{NS}:{item}')
            tag('item', f'{NS}:materials/tier_{tier}', f'{NS}:{item}')
        # --- storage blocks
        material = f'{NS}:{i}_ingot' if o['form'] == 'metal' else f'{NS}:{i}'
        for block, kind in storage_blocks(o):
            entry['storage'].append({'id': block, 'kind': kind})
            tex = {'metal': 'storage_metal', 'gem': 'storage_gem', 'raw': 'raw_ore_block'}[kind]
            out.json(f'assets/{NS}/models/block/{block}.json', {'parent': f'{NS}:block/tinted_cube', 'textures': {'all': f'{NS}:block/{tex}'}})
            out.json(f'assets/{NS}/blockstates/{block}.json', {'variants': {'': {'model': f'{NS}:block/{block}'}}})
            out.json(f'assets/{NS}/items/{block}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{block}', 'tints': tint}})
            out.json(f'data/{NS}/loot_table/blocks/{block}.json', self_drop(block))
            lang_en[f'block.{NS}.{block}'] = f'Block of Raw {o["en"]}' if kind == 'raw' else f'Block of {o["en"]}'
            lang_tr[f'block.{NS}.{block}'] = f'Ham {o["tr"]} Bloğu' if kind == 'raw' else f'{o["tr"]} Bloğu'
            tag('block', 'minecraft:mineable/pickaxe', f'{NS}:{block}')
            for t in mining_tags(min(tier, 4)):
                tag('block', t, f'{NS}:{block}')
            tag('block', 'c:storage_blocks', f'{NS}:{block}')
            tag('item', 'c:storage_blocks', f'{NS}:{block}')
            unit = f'{NS}:raw_{i}' if kind == 'raw' else material
            out.json(f'data/{NS}/recipe/{block}.json', {'type': 'minecraft:crafting_shaped', 'category': 'building',
                                                        'key': {'#': unit}, 'pattern': ['###', '###', '###'], 'result': {'id': f'{NS}:{block}'}})
            out.json(f'data/{NS}/recipe/{unit.split(":")[1]}_from_{block}.json', {
                'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [f'{NS}:{block}'],
                'result': {'count': 9, 'id': unit}})
        # --- smelting
        if o['form'] == 'metal' and variants(o):
            xp = round(0.5 + 0.25 * tier, 2)
            for kind, time in (('smelting', 200), ('blasting', 100)):
                out.json(f'data/{NS}/recipe/{i}_ingot_from_{kind}_raw_{i}.json', {
                    'type': f'minecraft:{kind}', 'category': 'misc', 'cookingtime': time, 'experience': xp, 'group': f'{i}_ingot',
                    'ingredient': f'{NS}:raw_{i}', 'result': {'id': f'{NS}:{i}_ingot'}})
                out.json(f'data/{NS}/recipe/{i}_ingot_from_{kind}_{i}_ore.json', {
                    'type': f'minecraft:{kind}', 'category': 'misc', 'cookingtime': time, 'experience': xp, 'group': f'{i}_ingot',
                    'ingredient': f'#c:ores/{i}', 'result': {'id': f'{NS}:{i}_ingot'}})
        # --- worldgen
        if o['place'] and tier in catalog.TIER_GEN:
            entry['spawns'] = True
            worldgen(out, o)
        runtime.append(entry)
        lang_en[f'{NS}.ore.tier.{tier}'] = f'T{tier} - {RARITY_EN[tier]}'
        lang_tr[f'{NS}.ore.tier.{tier}'] = f'T{tier} - {RARITY_TR[tier]}'

    gear.build(out, catalog.ORES, tag, lang_en, lang_tr, runtime)
    gear.mob_tags(out)
    special_uses.build(out, catalog.ORES, tag, lang_en, lang_tr, runtime)
    for (reg, path), entries in sorted(tags.items()):
        ns, p = path.split(':')
        folder = reg
        out.json(f'data/{ns}/tags/{folder}/{p}.json', {'replace': False, 'values': sorted(set(entries))})
    lang_en[f'itemGroup.{NS}.ores'] = 'Pipican Ores'
    lang_tr[f'itemGroup.{NS}.ores'] = 'Pipican Cevherleri'
    merge_lang(os.path.join(root, f'assets/{NS}/lang/en_us.json'), lang_en)
    merge_lang(os.path.join(root, f'assets/{NS}/lang/tr_tr.json'), lang_tr)

    # Remove files an earlier run wrote that this one didn't - except tags, which other generators and hand-written
    # files share (an emptied tag file would be removed by hand).
    manifest = os.path.join(root, NS, 'ores_generated.txt')
    if os.path.exists(manifest):
        for rel in open(manifest).read().split():
            if rel not in out.written and '/tags/' not in rel and os.path.exists(os.path.join(root, rel)):
                os.remove(os.path.join(root, rel))
    gear.requirements(root)
    out.json(f'{NS}/ores.json', runtime)
    with open(manifest, 'w') as f:
        f.write('\n'.join(sorted(out.written)) + '\n')
    return runtime


def mining_tags(tier):
    """Which pickaxe it takes: T1 stone, T2-3 iron, T4 diamond, T5 netherite, T6+ only this mod's tools."""
    if tier <= 1:
        return ['minecraft:needs_stone_tool']
    if tier <= 3:
        return ['minecraft:needs_iron_tool']
    if tier == 4:
        return ['minecraft:needs_diamond_tool']
    out = ['minecraft:needs_diamond_tool', 'minecraft:incorrect_for_diamond_tool']
    if tier >= 6:
        out.append('minecraft:incorrect_for_netherite_tool')
    return out


def ore_loot(block, drop, form, fortune=True):
    silk = {'condition': 'minecraft:match_tool', 'predicate': {'predicates': {'minecraft:enchantments': [
        {'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}
    return {'type': 'minecraft:block', 'random_sequence': f'{NS}:blocks/{block}', 'pools': [{
        'rolls': 1.0, 'bonus_rolls': 0.0, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'conditions': [silk], 'name': f'{NS}:{block}'},
            {'type': 'minecraft:item', 'name': drop, 'functions': ([
                {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'}] if fortune else []) + [
                {'function': 'minecraft:explosion_decay'}]}]}]}]}


def self_drop(block):
    return {'type': 'minecraft:block', 'random_sequence': f'{NS}:blocks/{block}', 'pools': [{
        'rolls': 1.0, 'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
        'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{block}'}]}]}


def worldgen(out, o):
    i, tier = o['id'], o['tier']
    dim, biomes, band = catalog.PLACES[o['place']]
    if dim == 'overworld':
        biomes = biomes + catalog.region_biomes(o['place'])
    gen = catalog.TIER_GEN[tier]
    targets = []
    for block, base in variants(o):
        rule = {'stone': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:stone_ore_replaceables'},
                'deepslate': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'},
                'nether': {'predicate_type': 'minecraft:block_match', 'block': 'minecraft:netherrack'},
                'end': {'predicate_type': 'minecraft:block_match', 'block': 'minecraft:end_stone'}}[base]
        targets.append({'target': rule, 'state': {'Name': f'{NS}:{block}'}})
    # Rarer ores hide from the air more often, like diamonds.
    air = {1: 0.0, 2: 0.0, 3: 0.2, 4: 0.5, 5: 0.5, 6: 0.6, 7: 0.7}[tier]
    out.json(f'data/{NS}/worldgen/configured_feature/ore_{i}.json', {
        'type': 'minecraft:ore', 'config': {'size': gen['size'], 'discard_chance_on_air_exposure': air, 'targets': targets}})
    lo, hi, shape = catalog.BANDS[band]
    placement = []
    if 'count' in gen:
        placement.append({'type': 'minecraft:count', 'count': gen['count']})
    elif 'count_range' in gen:
        a, b = gen['count_range']
        placement.append({'type': 'minecraft:count', 'count': {'type': 'minecraft:uniform', 'min_inclusive': a, 'max_inclusive': b}})
    else:
        placement.append({'type': 'minecraft:rarity_filter', 'chance': gen['rarity']})
    placement += [{'type': 'minecraft:in_square'},
                  {'type': 'minecraft:height_range', 'height': {'type': f'minecraft:{shape}',
                                                                 'min_inclusive': {'absolute': lo}, 'max_inclusive': {'absolute': hi}}},
                  {'type': 'minecraft:biome'}]
    out.json(f'data/{NS}/worldgen/placed_feature/ore_{i}.json', {'feature': f'{NS}:ore_{i}', 'placement': placement})
    tagged = [b for b in biomes if b.startswith('#')]
    ids = [b for b in biomes if not b.startswith('#')]
    values = tagged + ([ids] if ids else [])
    holder = values[0] if len(values) == 1 else {'type': 'neoforge:or', 'values': values}
    out.json(f'data/{NS}/neoforge/biome_modifier/ore_{i}.json', {
        'type': 'neoforge:add_features', 'biomes': holder, 'features': f'{NS}:ore_{i}', 'step': 'underground_ores'})


def merge_lang(path, entries):
    data = {}
    if os.path.exists(path):
        with open(path, encoding='utf-8') as f:
            data = json.load(f)
    data.update(entries)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


if __name__ == '__main__':
    rt = build(sys.argv[1] if len(sys.argv) > 1 else 'src/main/resources')
    blocks = sum(len(e['ores']) + len(e['storage']) for e in rt)
    items = sum(len(e['items']) for e in rt)
    print(f'{len(rt)} ores: {blocks} blocks, {items} items, {sum(1 for e in rt if e.get("spawns"))} in worldgen')
