"""Phase 5: generates the Allomancy items (metal flakes, vials, Lerasium) from mistborn.py. Called by build_ores.py."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import mistborn  # noqa: E402

NS = 'pipican'


def build(out, tag, lang_en, lang_tr, runtime):
    rt = {e['id']: e for e in runtime}

    def item(ore, item_id, texture, color, en, tr, note, consume=None):
        entry = {'id': item_id, 'kind': 'extra'}
        if consume:
            entry['consume'] = consume
        rt[ore if ore in rt else 'tin']['items'].append(entry)
        out.json(f'assets/{NS}/models/item/{item_id}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{texture}'}})
        out.json(f'assets/{NS}/items/{item_id}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{item_id}',
                                                                 'tints': [{'type': 'minecraft:constant', 'value': (0xFF000000 | color) - (1 << 32)}]}})
        lang_en[f'item.{NS}.{item_id}'] = en
        lang_tr[f'item.{NS}.{item_id}'] = tr
        lang_en[f'{NS}.ore.{item_id}.note'] = note
        tag('item', f'{NS}:allomancy/{texture}', f'{NS}:{item_id}')

    def flakes(metal, en, tr, color, recipe):
        item(metal, f'{metal}_flakes', 'flakes', color, f'{en} Flakes', f'{tr} Pulu', f'Allomancy: mix into a vial with a glass bottle.')
        kind, *rest = recipe
        if kind == 'ingot':
            out.json(f'data/{NS}/recipe/allomancy/{metal}_flakes.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                     'ingredients': [rest[0]], 'result': {'id': f'{NS}:{metal}_flakes', 'count': 4}})
        else:
            parts, count = rest
            ingredients = [p if isinstance(p, list) else f'{NS}:{p}' for p in parts]
            out.json(f'data/{NS}/recipe/allomancy/{metal}_flakes.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                     'ingredients': ingredients, 'result': {'id': f'{NS}:{metal}_flakes', 'count': count}})

    for metal, (en, tr, color, recipe, seconds, does) in mistborn.METALS.items():
        flakes(metal, en, tr, color, recipe)
        item(metal, f'vial_of_{metal}', 'vial', color, f'Vial of {en}', f'{tr} Şişesi', f'Allomancy - drink to burn {en.lower()}: {does}',
             consume={'effects': [], 'action': f'burn:{metal}:{seconds}', 'animation': 'drink'})
        out.json(f'data/{NS}/recipe/allomancy/vial_of_{metal}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'brewing',
                 'ingredients': ['minecraft:glass_bottle', f'{NS}:{metal}_flakes', f'{NS}:{metal}_flakes'],
                 'result': {'id': f'{NS}:vial_of_{metal}'}})
    for metal, (en, tr, color, recipe) in mistborn.FLAKES_ONLY.items():
        flakes(metal, en, tr, color, recipe)
    # Lerasium: eat the ingot to become Mistborn (burn every metal at once), once.
    for entry in rt['lerasium']['items']:
        if entry['id'] == 'lerasium_ingot':
            entry['consume'] = {'effects': [], 'action': 'mistborn', 'animation': 'eat'}
    # What counts as metal for Steel (blocks to push off) and Iron (items to pull).
    for b in ('minecraft:iron_block', 'minecraft:gold_block', 'minecraft:copper_block', 'minecraft:raw_iron_block', 'minecraft:raw_gold_block',
              'minecraft:netherite_block', '#minecraft:anvil', 'minecraft:iron_bars', 'minecraft:iron_door', 'minecraft:iron_trapdoor',
              '#minecraft:rails', 'minecraft:hopper', 'minecraft:cauldron', 'minecraft:lightning_rod'):
        tag('block', f'{NS}:allomancy/metal_blocks', b)
    for e in runtime:
        if e['form'] == 'metal':
            for s_ in e['storage']:
                tag('block', f'{NS}:allomancy/metal_blocks', f'{NS}:{s_["id"]}')
    for i in ('#c:ingots', '#c:nuggets', '#c:raw_materials', '#minecraft:chains', 'minecraft:iron_sword', 'minecraft:iron_axe', 'minecraft:iron_pickaxe',
              'minecraft:iron_shovel', 'minecraft:iron_hoe', 'minecraft:golden_sword', 'minecraft:shears', 'minecraft:bucket', 'minecraft:iron_helmet',
              'minecraft:iron_chestplate', 'minecraft:iron_leggings', 'minecraft:iron_boots', 'minecraft:chainmail_helmet',
              'minecraft:chainmail_chestplate', 'minecraft:chainmail_leggings', 'minecraft:chainmail_boots', 'minecraft:golden_helmet',
              'minecraft:golden_chestplate', 'minecraft:golden_leggings', 'minecraft:golden_boots', 'minecraft:anvil', 'minecraft:minecart'):
        tag('item', f'{NS}:allomancy/metal_items', i)
    lang_en[f'{NS}.allomancy.burning'] = 'Burning: %s'
    lang_tr[f'{NS}.allomancy.burning'] = 'Yakılıyor: %s'
