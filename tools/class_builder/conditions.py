"""Turn condition phrases ("while wet", "underground", "below half health") into pipican condition JSON.

parse(text, ctx) -> condition dict, or None if any part of the text isn't understood.
Atoms are found anywhere in the text; whatever is left over must be filler words, so an unknown
qualifier ("while your eidolon is destroyed") makes the whole condition unsupported.
"""
import re


def state(s):
    return {'type': 'pipican:state', 'state': s}


def not_(c):
    return {'type': 'pipican:not', 'condition': c}


NO_SKY = not_(state('sees_sky'))
HEAVY = '#pipican:armor/heavy'
ARMOR_TAGS = {'heavy': '#pipican:armor/heavy', 'medium': '#pipican:armor/medium', 'light': '#pipican:armor/light'}
WORDS = {'one': 1, 'two': 2, 'three': 3, 'four': 4, 'a': 1, 'an': 1, 'any': 1}

# Kinds: "place" conditions joined by "and" mean "in any of these places" ("in the Nether and deserts").
PLACE, OTHER = 'place', 'other'
# The Cleric's "icon of faith": a totem or golden item in the off-hand.
ICON = ['minecraft:totem_of_undying', 'minecraft:golden_sword', 'minecraft:golden_axe', 'minecraft:golden_pickaxe', 'minecraft:golden_shovel',
        'minecraft:golden_hoe', 'minecraft:golden_apple', 'minecraft:enchanted_golden_apple', 'minecraft:gold_ingot', 'minecraft:gold_nugget',
        'minecraft:golden_carrot', 'minecraft:clock']


GROUND = {'stone': '#minecraft:base_stone_overworld', 'dirt': '#minecraft:dirt', 'grass': '#minecraft:dirt',
          'sand': '#minecraft:sand', 'gravel': 'minecraft:gravel', 'netherrack': '#minecraft:base_stone_nether',
          'nether blocks': '#minecraft:base_stone_nether'}

# Block tags the parsed conditions need; build.py writes them to data/pipican/tags/block/.
GROUND_TAGS = {}


def _ground(text):
    """"stone, dirt or sand" -> one block tag/id, or a pipican tag grouping several."""
    words = [k for k in re.split(r',\s*or\s+|,\s*|\s+or\s+|\s+and\s+', text) if k in GROUND]
    kinds = sorted({GROUND[k] for k in words})
    if len(kinds) == 1:
        return kinds[0]
    name = 'ground/' + '_'.join(sorted({w.replace(' ', '_') for w in words}))
    GROUND_TAGS[name] = kinds
    return '#pipican:' + name


def _frac(word):
    word = word.lower()
    if word == 'half':
        return 0.5
    if word in ('a quarter', 'quarter'):
        return 0.25
    return float(word.rstrip('%')) / 100


def _atoms(ctx):
    full = (ctx.get('resource_name') or '\0').lower()
    # "Eldritch Charge" can be written as just "Charge".
    rn = '(?:' + re.escape(full) + ('|' + re.escape(full.split()[-1]) if ' ' in full else '') + ')'
    num = r'(\d+(?:\.\d+)?%|half|a quarter)'
    import named
    return named.atoms(ctx) + [
        # health
        (rf'(?:you\'?re |you are |while )?(?:below|under|at or below) {num}(?: of your)?(?: max)? (?:health|hp)', lambda m: {'type': 'pipican:health', 'max': _frac(m.group(1))}, OTHER),
        (rf'(?:above|over) {num}(?: of your)?(?: max)? (?:health|hp)', lambda m: {'type': 'pipican:health', 'min': _frac(m.group(1))}, OTHER),
        (r'(?:at )?full health', lambda m: {'type': 'pipican:health', 'min': 1.0}, OTHER),
        # resource bar
        (rf'{rn} is (?:above|over) {num}', lambda m: {'type': 'pipican:resource', 'min': _frac(m.group(1))}, OTHER),
        (rf'{rn} is full', lambda m: {'type': 'pipican:resource', 'min': 0.999}, OTHER),
        (rf'{rn} is (?:below|under) {num}', lambda m: {'type': 'pipican:resource', 'max': _frac(m.group(1))}, OTHER),
        (rf'(?:below|under) {num} {rn}', lambda m: {'type': 'pipican:resource', 'max': _frac(m.group(1))}, OTHER),
        (rf'you have (?:over|more than) {num} {rn}', lambda m: {'type': 'pipican:resource', 'min': _frac(m.group(1))}, OTHER),
        (rf'{rn} is full|(?:at )?full {rn}', lambda m: {'type': 'pipican:resource', 'min': 1.0}, OTHER),
        (rf'{rn} is empty', lambda m: {'type': 'pipican:resource', 'max': 0.0001}, OTHER),
        # water & weather
        (r'in water and (?:in the )?air', lambda m: {'type': 'pipican:any_of', 'conditions': [state('in_water'), not_(state('on_ground'))]}, OTHER),
        (r'(?:in )?rain or water|wet', lambda m: state('wet'), OTHER),
        (r'underwater|submerged', lambda m: state('underwater'), PLACE),
        (r'(?:in|standing in) water', lambda m: state('in_water'), PLACE),
        (r'on dry land|on land|out of (?:the )?water', lambda m: not_(state('in_water')), PLACE),
        (r'swimming', lambda m: state('swimming'), OTHER),
        (r'(?:in )?lava', lambda m: state('in_lava'), PLACE),
        (r'(?:in |during )?(?:thunderstorms?|storms?|thunder)', lambda m: state('thundering'), PLACE),
        (r'(?:in |during )?(?:the )?rain', lambda m: state('raining'), PLACE),
        # sky, time, place
        (r'underground(?: with no sky access)?|with no sky access|without sky access|no sky access', lambda m: NO_SKY, PLACE),
        (r'(?:while )?travelling outdoors', lambda m: state('sees_sky'), PLACE),
        (r'outdoors|under the open sky|in the open|outside', lambda m: state('sees_sky'), PLACE),
        (r'(?:at |during the )?night(?:time)?', lambda m: state('night'), OTHER),
        (r'(?:in )?daylight|during the day|in the day(?:time)?|by day', lambda m: state('day'), OTHER),
        (r'(?:in )?the nether', lambda m: {'type': 'pipican:dimension', 'dimensions': ['minecraft:the_nether']}, PLACE),
        (r'(?:in )?the end\b', lambda m: {'type': 'pipican:dimension', 'dimensions': ['minecraft:the_end']}, PLACE),
        (r'(?:in )?(?:the )?overworld', lambda m: {'type': 'pipican:dimension', 'dimensions': ['minecraft:overworld']}, PLACE),
        (r'outside (?:of )?(?:a )?forests?', lambda m: not_({'type': 'pipican:biome', 'biomes': '#minecraft:is_forest'}), PLACE),
        (r'(?:in )?(?:a )?forests?', lambda m: {'type': 'pipican:biome', 'biomes': '#minecraft:is_forest'}, PLACE),
        (r'(?:in )?(?:a )?deserts?', lambda m: {'type': 'pipican:biome', 'biomes': ['minecraft:desert']}, PLACE),
        (r'(?:in )?(?:a )?jungles?', lambda m: {'type': 'pipican:biome', 'biomes': '#minecraft:is_jungle'}, PLACE),
        (r'(?:in )?(?:the )?oceans?', lambda m: {'type': 'pipican:biome', 'biomes': '#minecraft:is_ocean'}, PLACE),
        (r'(?:in )?(?:a )?(?:snowy|cold|frozen) biomes?', lambda m: {'type': 'pipican:biome', 'biomes': '#pipican:cold'}, PLACE),
        (r'(?:in )?natural biomes?', lambda m: {'type': 'pipican:biome', 'biomes': '#pipican:natural'}, PLACE),
        # ground
        (r'(?:standing |you stand )?on ((?:stone|dirt|sand|gravel|grass|netherrack|nether blocks)(?:(?:, | or | and |, or )(?:stone|dirt|sand|gravel|grass|netherrack|nether blocks))*)',
         lambda m: {'type': 'pipican:standing_on', 'blocks': _ground(m.group(1))}, OTHER),
        # light
        (r'(?:in )?light level (\d+) or (?:lower|less|below)', lambda m: {'type': 'pipican:light', 'max': int(m.group(1)) + 1}, OTHER),
        (r'(?:in )?light level (\d+) or (?:higher|more|above)', lambda m: {'type': 'pipican:light', 'min': int(m.group(1))}, OTHER),
        (r'(?:in )?(?:darkness|the dark|low light|dim light|shadows?)', lambda m: {'type': 'pipican:light', 'max': 8}, OTHER),
        (r'(?:in )?(?:bright light|full light)', lambda m: {'type': 'pipican:light', 'min': 12}, OTHER),
        # movement
        (r'(?:after )?standing still for (\d+(?:\.\d+)?)\s?s(?:econds?)?', lambda m: {'type': 'pipican:still', 'seconds': float(m.group(1))}, OTHER),
        (r'(?:standing still|not moving|stationary)', lambda m: {'type': 'pipican:still'}, OTHER),
        (r'moving', lambda m: not_({'type': 'pipican:still', 'seconds': 0.5}), OTHER),
        (r'out of combat|not in combat', lambda m: not_(state('in_combat')), OTHER),
        (r'(?:in|during) combat|fighting', lambda m: state('in_combat'), OTHER),
        (r'not mounted|not riding|on foot|dismounted', lambda m: not_(state('mounted')), OTHER),
        (r'mounted|riding(?: a \w+)?|on horseback|you ride', lambda m: state('mounted'), OTHER),
        (r'not sprinting', lambda m: not_(state('sprinting')), OTHER),
        (r'sprinting|charging', lambda m: state('sprinting'), OTHER),
        (r'blocking(?: with a shield)?|shield raised', lambda m: state('blocking'), OTHER),
        (r'dual-wielding|holding a weapon in (?:each|both) hands?|with a weapon in each hand', lambda m: {'type': 'pipican:all_of', 'conditions': [
            {'type': 'pipican:holding', 'items': '#pipican:weapons/melee', 'hand': 'main'},
            {'type': 'pipican:holding', 'items': '#pipican:weapons/melee', 'hand': 'off'}]}, OTHER),
        (r'(slowed|poisoned|withered|weakened|blinded)', lambda m: {'type': 'pipican:effect', 'effects': 'minecraft:' + {
            'slowed': 'slowness', 'poisoned': 'poison', 'withered': 'wither', 'weakened': 'weakness', 'blinded': 'blindness'}[m.group(1)]}, OTHER),
        (r'(?:with|holding|using) your favored weapon(?: type)?', lambda m: {'type': 'pipican:holding_favored'}, OTHER),
        (r'(?:with|holding|using) (?:any|a) weapon that isn\'t your bound weapon|(?:with|holding) a weapon other than your bound weapon',
         lambda m: {'type': 'pipican:all_of', 'conditions': [
             {'type': 'pipican:any_of', 'conditions': [{'type': 'pipican:holding', 'items': '#pipican:weapons/melee'}, {'type': 'pipican:holding', 'items': '#pipican:weapons/ranged'}]},
             not_({'type': 'pipican:item_rule', 'rule': 'bound_weapon'})]}, OTHER),
        (r'(?:with|holding|using) (?:your |the )?bound weapon', lambda m: {'type': 'pipican:item_rule', 'rule': 'bound_weapon'}, OTHER),
        (r'(?:with|holding|using) (?:your |a )?sigil(?:-marked)? weapons?', lambda m: {'type': 'pipican:item_rule', 'rule': 'sigil_weapon'}, OTHER),
        (r'(?:with|wearing) (\d|one|two|three|four) sigil(?:-marked)? (?:armor )?pieces', lambda m: {'type': 'pipican:item_rule', 'rule': 'sigil_armor',
         'min': int(WORDS.get(m.group(1), m.group(1)))}, OTHER),
        (r'(?:while )?(?:a |holding a )?book(?:, writable book or enchanted book)? is in your off-hand|(?:with|holding) a book in your off-hand',
         lambda m: {'type': 'pipican:holding', 'items': ['minecraft:book', 'minecraft:writable_book', 'minecraft:enchanted_book'], 'hand': 'off'}, OTHER),
        (r'with no book anywhere in your inventory|without a book in your inventory', lambda m: not_({'type': 'pipican:carrying',
         'items': ['minecraft:book', 'minecraft:writable_book', 'minecraft:written_book', 'minecraft:enchanted_book']}), OTHER),
        (r'without (?:an|your) icon(?: of faith)?(?: in (?:hand|your off-hand))?', lambda m: not_({'type': 'pipican:holding', 'items': ['minecraft:totem_of_undying', 'minecraft:golden_sword', 'minecraft:golden_axe', 'minecraft:golden_pickaxe', 'minecraft:golden_shovel',
            'minecraft:golden_hoe', 'minecraft:golden_apple', 'minecraft:enchanted_golden_apple', 'minecraft:gold_ingot', 'minecraft:gold_nugget',
            'minecraft:golden_carrot', 'minecraft:clock'], 'hand': 'off'}), OTHER),
        (r'(?:while )?holding a totem_of_undying or golden item in the off-hand', lambda m: {'type': 'pipican:holding', 'items': [
            'minecraft:totem_of_undying', 'minecraft:golden_sword', 'minecraft:golden_axe', 'minecraft:golden_pickaxe', 'minecraft:golden_shovel',
            'minecraft:golden_hoe', 'minecraft:golden_apple', 'minecraft:enchanted_golden_apple', 'minecraft:gold_ingot', 'minecraft:gold_nugget',
            'minecraft:golden_carrot', 'minecraft:clock'], 'hand': 'off'}, OTHER),
        (r'(?:with |holding )?a banner in your off-hand|banner in off-hand', lambda m: {'type': 'pipican:holding', 'items': '#minecraft:banners', 'hand': 'off'}, OTHER),
        (r'holding a weapon in your off-hand', lambda m: {'type': 'pipican:holding', 'items': '#pipican:weapons/melee', 'hand': 'off'}, OTHER),
        (r'with (?:your )?staff(?: of magic)?|holding a staff', lambda m: {'type': 'pipican:holding', 'items': ['minecraft:blaze_rod', 'minecraft:stick']}, OTHER),
        (r'(?:while )?standing on solid ground|on solid ground', lambda m: state('on_ground'), OTHER),
        (r'(?:while )?(?:holding|with) a trident', lambda m: {'type': 'pipican:holding', 'items': '#pipican:weapons/tridents'}, OTHER),
        (r'(?:while )?wearing any copper armor(?: piece)?', lambda m: {'type': 'pipican:wearing', 'items': [
            'minecraft:copper_helmet', 'minecraft:copper_chestplate', 'minecraft:copper_leggings', 'minecraft:copper_boots']}, OTHER),
        (r'without an icon in hand', lambda m: not_({'type': 'pipican:holding', 'items': ICON, 'hand': 'off'}), OTHER),
        (r'(?:while )?holding your icon', lambda m: {'type': 'pipican:holding', 'items': ICON, 'hand': 'off'}, OTHER),
        (r'(?:when |while )?(?:more than|over) (\d+) blocks (?:away )?from your temple', lambda m: not_({'type': 'pipican:temple', 'radius': float(m.group(1))}), PLACE),
        (r'(?:inside|in|within) your temple(?: of faith)?', lambda m: {'type': 'pipican:temple'}, PLACE),
        (r'(?:while )?(?:unarmed|bare-handed|with (?:an )?empty hands?|with your fists)', lambda m: {'type': 'pipican:holding', 'items': ['minecraft:air'], 'hand': 'main'}, OTHER),
        (r'not holding your staff', lambda m: not_({'type': 'pipican:holding', 'items': ['minecraft:blaze_rod', 'minecraft:stick']}), OTHER),
        (r'holding your staff|with your staff', lambda m: {'type': 'pipican:holding', 'items': ['minecraft:blaze_rod', 'minecraft:stick']}, OTHER),
        (r'not sneaking', lambda m: not_(state('sneaking')), OTHER),
        (r'sneaking|crouching|you sneak|you crouch', lambda m: state('sneaking'), OTHER),
        (r'(?:on fire|burning)', lambda m: state('on_fire'), OTHER),
        # equipment
        (r'(?:without|with no) (?:a )?shield(?: in hand)?|not holding a shield', lambda m: not_({'type': 'pipican:holding', 'items': '#pipican:weapons/shields'}), OTHER),
        (r'(?:holding|with) (?:a )?shield(?: in hand)?|(?:a )?shield in hand|you hold a shield', lambda m: {'type': 'pipican:holding', 'items': '#pipican:weapons/shields'}, OTHER),
        (r'wearing (\d|one|two|three|four) or more (heavy|medium|light) armor pieces|wearing (?:at least )?(\d|one|two|three|four)\+? (?:pieces of )?(heavy|medium|light) armor(?: pieces?)?',
         lambda m: {'type': 'pipican:wearing', 'items': ARMOR_TAGS[m.group(2) or m.group(4)], 'min': int(WORDS.get(m.group(1) or m.group(3), m.group(1) or m.group(3)))}, OTHER),
        (r'not wearing leather(?: armor)?', lambda m: {'type': 'pipican:wearing', 'items': [
            'minecraft:leather_helmet', 'minecraft:leather_chestplate', 'minecraft:leather_leggings', 'minecraft:leather_boots'], 'min': 0, 'max': 0}, OTHER),
        (r'wearing leather(?: armor)?', lambda m: {'type': 'pipican:wearing', 'items': [
            'minecraft:leather_helmet', 'minecraft:leather_chestplate', 'minecraft:leather_leggings', 'minecraft:leather_boots']}, OTHER),
        (r'wearing (?:any )?(heavy|medium|light) armor', lambda m: {'type': 'pipican:wearing', 'items': ARMOR_TAGS[m.group(1)]}, OTHER),
        (r'wearing no medium or heavy armor', lambda m: not_({'type': 'pipican:any_of', 'conditions': [
            {'type': 'pipican:wearing', 'items': ARMOR_TAGS['medium']}, {'type': 'pipican:wearing', 'items': ARMOR_TAGS['heavy']}]}), OTHER),
        (r'wearing any medium or heavy armor', lambda m: {'type': 'pipican:any_of', 'conditions': [
            {'type': 'pipican:wearing', 'items': ARMOR_TAGS['medium']}, {'type': 'pipican:wearing', 'items': ARMOR_TAGS['heavy']}]}, OTHER),
        (r'(?:not wearing (?:any )?armor|wearing no armor|without armor|unarmored)', lambda m: {'type': 'pipican:wearing', 'items': '#pipican:armor/any', 'min': 0, 'max': 0}, OTHER),
        (r'your eidolon is destroyed|without your eidolon|your eidolon is dead', lambda m: not_({'type': 'pipican:has_eidolon'}), OTHER),
        # people & mobs around
        (r'no other players? (?:is |are )?within (\d+) blocks', lambda m: {'type': 'pipican:nearby', 'entities': 'players', 'radius': float(m.group(1)), 'min': 0, 'max': 0}, OTHER),
        (r'(?:another|other|an allied) players? (?:is |are )?within (\d+) blocks|(?:with )?an ally within (\d+) blocks|(?:near|close to) (?:another player|an ally|allies)',
         lambda m: {'type': 'pipican:nearby', 'entities': 'players', 'radius': float(m.group(1) or m.group(2) or 8)}, OTHER),
        (r'(?:a|any) (?:hostile )?mobs? (?:is |are )?within (\d+) blocks|hostile mobs? within (\d+) blocks',
         lambda m: {'type': 'pipican:nearby', 'entities': 'hostile_mobs', 'radius': float(m.group(1) or m.group(2))}, OTHER),
        (r'no hostile mobs? (?:is |are )?within (\d+) blocks', lambda m: {'type': 'pipican:nearby', 'entities': 'hostile_mobs', 'radius': float(m.group(1)), 'min': 0, 'max': 0}, OTHER),
        (r'(\d+)\+ hostile mobs (?:are )?within (\d+) blocks', lambda m: {'type': 'pipican:nearby', 'entities': 'hostile_mobs', 'radius': float(m.group(2)), 'min': int(m.group(1))}, OTHER),
        (r'(?:surrounded|outnumbered)', lambda m: {'type': 'pipican:nearby', 'entities': 'hostile_mobs', 'radius': 6.0, 'min': 3}, OTHER),
    ]


FILLER = re.compile(r"^(?:[\s,()]|\band\b|\bor\b|\bwhile\b|\bwhen\b|\bwhenever\b|\bas long as\b|\byou're\b|\byou are\b|\byou\b|"
                    r"\bare\b|\bis\b|\bstanding\b|\bbeing\b|\bboth\b|\beither\b|\bat all\b|\bin\b|\bthe\b|\bat\b|\bof\b|"
                    r"\bif\b|\bonly\b)*$", re.I)


def parse(text, ctx):
    t = ' ' + text.lower().strip().rstrip('.:,') + ' '
    found = []
    for pat, build, kind in _atoms(ctx):
        for m in re.finditer(r'(?<![\w-])(?:' + pat + r')(?![\w-])', t):
            if any(not (m.end() <= s or m.start() >= e) for s, e, _, _ in found):
                continue  # already covered by an earlier (more specific) atom
            found.append((m.start(), m.end(), build(m), kind))
    if not found:
        return None
    found.sort()
    rest = []
    pos = 0
    for s, e, _, _ in found:
        rest.append(t[pos:s])
        pos = e
    rest.append(t[pos:])
    if not FILLER.match(''.join(rest)):
        return None
    conds = [c for _, _, c, _ in found]
    if len(conds) == 1:
        return conds[0]
    joined = ''.join(rest)
    any_of = re.search(r'\bor\b', joined) or all(k == PLACE for _, _, _, k in found)
    return {'type': 'pipican:any_of' if any_of else 'pipican:all_of', 'conditions': conds}


# Where a condition phrase starts inside a bonus phrase ("+10% damage at night").
TRAILING = re.compile(
    r'\s+(?=(?:while|when|whenever|as long as|if)\b|at night\b|during\b|in daylight\b|by day\b|underground\b|underwater\b|'
    r'outdoors\b|under the open sky\b|with no sky access\b|without sky access\b|in (?:the )?(?:nether|end|overworld)\b|'
    r'in (?:the )?rain\b|in (?:rain|thunderstorms?|storms?)\b|in water\b|in rain or water\b|in (?:a )?(?:forests?|deserts?|jungles?|oceans?)\b|'
    r'in natural biomes?\b|in (?:snowy|cold|frozen) biomes?\b|in (?:darkness|the dark|low light|dim light|shadows?|bright light)\b|'
    r'in light level\b|below half health\b|below \d+% health\b|above half health\b|at full health\b|'
    r'with (?:your |the |a |any )?(?:bound weapon|sigil(?:-marked)? weapons?|staff|weapon that isn\'t your bound weapon)\b|with \d sigil\b|'
    r'with no book\b|while (?:a )?book\b|holding a weapon in your off-hand\b|'
    r'on land\b|with shield\b|'
    r'while unarmed\b|with your fists\b|bare-handed\b|'
    r'in combat\b|out of combat\b|inside your temple\b|in your temple\b|when more than \d+ blocks\b|'
    r'outside (?:of )?forests?\b|on solid ground\b|without an icon\b|while holding\b|while travelling\b|while wearing\b|while not\b|'
    r'in human form\b|in (?:any )?(?:animal )?form\b|while transformed\b|in water and air\b|'
    r'on foot\b|on (?:stone|dirt|sand|gravel|netherrack|nether blocks)\b|(?:standing|you stand) on\b|in lava\b|without (?:a )?shield\b|with (?:a )?shield\b|on dry land\b|out of (?:the )?water\b)', re.I)
