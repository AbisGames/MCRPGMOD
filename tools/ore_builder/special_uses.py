"""Phase 3: generates the special ores' uses from specials.py (fuel, brewing data for Java, recipes, trades, off-hand
focus bonuses, extra items and tooltips). Called by build_ores.py."""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'class_builder'))
import specials  # noqa: E402

NS = 'pipican'


def build(out, ores, tag, lang_en, lang_tr, runtime):
    import effects as fx
    rt = {e['id']: e for e in runtime}
    color = {o['id']: o['color'] for o in ores}
    storage = {e['id']: [s['id'] for s in e['storage'] if s['kind'] != 'raw'] for e in runtime}

    # Extra items (Cinnabar smelts into Mercury): registered with their ore, tinted with their own colour.
    for item, spec in specials.EXTRA_ITEMS.items():
        rt[spec['from']]['items'].append({'id': item, 'kind': 'extra'})
        out.json(f'assets/{NS}/models/item/{item}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/gem'}})
        tint = (0xFF000000 | spec['color']) - (1 << 32)
        out.json(f'assets/{NS}/items/{item}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{item}',
                                                              'tints': [{'type': 'minecraft:constant', 'value': tint}]}})
        lang_en[f'item.{NS}.{item}'] = spec['en']
        lang_en[f'{NS}.ore.{item}.note'] = spec['note']
        lang_tr[f'item.{NS}.{item}'] = spec['tr']

    # Fuel: the ore, and its block nine times over.
    fuels = {}
    for ore, ticks in specials.FUEL.items():
        # Metals burn as their ingot (and raw ore), gems as themselves.
        o = next(x for x in ores if x['id'] == ore)
        for item in ([f'{ore}_ingot', f'raw_{ore}'] if o['form'] == 'metal' else [ore]):
            fuels[f'{NS}:{item}'] = {'burn_time': ticks}
        for block in storage.get(ore, []):
            fuels[f'{NS}:{block}'] = {'burn_time': min(ticks * 9, 2_000_000)}
    out.json('data/neoforge/data_maps/item/furnace_fuels.json', {'values': fuels})

    # Data the Java hooks read: brews, consumables, coatings, block behaviours.
    for ore, effect in specials.BREWS.items():
        rt[ore].setdefault('special', {})['brew'] = effect
    rt[specials.EXTENDER].setdefault('special', {})['brew_extend'] = True
    rt['cinnabar'].setdefault('special', {})['amplifier_item'] = specials.AMPLIFIER
    for ore, use in specials.CONSUME.items():
        rt[ore].setdefault('special', {})['consume'] = {
            'effects': [{'effect': f'minecraft:{e}', 'seconds': s, 'amplifier': a} for e, s, a in use.get('effects', [])],
            'action': use.get('action', '')}
    for ore, (slot, text) in specials.COATINGS.items():
        rt[ore].setdefault('special', {})['coating'] = slot
        lang_en[f'{NS}.coating.{ore}'] = text
        lang_en[f'{NS}.coating.{ore}.slot'] = {'weapon': 'Coats a weapon', 'helmet': 'Coats a helmet', 'armor': 'Coats armor',
                                               'shield': 'Coats a shield', 'tool': 'Fuses onto a tool'}[slot] + ' at an anvil'
    for ore, kind in specials.BLOCKS.items():
        rt[ore].setdefault('special', {})['block'] = kind
    lang_en[f'{NS}.coating.tooltip'] = 'Coated: %s'
    lang_tr[f'{NS}.coating.tooltip'] = 'Kaplama: %s'

    # Recipes.
    for n, (kind, ingredients, result, count, *components) in enumerate(specials.RECIPES):
        res = {'id': result, 'count': count}
        if components:
            res['components'] = components[0]
        refs = ingredients if isinstance(ingredients, list) else [ingredients] if isinstance(ingredients, str) else list(ingredients[1].values())
        source = next(r for r in refs if isinstance(r, str) and r.startswith(f'{NS}:')).split(':')[1]
        name = f'special/{result.split(":")[1]}_from_{source}'
        if kind == 'shapeless':
            data = {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': ingredients, 'result': res}
        elif kind == 'shaped':
            pattern, key = ingredients
            data = {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key, 'result': res}
        else:
            data = {'type': f'minecraft:{kind}', 'category': 'misc', 'cookingtime': 200, 'experience': 0.5, 'ingredient': ingredients,
                    'result': {'id': result}}
        out.json(f'data/{NS}/recipe/{name}.json', data)

    # Valyrian steel weapons, reforged from found ingots.
    for kind, pattern in specials.VALYRIAN_RECIPES.items():
        out.json(f'data/{NS}/recipe/valyrian_steel_{kind}.json', {'type': 'minecraft:crafting_shaped', 'category': 'combat', 'pattern': pattern,
                 'key': {'#': f'{NS}:valyrian_steel_ingot', '/': 'minecraft:stick'}, 'result': {'id': f'{NS}:valyrian_steel_{kind}'}})
    for ore, spread in specials.SPREAD.items():
        rt[ore].setdefault('special', {})['spread'] = spread

    # Trades: the wandering trader and one profession buy each trade good.
    for item, (count, emeralds, profession) in specials.TRADES.items():
        trade = f'{NS}:trade/{item}_emerald'
        out.json(f'data/{NS}/villager_trade/trade/{item}_emerald.json', {
            'wants': {'id': f'{NS}:{item}', 'count': count}, 'gives': {'id': 'minecraft:emerald', 'count': emeralds},
            'max_uses': 12.0, 'xp': 10.0, 'reputation_discount': 0.05})
        tag('villager_trade', 'minecraft:wandering_trader/buying', trade)
        tag('villager_trade', f'minecraft:{profession}/level_2', trade)

    # Off-hand focuses: class effects while the ore is held in the off hand.
    ctx = {'class_id': 'gear', 'special_name': None, 'resource_name': None}
    for ore, phrases in specials.FOCUS.items():
        got = []
        for p in phrases:
            if isinstance(p, dict):
                got.append(p)
                continue
            e, unsupported = fx.translate(p, ctx)
            if not e or unsupported:
                raise SystemExit(f'focus {ore}: can\'t translate "{p}"')
            got.extend(e)
        out.json(f'data/{NS}/{NS}/gear_set/focus_{ore}.json', {'effects': [{
            'type': 'pipican:conditional', 'when': {'type': 'pipican:holding', 'items': f'{NS}:{ore}', 'hand': 'off'}, 'effects': got}]})
        lang_en[f'{NS}.focus.{ore}'] = 'In the off hand: ' + '; '.join(p for p in phrases if isinstance(p, str))
