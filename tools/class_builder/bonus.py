"""pipican:bonus kinds (XP, potion durations, hunger, trade prices, draw/reload speed) and per-weapon attack speed."""
import re

N = r'(\d+(?:\.\d+)?)'

# "+10% <what>" (number first)
LEADING = [
    (r'(?:experience|xp)(?: gained| gain| from all sources)?', 'experience'),
    (r'(?:potion|potion effect) duration|duration of potions(?: you drink)?|potion effect length', 'potion_duration'),
    (r'trade prices?|villager prices', 'trade_price'),
    (r'(?:crossbow )?reload(?:ing)? speed', 'reload_speed'),
    (r'(?:bow )?draw speed', 'draw_speed'),
]

# "<potions...> last 20% longer/shorter"
POTIONS = [
    (r'(?:your |the )?(?:potions|potion effects)(?: you drink| on you)?', 'potion_duration'),
    (r'(?:your )?(?:harmful potions|harmful potion effects|potion debuffs|debuffs)(?: on you)?', 'harmful_duration'),
    (r'(?:beneficial|good) (?:potions|potion effects)(?: on you)?', 'beneficial_duration'),
    (r'harmful (?:potion effects|effects) you apply|harmful (?:splash )?potions you throw', 'applied_harmful_duration'),
    (r'(?:potion effects|effects) you apply|thrown splash(?:/lingering)? potions', 'applied_duration'),
]

WEAPON_SPEED = {'sword': '#pipican:weapons/swords', 'axe': '#pipican:weapons/axes', 'mace': '#pipican:weapons/maces',
                'spear': '#pipican:weapons/spears', 'trident': '#pipican:weapons/tridents'}


def _effect(kind, v, s):
    e = {'type': 'pipican:bonus', 'kind': kind, 'amount': round(v / 100, 6)}
    if s:
        e['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in s]
    return e


def leading(value, what, scal):
    """Number-led clause: value is the signed number, what the text after "%"."""
    for pat, kind in LEADING:
        if re.fullmatch(pat, what):
            v, s = scal(value)
            return [_effect(kind, v, s)]
    m = re.fullmatch(r'(sword|axe|mace|spear|trident) attack speed', what)
    if m:
        v, s = scal(value)
        attr = {'type': 'pipican:attribute', 'attribute': 'minecraft:attack_speed', 'amount': round(v / 100, 6), 'operation': 'add_multiplied_base'}
        if s:
            attr['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in s]
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:holding', 'items': WEAPON_SPEED[m.group(1)]}, 'effects': [attr]}]
    return None


def special(low, scal):
    """Clauses that don't start with the number."""
    for pat, kind in POTIONS:
        m = re.fullmatch(rf'(?:{pat}) last {N}% (longer|shorter)', low)
        if m:
            v, s = scal(float(m.group(1)) * (1 if m.group(2) == 'longer' else -1))
            return [_effect(kind, v, s)]
    m = re.fullmatch(rf'(?:you )?(?:lose|get) hunger {N}% (slower|faster)|(?:you )?get hungry {N}% (slower|faster)', low)
    if m:
        num, way = (m.group(1), m.group(2)) if m.group(1) else (m.group(3), m.group(4))
        v, s = scal(float(num) * (1 if way == 'faster' else -1))
        return [_effect('hunger', v, s)]
    m = re.fullmatch(rf'(reloading|drawing a bow|bow draw) is {N}% faster', low)
    if m:
        v, s = scal(float(m.group(2)))
        return [_effect('reload_speed' if m.group(1) == 'reloading' else 'draw_speed', v, s)]
    # "potion duration +0.5%" (after "per Flux" was split off as scaling)
    m = re.fullmatch(rf'(.+?) \+?([+-]?{N[1:-1]})%', low)
    if m:
        try:
            return leading(float(m.group(2)), m.group(1).strip(), scal)
        except ValueError:
            return None
    return None
