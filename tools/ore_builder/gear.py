"""Armor, tools and weapons for every set (sets.py), generated next to the ores by build_ores.py.

Per set: items (helmet, chestplate, leggings, boots, sword, axe, spear, pickaxe, shovel, hoe), tinted models,
the worn-armor asset, crafting recipes, vanilla and pipican tags, the tool's mining level, stat requirements, and a
gear-set file (data/pipican/pipican/gear_set/<set>.json) with the set bonuses as class effects.
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'class_builder'))
import sets  # noqa: E402

NS = 'pipican'
ARMOR = ['helmet', 'chestplate', 'leggings', 'boots']
WEAPONS = ['sword', 'axe', 'spear']
TOOLS = ['pickaxe', 'shovel', 'hoe']
NAMES_EN = {'helmet': 'Helmet', 'chestplate': 'Chestplate', 'leggings': 'Leggings', 'boots': 'Boots', 'sword': 'Sword',
            'axe': 'Axe', 'spear': 'Spear', 'pickaxe': 'Pickaxe', 'shovel': 'Shovel', 'hoe': 'Hoe'}
NAMES_TR = {'helmet': 'Miğferi', 'chestplate': 'Göğüslüğü', 'leggings': 'Dizlikleri', 'boots': 'Botları', 'sword': 'Kılıcı',
            'axe': 'Baltası', 'spear': 'Mızrağı', 'pickaxe': 'Kazması', 'shovel': 'Küreği', 'hoe': 'Çapası'}
PATTERNS = {'helmet': ['###', '# #'], 'chestplate': ['# #', '###', '###'], 'leggings': ['###', '# #', '# #'], 'boots': ['# #', '# #'],
            'sword': ['#', '#', '/'], 'axe': ['##', '#/', ' /'], 'spear': ['  #', ' / ', '/  '], 'pickaxe': ['###', ' / ', ' / '],
            'shovel': ['#', '/', '/'], 'hoe': ['##', ' /', ' /']}
HANDLE = {'sword': 'handle_short', 'axe': 'handle_long', 'spear': 'handle_spear', 'pickaxe': 'handle_long', 'shovel': 'handle_long',
          'hoe': 'handle_long'}
VANILLA_TAG = {'helmet': 'minecraft:head_armor', 'chestplate': 'minecraft:chest_armor', 'leggings': 'minecraft:leg_armor',
               'boots': 'minecraft:foot_armor', 'sword': 'minecraft:swords', 'axe': 'minecraft:axes', 'spear': 'minecraft:spears',
               'pickaxe': 'minecraft:pickaxes', 'shovel': 'minecraft:shovels', 'hoe': 'minecraft:hoes'}

# Per tier: armor defense (helmet, chest, legs, boots), armor durability multiplier, toughness, knockback resistance,
# tool durability, mining speed, attack damage bonus, enchantability. T4 = diamond, T5 = netherite.
TIER = {
    1: ((2, 5, 4, 1), 13, 0.0, 0.0, 220, 5.5, 1.5, 12),
    2: ((2, 6, 5, 2), 15, 0.5, 0.0, 300, 6.0, 2.0, 12),
    3: ((2, 7, 5, 2), 22, 1.0, 0.0, 700, 7.0, 2.5, 12),
    4: ((3, 8, 6, 3), 33, 2.0, 0.0, 1561, 8.0, 3.0, 10),
    5: ((3, 8, 6, 3), 37, 3.0, 0.1, 2031, 9.0, 4.0, 15),
    6: ((3, 9, 7, 3), 45, 4.0, 0.1, 2600, 10.0, 5.0, 15),
    7: ((4, 9, 7, 4), 55, 5.0, 0.15, 3300, 11.0, 6.0, 15),
    8: ((4, 10, 8, 4), 70, 6.0, 0.2, 4200, 12.0, 7.0, 18),
    9: ((5, 10, 8, 5), 90, 8.0, 0.25, 6000, 14.0, 9.0, 20),
}
# Points of Strength (melee weapons, armor) or Dexterity (spears) a tier's gear asks for.
REQUIREMENT = {1: 2, 2: 3, 3: 5, 4: 6, 5: 9, 6: 12, 7: 15, 8: 20, 9: 25}


def incorrect_tag(tier, traits):
    """The block tag a tier's tools can't mine: T1-T2 like iron, T3 like diamond, T4 like netherite, then one tier up."""
    if 'mine_all' in traits:
        return f'{NS}:incorrect_for_nothing'
    return f'{NS}:incorrect_for_tier_{tier}_tool'


def incorrect_tags():
    """-> {tag path: entries} for every tier's incorrect-for tag."""
    out = {f'{NS}:incorrect_for_nothing': []}
    for tier in range(1, 10):
        if tier <= 2:
            entries = ['#minecraft:incorrect_for_iron_tool']
        elif tier == 3:
            entries = ['#minecraft:incorrect_for_diamond_tool']
        elif tier == 4:
            entries = ['#minecraft:incorrect_for_netherite_tool']
        else:
            entries = [f'#{NS}:ores/tier_{t}' for t in range(tier + 2, 10)]
        out[f'{NS}:incorrect_for_tier_{tier}_tool'] = entries
    return out


def build(out, ores, tag, lang_en, lang_tr, runtime):
    import effects as fx
    by_id = {o['id']: o for o in ores}
    rt = {e['id']: e for e in runtime}
    for path, entries in incorrect_tags().items():
        tag('block', path, *entries) if entries else tag('block', path)
    for sid, spec in sets.SETS.items():
        o = by_id[sid]
        tier, color = o['tier'], 0xFF000000 | o['color']
        style = sets.STYLES[spec['style']]
        traits = spec['traits']
        defense, dur_mult, tough, kb, tool_dur, speed, dmg, ench = TIER[tier]
        dur_k = 0.5 if 'fragile' in traits else 2.0 if 'durable' in traits else 1.0
        material = f'{NS}:{sid}_ingot' if o['form'] == 'metal' else f'{NS}:{sid}'
        weapons_only = sid in sets.WEAPONS_ONLY
        kinds = WEAPONS if weapons_only else ARMOR + WEAPONS + TOOLS
        rt[sid]['set'] = {
            'kinds': kinds, 'traits': traits, 'weight': style['weight'],
            'defense': [max(1, round(d * style['armor'])) for d in defense],
            'armor_durability': max(1, round(dur_mult * dur_k)), 'toughness': round(tough + style['tough'], 2),
            'knockback_resistance': round(kb + style['kb'], 2),
            'tool_durability': max(1, round(tool_dur * dur_k)), 'speed': round(speed * style['speed'], 2), 'damage': dmg,
            'swing': style['swing'], 'enchantability': max(1, ench + style['ench']),
            'incorrect': incorrect_tag(tier, traits), 'repair': f'{NS}:repairs/{sid}',
        }
        tag('item', f'{NS}:repairs/{sid}', material)
        for kind in kinds:
            item = f'{sid}_{kind}'
            tint_head = {'type': 'minecraft:constant', 'value': color - (1 << 32)}
            if os.path.exists(os.path.join(out.root, 'assets', NS, 'textures', 'item', item + '.png')):
                # Hand-made art wins.
                out.json(f'assets/{NS}/models/item/{item}.json', {'parent': 'minecraft:item/generated' if kind in ARMOR else 'minecraft:item/handheld',
                                                                  'textures': {'layer0': f'{NS}:item/{item}'}})
                tints = []
            elif kind in ARMOR:
                out.json(f'assets/{NS}/models/item/{item}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/gear/{kind}'}})
                tints = [tint_head]
            else:
                out.json(f'assets/{NS}/models/item/{item}.json', {'parent': 'minecraft:item/handheld', 'textures': {
                    'layer0': f'{NS}:item/gear/{HANDLE[kind]}', 'layer1': f'{NS}:item/gear/{kind}'}})
                tints = [{'type': 'minecraft:constant', 'value': -1}, tint_head]
            out.json(f'assets/{NS}/items/{item}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{item}', 'tints': tints}})
            lang_en[f'item.{NS}.{item}'] = f'{o["en"]} {NAMES_EN[kind]}'
            lang_tr[f'item.{NS}.{item}'] = f'{o["tr"]} {NAMES_TR[kind]}'
            tag('item', VANILLA_TAG[kind], f'{NS}:{item}')
            tag('item', f'{NS}:sets/{sid}/{"armor" if kind in ARMOR else "weapons"}', f'{NS}:{item}')
            for t in traits:
                tag('item', f'{NS}:traits/{t}', f'{NS}:{item}')
            if kind in ARMOR:
                tag('item', 'minecraft:trimmable_armor', f'{NS}:{item}')
                tag('item', f'{NS}:ore_gear/armor_{style["weight"]}', f'{NS}:{item}')
                tag('item', f'{NS}:gear/armor_tier_{tier}', f'{NS}:{item}')
            elif kind == 'spear':
                tag('item', f'{NS}:gear/spears_tier_{tier}', f'{NS}:{item}')
            elif kind in ('sword', 'axe'):
                tag('item', f'{NS}:gear/melee_tier_{tier}', f'{NS}:{item}')
            if sid not in sets.WEAPONS_ONLY:
                key = {'#': material}
                if kind not in ARMOR:
                    key['/'] = 'minecraft:stick'
                out.json(f'data/{NS}/recipe/{item}.json', {'type': 'minecraft:crafting_shaped', 'category': 'equipment' if kind in ARMOR else 'tools',
                                                           'key': key, 'pattern': PATTERNS[kind], 'result': {'id': f'{NS}:{item}'}})
        if not weapons_only:
            layer = {'texture': f'{NS}:plate_{style["weight"]}', 'dyeable': {'color_when_undyed': o['color']}}
            out.json(f'assets/{NS}/equipment/{sid}.json', {'layers': {'humanoid': [layer], 'humanoid_leggings': [layer]}})
        # Set bonuses: class effects that hold while wearing the full set / holding a set weapon / wearing the helmet.
        ctx = {'class_id': 'gear', 'special_name': None, 'resource_name': None}

        def effects(phrases):
            got = []
            for p in phrases:
                if isinstance(p, dict):
                    got.append(p)
                    continue
                e, unsupported = fx.translate(p, ctx)
                if not e or unsupported:
                    raise SystemExit(f'set {sid}: can\'t translate "{p}"')
                got.extend(e)
            return got
        bonus = []
        if spec['armor'] and not weapons_only:
            bonus.append({'type': 'pipican:conditional', 'when': {'type': 'pipican:wearing', 'items': f'#{NS}:sets/{sid}/armor', 'min': 4},
                          'effects': effects(spec['armor'])})
        if spec['weapon']:
            bonus.append({'type': 'pipican:conditional', 'when': {'type': 'pipican:holding', 'items': f'#{NS}:sets/{sid}/weapons', 'hand': 'main'},
                          'effects': effects(spec['weapon'])})
        if spec['helmet']:
            bonus.append({'type': 'pipican:conditional', 'when': {'type': 'pipican:wearing', 'items': f'{NS}:{sid}_helmet', 'min': 1},
                          'effects': effects(spec['helmet'])})
        out.json(f'data/{NS}/{NS}/gear_set/{sid}.json', {'effects': bonus})


def mob_tags(out):
    """The entity tags set bonuses refer to ("against monsters"), as the class builder writes them."""
    import combat
    for name, values in sorted({**combat.MOB_TAGS, **combat.GENERATED}.items()):
        out.json(f'data/{NS}/tags/entity_type/{name}.json', {'values': values})


def requirements(root):
    """Writes the gear's stat requirements into the Strength and Dexterity files (their '#pipican:gear/' entries)."""
    import json
    for stat, tags in (('strength', ('melee', 'armor')), ('dexterity', ('spears',))):
        path = os.path.join(root, f'data/{NS}/{NS}/stat/{stat}.json')
        with open(path, encoding='utf-8') as f:
            data = json.load(f)
        keep = [g for g in data.get('gear', []) if not (isinstance(g['items'], str) and g['items'].startswith(f'#{NS}:gear/'))]
        for t in tags:
            keep += [{'items': f'#{NS}:gear/{t}_tier_{tier}', 'min': m} for tier, m in REQUIREMENT.items()]
        data['gear'] = keep
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write('\n')
