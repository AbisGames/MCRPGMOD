"""pipican:bonus kinds (XP, potion durations, hunger, trade prices, draw/reload speed) and per-weapon attack speed."""
import re

N = r'(\d+(?:\.\d+)?)'

# "+10% <what>" (number first)
LEADING = [
    (r'(?:experience|xp)(?: gained| gain| from all sources)?', 'experience'),
    (r'(?:potion|potion effect) duration|duration of potions(?: you drink)?|potion effect length', 'potion_duration'),
    (r'(?:villager )?trade prices?|villager prices', 'trade_price'),
    (r'(?:crossbow )?reload(?:ing)? speed', 'reload_speed'),
    (r'(?:bow )?draw speed', 'draw_speed'),
    (r'(?:natural )?(?:health )?regeneration|natural regen', 'natural_regen'),
    (r'hunger(?: drain)?|hunger loss|food drain', 'hunger'),
    (r'healing received(?: from every source)?|healing taken|incoming healing', 'healing_received'),
    (r'healing given|healing done|healing(?: you give)?', 'healing_given'),
    (r'affinity floor', 'affinity_floor'),
    (r'(?:mob )?detection range|detection range of mobs', 'detection_range'),
    (r'skill training speed|skill training|mastery gain|skill xp', 'skill_training'),
]

# "<potions...> last 20% longer/shorter"
POTIONS = [
    (r'(?:your |the )?(?:potions|potion effects)(?: you drink| on you)?', 'potion_duration'),
    (r'(?:your )?(?:harmful potions|harmful potion effects|harmful effects|potion debuffs|debuffs)(?: on you)?', 'harmful_duration'),
    (r'(?:beneficial|good) (?:potions|potion effects)(?: on you)?', 'beneficial_duration'),
    (r'harmful (?:potion effects|effects) you apply|harmful (?:splash )?potions you throw', 'applied_harmful_duration'),
    (r'(?:potion effects|effects) you apply|thrown splash(?:/lingering)? potions', 'applied_duration'),
]

WEAPON_SPEED = {'sword': '#pipican:weapons/swords', 'axe': '#pipican:weapons/axes', 'mace': '#pipican:weapons/maces',
                'spear': '#pipican:weapons/spears', 'trident': '#pipican:weapons/tridents'}


MOUNT_ATTRS = {'speed': 'minecraft:movement_speed', 'movement speed': 'minecraft:movement_speed', 'jump': 'minecraft:jump_strength',
               'jump strength': 'minecraft:jump_strength', 'jump height': 'minecraft:jump_strength', 'max health': 'minecraft:max_health',
               'health': 'minecraft:max_health', 'armor': 'minecraft:armor', 'armour': 'minecraft:armor'}


def _mount_or_attr(attr, amount, op, s, mount=True):
    e = {'type': 'pipican:mount' if mount else 'pipican:attribute', 'attribute': attr, 'amount': round(amount, 6)}
    if op != 'add_value':
        e['operation'] = op
    if s:
        e['scaling'] = [{'source': src, 'per': round(per / (100 if op != 'add_value' else 1), 6)} for src, per in s]
    return e


def mount_flat(low, scal):
    """Flat mount bonuses: "+2 mount max health", "mount +2 armor", "mount armor +1" (before "per 10 Horsemanship")."""
    m = re.fullmatch(rf'\+?{N} mount (max health|health|armor|armour)|(?:your )?mount (?:gets )?\+{N} (max health|health|armor|armour)|mount (max health|armor|armour) \+{N}', low)
    if not m:
        return None
    num = m.group(1) or m.group(3) or m.group(6)
    what = m.group(2) or m.group(4) or m.group(5)
    v, s = scal(float(num))
    return [_mount_or_attr(MOUNT_ATTRS[what], v, 'add_value', s)]


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
    m = re.fullmatch(r'(sprint speed|hunger from sprinting|sprinting hunger)', what)
    if m:
        v, s = scal(value)
        inner = _effect('hunger', v, s) if 'hunger' in what else _mount_or_attr('minecraft:movement_speed', v / 100, 'add_multiplied_base', s, mount=False)
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'sprinting'}, 'effects': [inner]}]
    m = re.fullmatch(r'mount (speed|movement speed|jump|jump strength|jump height|max health|health|armor)', what)
    if m:
        v, s = scal(value)
        attr = MOUNT_ATTRS[m.group(1)]
        return [_mount_or_attr(attr, v / 100, 'add_multiplied_base', s)]
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
    import named
    sm = named.summons(low, scal)
    if sm:
        return sm
    ch = chances(low, scal)
    if ch:
        return ch
    mf = mount_flat(low, scal)
    if mf:
        return mf
    m = re.fullmatch(r'immunity to (.+)', low)
    if m:
        import combat
        im = combat.immune('immune to ' + m.group(1))
        if im:
            return im
    if re.fullmatch(r'(?:saint: )?healing doubled', low):
        return [_effect('healing_given', 100, None)]
    m = re.fullmatch(rf'\+{N}% healing (?:you give and receive|done and received|given and received)', low)
    if m:
        v, s = scal(float(m.group(1)))
        return [_effect('healing_given', v, s), _effect('healing_received', v, s)]
    m = re.fullmatch(rf'\+{N} to every stat', low)
    if m:
        import effects as fx
        return [{'type': 'pipican:stat', 'stat': 'pipican:' + st, 'amount': int(float(m.group(1)))} for st in fx.CORE_STATS]
    m = re.fullmatch(r'no fall damage(?: below (\d+) blocks)?', low)
    if m:
        if m.group(1):
            return [{'type': 'pipican:attribute', 'attribute': 'minecraft:safe_fall_distance', 'amount': float(m.group(1)) - 3}]
        return [{'type': 'pipican:damage_taken', 'damage_types': '#minecraft:is_fall', 'multiplier': 0.0}]
    if re.fullmatch(r"(?:you )?can'?t sprint|you cannot sprint", low):
        return [{'type': 'pipican:bonus', 'kind': 'no_sprint', 'amount': 1.0}]
    if re.fullmatch(r'(?:knockback immunity|immune to knockback|immunity to knockback)', low):
        return [{'type': 'pipican:attribute', 'attribute': 'minecraft:knockback_resistance', 'amount': 1.0}]
    m = re.fullmatch(rf'(?:your )?skills train {N}% (faster|slower)', low)
    if m:
        v, s = scal(float(m.group(1)) * (1 if m.group(2) == 'faster' else -1))
        return [_effect('skill_training', v, s)]
    m = re.fullmatch(r'immune to (lightning|fire|fall|drowning|freezing|explosion|explosive)(?: damage)?', low)
    if m:
        import effects as fx
        tag = fx.DAMAGE_TAGS.get({'explosive': 'explosion'}.get(m.group(1), m.group(1)))
        if tag:
            return [{'type': 'pipican:damage_taken', 'damage_types': tag, 'multiplier': 0.0}]
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


# ---------------------------------------------------------------- resource bar filling / spending

def _on(event, actions, **extra):
    return dict({'type': 'pipican:on', 'event': event, 'actions': actions}, **extra)


def _fill(amount=None, percent=None):
    a = {'type': 'pipican:resource'}
    if amount is not None:
        a['amount'] = float(amount)
    if percent is not None:
        a['percent'] = float(percent)
    return a


HITS = {'': 'hit', 'melee ': 'melee_hit', 'ranged ': 'ranged_hit', 'arrow ': 'ranged_hit'}


def _spend_outcome(t):
    """What spending does: "gain Speed II for 3s", "heal 2 HP", "mobs within 8 blocks get Weakness I for 4s"."""
    import combat
    t = t.strip().rstrip('.')
    m = re.fullmatch(r'(?:you )?heals? (?:you )?(?:for )?(\d+(?:\.\d+)?) ?(?:hp|health)', t)
    if m:
        return [{'type': 'pipican:heal', 'target': 'self', 'amount': float(m.group(1))}]
    m = re.fullmatch(r'(?:all )?(?:hostile )?mobs within (\d+) blocks (?:get|are given|receive) (.+)', t)
    if m:
        inner = combat.outcome('give ' + m.group(2), 'victim')
        if inner and len(inner) == 1 and inner[0]['type'] == 'pipican:effect':
            return [dict(inner[0], target='hostiles', radius=float(m.group(1)))]
        return None
    m = re.fullmatch(rf'your next {N} (melee )?hits deal \+{N}% damage', t)
    if m:
        return [{'type': 'pipican:empower', 'target': 'self', 'multiplier': round(1 + float(m.group(3)) / 100, 6), 'hits': int(float(m.group(1))), 'seconds': 15.0}]
    m = re.fullmatch(r'a shockwave hits (?:all )?mobs within (\d+) blocks for (\d+(?:\.\d+)?) damage', t)
    if m:
        return [{'type': 'pipican:damage', 'target': 'hostiles', 'radius': float(m.group(1)), 'amount': float(m.group(2))}]
    out = combat.outcome(t, 'self')
    if out and all(a['type'] == 'pipican:effect' and a.get('target') == 'self' for a in out):
        return out
    return None


def resource(low, ctx):
    """Whole-segment resource phrases: filling from hits/kills/damage/sleep, spending triggers, fill speed, ability cost."""
    rn = (ctx.get('resource_name') or '').lower()
    t = low.strip().rstrip('.')
    if rn:
        # "Mystic Flux" may be written as just "Flux".
        r = '(?:' + re.escape(rn) + ('|' + re.escape(rn.split()[-1]) if ' ' in rn else '') + ')'
        m = re.fullmatch(rf'(?:your )?(|melee |ranged |arrow )(?:hits|attacks) (?:refill|refills|restore|restores|give|gives you|add|adds) {N} {r}', t)
        if m:
            return [_on(HITS[m.group(1)], [_fill(m.group(2))])]
        m = re.fullmatch(rf'{r} (?:fills|refills) by {N} (?:every time you take damage|whenever you take damage|per hit taken|when you are hit)', t)
        if m:
            return [_on('hurt', [_fill(m.group(1))])]
        m = re.fullmatch(rf'(?:every|each) kill (?:refills|restores|gives you|gives) {N} {r}|killing a mob (?:refills|restores) {N} {r}', t)
        if m:
            return [_on('kill', [_fill(m.group(1) or m.group(2))])]
        m = re.fullmatch(rf'{r} (?:refills?|is refilled) (?:completely|fully) (?:when|whenever|after) you sleep', t)
        if m:
            return [_on('wake', [_fill(percent=1)])]
        m = re.fullmatch(rf'{r} fills {N}% (faster|slower)|{r} fills (twice|three times) as fast', t)
        if m:
            v = float(m.group(1)) * (1 if m.group(2) == 'faster' else -1) if m.group(1) else {'twice': 100, 'three times': 200}[m.group(3)]
            return [_effect('resource_gain', v, None)]
        m = re.fullmatch(rf'(?:whenever|when|every time|each time) you spend {r},? (.+)', t) or re.fullmatch(rf'after spending {r},? (.+)', t)
        if m:
            out = _spend_outcome(m.group(1))
            if out:
                return [_on('spend', out)]
        m = re.fullmatch(rf'abilities cost {N}% (less|more) {r}', t)
        if m:
            return [_effect('ability_cost', float(m.group(1)) * (-1 if m.group(2) == 'less' else 1), None)]
    m = re.fullmatch(rf'(?:villager )?trades cost {N}% (less|more)', t)
    if m:
        return [_effect('trade_price', float(m.group(1)) * (-1 if m.group(2) == 'less' else 1), None)]
    m = re.fullmatch(rf'natural (?:health )?regeneration is {N}% (faster|slower)', t)
    if m:
        return [_effect('natural_regen', float(m.group(1)) * (1 if m.group(2) == 'faster' else -1), None)]
    return None


def fill_rules(note):
    """The resource's "starts full" note: "fills by 2 per hit taken and 1 per hit dealt, drains 0.5/s out of combat"."""
    note = (note or '').lower()
    out = []
    for n, what in re.findall(rf'{N} per (hit taken|hit dealt|kill)', note):
        out.append(_on({'hit taken': 'hurt', 'hit dealt': 'hit', 'kill': 'kill'}[what], [_fill(n)]))
    return out, 'out of combat' in note


# ---------------------------------------------------------------- short boosts after something happens

AFTER = [
    (r'after (?:you take damage|taking damage|being hit|you are hit|you get hit)|when you take damage|whenever you take damage', 'hurt'),
    (r'after (?:hitting a mob|you hit a mob|hitting something|each hit|a hit)|when you hit a mob', 'hit'),
    (r'after (?:a kill|killing a mob|you kill a mob|each kill)|when you kill a mob', 'kill'),
    (r'after blocking(?: an attack| a hit)?(?: with a shield)?', 'block'),
]


def _boost(inner, seconds, ctx):
    import effects as fx
    eff, uns = fx.translate(inner, dict(ctx, implicit_per_point=False))
    if not eff or uns:
        return None
    return {'type': 'pipican:boost', 'seconds': float(seconds), 'effects': eff}


def boost(t, ctx):
    """"+15% movement speed for 2s after you take damage", "after you take damage, +15% speed for 2s",
    "whenever you spend Ward, gain +3 armor for 10s"."""
    for pat, event in AFTER:
        m = re.fullmatch(rf'(.+?) for {N}\s?s(?:econds?)? (?:{pat})', t) or re.fullmatch(rf'(?:{pat}),? (?:gain |get |you gain |you get )?(.+?) for {N}\s?s(?:econds?)?', t)
        if m:
            b = _boost(m.group(1), m.group(2), ctx)
            if b:
                return [_on(event, [b])]
    rn = (ctx.get('resource_name') or '').lower()
    if rn:
        r = '(?:' + re.escape(rn) + ('|' + re.escape(rn.split()[-1]) if ' ' in rn else '') + ')'
        m = re.fullmatch(rf'(?:whenever|when|every time|each time) you spend {r},? (?:gain |get |you gain |you get )?(.+?) for {N}\s?s(?:econds?)?', t)
        if m:
            b = _boost(m.group(1), m.group(2), ctx)
            if b:
                return [_on('spend', [b])]
    return None


def mount_subject(t):
    """"your mount gets +4 armor and +10% speed": every part goes to the mount, or nothing applies.
    Returns effects, 'unsupported', or None if the line isn't about the mount."""
    m = re.fullmatch(r'(?:your )?mount (?:gets|has|gains) (.+)', t)
    if not m:
        return None
    out = []
    for part in re.split(r',\s*(?:and\s+)?|\s+and\s+', m.group(1)):
        pm = re.fullmatch(rf'\+?{N}(%?) (speed|movement speed|jump|jump strength|jump height|max health|health|armor|armour)', part.strip())
        if not pm:
            return 'unsupported'
        pct = pm.group(2) == '%'
        if not pct and pm.group(3) not in ('max health', 'health', 'armor', 'armour'):
            return 'unsupported'
        out.append(_mount_or_attr(MOUNT_ATTRS[pm.group(3)], float(pm.group(1)) / (100 if pct else 1),
                                  'add_multiplied_base' if pct else 'add_value', None))
    return out


# ---------------------------------------------------------------- bleeding, reaction chances, knockback, extra drops

SWORDS = '#pipican:weapons/swords'
CHANCE_NAMES = {'ignite': 'ignite', 'fire': 'ignite', 'burn': 'ignite', 'poison': 'poison', 'wither': 'wither',
                'blindness': 'blindness', 'blind': 'blindness', 'charm': 'pacify', 'lightning': 'lightning',
                'slow': 'slowness', 'slowness': 'slowness', 'weakness': 'weakness', 'nausea': 'nausea', 'zeal': 'zeal', 'curse': 'curse', 'decoy': 'decoy', 'seize chaos': 'seize chaos',
                'dirty fighting': 'dirty fighting'}


def _bleed(**kw):
    return dict({'type': 'pipican:bleed'}, **kw)


EFFECT_WORDS = {'poison': 'poison', 'slowness': 'slowness', 'weakness': 'weakness', 'wither': 'wither', 'blindness': 'blindness',
                'nausea': 'nausea', 'hunger': 'hunger', 'mining fatigue': 'mining_fatigue', 'glowing': 'glowing'}
LEVELS = {'i': 0, 'ii': 1, 'iii': 2, 'iv': 3}


def _applied(effect, **kw):
    e = {'type': 'pipican:applied_effect', 'effect': 'minecraft:' + EFFECT_WORDS[effect]}
    e.update(kw)
    return [e]


def _effect_ids(text):
    """"poison, hunger and slowness" / "blindness/darkness" / "invisibility potions" -> effect ids, or None."""
    import combat
    out = []
    for part in re.split(r',\s*(?:and\s+)?|\s+and\s+|/', re.sub(r'\s+(?:potions?|effects?)$', '', text.strip())):
        part = re.sub(r'\s+(?:potions?|effects?)$', '', part.strip())
        if part == 'hero of the village':
            out.append('minecraft:hero_of_the_village')
        elif part in combat.EFFECT_NAMES:
            out.append('minecraft:' + combat.EFFECT_NAMES[part])
        else:
            return None
    return out or None


def _duration(ids, k):
    return [{'type': 'pipican:effect_duration', 'effects': ids if len(ids) > 1 else ids[0], 'multiplier': round(k, 6)}]


def misc(t, scal):
    """Projectile, armor-piercing, food and applied-effect lines."""
    eff = '|'.join(EFFECT_WORDS)
    mf = mount_flat(t, scal)
    if mf:
        return mf
    # effect durations on you
    m = re.fullmatch(r'(.+?) lasts? (twice|three times|half) as long(?: on you)?', t)
    if m and (ids := _effect_ids(m.group(1))):
        return _duration(ids, {'twice': 2, 'three times': 3, 'half': 0.5}[m.group(2)])
    m = re.fullmatch(rf'(.+?) wears? off {N}% (faster|slower)(?: on you)?', t)
    if m and (ids := _effect_ids(m.group(1))):
        k = 1 + float(m.group(2)) / 100
        return _duration(ids, 1 / k if m.group(3) == 'faster' else k)
    m = re.fullmatch(r'(?:you )?lose (.+?) (twice|three times) as fast', t)
    if m and (ids := _effect_ids(m.group(1))):
        return _duration(ids, 0.5 if m.group(2) == 'twice' else 1 / 3)
    # mobs that leave you alone
    import combat
    m = re.fullmatch(r'(.+?) (?:mobs )?(?:ignore you|never attack you|never target you)( unless you attack them)?', t)
    if m and m.group(1) not in ('mobs', 'hostile mobs') and (group := combat.mobs(m.group(1))) is not None:
        e = {'type': 'pipican:ignored_by', 'mobs': group}
        if m.group(2):
            e['unless_attacked'] = True
        return [e]
    m = re.fullmatch(r'(.+?) never attack you while you hold gold', t)
    if m and (group := combat.mobs(m.group(1))) is not None:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:holding', 'items': '#minecraft:piglin_loved'},
                 'effects': [{'type': 'pipican:ignored_by', 'mobs': group}]}]
    if re.fullmatch(r"(?:you )?can'?t eat while in combat", t):
        return [_effect('no_eating_in_combat', 100, None)]
    m = re.fullmatch(rf'([+-]{N})% anvil costs?(?: for you)?|anvil costs (halved)(?: for you)?', t)
    if m:
        v, s = scal(float(m.group(1)) if m.group(1) else -50.0)
        return [_effect('anvil_cost', v, s)]
    m = re.fullmatch(rf'anvil (?:repairs and renames|uses|repairs) cost {N} fewer levels?', t)
    if m:
        return [_effect('anvil_levels', float(m.group(1)) * 100, None)]
    m = re.fullmatch(rf'enchanting(?: at a table)? costs {N} fewer levels?(?: and {N} fewer lapis)?', t)
    if m:
        v, sc = scal(float(m.group(1)) * 100)  # whole levels; _effect divides by 100
        out = [_effect('enchant_levels', v, sc)]
        if m.group(2):
            if sc:
                return None
            out.append(_effect('enchant_lapis', float(m.group(2)) * 100, None))
        return out
    m = re.fullmatch(rf'\+{N} (?:more |extra )?looting levels?(?: on all kills)?', t)
    if m and float(m.group(1)) == int(float(m.group(1))):
        return [_effect('looting', float(m.group(1)) * 100, None)]
    m = re.fullmatch(rf'(?:all )?potions you drink last {N}% (longer|shorter)', t)
    if m:
        v, sc = scal(float(m.group(1)) * (1 if m.group(2) == 'longer' else -1))
        return [_effect('drunk_potion_duration', v, sc)]
    simple = [
        (rf'\+{N}% bone ?meal effect', 'bone_meal'), (rf'\+{N}% breeding speed', 'breeding_speed'),
        (rf'\+{N}% (?:ender )?pearl range', 'pearl_range'),
    ]
    for pat, kind in simple:
        m = re.fullmatch(pat, t)
        if m:
            v, sc = scal(float(m.group(1)))
            return [_effect(kind, v, sc)]
    m = re.fullmatch(rf'(?:your )?arrows pierce {N} (?:more )?mobs?', t)
    if m:
        return [_effect('arrow_pierce', float(m.group(1)) * 100, None)]
    m = re.fullmatch(rf'any weapon(?:/tool| or tool)? does at least {N} damage', t)
    if m:
        return [_effect('min_melee_damage', float(m.group(1)) * 100, None)]
    if re.fullmatch(r"you can'?t sleep(?: in beds)?|you can never sleep", t):
        return [_effect('no_sleep', 100, None)]
    m = re.fullmatch(r'eating cooked meat gives (.+)', t)
    if m:
        import combat
        out = combat.outcome('gain ' + m.group(1), 'self')
        if out:
            return [{'type': 'pipican:on', 'event': 'eat', 'weapons': '#pipican:food/cooked_meat', 'actions': out}]
    m = re.fullmatch(r'(?:your )?mount gets (.+?) while you ride', t)
    if m:
        import combat
        out = combat.outcome('give ' + m.group(1), 'mount')
        if out and out[0].get('type') == 'pipican:effect':
            return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'mounted'},
                     'effects': [{'type': 'pipican:on', 'event': 'second', 'actions': out}]}]
    m = re.fullmatch(rf'(?:minions?|servants?|summons?) \+{N} max health', t)
    if m:
        return [{'type': 'pipican:summon_mod', 'hearts': round(float(m.group(1)) / 2, 6)}]
    m = re.fullmatch(rf'\+{N}% oxygen', t)
    if m:
        v, sc = scal(float(m.group(1)))
        e = {'type': 'pipican:attribute', 'attribute': 'minecraft:oxygen_bonus', 'amount': round(v / 100, 6)}
        if sc:
            e['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in sc]
        return [e]
    if re.fullmatch(r'no mining slowdown underwater', t):
        return [{'type': 'pipican:attribute', 'attribute': 'minecraft:submerged_mining_speed', 'amount': 0.8}]
    if re.fullmatch(r'(?:you )?breathe underwater', t):
        return [{'type': 'pipican:mob_effect', 'effect': 'minecraft:water_breathing'}]
    m = re.fullmatch(rf'swim {N}% faster', t)
    if m:
        return [{'type': 'pipican:attribute', 'attribute': 'minecraft:water_movement_efficiency', 'amount': round(float(m.group(1)) / 100, 6)}]
    m = re.fullmatch(r'(.+?) are neutral to you', t)
    if m:
        group = combat.mobs(m.group(1))
        if group is not None:
            return [{'type': 'pipican:ignored_by', 'mobs': group, 'unless_attacked': True}]
    m = re.fullmatch(rf'(?:your )?(?:ender )?pearl cooldown ([+-]{N})%|([+-]{N})% (?:ender )?pearl cooldown', t)
    if m:
        v, sc = scal(float(m.group(1) or m.group(3)))
        return [_effect('pearl_cooldown', v, sc)]
    m = re.fullmatch(rf'ender pearls deal no damage to you(?: and have {N}% shorter cooldown)?', t)
    if m:
        out = [{'type': 'pipican:damage_taken', 'damage_types': '#pipican:is_ender_pearl', 'multiplier': 0.0}]
        if m.group(1):
            out.append(_effect('pearl_cooldown', -float(m.group(1)), None))
        return out
    m = re.fullmatch(rf'(?:your )?(.+?) effects last {N}% (longer|shorter)', t)
    if m and (ids := _effect_ids(m.group(1))):
        return _duration(ids, 1 + float(m.group(2)) / 100 * (1 if m.group(3) == 'longer' else -1))
    m = re.fullmatch(rf'(?:your )?potion debuffs last {N}% (longer|shorter)', t)
    if m:
        return [_effect('harmful_duration', float(m.group(1)) * (1 if m.group(2) == 'longer' else -1), None)]
    m = re.fullmatch(rf'potion effects on you tick down {N}% slower(?: \(good ones last longer\))?', t)
    if m:
        k = float(m.group(1)) / 100
        return [_effect('potion_duration', round(100 * (1 / (1 - k) - 1), 4), None)]
    if re.fullmatch(r'neutral mobs never turn hostile on you', t):
        return [{'type': 'pipican:ignored_by', 'mobs': '#pipican:mobs/neutral', 'unless_attacked': True}]
    if re.fullmatch(r"(?:also )?immune to wind charges and explosions'? knockback", t):
        return [{'type': 'pipican:attribute', 'attribute': 'minecraft:explosion_knockback_resistance', 'amount': 1.0}]
    if re.fullmatch(r'critical hits deal maximum damage \(\+(\d+)% critical damage\)', t):
        return [{'type': 'pipican:critical', 'damage': round(float(re.search(r'\d+', t).group()) / 100, 6)}]
    m = re.fullmatch(rf'(?:you )?takes? {N}% more damage from the first hit of every fight', t)
    if m:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:not', 'condition': {'type': 'pipican:state', 'state': 'in_combat'}},
                 'effects': [{'type': 'pipican:damage_taken', 'multiplier': round(1 + float(m.group(1)) / 100, 6)}]}]
    m = re.fullmatch(rf'sprint-hits stun (?:for )?{N}s', t)
    if m:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'sprinting'}, 'effects': [
            {'type': 'pipican:on', 'event': 'melee_hit', 'actions': [{'type': 'pipican:effect', 'target': 'victim', 'effects': [
                {'effect': 'minecraft:slowness', 'seconds': float(m.group(1)), 'amplifier': 9}]}]}]}]
    if re.fullmatch(r'no hunger from sprinting', t):
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'sprinting'}, 'effects': [_effect('hunger', -100, None)]}]
    m = re.fullmatch(rf"you can'?t hold a melee weapon in your off-hand or main hand without -{N}%(?: damage)?", t)
    if m:
        when = {'type': 'pipican:any_of', 'conditions': [{'type': 'pipican:holding', 'items': '#pipican:weapons/melee', 'hand': 'main'},
                                                         {'type': 'pipican:holding', 'items': '#pipican:weapons/melee', 'hand': 'off'}]}
        return [{'type': 'pipican:conditional', 'when': when, 'effects': [{'type': 'pipican:damage_dealt', 'multiplier': round(1 - float(m.group(1)) / 100, 6)}]}]
    m = re.fullmatch(rf"you can'?t hold any other weapon in your main hand without -{N}%(?: damage)?", t)
    if m:
        when = {'type': 'pipican:all_of', 'conditions': [
            {'type': 'pipican:any_of', 'conditions': [{'type': 'pipican:holding', 'items': '#pipican:weapons/melee'}, {'type': 'pipican:holding', 'items': '#pipican:weapons/ranged'}]},
            {'type': 'pipican:not', 'condition': {'type': 'pipican:item_rule', 'rule': 'bound_weapon'}}]}
        return [{'type': 'pipican:conditional', 'when': when, 'effects': [{'type': 'pipican:damage_dealt', 'multiplier': round(1 - float(m.group(1)) / 100, 6)}]}]
    m = re.fullmatch(rf'(?:your )?(?:marked targets?|prey|challenged foes?)(?: within (your reach|{N} blocks))? deals? {N}% less damage(?: to you)?', t)
    if m:
        victim = {'marked': True}
        if m.group(1):
            victim['within'] = 4.5 if m.group(1) == 'your reach' else float(m.group(2))
        return [{'type': 'pipican:damage_taken', 'victim': victim, 'multiplier': round(1 - float(m.group(3)) / 100, 6)}]
    m = re.fullmatch(rf'(?:your )?first trade with (?:a new|each) villager is {N}% off', t)
    if m:
        return [_effect('first_trade_discount', float(m.group(1)), None)]
    if re.fullmatch(r"villagers (?:won'?t|refuse to|will not) trade with you", t):
        return [_effect('no_trade', 100, None)]
    if re.fullmatch(r'(?:you are |you\'re )?immune to your own explosions', t):
        return [_effect('self_blast_immune', 100, None)]
    import triggers as trg
    m = re.fullmatch(rf'\+?{N}% chance (?:that )?a crafted redstone item is doubled', t)
    if m:
        v, sc = scal(float(m.group(1)))
        e = {'type': 'pipican:craft_bonus', 'items': trg.REDSTONE, 'chance': round(v / 100, 6)}
        if sc:
            e['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in sc]
        return [e]
    m = re.fullmatch(rf'\+?{N}% chance(?: per ([a-z]+) point)? to get an extra output item when crafting arrows, rails or redstone components', t)
    if m:
        v, sc = scal(float(m.group(1)))
        e = {'type': 'pipican:craft_bonus', 'items': ['minecraft:arrow'] + trg.RAILS + trg.REDSTONE, 'chance': round(v / 100, 6)}
        if m.group(2):
            e['chance'] = 0.0
            e['scaling'] = [{'source': 'special_stat', 'per': round(float(m.group(1)) / 100, 6)}]
        elif sc:
            e['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in sc]
        return [e]
    if re.fullmatch(r'crafting tipped arrows gives double', t):
        return [{'type': 'pipican:craft_bonus', 'items': 'minecraft:tipped_arrow', 'chance': 1.0}]
    m = re.fullmatch(rf'potions you brew last {N}% longer', t)
    if m:
        v, sc = scal(float(m.group(1)))
        return [_effect('brewed_duration', v, sc)]
    m = re.fullmatch(rf'\+{N}% duration of potions you brew and drink', t)
    if m:
        v, sc = scal(float(m.group(1)))
        return [_effect('brewed_duration', v, sc), _effect('drunk_potion_duration', v, sc)]
    m = re.fullmatch(rf'eating berries(?:/| and | or )crops restores \+{N} hunger', t)
    if m:
        foods = ['minecraft:sweet_berries', 'minecraft:glow_berries', 'minecraft:carrot', 'minecraft:potato', 'minecraft:baked_potato',
                 'minecraft:beetroot', 'minecraft:melon_slice']
        return [{'type': 'pipican:on', 'event': 'eat', 'weapons': foods, 'actions': [{'type': 'pipican:feed', 'hunger': int(float(m.group(1)))}]}]
    m = re.fullmatch(rf'crops grow {N}% faster near you', t)
    if m:
        v, sc = scal(float(m.group(1)))
        return [_effect('crop_growth', v, sc)]
    m = re.fullmatch(rf'(?:scrutinised|scrutinized|marked|challenged) (?:mobs|targets|foes) drop \+{N} loot', t)
    if m:
        return [_effect('marked_loot', float(m.group(1)) * 100, None)]
    if re.fullmatch(r"(?:your )?shields can'?t be disabled", t):
        return [_effect('shield_disable', -100, None)]
    m = re.fullmatch(rf'shields recover from axe-disable {N}% faster(?: and block 100% of melee damage from the front)?', t)
    if m:
        return [_effect('shield_disable', -float(m.group(1)), None)]
    if re.fullmatch(r'no footstep sounds|your footsteps make no sound', t):
        return [_effect('quiet_steps', 100, None)]
    if re.fullmatch(r'no nausea after teleporting', t):
        return [{'type': 'pipican:immune', 'effects': 'minecraft:nausea'}]
    m = re.fullmatch(rf'\+{N}% healing (?:you give and receive|done and received|given and received)', t)
    if m:
        v, sc = scal(float(m.group(1)))
        return [_effect('healing_given', v, sc), _effect('healing_received', v, sc)]
    m = re.fullmatch(rf'your ({eff}) is ({eff}) (ii|iii|iv)', t)
    if m and m.group(1) == m.group(2):
        return _applied(m.group(1), min_amplifier=LEVELS[m.group(3)])
    m = re.fullmatch(rf'\+{N}% durability on crafted (?:tools|tools and weapons|gear)', t)
    if m:
        v, sc = scal(float(m.group(1)))
        return [_effect('crafted_durability', v, sc)]
    if re.fullmatch(r'sunlight burns you like a zombie', t):
        return [_effect('sun_burn', 100, None)]
    if re.fullmatch(r'you can no longer benefit from beacons', t):
        return [_effect('no_beacon', 100, None)]
    if re.fullmatch(r'you never need sleep to skip phantoms(?: \(no phantoms spawn for you\))?|no phantoms spawn for you', t):
        return [_effect('no_phantoms', 100, None)]
    m = re.fullmatch(rf'(?:enchanting at a table gives a )?\+?{N}% (?:extra-enchantment chance|chance of an extra enchantment on the item)', t)
    if m:
        v, sc = scal(float(m.group(1)))
        return [_effect('extra_enchant', v, sc)]
    m = re.fullmatch(rf'enchanting at a table gives a {N}% chance of an extra enchantment on the item', t)
    if m:
        return [_effect('extra_enchant', float(m.group(1)), None)]
    m = re.fullmatch(rf'(?:your )?weapon hits deal {N}% of their damage as magic damage(?: \(ignores armor\))?', t)
    if m:
        return [_effect('magic_portion', float(m.group(1)), None)]
    m = re.fullmatch(rf'\+{N}% (?:magic portion|magic damage portion)', t)
    if m:
        return [_effect('magic_portion', float(m.group(1)), None)]
    m = re.fullmatch(rf'-?{N} enchanting cost', t)
    if m:
        return [_effect('enchant_levels', float(m.group(1)) * 100, None)]
    m = re.fullmatch(rf'(?:\+{N}% (?:more )?weapon resistance|weapon resistance \+{N}%)', t)
    if m:
        v, sc = scal(float(m.group(1) or m.group(2)))
        out = []
        for tag in ('#pipican:is_melee', '#minecraft:is_projectile'):
            e = {'type': 'pipican:damage_taken', 'damage_types': tag, 'multiplier': round(1 - v / 100, 6)}
            if sc:
                e['scaling'] = [{'source': src, 'per': round(-per / 100, 6)} for src, per in sc]
            out.append(e)
        return out
    m = re.fullmatch(rf'\+{N}% optimization amount|optimization amount \+{N}|\+{N} optimization amount', t)
    if m:
        v, sc = scal(float(m.group(1) or m.group(2) or m.group(3)))
        e = {'type': 'pipican:optimization', 'amount': round(v, 6)}
        if sc:
            e['amount'] = 0.0
            e['scaling'] = [{'source': src, 'per': round(per, 6)} for src, per in sc]
        return [e]
    if re.fullmatch(r'optimization amount doubled', t):
        return [{'type': 'pipican:optimization', 'amount': 3.0}]
    m = re.fullmatch(rf'detection reduction \+{N}%', t)
    if m:
        v, sc = scal(-float(m.group(1)))
        return [_effect('detection_range', v, sc)]
    m = re.fullmatch(rf"-{N}% damage with every weapon type that isn'?t your favou?red one", t)
    if m:
        when = {'type': 'pipican:all_of', 'conditions': [
            {'type': 'pipican:any_of', 'conditions': [{'type': 'pipican:holding', 'items': '#pipican:weapons/melee'}, {'type': 'pipican:holding', 'items': '#pipican:weapons/ranged'}]},
            {'type': 'pipican:not', 'condition': {'type': 'pipican:holding_favored'}}]}
        return [{'type': 'pipican:conditional', 'when': when, 'effects': [{'type': 'pipican:damage_dealt', 'multiplier': round(1 - float(m.group(1)) / 100, 6)}]}]
    m = re.fullmatch(r'(?:servants|minions|summons) never expire(?: until killed)?', t)
    if m:
        return [{'type': 'pipican:summon_mod', 'seconds': 86400.0}]
    m = re.fullmatch(rf"you never naturally regenerate above half health", t)
    if m:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:health', 'min': 0.5}, 'effects': [_effect('natural_regen', -100, None)]}]
    if re.fullmatch(r"(?:you'?re |you are )?immune to your own chaos backfires", t):
        return [_effect('self_blast_immune', 100, None)]
    if re.fullmatch(r'anvils never break when you use them', t):
        return [_effect('anvil_safe', 100, None)]
    # sprint hits
    m = re.fullmatch(rf'sprint-hits deal \+{N}% damage( and knock mobs back)?|sprint-hit bonus \+{N}%()', t)
    if m:
        v, s = scal(float(m.group(1) or m.group(3)))
        d = {'type': 'pipican:damage_dealt', 'damage_types': '#pipican:is_melee', 'multiplier': round(1 + v / 100, 6)}
        if s:
            d['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in s]
        inner = [d]
        if m.group(2):
            inner.append(_effect('knockback', 100, None))
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'sprinting'}, 'effects': inner}]
    m = re.fullmatch(rf"your first hit on a mob that isn'?t targeting you deals \+{N}% damage", t)
    if m:
        return [{'type': 'pipican:damage_dealt', 'victim': {'not_targeting_you': True}, 'multiplier': round(1 + float(m.group(1)) / 100, 6)}]
    if re.fullmatch(r'(?:you )?never trample farmland', t):
        return [_effect('no_trample', 100, None)]
    if re.fullmatch(r'rotten flesh, spider eyes and poisonous potatoes give no bad effects|food never gives you (?:bad|harmful) effects', t):
        return [_effect('food_safe', 100, None)]
    if re.fullmatch(r"you can'?t be hit by witch(?:es)?'? potions|you take no damage from witch potions(?: thrown by witches)?", t):
        return [_effect('witch_ward', 100, None)]
    m = re.fullmatch(rf'(?:(sword|axe|mace|spear|trident)s: )?(?:your )?(?:hits|attacks) ignore {N}% (?:of )?(?:the target\'s )?armou?r', t)
    if m:
        v, s = scal(float(m.group(2)))
        e = _effect('armor_pierce', v, s)
        if m.group(1):
            return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:holding', 'items': WEAPON_SPEED[m.group(1)]}, 'effects': [e]}]
        return [e]
    m = re.fullmatch(rf'(?:your )?arrows ignore {N}% (?:of )?armou?r(?: and fly {N}% faster)?', t)
    if m:
        out = [_effect('ranged_armor_pierce', float(m.group(1)), None)]
        if m.group(2):
            out.append(_effect('projectile_speed', float(m.group(2)), None))
        return out
    m = re.fullmatch(rf'\+?{N}% (?:arrow speed|arrow velocity)|(?:your )?arrows fly {N}% faster', t)
    if m:
        v, s = scal(float(m.group(1) or m.group(2)))
        return [_effect('projectile_speed', v, s)]
    m = re.fullmatch(rf'\+?{N}% chance (?:that )?a (?:bow )?shot fires a second arrow', t)
    if m:
        v, s = scal(float(m.group(1)))
        return [_effect('extra_arrow', v, s)]
    m = re.fullmatch(rf'\+?{N}% damage on hits to the upper part of a mob|\+?{N}% headshot damage', t)
    if m:
        v = float(m.group(1) or m.group(2))
        return [{'type': 'pipican:damage_dealt', 'victim': {'upper_hit': True}, 'multiplier': round(1 + v / 100, 6)}]
    # applied effects
    m = re.fullmatch(rf'(?:your |the )?({eff}) (?:you apply |applied by you )?also gives ({eff}) (i|ii|iii)', t) or \
        re.fullmatch(rf'({eff.replace("poison", "poisoned").replace("wither", "withered")}) mobs also get ({eff}) (i|ii|iii)', t)
    if m:
        src = {'poisoned': 'poison', 'withered': 'wither'}.get(m.group(1), m.group(1))
        if src not in EFFECT_WORDS:
            return None
        return _applied(src, also={'minecraft:' + EFFECT_WORDS[m.group(2)]: LEVELS[m.group(3)]})
    m = re.fullmatch(rf'(?:({eff}) (?:applied by you|you apply) is ({eff}) (ii|iii|iv)|({eff}) (ii|iii|iv) always)', t)
    if m:
        a, b, lv = (m.group(1), m.group(2), m.group(3)) if m.group(1) else (m.group(4), m.group(4), m.group(5))
        if a != b:
            return None
        return _applied(a, min_amplifier=LEVELS[lv])
    m = re.fullmatch(rf'\+?{N}s ({eff}) duration(?: you apply)?|({eff}) duration \+?{N}s', t)
    if m:
        num, what = (m.group(1), m.group(2)) if m.group(1) else (m.group(4), m.group(3))
        v, s = scal(float(num))
        e = _applied(what, seconds=round(v, 6))
        if s:
            e[0]['scaling'] = [{'source': src, 'per': round(per, 6)} for src, per in s]
        return e
    m = re.fullmatch(rf'\+?{N}% ({eff}) duration(?: you apply)?|({eff}) duration \+?{N}%', t)
    if m:
        num, what = (m.group(1), m.group(2)) if m.group(1) else (m.group(4), m.group(3))
        v, s = scal(float(num))
        e = _applied(what, duration=round(v / 100, 6))
        if s:
            e[0]['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in s]
        return e
    return None


def chances(low, scal):
    """Bleed lines, "+5% ignite chance", parry/knockback/extra-drop chances. `low` is one clause."""
    t = low.strip().rstrip('.')
    mi = misc(t, scal)
    if mi:
        return mi
    m = re.fullmatch(rf'critical (sword |axe )?hits cause bleeding - the mob takes {N} damage per second for {N}s', t)
    if m:
        e = _bleed(damage=float(m.group(2)), seconds=float(m.group(3)))
        if m.group(1):
            e['weapons'] = SWORDS if 'sword' in m.group(1) else '#pipican:weapons/axes'
        return [e]
    m = re.fullmatch(rf'\+?{N}s bleed duration', t)
    if m:
        v, s = scal(float(m.group(1)))
        e = _bleed(seconds=round(v, 6))
        if s:
            e['scaling'] = [{'source': src, 'per': round(per, 6)} for src, per in s]
        return [e]
    m = re.fullmatch(rf'\+?{N} bleed damage(?: per (?:tick|second))?(?: on criticals)?', t)
    if m:
        return [_bleed(damage=float(m.group(1)))]
    m = re.fullmatch(rf'bleed stacks up to {N} times', t)
    if m:
        return [_bleed(max_stacks=int(float(m.group(1))))]
    if re.fullmatch(r'bleeding mobs are slowed', t):
        return [_bleed(slows=True)]
    if re.fullmatch(r'every hit \(not just crits\) causes bleeding|every hit causes bleeding', t):
        return [_bleed(any_hit=True)]
    m = re.fullmatch(rf'bleeding mobs take \+?{N}% more damage from you', t)
    if m:
        v, s = scal(float(m.group(1)))
        e = {'type': 'pipican:damage_dealt', 'victim': {'bleeding': True}, 'multiplier': round(1 + v / 100, 6)}
        if s:
            e['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in s]
        return [e]
    # "+5% ignite chance", "ignite chance +10%", "+0.5% poison chance", "charm chance doubled"
    words = '|'.join(sorted(CHANCE_NAMES, key=len, reverse=True))
    m = re.fullmatch(rf'\+?{N}% ({words}) chance', t) or re.fullmatch(rf'({words}) chance \+?{N}%', t)
    if m:
        num, word = (m.group(1), m.group(2)) if m.re.pattern.startswith('\\+') else (m.group(2), m.group(1))
        v, s = scal(float(num))
        e = {'type': 'pipican:reaction_chance', 'name': CHANCE_NAMES[word], 'amount': round(v / 100, 6)}
        if s:
            e['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in s]
        return [e]
    m = re.fullmatch(rf'\+?{N}% chance (?:that )?your hits charm a mob', t)
    if m:
        v, s = scal(float(m.group(1)))
        e = {'type': 'pipican:reaction_chance', 'name': 'pacify', 'amount': round(v / 100, 6)}
        if s:
            e['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in s]
        return [e]
    m = re.fullmatch(rf'\+?{N}% chance that a harmful potion effect or magic attack aimed at you is cancelled', t)
    if m:
        v, s = scal(float(m.group(1)))
        return [_effect('spell_ward', v, s)]
    m = re.fullmatch(rf'\+?{N}% chance (?:that )?your hits curse a mob', t)
    if m:
        v, s = scal(float(m.group(1)))
        e = {'type': 'pipican:reaction_chance', 'name': 'curse', 'amount': round(v / 100, 6)}
        if s:
            e['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in s]
        return [e]
    m = re.fullmatch(rf'({words}) chance (doubled|tripled)', t)
    if m:
        return [{'type': 'pipican:reaction_chance', 'name': CHANCE_NAMES[m.group(1)], 'multiplier': 2.0 if m.group(2) == 'doubled' else 3.0}]
    # knockback
    m = re.fullmatch(rf'(?:your )?(?:melee )?hits knock (?:mobs|enemies) back {N}% (farther|further|less)', t) or \
        re.fullmatch(rf'(?:melee )?knockback \+{N}%()', t) or re.fullmatch(rf'\+{N}% (?:melee )?knockback()(?! resistance)', t)
    if m:
        v, s = scal(float(m.group(1)) * (-1 if m.group(2) == 'less' else 1))
        return [_effect('knockback', v, s)]
    # extra drops
    m = re.fullmatch(rf'\+?{N}% (?:chance (?:for|of|to get) one extra item (?:from mob loot|when a mob drops loot)|extra mob loot chance|chance of one extra roll of loot)', t)
    if m:
        v, s = scal(float(m.group(1)))
        return [_effect('extra_loot', v, s)]
    m = re.fullmatch(rf'\+?{N}% chance (?:of|for) (?:an )?extra ore drops?', t)
    if m:
        v, s = scal(float(m.group(1)))
        return [_effect('extra_ore', v, s)]
    m = re.fullmatch(rf'\+?{N}% chance (?:of|for) double crop drops', t)
    if m:
        v, s = scal(float(m.group(1)))
        return [_effect('extra_crop', v, s)]
    return None


def scaled_reaction(t, ctx):
    """"per 10 Radiance, +5% chance your hits give Blindness for 2s": a reaction whose chance grows with the special stat."""
    import combat
    sp = (ctx.get('special_name') or '').lower()
    if not sp:
        return None
    m = re.fullmatch(rf'per {N} {re.escape(sp)}(?: points?)?,? \+?{N}% (chance .+)', t)
    if not m:
        return None
    reacted = combat.reaction(f'{m.group(2)}% {m.group(3)}')
    if not reacted or len(reacted) != 1 or reacted[0].get('type') != 'pipican:on' or 'name' not in reacted[0]:
        return None
    r = dict(reacted[0], chance=0.0)
    per = float(m.group(2)) / 100 / float(m.group(1))
    return [r, {'type': 'pipican:reaction_chance', 'name': r['name'], 'scaling': [{'source': 'special_stat', 'per': round(per, 6)}]}]
