"""Skill "Trained by" text -> skill trigger JSON (pipican:did / pipican:filtered / pipican:any)."""
import re

import combat
import conditions as cond

REDSTONE = ['minecraft:repeater', 'minecraft:comparator', 'minecraft:piston', 'minecraft:sticky_piston', 'minecraft:observer',
            'minecraft:dropper', 'minecraft:dispenser', 'minecraft:hopper', 'minecraft:redstone_torch', 'minecraft:lever',
            'minecraft:daylight_detector', 'minecraft:target', 'minecraft:redstone_lamp', 'minecraft:note_block', 'minecraft:tnt']
RAILS = ['minecraft:rail', 'minecraft:powered_rail', 'minecraft:detector_rail', 'minecraft:activator_rail']
ARROWS = ['minecraft:arrow', 'minecraft:spectral_arrow', 'minecraft:tipped_arrow']


def did(action, **kw):
    return dict({'type': 'pipican:did', 'action': action}, **kw)


# (pattern, builder). Patterns match one part of the text ("brewing potions", "killing undead mobs while wet").
SIMPLE = [
    (r'(?:landing )?critical hits?|crits?', lambda m: did('crit')),
    (r'making mobs bleed|causing bleeding', lambda m: did('bleed')),
    (r'dodging(?: attacks| hits)?(?: with \w+)?', lambda m: did('dodge')),
    (r'healing damage(?: \(any source\))?|healing yourself|being healed', lambda m: did('heal')),
    (r'teleporting(?: with pearls and blinks| with ender pearls)?|throwing ender pearls', lambda m: did('teleport')),
    (r'enchanting items(?: at an enchanting table)?|enchanting', lambda m: did('enchant')),
    (r'brewing(?: poison)? potions|brewing poison|brewing', lambda m: did('brew')),
    (r'crafting (?:and placing )?redstone components(?:, rails and mechanisms)?', lambda m: did('craft', items=REDSTONE + RAILS)),
    (r'trading(?: with villagers(?:, wandering traders and piglins)?)?', lambda m: did('trade')),
    (r'taming(?: animals)?', lambda m: did('tame')),
    (r'breeding(?: animals)?', lambda m: did('breed')),
    (r'mining ores and stone', lambda m: {'type': 'pipican:any', 'triggers': [did('mine', blocks='#c:ores'),
                                                                              did('mine', blocks='#minecraft:base_stone_overworld')]}),
    (r'mining ores', lambda m: did('mine', blocks='#c:ores')),
    (r'mining stone', lambda m: did('mine', blocks='#minecraft:base_stone_overworld')),
    (r'harvesting fully grown crops(?:, berries, flowers and mushrooms)?|harvesting crops', lambda m: did('harvest')),
    (r'planting (?:trees|saplings)', lambda m: did('plant', blocks='#minecraft:saplings')),
    (r'growing trees with bone meal|using bone meal', lambda m: did('bone_meal')),
    (r'using anvils(?:, grindstones and smithing tables)?|using enchanted books on an anvil|combining enchanted books', lambda m: did('anvil')),
    (r'gaining experience levels|levelling up', lambda m: did('level_up')),
    (r'drinking potions', lambda m: did('drink')),
    (r'fishing', lambda m: did('fish')),
    (r'taking fall damage', lambda m: did('take_fall')),
    (r'(?:your )?(?:tamed animals|minions|undead servants|summons|pets) killing mobs', lambda m: did('pet_kill')),
    (r'travelling on (?:horses|mounts)(?:, camels, donkeys, pigs and striders)?|riding', lambda m: {'type': 'pipican:move', 'mode': 'ride', 'per_blocks': 20}),
    (r'swimming distance|swimming', lambda m: {'type': 'pipican:move', 'mode': 'swim', 'per_blocks': 10}),
    (r'sprint-jumping|jumping', lambda m: {'type': 'pipican:jump'}),
    (r'healing other players|healing others(?: with [a-z\' ]+)?|healing allies', lambda m: did('heal_other')),
    (r'charming mobs|charming', lambda m: did('charm')),
    (r'damaging mobs with explosions|blowing up mobs', lambda m: did('explosion_hit')),
    (r'killing mobs with execute|executing mobs', lambda m: did('execute')),
    (r'poisoning mobs', lambda m: did('poison')),
    (r'absorbing damage with [a-z ]+', lambda m: did('absorb')),
    (r'throwing wind charges', lambda m: did('use_item', items=['minecraft:wind_charge'])),
    (r'taking big hits \((\d+)\+ damage\) and surviving', lambda m: filtered(did('hurt'), min_amount=float(m.group(1)))),
]


def filtered(trigger, when=None, victims=None, victim=None, min_amount=None):
    if not (when or victims or victim or min_amount):
        return trigger
    f = {'type': 'pipican:filtered', 'trigger': trigger}
    if when:
        f['when'] = when
    if victims:
        f['victims'] = victims
    if victim:
        f['victim'] = victim
    if min_amount:
        f['min_amount'] = min_amount
    return f


def _qualified(base, rest, ctx):
    """"killing <mobs...> <while ...>" -> filtered trigger, or None."""
    rest = rest.strip()
    when = None
    m = re.search(r'(?:^|\s+)\b(while|when|in|inside|at|outdoors|underground|underwater|during|with your)\b.*$', rest)
    if m:
        tail = m.group(0).strip()
        when = _ability_active(tail, ctx) or cond.parse(tail, ctx)
        if when is None:
            return None
        rest = rest[:m.start()].strip()
    rest = re.sub(r'^(?:hostile )?mobs$|^hostile mobs$', '', rest)
    victims = victim = None
    if rest:
        victim = combat.victim_state(rest)
        if victim is None:
            group = re.sub(r'\s*\(.*\)$', '', rest)
            group = re.sub(r'\s+mobs$', '', group)
            victims = combat.mobs(group)
            if victims is None:
                return None
    return filtered(base, when, victims, victim)


def _ability_id(name, ctx):
    import abilities as ab
    cid = ctx.get('class_id')
    for (owner, aname) in ab.A:
        if owner == cid and aname.lower() == name.strip().lower():
            return f'pipican:{cid}/' + re.sub(r'[^a-z0-9]+', '_', aname.lower().replace("'", '')).strip('_')
    return None


def _ability_active(text, ctx):
    """"while Stand Guard is active", "in Battle Stance", "while Berserk" -> ability_active, if the class has it."""
    m = re.fullmatch(r'(?:while|when|in) (?:your )?(.+?)(?: is active| is on)?', text.strip())
    if not m:
        return None
    aid = _ability_id(m.group(1), ctx)
    return {'type': 'pipican:ability_active', 'ability': aid} if aid else None


REACTION_ALIASES = {'premonitions': 'premonition', 'evil eye': 'curse', 'stay the hand': 'stay the hand', 'zeal': 'zeal'}


def _reaction_name(name, ctx):
    import named
    names = named.REACTION_NAMES.get(ctx.get('class_id'), set())
    n = name.strip().lower()
    n = REACTION_ALIASES.get(n, n)
    for cand in (n, re.sub(r's$', '', n)):
        if cand in names:
            return cand
    return None


def _part(t, ctx):
    t = t.strip()
    # "landing Trickery hits", "triggering Seize Chaos", "using up premonitions", "surviving hits with Zeal"
    m = re.fullmatch(r'(?:landing|triggering|using up|cancelling attacks with|surviving hits with|cursing mobs with|stealing effects with|'
                     r'healing with|hitting mobs with) (.+?)(?: hits| shots)?', t)
    if m and _reaction_name(m.group(1), ctx):
        return {'type': 'pipican:reaction', 'name': _reaction_name(m.group(1), ctx)}
    # "seconds spent in Divine Ecstasy", "time spent in animal forms"
    m = re.fullmatch(r'(?:seconds|time) spent in (.+)', t)
    if m:
        import named
        if re.fullmatch(r'(?:animal )?forms?', m.group(1)):
            forms = [w for n, (w, _) in named.NAMED.get(ctx.get('class_id'), {}).items() if n.endswith(' form') and w.get('type') == 'pipican:ability_active']
            if forms:
                return filtered(did('active_second'), when=forms[0] if len(forms) == 1 else {'type': 'pipican:any_of', 'conditions': forms})
        aid = _ability_id(m.group(1), ctx)
        if aid:
            return filtered(did('active_second'), when={'type': 'pipican:ability_active', 'ability': aid})
    # "killing mobs while they have the Wither effect"
    m = re.fullmatch(r'killing mobs while they have the ([a-z ]+?) effect', t)
    if m and m.group(1) in combat.EFFECT_NAMES:
        return filtered({'type': 'pipican:kill'}, victim={'effects': 'minecraft:' + combat.EFFECT_NAMES[m.group(1)]})
    # "landing Divine Smite hits", "triggering Seize Chaos", "killing mobs with Execute", "using Optimization"
    m = re.fullmatch(r'(?:using|landing|triggering|casting|cursing mobs with|stealing effects with|killing mobs with|healing (?:others )?with) (.+?)(?: hits| shots)?', t)
    if m and _ability_id(m.group(1), ctx):
        return {'type': 'pipican:ability', 'ability': _ability_id(m.group(1), ctx)}
    for pat, build in SIMPLE:
        m = re.fullmatch(pat, t)
        if m:
            return build(m)
    rn = (ctx.get('resource_name') or '').lower()
    if rn and re.fullmatch(rf'spending (?:{re.escape(rn)}|{re.escape(rn.split()[-1])})', t):
        return did('spend')
    m = re.fullmatch(r'killing (.*)', t)
    if m:
        rest = m.group(1)
        sprint = re.fullmatch(r'mobs with sprint-hits', rest)
        if sprint:
            return filtered({'type': 'pipican:kill'}, when=cond.state('sprinting'))
        return _qualified({'type': 'pipican:kill'}, rest, ctx)
    m = re.fullmatch(r'(?:hitting|damaging|fighting) (.*)', t)
    if m:
        return _qualified({'type': 'pipican:melee_hit'}, m.group(1), ctx)
    m = re.fullmatch(r'taking damage (while .+)', t)
    if m:
        when = cond.parse(m.group(1), ctx)
        return filtered(did('hurt'), when=when) if when else None
    m = re.fullmatch(r'fighting (in .+|while .+)', t)
    if m:
        when = cond.parse(m.group(1), ctx)
        return filtered({'type': 'pipican:melee_hit'}, when=when) if when else None
    return None


def map_text(text, ctx):
    """-> (trigger, exact) or None when nothing in the text could be read."""
    t = text.lower().strip().rstrip('.')
    whole = _part(t, ctx)
    if whole is not None:
        return whole, True
    parts = [p for p in re.split(r',\s*(?:and\s+)?|\s+and\s+(?=(?:killing|hitting|brewing|taming|breeding|crafting|planting|growing|'
                                 r'using|healing|teleporting|mining|harvesting|enchanting|combining|drinking|dodging|spending|taking|'
                                 r'sprint-jumping|jumping|fighting|swimming|throwing|landing|making|trading|gaining|fishing)\b)', t) if p.strip()]
    if len(parts) < 2:
        return None
    mapped = [_part(p, ctx) for p in parts]
    found = [m for m in mapped if m is not None]
    if not found:
        return None
    trig = found[0] if len(found) == 1 else {'type': 'pipican:any', 'triggers': found}
    return trig, len(found) == len(parts)
