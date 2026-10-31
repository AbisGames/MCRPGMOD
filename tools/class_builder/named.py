"""Named class passives ("Berserk: while Rage is above 75%: ...") and the lines that refer back to them
("+0.2% speed per Fury while Berserk", "Berserk melee bonus +1% per 2 Fury", "when you go Berserk, ..."),
plus the class mechanics built from generic effects: execute, auras, challenge, scrutiny, favored weapon, Zeal."""
import re

N = r'(\d+(?:\.\d+)?)'
# class id -> {lowercase name: (condition, effects inside it)}
NAMED = {}
# class id -> {reaction name: [extra actions]}: "after Zeal triggers, immune to Slowness for 5s" rides on Zeal's reaction.
EXTRA = {}
TRIGGERED = {'zeal': 'zeal', 'decoy': 'decoy'}
# class id -> {cleaned lowercase body: label}, so a mechanic can be named after its line ("Inspiring Song: players within ...")
LABELS = {}
# class id -> aura names
AURAS = {}
# class id -> {label: plain damage bonuses its line defines} ("Treacherous Strike: +40% damage to mobs that weren't targeting you")
STATIC = {}
# class id -> every pipican:on reaction name its lines produced
REACTION_NAMES = {}
# class id -> {label: the pipican:on reaction its line defines} ("Guarded Casting: whenever you spend Ward, ...")
REACTIONS = {}
# class id -> functions that adjust the finished class JSON (a weakness that changes how a passive's state works)
PATCHES = {}
LABEL = re.compile(r"^([A-Z][A-Za-z' ]{2,30}?):\s*(.+)$")


def learn(cid, lines, ctx):
    """Pre-pass over a class's lines: remember every "Name: <conditional bonus>" definition."""
    import effects as fx
    found = NAMED.setdefault(cid, {})
    labels = LABELS.setdefault(cid, {})
    for line in lines:
        m = LABEL.match((line or '').strip())
        if not m:
            continue
        body = re.sub(r'\s*\(including you\)', ' including you', m.group(2), flags=re.I)
        body = fx._clean(re.sub(r'\s*\([^)]*\)', '', body)).lower().strip().rstrip('.')
        labels[body] = m.group(1).strip()
        eff, uns = fx.translate(m.group(2), dict(ctx, class_id=None))
        # Only auras that really work can be upgraded ("Song range +1").
        if any(e.get('type') == 'pipican:aura' for e in eff) and not uns:
            AURAS.setdefault(cid, set()).add(m.group(1).strip().lower())
        single = eff[0] if len(eff) == 1 else None
        if single and single.get('type') == 'pipican:conditional' and len(single.get('effects', [])) == 1:
            single = single['effects'][0]  # "when you take a hit below half health, 25% chance to blink"
        if eff and not uns and all(e.get('type') in ('pipican:damage_dealt', 'pipican:damage_taken') and 'scaling' not in e for e in eff):
            STATIC.setdefault(cid, {}).setdefault(m.group(1).strip().lower(), eff)
        if single and single.get('type') == 'pipican:on' and not uns:
            REACTIONS.setdefault(cid, {}).setdefault(m.group(1).strip().lower(), single)
        whens = [e.get('when') for e in eff if e.get('type') == 'pipican:conditional']
        if eff and not uns and len(whens) == len(eff) and all(w == whens[0] for w in whens):
            inner = [x for e in eff for x in e['effects']]
            found.setdefault(m.group(1).strip().lower(), (whens[0], inner))


def atoms(ctx):
    """Condition phrases for this class's named states: "while Berserk", "in Battle Stance", "while Stand Guard is active"."""
    out = []
    forms = [w for n, (w, _) in NAMED.get(ctx.get('class_id'), {}).items() if n.endswith(' form') and w.get('type') == 'pipican:ability_active']
    if forms:
        any_form = forms[0] if len(forms) == 1 else {'type': 'pipican:any_of', 'conditions': forms}
        out.append((r'(?:while )?(?:in any (?:animal )?form|transformed|in an animal form)', lambda m, w=any_form: w, 'other'))
        out.append((r'(?:while )?in human form', lambda m, w=any_form: {'type': 'pipican:not', 'condition': w}, 'other'))
    named = NAMED.get(ctx.get('class_id'), {})
    for name, (when, _) in sorted(named.items(), key=lambda kv: -len(kv[0])):
        pat = rf'(?:while |when |in |during )(?:in )?(?:your )?{re.escape(name)}(?: is active| is on)?'
        out.append((pat, lambda m, w=when: w, 'other'))
    # "in Ecstasy" for Divine Ecstasy: a state's last word, when no other state ends with it.
    for name, (when, _) in named.items():
        last = name.split()[-1]
        if ' ' in name and len(last) > 3 and last not in named and sum(n.split()[-1] == last for n in named) == 1:
            out.append((rf'(?:while |in |during )(?:in )?{re.escape(last)}', lambda m, w=when: w, 'other'))
    return out


def _scaled_copy(effects, per, source, only=None):
    """"Battle Stance bonuses +1% per 4 Resolve": every % bonus of the state grows by `per` (a fraction) per point."""
    out = []
    for e in effects:
        if only and not only(e):
            continue
        if 'multiplier' in e and e['type'] in ('pipican:damage_dealt', 'pipican:damage_taken'):
            sign = 1 if e['multiplier'] > 1 else -1
            c = {k: v for k, v in e.items() if k not in ('multiplier', 'scaling')}
            c['multiplier'] = 1.0
            c['scaling'] = [{'source': source, 'per': round(sign * per, 6)}]
            out.append(c)
    return out


def _stat_source(word, ctx):
    sp = (ctx.get('special_name') or '').lower()
    return 'special_stat' if word.lower() in (sp, 'point', 'points') else None


def _first_targets(reaction):
    """Target fields of a reaction's first area action, for an added action that should hit the same mobs."""
    actions = reaction.get('actions', [])
    selfish = any(a.get('target') == 'self' for a in actions)
    for a in actions:
        if a.get('target') in ('hostiles', 'mobs', 'allies', 'players'):
            out = {k: a[k] for k in ('target', 'radius', 'entities') if k in a}
            if out['target'] == 'players' and selfish:
                out['target'] = 'allies'  # "heals you and players within 16 blocks"
            return out
    return None


def _glow(target, radius, state=None):
    a = {'type': 'pipican:effect', 'target': target, 'radius': radius, 'effects': [{'effect': 'minecraft:glowing', 'seconds': 2.0}]}
    if state:
        a['state'] = state
    return a


def glows(t, ctx):
    """"invisible mobs glow", "hostile mobs within 10 blocks that are targeting you glow", "+8 blocks glow range"."""
    t = re.sub(r' (?:for you|through walls)$', '', t.strip())
    m = re.fullmatch(rf'glow range of ([a-z\' ]+) \+{N}(?: blocks?)?', t)
    if m and m.group(1) in REACTIONS.get(ctx.get('class_id'), {}):
        return [{'type': 'pipican:reaction_mod', 'name': m.group(1), 'radius': float(m.group(2))}]
    m = re.fullmatch(rf'(?:your )?(?:prey|marked targets?) glows?(?: through walls)?(?: up to {N} blocks)?', t)
    if m:
        return [{'type': 'pipican:on', 'event': 'second', 'actions': [_glow('mobs', float(m.group(1) or 32), {'marked': True})]}]
    m = re.fullmatch(rf'(invisible|hostile) mobs( and players)?(?: within {N} blocks)?( that are targeting you)? glow', t) or \
        re.fullmatch(rf"you can'?t be surprised - hostile mobs within {N} blocks and all invisible creatures glow()()()", t)
    if not m:
        m2 = re.fullmatch(rf'(?:\+{N} blocks? glow range|glow range \+{N}(?: blocks?)?)(?: per (?:{N} )?([a-z]+)(?: points?)?)?', t)
        if m2:
            value = float(m2.group(1) or m2.group(2))
            glowing = [n for n, r in REACTIONS.get(ctx.get('class_id'), {}).items()
                       if any(e.get('effect') == 'minecraft:glowing' for a in r.get('actions', []) for e in a.get('effects', []))]
            if len(glowing) != 1:
                return None
            e = {'type': 'pipican:reaction_mod', 'name': glowing[0]}
            if m2.group(4):
                src = _stat_source(m2.group(4), ctx)
                if not src:
                    return None
                e['radius'] = 0.0
                e['scaling'] = [{'source': src, 'per': round(value / float(m2.group(3) or 1), 6)}]
            else:
                e['radius'] = value
            return [e]
        return None
    if m.re.pattern.startswith('you'):
        r = float(m.group(1))
        actions = [_glow('hostiles', r), _glow('mobs', r, {'effects': 'minecraft:invisibility'}),
                   _glow('players', r, {'effects': 'minecraft:invisibility'})]
    else:
        r = float(m.group(3) or 16)
        state = {'effects': 'minecraft:invisibility'} if m.group(1) == 'invisible' else {}
        if m.group(4):
            state['targeting_you'] = True
        actions = [_glow('mobs' if m.group(1) == 'invisible' else 'hostiles', r, state or None)]
        if m.group(2):
            actions.append(_glow('players', r, {'effects': 'minecraft:invisibility'}))
    return [{'type': 'pipican:on', 'event': 'second', 'actions': actions}]


def static_mods(t, ctx):
    """"+15% more Treacherous Strike damage", "Per 10 Subtlety, +3% Treacherous Strike": another copy of a named damage bonus."""
    statics = STATIC.get(ctx.get('class_id'), {})
    if not statics:
        return None
    # "Purge bonus" names "Purge the Unclean": a label's first or last word works when no other label shares it.
    aliases = {}
    for n in statics:
        for w in {n.split()[0], n.split()[-1]}:
            if len(w) > 3 and w not in statics:
                aliases[w] = None if w in aliases else n
    statics = dict(statics, **{w: statics[n] for w, n in aliases.items() if n})
    names = '|'.join(sorted((re.escape(n) for n in statics), key=len, reverse=True))
    m = re.fullmatch(rf'\+{N}% (?:more )?({names})(?: damage| bonus)?(?: per (?:{N} )?([a-z]+)(?: points?)?)?', t) or \
        re.fullmatch(rf'({names})(?: damage| bonus)? \+{N}%(?: per (?:{N} )?([a-z]+)(?: points?)?)?()', t)
    if not m:
        return None
    if m.re.pattern.startswith(r'\+'):
        value, name, n, stat = float(m.group(1)), m.group(2), m.group(3), m.group(4)
    else:
        name, value, n, stat = m.group(1), float(m.group(2)), m.group(3), m.group(4)
    src = _stat_source(stat, ctx) if stat else None
    if stat and not src:
        return None
    out = []
    for e in statics[name]:
        c = {k: v for k, v in e.items() if k != 'multiplier'}
        sign = 1 if e['multiplier'] >= 1 else -1
        if src:
            c['multiplier'] = 1.0
            c['scaling'] = [{'source': src, 'per': round(sign * value / 100 / float(n or 1), 6)}]
        else:
            c['multiplier'] = round(1 + sign * value / 100, 6)
        out.append(c)
    return out


LEVEL_WORDS = {'ii': 1, 'iii': 2, 'iv': 3}


def _amplify(names, t):
    """"Dirty Fighting slow is Slowness II", "Stigmata Slowness becomes Slowness II", "Dreadful Threat Weakness II"
    -> (name, {effect id: level}) or None."""
    import combat
    effs = '|'.join(sorted((re.escape(k) for k in combat.EFFECT_NAMES), key=len, reverse=True))
    m = re.fullmatch(rf'({names}) (?:[a-z]+ (?:is|becomes) )?({effs}) (ii|iii|iv)', t)
    if m:
        return m.group(1), {'minecraft:' + combat.EFFECT_NAMES[m.group(2)]: LEVEL_WORDS[m.group(3)]}
    return None


def reaction_mods(t, ctx):
    """Upgrades to a named reaction -> pipican:reaction_mod ("Guarded Casting lasts 5s longer")."""
    import combat
    reactions = REACTIONS.get(ctx.get('class_id'), {})
    if not reactions:
        return None
    names = '|'.join(sorted((re.escape(n) for n in reactions), key=len, reverse=True))
    per = rf'(?: per (?:{N} )?([a-z]+(?: (?!points?\b)[a-z]+)?)(?: points?)?)?'
    original_t = t
    # "Resolve trigger heals 10 HP instead": a name's last word stands for it when no other name ends that way.
    last = {}
    for n in reactions:
        last.setdefault(n.split()[-1], []).append(n)
    for word, full in last.items():
        if len(full) == 1 and word != full[0] and len(word) > 3 and re.match(re.escape(word) + r'\b', t):
            t = full[0] + t[len(word):]
            break
    # "Containing Strike lasts 2s": a new total for its effect time.
    m = re.fullmatch(rf'({names}) lasts {N}s', t)
    if m:
        secs = [e.get('seconds') for a in reactions[m.group(1)].get('actions', []) for e in ([a] if a.get('type') == 'pipican:boost' else a.get('effects', []))
                if e.get('seconds')]
        if secs and float(m.group(2)) > secs[0]:
            return [{'type': 'pipican:reaction_mod', 'name': m.group(1), 'seconds': round(float(m.group(2)) - secs[0], 6)}]

    def mod(name, **kw):
        e = {'type': 'pipican:reaction_mod', 'name': name}
        e.update(kw)
        return [e]

    def scaled(name, field, value, n, stat):
        if not stat:
            return mod(name, **{field: value})
        src = _stat_source(stat, ctx)
        if not src:
            return None
        return mod(name, **{field: 0.0, 'scaling': [{'source': src, 'per': round(value / float(n or 1), 6)}]})

    # Charm upgrades find the class's charm reaction by what it does ("Subtle Charm" pacifies the mob it hits).
    charm = [(r.get('name') or n, r) for n, r in reactions.items()
             if any(a.get('type') == 'pipican:control' and a.get('mode') == 'pacify' for a in r.get('actions', []))]
    if len(charm) == 1:
        cname, creact = charm[0]
        m = re.fullmatch(rf'charms? (?:last|lasts) {N}s longer', original_t) or re.fullmatch(rf'charm duration \+{N}s' + per, original_t)
        if m:
            n_, stat = (m.group(2), m.group(3)) if m.lastindex and m.lastindex >= 3 else (None, None)
            return scaled(cname, 'seconds', float(m.group(1)), n_, stat)
        if re.fullmatch(r'charmed mobs attack other hostile mobs', original_t):
            secs = [a.get('seconds', 10) for a in creact['actions'] if a.get('type') == 'pipican:control']
            return mod(cname, actions=[{'type': 'pipican:control', 'target': 'victim', 'mode': 'ally', 'seconds': float(secs[0])}])
    # "post-Zeal immunity lasts 3s longer", "+4 blocks Alertness range"
    m = re.fullmatch(rf'post-({names})(?: [a-z]+)? lasts \+?{N}s longer', original_t)
    if m:
        return mod(m.group(1), seconds=float(m.group(2)))
    m = re.fullmatch(rf'\+{N} blocks? ({names}) (?:range|radius)', original_t)
    if m:
        return mod(m.group(2), radius=float(m.group(1)))
    # "Speed boost lasts 2s longer": the class's one reaction that boosts speed
    m = re.fullmatch(rf'speed boost lasts \+?{N}s longer', original_t)
    if m:
        speedy = [(r.get('name') or n) for n, r in reactions.items() if any(
            a.get('type') == 'pipican:boost' and any(e.get('attribute') == 'minecraft:movement_speed' for e in a.get('effects', []))
            or any(e.get('effect') == 'minecraft:speed' for e in a.get('effects', [])) for a in r.get('actions', []))]
        if len(speedy) == 1:
            return mod(speedy[0], seconds=float(m.group(1)))
    # cooldowns: "Champion's Resolve cooldown -1% per Resolve point", "Trickery recharges twice as fast", "Pulse: Invocation every 20s"
    m = re.fullmatch(rf'({names}) cooldown ([+-]{N})%' + per, original_t)
    if m:
        return scaled(m.group(1), 'cooldown', float(m.group(2)) / 100, m.group(4), m.group(5))
    m = re.fullmatch(rf'({names}) recharges (twice|three times) as fast', original_t)
    if m:
        return mod(m.group(1), cooldown=-0.5 if m.group(2) == 'twice' else round(-2 / 3, 6))
    m = re.fullmatch(rf'({names}) every {N}s', original_t)
    if m and reactions[m.group(1)].get('cooldown_ticks'):
        base = reactions[m.group(1)]['cooldown_ticks'] / 20
        if float(m.group(2)) < base:
            return mod(m.group(1), cooldown=round(float(m.group(2)) / base - 1, 6))
    m = re.fullmatch(rf'({names}) heal speed \+{N}%' + per, original_t)
    if m and reactions[m.group(1)].get('cooldown_ticks'):
        return scaled(m.group(1), 'cooldown', -float(m.group(2)) / 100, m.group(3), m.group(4))
    # empowered hits: "Per 10 Battle Arcana, +1 escalating hit" ("+1 escalating hit per 10 battle arcana")
    m = re.fullmatch(rf'\+{N} ([a-z]+) hits?' + per, original_t)
    if m:
        hosts = [(r.get('name') or n) for n, r in reactions.items() if n.split()[0].startswith(m.group(2)[:6])
                 and any(a.get('type') == 'pipican:empower' for a in r.get('actions', []))]
        if len(hosts) == 1 and not m.group(4):
            return mod(hosts[0], hits=int(float(m.group(1))))
    # blink distance: "Per 10 Phantasm, blink distance +1", "+1 blink distance", "Blink distance +0.1 per Displacement"
    m = re.fullmatch(rf'blink distance \+{N}' + per, original_t) or re.fullmatch(rf'\+{N} blink distance()()', original_t)
    if m:
        blinkers = [(r.get('name') or n) for n, r in reactions.items() if any(a.get('type') == 'pipican:blink' for a in r.get('actions', []))]
        if len(blinkers) == 1:
            return scaled(blinkers[0], 'radius', float(m.group(1)), m.group(2), m.group(3))
    # "Blur: after a decoy, you're invisible for 1s"
    m = re.fullmatch(rf"after an? ({names}), you'?re invisible for {N}s", original_t)
    if m:
        return mod(m.group(1), actions=[{'type': 'pipican:effect', 'target': 'self', 'effects': [{'effect': 'minecraft:invisibility', 'seconds': float(m.group(2))}]}])
    # "Vanish also teleports you up to 6 blocks away from the attacker"
    m = re.fullmatch(rf'({names}) also teleports you up to {N} blocks away(?: from the attacker)?', original_t)
    if m:
        return mod(m.group(1), actions=[{'type': 'pipican:blink', 'distance': float(m.group(2))}])
    # "Invocation also heals players within its radius by 2"
    m = re.fullmatch(rf'({names}) also heals players within its radius by {N}', original_t)
    if m:
        tg = _first_targets(reactions[m.group(1)])
        if tg and 'radius' in tg:
            return mod(m.group(1), actions=[{'type': 'pipican:heal', 'target': 'allies', 'radius': tg['radius'], 'amount': float(m.group(2))}])
    # "Invocation stuns frightened (weakened) mobs for 1s"
    m = re.fullmatch(rf'({names}) stuns (?:frightened |weakened |frightened \(weakened\) )?mobs for {N}s', original_t)
    if m:
        tg = _first_targets(reactions[m.group(1)])
        if tg:
            a = {'type': 'pipican:effect', 'effects': [{'effect': 'minecraft:slowness', 'seconds': float(m.group(2)), 'amplifier': 9}]}
            a.update(tg)
            if 'weaken' in original_t or 'frighten' in original_t:
                a['state'] = {'effects': 'minecraft:weakness'}
            return mod(m.group(1), actions=[a])
    # "Escalating Violence also gives Speed I" when the reaction has no area: it's for you, as long as the reaction lasts
    m = re.fullmatch(rf'({names}) also gives (.+)', original_t)
    if m and _first_targets(reactions[m.group(1)]) is None:
        import combat
        secs = [a.get('seconds') for a in reactions[m.group(1)].get('actions', []) if a.get('seconds')]
        out = combat.outcome('gain ' + m.group(2) + (f' for {secs[0]:g}s' if secs and ' for ' not in m.group(2) else ''), 'self')
        if out:
            return mod(m.group(1), actions=out)
    amp = _amplify(names, t)
    if amp:
        return mod(amp[0], amplify=amp[1])
    import combat
    effs = '|'.join(sorted((re.escape(k) for k in combat.EFFECT_NAMES), key=len, reverse=True))
    m = re.fullmatch(rf'({effs}) after (?:a )?(kill|hit|crit) becomes ({effs}) (ii|iii|iv)', original_t)
    if m:
        want = 'minecraft:' + combat.EFFECT_NAMES[m.group(1)]
        event = {'kill': 'kill', 'hit': 'hit', 'crit': 'crit'}[m.group(2)]
        hits = [(r.get('name') or n) for n, r in reactions.items() if (r.get('event') == event or event == 'kill' and r.get('event') == 'execute') and
                any(e.get('effect') == want for a in r.get('actions', []) for e in a.get('effects', []))]
        if len(hits) == 1:
            return mod(hits[0], amplify={'minecraft:' + combat.EFFECT_NAMES[m.group(3)]: LEVEL_WORDS[m.group(4)]})
    # strength upgrades: "Guarded Casting armor +1 per 20 Warding", "+5% Trickery damage", "Escalating Violence +0.5% per Battle Arcana"
    def base_value(r):
        for a in r.get('actions', []):
            if a.get('type') == 'pipican:empower' and a.get('multiplier', 1) != 1:
                return (a['multiplier'] - 1) * 100
            if a.get('type') == 'pipican:boost' and a.get('effects'):
                e = a['effects'][0]
                if e.get('type') == 'pipican:attribute':
                    return e['amount'] * (100 if e.get('operation') else 1)
                if e.get('type') == 'pipican:damage_dealt':
                    return (e['multiplier'] - 1) * 100
                if e.get('type') == 'pipican:stat':
                    return float(e['amount'])
            if a.get('type') == 'pipican:random' and a.get('choices') and a['choices'][0]:
                inner = base_value({'actions': a['choices'][0]})
                if inner:
                    return inner
        return None
    m = re.fullmatch(rf'({names})(?: damage| bonus| armor| speed)? \+{N}%?' + per, t) or \
        re.fullmatch(rf'\+{N}%? (?:more )?({names})(?: damage| bonus)?' + per + '()', t)
    if m:
        if m.re.pattern.startswith('('):
            name, value, n, stat = m.group(1), float(m.group(2)), m.group(3), m.group(4)
        else:
            name, value, n, stat = m.group(2), float(m.group(1)), m.group(3), m.group(4)
        base = base_value(reactions[name])
        # "+1 Aptitude" is the special stat when a reaction shares its name.
        stat_name = name == (ctx.get('special_name') or '').lower() and '%' not in t
        if base and not stat_name and not (reactions[name].get('chance', 1.0) < 1 and 'damage' not in t):
            return scaled(name, 'strength', round(value / base, 6), n, stat)
    # chance upgrades: "Stay the Hand +0.3% per Rapport", "+5% Stay the Hand", "Hasty Escape chance +5%",
    # "Stay the Hand 30%" (a new total), "Hasty Escape always triggers"
    m = re.fullmatch(rf'({names})(?: chance)? \+{N}%' + per, t)
    m2 = re.fullmatch(rf'\+{N}% ({names})(?: chance)?', t)
    chancy = lambda nm: 'chance' in t or reactions[nm].get('chance', 1.0) < 1
    if m and not chancy(m.group(1)):
        m = None
    if m2 and not chancy(m2.group(2)):
        m2 = None
    if m or m2:
        name, value, n, stat = (m.group(1), m.group(2), m.group(3), m.group(4)) if m else (m2.group(2), m2.group(1), None, None)
        e = {'type': 'pipican:reaction_chance', 'name': name}
        if stat:
            src = _stat_source(stat, ctx)
            if not src:
                return None
            e['amount'] = 0.0
            e['scaling'] = [{'source': src, 'per': round(float(value) / 100 / float(n or 1), 6)}]
        else:
            e['amount'] = round(float(value) / 100, 6)
        return [e]
    m = re.fullmatch(rf'({names})(?: chance)? {N}%', t) or re.fullmatch(rf'({names}) always triggers()', t)
    if m:
        base = reactions[m.group(1)].get('chance', 1.0)
        total = 1.0 if not m.group(2) else float(m.group(2)) / 100
        if total > base:
            return [{'type': 'pipican:reaction_chance', 'name': m.group(1), 'amount': round(total - base, 6)}]
    # "hostile mobs you cancel with Stay the Hand fight other mobs for 5s"
    m = re.fullmatch(rf'(?:hostile )?mobs you cancel with ({names}) fight other mobs for {N}s', t)
    if m:
        return mod(m.group(1), actions=[{'type': 'pipican:control', 'target': 'victim', 'mode': 'ally', 'seconds': float(m.group(2))}])
    m = re.fullmatch(rf'({names})(?: [a-z]+){{0,2}} lasts \+?{N}s longer', t) or re.fullmatch(rf'({names}) lasts \+{N}s()', t)
    if m:
        return mod(m.group(1), seconds=float(m.group(2)))
    m = re.fullmatch(rf'({names}) (?:radius|range) ([+-]{N})(?: blocks?)?' + per, t)
    if m:
        return scaled(m.group(1), 'radius', float(m.group(2)), m.group(4), m.group(5))
    m = re.fullmatch(rf'doubles ({names}) radius', t)
    if m:
        tg = _first_targets(reactions[m.group(1)])
        if tg and 'radius' in tg:
            return mod(m.group(1), radius=float(tg['radius']))
    m = re.fullmatch(rf'({names}) (?:heals? )?\+{N}(?: hp)?' + per, t)
    if m and m.group(4):
        return scaled(m.group(1), 'heal', float(m.group(2)), m.group(3), m.group(4))
    # "Pious Restoration: heal 3 HP" (label stripped: "heal 3 hp" arrives with the name in front), "Resolve trigger heals 10 HP instead"
    m = re.fullmatch(rf'({names})(?: trigger)?:? heals? {N} hp(?: instead)?', t) or re.fullmatch(rf'({names}) heal amount {N}', t)
    if m:
        base = [a.get('amount', 0) for a in reactions[m.group(1)].get('actions', []) if a.get('type') == 'pipican:heal']
        if len(base) == 1 and float(m.group(2)) > base[0]:
            return mod(m.group(1), heal=round(float(m.group(2)) - base[0], 6))
    # "Fearsome Casting adds Slowness I", "Vanish also clears Slowness and Weakness", "also heals players within 6 blocks by 4"
    m = re.fullmatch(rf'({names}) (?:also )?(?:adds|also gives) (.+)', t)
    if m:
        tg = _first_targets(reactions[m.group(1)])
        out = combat.outcome('give ' + m.group(2), 'victim')
        if tg and out and out[0].get('type') == 'pipican:effect':
            out[0].update(tg)
            return mod(m.group(1), actions=out)
    m = re.fullmatch(rf'({names}) also heals players within {N} blocks by {N}', t)
    if m:
        return mod(m.group(1), actions=[{'type': 'pipican:heal', 'target': 'allies', 'radius': float(m.group(2)), 'amount': float(m.group(3))}])
    m = re.fullmatch(rf'({names}) also heals (?:you )?{N} hp', t)
    if m:
        return mod(m.group(1), actions=[{'type': 'pipican:heal', 'target': 'self', 'amount': float(m.group(2))}])
    m = re.fullmatch(rf'({names}) also (?:clears|removes|cures) (.+)', t)
    if m:
        import bonus
        ids = bonus._effect_ids(m.group(2))
        if ids:
            healed = [a.get('target') for a in reactions[m.group(1)].get('actions', []) if a.get('type') == 'pipican:heal']
            target = healed[0] if healed and healed[0] in ('victim', 'allies', 'players') else 'self'
            c = {'type': 'pipican:cleanse', 'target': target, 'effects': ids if len(ids) > 1 else ids[0]}
            for a in reactions[m.group(1)].get('actions', []):
                if a.get('type') == 'pipican:heal' and a.get('target') == target and 'radius' in a:
                    c['radius'] = a['radius']
            return mod(m.group(1), actions=[c])
    return None


def ability_mods(t, ctx):
    """Upgrades to one of the class's abilities -> pipican:ability_mod ("Stigmata lasts twice as long")."""
    abilities = {n: w['ability'] for n, (w, _) in NAMED.get(ctx.get('class_id'), {}).items() if w.get('type') == 'pipican:ability_active'}
    if not abilities:
        return None
    names = '|'.join(sorted((re.escape(n) for n in abilities), key=len, reverse=True))
    per = rf'(?: per (?:{N} )?([a-z]+)(?: points?)?)?'

    def mod(name, field, value, n=None, stat=None, div=1.0):
        e = {'type': 'pipican:ability_mod', 'ability': abilities[name]}
        if stat:
            src = _stat_source(stat, ctx)
            if not src:
                return None
            e[field] = 0.0
            e['scaling'] = [{'source': src, 'per': round(value / div / float(n or 1), 6)}]
        else:
            e[field] = round(value / div, 6) if field != 'uses' else int(value)
        return [e]

    import abilities as ab
    defs = {name.lower(): d for (owner, name), d in ab.A.items() if owner == ctx.get('class_id')}

    def first_targets(name):
        for a in defs.get(name, {}).get('actions', []):
            if a.get('target') in ('look', 'hostiles', 'mobs', 'victim'):
                return {k: a[k] for k in ('target', 'radius', 'range', 'cone') if k in a}
        return None
    # "charmed mobs also get Weakness I" (the Charm ability), "mobs hit by Dirge also get ..."
    for n in abilities:
        m = re.fullmatch(rf'(?:{re.escape(n)}e?d mobs|mobs hit by (?:your )?{re.escape(n)}) also get (.+)', t)
        if m:
            import combat
            out = combat.outcome('give ' + m.group(1), 'victim')
            tg = first_targets(n)
            if out and tg and out[0].get('type') == 'pipican:effect':
                out[0].update(tg)
                return [{'type': 'pipican:ability_mod', 'ability': abilities[n], 'actions': out}]
        # "charms last 3s longer", "Charm duration +0.1s per Glamour"
        m = re.fullmatch(rf'{re.escape(n)}s? (?:last|lasts) {N}s longer', t) or \
            re.fullmatch(rf'{re.escape(n)} duration \+{N}s(?: per (?:{N} )?([a-z]+)(?: points?)?)?', t)
        if m:
            if m.lastindex and m.lastindex >= 3 and m.group(3):
                src = _stat_source(m.group(3), ctx)
                if not src:
                    return None
                return [{'type': 'pipican:ability_mod', 'ability': abilities[n], 'seconds': 0.0,
                         'scaling': [{'source': src, 'per': round(float(m.group(1)) / float(m.group(2) or 1), 6)}]}]
            return [{'type': 'pipican:ability_mod', 'ability': abilities[n], 'seconds': float(m.group(1))}]
    amp = _amplify(names, t)
    if amp:
        return [{'type': 'pipican:ability_mod', 'ability': abilities[amp[0]], 'amplify': amp[1]}]
    m = re.fullmatch(rf'({names}) lasts (twice|three times) as long', t)
    if m:
        # Timed states last longer; toggles drain slower (whichever the ability is).
        a = mod(m.group(1), 'duration', {'twice': 1, 'three times': 2}[m.group(2)])
        a[0]['drain'] = {'twice': -0.5, 'three times': round(-2 / 3, 6)}[m.group(2)]
        return a
    m = re.fullmatch(rf'({names}) radius ([+-]{N})(?: blocks?)?' + per, t)
    if m:
        return mod(m.group(1), 'radius', float(m.group(2)), m.group(4), m.group(5))
    m = re.fullmatch(rf'({names}) bonuses (doubled|\+{N}%)', t)
    if m:
        return mod(m.group(1), 'strength', 1.0 if m.group(2) == 'doubled' else float(m.group(3)) / 100)
    m = re.fullmatch(rf'({names}) can be used (twice|three times) per rest', t)
    if m:
        return mod(m.group(1), 'uses', {'twice': 1, 'three times': 2}[m.group(2)])
    m = re.fullmatch(rf'({names}) costs {N}% less(?: [a-z]+)?', t)
    if m:
        a = mod(m.group(1), 'cost', -float(m.group(2)), div=100)
        a[0]['drain'] = a[0]['cost']
        return a
    m = re.fullmatch(rf'({names}) cooldown ([+-]{N})s', t)
    if m:
        return mod(m.group(1), 'cooldown_seconds', float(m.group(2)))
    m = re.fullmatch(rf'({names}) lasts longer: drain ([+-]{N})%' + per, t)
    if m:
        return mod(m.group(1), 'drain', float(m.group(2)), m.group(4), m.group(5), div=100)
    return None


def segment(t, ctx):
    """Whole-segment phrases. `t` is lowercase, label already stripped."""
    import bonus
    import combat
    import effects as fx
    per = rf'(?: per (?:{N} )?([a-z]+(?: (?!points?\b)[a-z]+)?)(?: points?)?)?'
    def scaled_field(effect, field, value, n, stat):
        if not stat:
            effect[field] = value
            return [effect]
        src = _stat_source(stat, ctx)
        if not src:
            return None
        effect[field] = 0.0
        effect['scaling'] = [{'source': src, 'per': round(value / float(n or 1), 6)}]
        return [effect]
    m = re.fullmatch(rf'absorption efficiency \+{N}%' + per, t)
    if m:
        return scaled_field({'type': 'pipican:resource_shield'}, 'efficiency', float(m.group(1)) / 100, m.group(2), m.group(3))
    rn = (ctx.get('resource_name') or '').lower()
    if rn and re.fullmatch(rf'{re.escape(rn)} works anywhere', t):
        return [{'type': 'pipican:resource_shield', 'absorb': 1.0}]
    m = re.fullmatch(rf'deadly aim bonus \+{N}%' + per, t)
    if m:
        return scaled_field({'type': 'pipican:deadly_aim'}, 'damage', float(m.group(1)) / 100, m.group(2), m.group(3))
    m = re.fullmatch(rf'(?:deadly )?aim time -{N}s' + per, t)
    if m:
        return scaled_field({'type': 'pipican:deadly_aim'}, 'hold_seconds', -float(m.group(1)), m.group(2), m.group(3))
    if re.fullmatch(r'deadly aim shots always crit', t):
        return [{'type': 'pipican:deadly_aim', 'crit': True}]
    m = re.fullmatch(rf'(?:your )?(?:animal )?forms drain {N}% (slower|faster)', t)
    if m:
        forms = [w['ability'] for n, (w, _) in NAMED.get(ctx.get('class_id'), {}).items() if n.endswith(' form') and w.get('type') == 'pipican:ability_active']
        if forms:
            k = float(m.group(1)) / 100 * (-1 if m.group(2) == 'slower' else 1)
            return [{'type': 'pipican:ability_mod', 'ability': f, 'drain': round(k, 6)} for f in forms]
    # "Icon of Faith bonuses double", "Stand Guard bonuses doubled": the named state's bonuses count twice as much.
    m = re.fullmatch(r'([a-z\' ]+?) bonuses (?:double|doubled)', t)
    if m and m.group(1) in NAMED.get(ctx.get('class_id'), {}):
        when, inner = NAMED[ctx['class_id']][m.group(1)]
        extra = []
        for e in inner:
            c = dict(e)
            if 'multiplier' in e and e['type'] in ('pipican:damage_dealt', 'pipican:damage_taken'):
                mult = e['multiplier']
                if mult <= 0 or 'scaling' in e:
                    return None
                c['multiplier'] = round((2 * mult - 1) / mult, 6)  # x m again would give m^2; this lands on 1 + 2(m - 1)
            elif e['type'] == 'pipican:attribute' and 'scaling' not in e:
                pass  # the same amount again
            else:
                return None
            extra.append(c)
        if extra:
            return [{'type': 'pipican:conditional', 'when': when, 'effects': extra}]
    # "+1% Weapon Mastery chance per 4 Discipline", "Weapon Mastery chance +15%": Weapon Mastery is a crit chance.
    m = re.fullmatch(rf'\+{N}% weapon mastery chance(?: per (?:{N} )?([a-z]+)(?: points?)?)?', t) or \
        re.fullmatch(rf'weapon mastery chance \+{N}%(?: per (?:{N} )?([a-z]+)(?: points?)?)?', t)
    if m:
        e = {'type': 'pipican:critical', 'chance': round(float(m.group(1)) / 100, 6)}
        if m.group(3):
            src = _stat_source(m.group(3), ctx)
            if not src:
                return None
            e['chance'] = 0.0
            e['chance_scaling'] = [{'source': src, 'per': round(float(m.group(1)) / 100 / float(m.group(2) or 1), 6)}]
        return [e]
    # "Firearm critical chance +0.3% per Trigger Finger" (firearms are crossbows here)
    m = re.fullmatch(rf'firearm critical chance \+{N}%(?: per (?:{N} )?([a-z]+(?: [a-z]+)?)(?: points?)?)?', t)
    if m:
        e = {'type': 'pipican:critical', 'weapons': '#pipican:weapons/crossbows', 'chance': round(float(m.group(1)) / 100, 6)}
        if m.group(3):
            src = _stat_source(m.group(3), ctx)
            if not src:
                return None
            e['chance'] = 0.0
            e['chance_scaling'] = [{'source': src, 'per': round(float(m.group(1)) / 100 / float(m.group(2) or 1), 6)}]
        return [e]
    # "allies near you +2% attack speed", "allies near you lose hunger 15% slower"
    m = re.fullmatch(r'(?:allies|other players|players) near you (?:get |gain |have )?(.+)', t)
    if m:
        rest = m.group(1)
        inner = _rest(rest, ctx) or (_rest('you ' + rest, ctx) if rest.startswith('lose ') else None)
        if inner:
            return [{'type': 'pipican:aura', 'radius': 8.0, 'effects': inner, 'include_self': False}]
    cursing = REACTION_NAMES.get(ctx.get('class_id'), set())
    if 'curse' in cursing:
        m = re.fullmatch(rf'curses last {N}s longer(?: per (?:{N} )?([a-z]+)(?: points?)?)?', t)
        if m:
            e = {'type': 'pipican:reaction_mod', 'name': 'curse', 'seconds': float(m.group(1))}
            if m.group(3):
                src = _stat_source(m.group(3), ctx)
                if not src:
                    return None
                e['seconds'] = 0.0
                e['scaling'] = [{'source': src, 'per': round(float(m.group(1)) / float(m.group(2) or 1), 6)}]
            return [e]
        m = re.fullmatch(rf'pain curse deals {N} per second', t)
        if m and float(m.group(1)) > 2:
            return [{'type': 'pipican:reaction_mod', 'name': 'curse', 'damage': float(m.group(1)) - 2}]
    # minions: "mobs hit by your minions get Weakness I for 3s", "minions also apply Slowness"
    m = re.fullmatch(r'mobs hit by your (?:minions|summons|servants|pets) get (.+)', t) or re.fullmatch(r'(?:minions|summons|servants) also apply (.+)', t)
    if m:
        import combat
        out = combat.outcome('give ' + m.group(1), 'victim')
        if out:
            return [{'type': 'pipican:on', 'event': 'pet_hit', 'actions': out}]
    # "Horde Master: +2 minions at a time, but each is 30% weaker" ("each is 30% weaker" alone)
    m = re.fullmatch(rf'each is {N}% weaker', t)
    if m:
        k = -float(m.group(1)) / 100
        return [{'type': 'pipican:summon_mod', 'damage': k, 'health': k}]
    # "Stand Guard triggers after 1s": the named stillness starts sooner (only the gap is added, no double bonus)
    m = re.fullmatch(rf'([a-z\' ]+?) triggers after {N}s', t)
    if m and m.group(1) in NAMED.get(ctx.get('class_id'), {}):
        when, inner = NAMED[ctx['class_id']][m.group(1)]
        if when.get('type') == 'pipican:still' and float(m.group(2)) < when.get('seconds', 0):
            early = {'type': 'pipican:all_of', 'conditions': [{'type': 'pipican:still', 'seconds': float(m.group(2))},
                                                              {'type': 'pipican:not', 'condition': when}]}
            return [{'type': 'pipican:conditional', 'when': early, 'effects': inner}]
    # "Quick Change: no cooldown between forms"
    if re.fullmatch(r'no cooldown between forms', t):
        forms = [w['ability'] for n, (w, _) in NAMED.get(ctx.get('class_id'), {}).items() if n.endswith(' form') and w.get('type') == 'pipican:ability_active']
        if forms:
            return [{'type': 'pipican:ability_mod', 'ability': f, 'cooldown_seconds': -3600.0} for f in forms]
    # healing range: "+3 blocks healing range", "+1 block healing range per 10 Vitality Flow"
    m = re.fullmatch(rf'\+{N} blocks? healing range(?: per (?:{N} )?([a-z]+(?: [a-z]+)?)(?: points?)?)?', t)
    if m:
        import abilities as ab
        healers = [(n, d) for (owner, n), d in ab.A.items() if owner == ctx.get('class_id')
                   and any(a.get('type') == 'pipican:heal' and a.get('radius') for a in d.get('actions', []))]
        if len(healers) == 1:
            aid = f"pipican:{ctx['class_id']}/" + re.sub(r'[^a-z0-9]+', '_', healers[0][0].lower().replace("'", '')).strip('_')
            e = {'type': 'pipican:ability_mod', 'ability': aid, 'radius': float(m.group(1))}
            if m.group(3):
                src = _stat_source(m.group(3), ctx)
                if not src:
                    return None
                e['radius'] = 0.0
                e['scaling'] = [{'source': src, 'per': round(float(m.group(1)) / float(m.group(2) or 1), 6)}]
            return [e]
    am = ability_mods(t, ctx) or reaction_mods(t, ctx) or glows(t, ctx) or static_mods(t, ctx)
    if am:
        return am
    # A tree node named after what it upgrades ("Worker of Miracles: heal amount 10") leaves the name out of its text.
    node = (ctx.get('node_name') or '').lower()
    abil = {n for n, (w, _) in NAMED.get(ctx.get('class_id'), {}).items() if w.get('type') == 'pipican:ability_active'}
    if node and not t.startswith(node) and (node in REACTIONS.get(ctx.get('class_id'), {}) or node in STATIC.get(ctx.get('class_id'), {}) or node in abil):
        am = reaction_mods(f'{node} {t}', ctx) or static_mods(f'{node} {t}', ctx) or ability_mods(f'{node} {t}', ctx)
        if am:
            return am
    named = NAMED.get(ctx.get('class_id'), {})
    for name, (when, inner) in sorted(named.items(), key=lambda kv: -len(kv[0])):
        n = re.escape(name)
        # "Berserk melee bonus +1% per 2 Fury", "Battle Stance bonuses +1% per 4 Resolve", "Scrutiny bonus +0.5% per point"
        m = re.fullmatch(rf'{n}( melee)? bonus(?:es)? \+{N}% per (?:{N} )?([a-z]+)(?: points?)?', t)
        if m:
            src = _stat_source(m.group(4), ctx)
            if src:
                per = float(m.group(2)) / 100 / float(m.group(3) or 1)
                only = (lambda e: e.get('weapons') == '#pipican:weapons/melee' or e.get('damage_types') == '#pipican:is_melee') if m.group(1) else None
                copies = _scaled_copy(inner, per, src, only)
                if copies:
                    return [{'type': 'pipican:conditional', 'when': when, 'effects': copies}]
        # "when you go Berserk, mobs within 6 blocks get Slowness I for 3s"
        m = re.fullmatch(rf'when you (?:go|enter|become) {n},? (.+)', t)
        if m:
            out = bonus._spend_outcome(m.group(1))
            if out:
                return [{'type': 'pipican:conditional', 'when': when, 'effects': [{'type': 'pipican:on', 'event': 'start', 'actions': out}]}]
        # "per 10 Vigilance, Stand Guard +5% damage" -> "+5% damage while Stand Guard" with the scaling prefix
        m = re.fullmatch(rf'(per {N} [a-z]+(?: points?)?,? )?{n} ([+-]{N}%? .+)', t)
        if m and 'bonus' not in m.group(3):
            eff, uns = fx.translate(f'{m.group(1) or ""}{m.group(3)}', ctx)
            if eff and not uns:
                return [{'type': 'pipican:conditional', 'when': when, 'effects': eff}]
    return mechanics(t, ctx)


def _effects_list(text, default_seconds=None):
    """"Weakness I and Slowness I for 8s" / "Nausea for 6s and Slowness I for 4s" -> effect entries, or None."""
    import combat
    names = '|'.join(sorted((re.escape(k) for k in combat.EFFECT_NAMES), key=len, reverse=True))
    parts = [p.strip() for p in re.split(r',\s*(?:and\s+)?|\s+and\s+', text.strip()) if p.strip()]
    out, pending = [], []
    for p in parts:
        m = re.fullmatch(rf'({names})(?: (i|ii|iii|iv|v))?(?: for {N}s)?', p)
        if not m:
            return None
        pending.append((m.group(1), m.group(2)))
        if m.group(3):
            out += [combat._effect_entry(n, float(m.group(3)), lv) for n, lv in pending]
            pending = []
    if pending:
        if default_seconds is None:
            return None
        out += [combat._effect_entry(n, default_seconds, lv) for n, lv in pending]
    return out


def _immune_boost(text):
    """"immune to Slowness, Weakness and Nausea for 5s" -> a boost, or None."""
    import combat
    m = re.fullmatch(rf'(?:you are |you\'re |become )?immune to (.+) for {N}s', text)
    if not m:
        return None
    eff = combat.immune('immune to ' + m.group(1))
    return {'type': 'pipican:boost', 'seconds': float(m.group(2)), 'effects': eff} if eff else None


def _heal_and_boost(text, ctx):
    """"heal 6 HP and gain +20% damage for 5s" -> actions, or None."""
    import bonus
    acts = []
    for part in re.split(r'\s+and\s+(?=gain|heal|get)', text.strip()):
        m = re.fullmatch(rf'heal {N} ?(?:hp|health)', part)
        if m:
            acts.append({'type': 'pipican:heal', 'target': 'self', 'amount': float(m.group(1))})
            continue
        m = re.fullmatch(rf'(?:gain|get) (.+?) for {N}s', part)
        b = bonus._boost(m.group(1), m.group(2), ctx) if m else None
        if not b:
            return None
        acts.append(b)
    return acts or None


def _rest(text, ctx):
    import effects as fx
    eff, uns = fx.translate(text, dict(ctx, implicit_per_point=False))
    return eff if eff and not uns else None


def _aura_word(word, cid):
    """The aura a line means: "song" -> "song" if some aura of the class is called "... Song"; "aura" -> all."""
    word = word.strip().lower()
    if word in ('aura', 'all your aura', 'all aura', 'your aura'):
        return ''
    word = re.sub(r'\s+aura$', '', word)
    for name in AURAS.get(cid, ()):
        if word == name or word in name.split() or word.split()[-1] in name.split():
            return word.split()[-1] if word not in name else word
    return None


def _per(m_per, m_stat, ctx):
    """" per 10 Resonance" / " per point" -> special_stat scaling divisor, or None if no scaling / not the special stat."""
    if not m_stat:
        return 1.0, False
    if not _stat_source(m_stat, ctx):
        return None, False
    return float(m_per or 1), True


def auras(t, ctx):
    cid = ctx.get('class_id')
    if not AURAS.get(cid):
        return None
    words = r'([a-z][a-z ]*?)'
    # range: "Inspiring Song range +1 block per 10 Resonance", "+1 block Song range", "+3 blocks chant range",
    # "Prayer aura range -2 blocks", "per 10 Command, +1 block to all your aura ranges"
    m = re.fullmatch(rf'{words} reduction (doubled|tripled)', t)
    if m and _aura_word(m.group(1), cid) is not None:
        return [{'type': 'pipican:aura_mod', 'name': _aura_word(m.group(1), cid), 'strength': 1.0 if m.group(2) == 'doubled' else 2.0}]
    m = re.fullmatch(rf'{words} reduction \+{N}% per (?:{N} )?([a-z]+)(?: points?)?', t)
    if m and _aura_word(m.group(1), cid) is not None and _stat_source(m.group(4), ctx):
        return [{'type': 'pipican:aura_mod', 'name': _aura_word(m.group(1), cid), 'effects': [{'type': 'pipican:damage_taken', 'multiplier': 1.0,
                 'scaling': [{'source': 'special_stat', 'per': round(-float(m.group(2)) / 100 / float(m.group(3) or 1), 6)}]}]}]
    m = re.fullmatch(rf'\+{N}% {words} reduction', t)
    if m and _aura_word(m.group(2), cid) is not None:
        return [{'type': 'pipican:aura_mod', 'name': _aura_word(m.group(2), cid),
                 'effects': [{'type': 'pipican:damage_taken', 'multiplier': round(1 - float(m.group(1)) / 100, 6)}]}]
    m = re.fullmatch(rf'{words} gives \+{N}% more (speed|movement speed|damage|armor)', t)
    if m and _aura_word(m.group(1), cid) is not None:
        eff = _rest(f'+{m.group(2)}% {"movement speed" if m.group(3) == "speed" else m.group(3)}', ctx)
        if eff:
            return [{'type': 'pipican:aura_mod', 'name': _aura_word(m.group(1), cid), 'effects': eff}]
    m = re.fullmatch(rf'{words} range ([+-]{N})(?: blocks?)?(?: per (?:{N} )?([a-z]+)(?: points?)?)?', t) or \
        re.fullmatch(rf'([+-]?{N}) blocks? (?:to )?{words} ranges?(?: per (?:{N} )?([a-z]+)(?: points?)?)?()', t)
    if m:
        if m.re.pattern.startswith(words):
            word, amount, per, stat = m.group(1), m.group(2), m.group(4), m.group(5)
        else:
            amount, word, per, stat = m.group(1), m.group(3), m.group(4), m.group(5)
        name = _aura_word(word, cid)
        div, scaled = _per(per, stat, ctx)
        if name is not None and div:
            e = {'type': 'pipican:aura_mod', 'name': name}
            if scaled:
                e['scaling'] = [{'source': 'special_stat', 'per': round(float(amount) / div, 6)}]
            else:
                e['radius'] = float(amount)
            return [e]
    # strength: "Song bonuses doubled", "+5% Song strength", "chant bonuses +50%", "+1% strength of your Song buffs" (per point)
    m = re.fullmatch(rf'{words} bonuses (doubled|tripled)', t)
    if m and _aura_word(m.group(1), cid) is not None:
        return [{'type': 'pipican:aura_mod', 'name': _aura_word(m.group(1), cid), 'strength': 1.0 if m.group(2) == 'doubled' else 2.0}]
    m = re.fullmatch(rf'\+{N}% {words} strength|{words} bonuses \+{N}%|\+{N}% strength of your {words} buffs', t)
    if m:
        num = m.group(1) or m.group(4) or m.group(5)
        word = m.group(2) or m.group(3) or m.group(6)
        name = _aura_word(word, cid)
        if name is not None:
            e = {'type': 'pipican:aura_mod', 'name': name}
            if ctx.get('implicit_per_point'):
                e['strength_scaling'] = [{'source': 'special_stat', 'per': round(float(num) / 100, 6)}]
            else:
                e['strength'] = round(float(num) / 100, 6)
            return [e]
    # extra bonuses: "+3% Song damage bonus", "+0.5% Battle Chant damage bonus" (per point), "Song also gives +1 armor",
    # "players in your chant get +5% attack speed", "Prayer aura also gives allies +5% damage reduction",
    # "allies in your Prayer aura deal +5% more damage"
    m = re.fullmatch(rf'\+{N}% (?:more )?{words} damage bonus', t)
    extra = None
    if m:
        word, extra = m.group(2), f'+{m.group(1)}% damage'
    m = m or re.fullmatch(rf'{words}(?: aura)? also gives(?: allies)? (.+)', t)
    if m and extra is None and m.re.pattern.startswith(words):
        word, extra = m.group(1), m.group(2)
    if extra is None:
        m = re.fullmatch(rf'(?:players|allies|everyone) in your {words}(?: aura)? (?:get|gets|deal|have) (.+)', t)
        if m:
            word, extra = m.group(1), m.group(2)
            extra = re.sub(r'^(\w+(?: \w+)?) (i|ii|iii|iv)$', r'permanent \1 \2', extra)
    if extra is None:
        m = re.fullmatch(rf'(?:players|allies|everyone) in your {words}(?: aura)? regenerate \+?{N}% faster', t)
        if m:
            word, extra = m.group(1), f'+{m.group(2)}% natural regeneration'
    if extra is not None:
        name = _aura_word(word, cid)
        extra = re.sub(r'^\+?(\d+(?:\.\d+)?)% damage reduction$', r'-\1% damage taken', extra)
        extra = re.sub(r'^\+?(\d+(?:\.\d+)?)% more damage$', r'+\1% damage', extra)
        if name is not None:
            if ctx.get('implicit_per_point'):
                mm = re.fullmatch(rf'\+{N}% damage', extra)
                eff = [{'type': 'pipican:damage_dealt', 'multiplier': 1.0, 'scaling': [{'source': 'special_stat', 'per': round(float(mm.group(1)) / 100, 6)}]}] if mm else None
            else:
                eff = _rest(extra, dict(ctx, implicit_per_point=False))
            if eff:
                return [{'type': 'pipican:aura_mod', 'name': name, 'effects': eff}]
    # "you also gain your own Prayer bonus"
    m = re.fullmatch(rf'you also (?:gain|get) your own {words} bonus(?:es)?', t)
    if m and _aura_word(m.group(1), cid) is not None:
        return [{'type': 'pipican:aura_mod', 'name': _aura_word(m.group(1), cid), 'include_self': True}]
    return None


def summons(t, scal):
    """Minion / servant / summon upgrades."""
    who = r'(?:minions?|servants?|summons?|summoned creatures?|conjured creatures?)'
    m = re.fullmatch(rf'\+?{N}% {who} damage|{who} \+{N}% damage|{who} damage \+{N}%', t)
    if m:
        v, s = scal(float(m.group(1) or m.group(2) or m.group(3)))
        e = {'type': 'pipican:summon_mod', 'damage': round(v / 100, 6)}
        if s:
            e['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in s]
        return [e]
    m = re.fullmatch(rf'\+?{N}% damage and health for (?:your {who}|undead you command)', t)
    if m:
        v, s = scal(float(m.group(1)))
        e = {'type': 'pipican:summon_mod', 'damage': round(v / 100, 6), 'health': round(v / 100, 6)}
        if s:
            e['scaling'] = [{'source': src, 'per': round(per / 100, 6)} for src, per in s]
        return [e]
    m = re.fullmatch(rf'{who} last {N}s longer', t)
    if m:
        return [{'type': 'pipican:summon_mod', 'seconds': float(m.group(1))}]
    m = re.fullmatch(rf'\+{N} {who}(?: at a time)?', t)
    if m and float(m.group(1)) == int(float(m.group(1))):
        v, s = scal(float(m.group(1)))
        if s:
            return None  # "+1 minion per 20 Summoning": counts can't scale
        return [{'type': 'pipican:summon_mod', 'count': int(float(m.group(1)))}]
    return None


ITEM_CONDS = {'bound weapon': {'type': 'pipican:item_rule', 'rule': 'bound_weapon'},
              'sigil weapon': {'type': 'pipican:item_rule', 'rule': 'sigil_weapon'},
              'staff': {'type': 'pipican:holding', 'items': ['minecraft:blaze_rod', 'minecraft:stick']}}


def _bonus(kind, value, per_stat=None):
    e = {'type': 'pipican:bonus', 'kind': kind}
    if per_stat:
        e['amount'] = 0.0
        e['scaling'] = [{'source': 'special_stat', 'per': round(value / per_stat, 6)}]
    else:
        e['amount'] = round(value, 6)
    return e


def durability(t, ctx):
    """Durability lines -> pipican:bonus durability / armor_durability / weapon_durability."""
    per = rf'(?: per (?:{N} )?([a-z]+)(?: points?)?)?'

    def scaled(value, n, stat):
        if not stat:
            return value, None
        if not _stat_source(stat, ctx):
            return None, None
        return value, float(n or 1)

    m = re.fullmatch(rf'(?:your )?gear(?: you wear and hold)? loses durability {N}% (slower|faster)', t)
    if m:
        return [_bonus('durability', float(m.group(1)) / 100 * (-1 if m.group(2) == 'slower' else 1))]
    m = re.fullmatch(rf'\+{N}% durability on everything you wear', t)
    if m:
        v = float(m.group(1))
        return [_bonus('armor_durability', -v / (100 + v))]
    m = re.fullmatch(rf'\+{N}% chance that a durability point is not used' + per, t)
    if m:
        v, d = scaled(-float(m.group(1)) / 100, m.group(2), m.group(3))
        return None if v is None else [_bonus('durability', v, d)]
    m = re.fullmatch(rf'armor durability loss -{N}%' + per, t)
    if m:
        v, d = scaled(-float(m.group(1)) / 100, m.group(2), m.group(3))
        return None if v is None else [_bonus('armor_durability', v, d)]
    items = dict(ITEM_CONDS, **{'favored weapon': {'type': 'pipican:holding_favored'}})
    names = '|'.join(re.escape(k) for k in items)
    m = re.fullmatch(rf'(?:your )?({names})s? (?:loses?|lose) durability {N}% (slower|faster) per (?:{N} )?([a-z]+)(?: points?)?', t)
    if m:
        v, d = scaled(float(m.group(2)) / 100 * (-1 if m.group(3) == 'slower' else 1), m.group(4), m.group(5))
        if v is not None:
            return [{'type': 'pipican:conditional', 'when': items[m.group(1)], 'effects': [_bonus('weapon_durability', v, d)]}]
        return None
    m = re.fullmatch(rf'(?:your )?({names}) slowly repairs {N} durability every 10s', t)
    if m:
        return [{'type': 'pipican:conditional', 'when': items[m.group(1)], 'effects': [_bonus('weapon_repair', float(m.group(2)))]}]
    m = re.fullmatch(rf'armor never drops below {N}% durability(?: while worn)?', t)
    if m:
        return [_bonus('armor_floor', float(m.group(1)) / 100)]
    m = re.fullmatch(rf'(?:your )?({names})s? (?:loses?|lose) durability {N}% (slower|faster)', t) or \
        re.fullmatch(rf'(?:your )?({names})s? durability loss ()()-{N}%' + per, t)
    if m:
        if m.group(3) is not None and m.group(3) != '':
            v, d = -float(m.group(2)) / 100 * (1 if m.group(3) == 'slower' else -1), None
        else:
            v, d = scaled(-float(m.group(4)) / 100, m.group(5), m.group(6))
            if v is None:
                return None
        return [{'type': 'pipican:conditional', 'when': items[m.group(1)], 'effects': [_bonus('weapon_durability', v, d)]}]
    m = re.fullmatch(rf'(?:your )?({names}) (?:can\'t break|never breaks|can\'t be destroyed)', t)
    if m:
        return [{'type': 'pipican:conditional', 'when': items[m.group(1)], 'effects': [_bonus('weapon_durability', -1.0)]}]
    return None


def reactions_more(t, ctx):
    """More reaction phrasings: "mobs you hit get Slowness I for 2s" (optionally "in water" / "in darkness"),
    "when you take damage, become invisible for 2s (60s cooldown)", "after teleporting, ...", "hostile mobs within 12 blocks
    glow while you sneak"."""
    import conditions as cond
    m = re.fullmatch(r'(?:mobs you hit|your hits)(?: (in .+?|while .+?))? (?:get|give(?: them)?) (.+)', t)
    if m:
        entries = _effects_list(m.group(2), 3.0)
        if entries:
            r = {'type': 'pipican:on', 'event': 'hit', 'actions': [{'type': 'pipican:effect', 'target': 'victim', 'effects': entries}]}
            if m.group(1):
                when = cond.parse(m.group(1), ctx)
                return [{'type': 'pipican:conditional', 'when': when, 'effects': [r]}] if when else None
            return [r]
    m = re.fullmatch(rf'when you take damage,? (?:become |you become |turn )?(invisible|.+?) for {N}s(?: with an? {N}s cooldown)?', t)
    if m:
        entries = [{'effect': 'minecraft:invisibility', 'seconds': float(m.group(2))}] if m.group(1) == 'invisible' else _effects_list(f'{m.group(1)} for {m.group(2)}s')
        if entries:
            r = {'type': 'pipican:on', 'event': 'hurt', 'actions': [{'type': 'pipican:effect', 'effects': entries}]}
            if m.group(3):
                r['cooldown_ticks'] = int(float(m.group(3)) * 20)
            return [r]
    m = re.fullmatch(r'after (?:any )?teleport(?:ing)?(?: \(pearl or blink\))?,? (.+)', t)
    if m:
        what = m.group(1)
        mm = re.fullmatch(r'mobs near your arrival point get (.+)', what)
        if mm:
            entries = _effects_list(mm.group(1), 3.0)
            if entries:
                return [{'type': 'pipican:on', 'event': 'teleport', 'actions': [{'type': 'pipican:effect', 'target': 'hostiles', 'radius': 4.0, 'effects': entries}]}]
        entries = _effects_list(what)
        if entries:
            return [{'type': 'pipican:on', 'event': 'teleport', 'actions': [{'type': 'pipican:effect', 'effects': entries}]}]
    m = re.fullmatch(rf'hostile mobs within {N} blocks glow (?:while|when) (.+)', t)
    if m:
        when = cond.parse('while ' + m.group(2), ctx)
        if when:
            return [{'type': 'pipican:conditional', 'when': when, 'effects': [{'type': 'pipican:on', 'event': 'second', 'actions': [
                {'type': 'pipican:effect', 'target': 'hostiles', 'radius': float(m.group(1)), 'effects': [{'effect': 'minecraft:glowing', 'seconds': 2.0}]}]}]}]
    return None


def triggers_more(t, ctx):
    """Instrument, lightning, kill-heal and resource-threshold reactions; "X also gives Y" for named states."""
    import bonus
    import conditions as cond
    rn = (ctx.get('resource_name') or '').lower()
    r = None
    if rn:
        r = '(?:' + re.escape(rn) + ('|' + re.escape(rn.split()[-1]) if ' ' in rn else '') + ')'
    # "playing a goat horn or note block refills 10 Melody with a 30s cooldown"
    m = re.fullmatch(rf'(?:playing|blowing) a goat horn(?: or (?:a )?note block)? (.+?)(?: with an? {N}s cooldown)?', t)
    if m:
        what, cd = m.group(1), m.group(2)
        acts = None
        mm = re.fullmatch(rf'refills {N} (.+)', what)
        if mm and r and re.fullmatch(r, mm.group(2)):
            acts = [{'type': 'pipican:resource', 'amount': float(mm.group(1))}]
        mm = re.fullmatch(rf'gives players within {N} blocks (.+)', what)
        if mm:
            entries = _effects_list(mm.group(2))
            if entries:
                acts = [{'type': 'pipican:effect', 'target': 'players', 'radius': float(mm.group(1)), 'effects': entries},
                        {'type': 'pipican:effect', 'effects': entries}]
        if acts:
            out = {'type': 'pipican:on', 'event': 'instrument', 'actions': acts}
            if cd:
                out['cooldown_ticks'] = int(float(cd) * 20)
            return [out]
    # "being struck refills Charge and gives +6 max health and Strength I for 60s", "a lightning strike drains all Spare Parts"
    m = re.fullmatch(rf'being struck(?: by lightning)? refills {r} and gives (.+?) and (.+?) for {N}s', t) if r else None
    if m:
        eff = _rest(m.group(1), ctx)
        entries = _effects_list(f'{m.group(2)} for {m.group(3)}s')
        if eff and entries:
            r = {'type': 'pipican:on', 'event': 'lightning', 'name': 'struck by lightning', 'cooldown_ticks': 40, 'actions': [
                {'type': 'pipican:resource', 'percent': 1.0}, {'type': 'pipican:boost', 'seconds': float(m.group(3)), 'effects': eff},
                {'type': 'pipican:effect', 'effects': entries}]}
            REACTIONS.setdefault(ctx.get('class_id'), {}).setdefault('struck by lightning', r)
            return [r]
    m = re.fullmatch(rf'a lightning strike drains all {r}', t) if r else None
    if m:
        return [{'type': 'pipican:on', 'event': 'lightning', 'cooldown_ticks': 40, 'actions': [{'type': 'pipican:resource', 'percent': -1.0}]}]
    # "every kill refills 10% health"
    m = re.fullmatch(rf'every kill (?:refills|restores|heals you) {N}% (?:of your )?(?:health|hp)', t)
    if m:
        return [{'type': 'pipican:on', 'event': 'kill', 'actions': [{'type': 'pipican:heal', 'target': 'self', 'percent': float(m.group(1)) / 100}]}]
    # "once per rest, refill 50% Mana instantly when you drop below 10%"
    m = re.fullmatch(rf'(once per rest, )?refill {N}% {r} instantly when you drop below {N}%', t) if r else None
    if m:
        out = {'type': 'pipican:on', 'event': 'start', 'actions': [{'type': 'pipican:resource', 'percent': float(m.group(2)) / 100}]}
        if m.group(1):
            out['once_per_rest'] = True
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:resource', 'max': float(m.group(3)) / 100}, 'effects': [out]}]
    # "Battle Stance also gives knockback immunity", "Ecstasy also gives Regeneration I", "Stigmata also gives Resistance I"
    m = re.fullmatch(r'forms also give (.+)', t)
    if m:
        forms = [w for n, (w, _) in NAMED.get(ctx.get('class_id'), {}).items() if n.endswith(' form') and w.get('type') == 'pipican:ability_active']
        eff = _rest(m.group(1), ctx) if forms else None
        if eff:
            return [{'type': 'pipican:conditional', 'when': forms[0] if len(forms) == 1 else {'type': 'pipican:any_of', 'conditions': forms}, 'effects': eff}]
    m = re.fullmatch(r'([a-z][a-z \']*?) also gives (.+)', t)
    if m:
        word = m.group(1).strip()
        for name, (when, _) in NAMED.get(ctx.get('class_id'), {}).items():
            if word == name or word in name.split():
                extra = m.group(2)
                extra = re.sub(r'^(\w+(?: \w+)?) (i|ii|iii|iv)$', r'permanent \1 \2', extra)
                eff = _rest(extra, ctx)
                if eff:
                    return [{'type': 'pipican:conditional', 'when': when, 'effects': eff}]
    return None


CURSE_KINDS = [
    (r'stops attacking', lambda s: [{'type': 'pipican:control', 'target': 'victim', 'mode': 'pacify', 'seconds': s}]),
    (r'(slowness|weakness|poison|wither|blindness)(?: (i|ii|iii|iv))?', None),
    (rf'{N} damage per second', None),
]



def world(low, ctx):
    """Single-mechanic lines with their own game hooks: pets, brewing, loot chests, mob misses, servants, refunds.
    `low` is lowercase with the label stripped. -> effects or None."""
    import combat
    sp = (ctx.get('special_name') or '').lower()
    cid = ctx.get('class_id')
    pct = lambda v: round(float(v) / 100, 6)

    def bonus_kind(kind, amount, per=None):
        e = {'type': 'pipican:bonus', 'kind': kind, 'amount': 0.0 if per else amount}
        if per:
            e['scaling'] = [{'source': 'special_stat', 'per': per}]
        return [e]

    rn = (ctx.get('resource_name') or '').lower()
    eff = '|'.join(sorted((re.escape(k) for k in combat.EFFECT_NAMES), key=len, reverse=True))
    import conditions as cond
    # Steal Spell: a warded spell refills the bar; chance upgrades.
    m = re.fullmatch(rf'you gain {N} ([a-z ]+) instead', low)
    if m and m.group(2) == rn:
        return [{'type': 'pipican:on', 'event': 'ward', 'actions': [{'type': 'pipican:resource', 'amount': float(m.group(1))}]}]
    m = re.fullmatch(rf'\+{N}% steal spell chance', low) or re.fullmatch(rf'\+{N}% chance to steal an incoming harmful effect', low)
    if m:
        if ctx.get('implicit_per_point'):
            return bonus_kind('spell_ward', 0.0, pct(m.group(1)))
        return bonus_kind('spell_ward', pct(m.group(1)))
    if cid == 'warlock' and re.fullmatch(r'steal spell chance doubles', low):
        # Doubles the base 15% (per-point and upgrade chances aren't doubled).
        return bonus_kind('spell_ward', 0.15)
    # "in water, take 20% less damage from weapons and projectiles" (+ "Watery Form reduction +0.2% per Tide")
    m = re.fullmatch(rf'(in water|while wearing 4 heavy pieces), take {N}% less damage from weapons and projectiles', low)
    if m and (when := cond.parse(m.group(1), ctx)):
        k = round(1 - float(m.group(2)) / 100, 6)
        return [{'type': 'pipican:conditional', 'when': when, 'effects': [
            {'type': 'pipican:damage_taken', 'damage_types': '#pipican:is_melee', 'multiplier': k},
            {'type': 'pipican:damage_taken', 'damage_types': '#minecraft:is_projectile', 'multiplier': k}]}]
    m = re.fullmatch(rf'watery form reduction \+{N}% per ([a-z]+)', low)
    if m and m.group(2) == sp and (when := cond.parse('in water', ctx)):
        sc = [{'source': 'special_stat', 'per': round(-float(m.group(1)) / 100, 6)}]
        return [{'type': 'pipican:conditional', 'when': when, 'effects': [
            {'type': 'pipican:damage_taken', 'damage_types': '#pipican:is_melee', 'multiplier': 1.0, 'scaling': sc},
            {'type': 'pipican:damage_taken', 'damage_types': '#minecraft:is_projectile', 'multiplier': 1.0, 'scaling': sc}]}]
    # Inquisitor scrutiny
    m = re.fullmatch(rf'scrutinised mobs within {N} blocks get weakness i', low)
    if m:
        return bonus_kind('scrutiny_weakness', float(m.group(1)))
    m = re.fullmatch(rf'scrutinised mobs take \+{N}% damage from everyone', low)
    if m:
        return bonus_kind('scrutiny_expose', pct(m.group(1)))
    m = re.fullmatch(rf'scrutinise {N}s faster per {N} ([a-z]+)', low)
    if m and m.group(3) == sp:
        return [{'type': 'pipican:scrutiny', 'scaling': [{'source': 'special_stat', 'per': round(-float(m.group(1)) / float(m.group(2)), 6)}]}]
    m = re.fullmatch(rf'grindstone disenchanting gives \+{N}% more xp per (?:{N} )?([a-z]+)', low)
    if m and m.group(3) == sp:
        return bonus_kind('grindstone_xp', 0.0, round(float(m.group(1)) / 100 / float(m.group(2) or 1), 6))
    if re.fullmatch(r'optimization no longer resets on death', low):
        return bonus_kind('keep_optimization', 1.0)
    if re.fullmatch(r'(?:it|your bound weapon|bound weapon) never drops on death', low):
        return bonus_kind('keep_hotbar_weapon', 1.0)
    # Oracle: Ecstasy upgrades
    ecstasy = next(((n, w) for n, (w, _) in NAMED.get(cid, {}).items() if n.endswith('ecstasy') and w.get('type') == 'pipican:ability_active'), None)
    if ecstasy:
        ename, ewhen = ecstasy
        m = re.fullmatch(r'the after-effects of (?:divine )?ecstasy last half as long', low)
        if m and f'{ename} after-effects' in REACTIONS.get(cid, {}):
            return [{'type': 'pipican:reaction_mod', 'name': f'{ename} after-effects', 'duration': -0.5}]
        m = re.fullmatch(rf'(?:divine )?ecstasy also gives ({eff}) (i|ii); after-effects add ({eff}) {N}s', low)
        if m and f'{ename} after-effects' in REACTIONS.get(cid, {}):
            return [{'type': 'pipican:conditional', 'when': ewhen, 'effects': [{'type': 'pipican:mob_effect',
                     'effect': 'minecraft:' + combat.EFFECT_NAMES[m.group(1)], 'amplifier': combat.ROMAN[m.group(2)]}]},
                    {'type': 'pipican:reaction_mod', 'name': f'{ename} after-effects', 'actions': [{'type': 'pipican:effect', 'target': 'self',
                     'effects': [{'effect': 'minecraft:' + combat.EFFECT_NAMES[m.group(3)], 'seconds': float(m.group(4))}]}]}]
        m = re.fullmatch(rf"(?:divine )?ecstasy bonuses \+{N}%, but you can'?t leave it early and it drains {N}% faster", low)
        if m:
            return [{'type': 'pipican:ability_mod', 'ability': ewhen['ability'], 'strength': pct(m.group(1)), 'drain': pct(m.group(2)), 'locked': True}]
    # Sorcerer: Strain detonates when full (Sorcerous Outburst) and its upgrades.
    m = re.fullmatch(rf'when ([a-z ]+) reaches max it detonates: an explosion around you \(power {N} \+ 1 per {N} ([a-z]+)\) that damages everything nearby except you, breaks no blocks, and resets ([a-z ]+) to 0\. allies are not safe', low)
    if m and m.group(1) == rn and m.group(5) == rn and m.group(4) == sp:
        r = {'type': 'pipican:on', 'event': 'resource_full', 'name': 'sorcerous outburst',
             'actions': [{'type': 'pipican:explode', 'power': float(m.group(2))}, {'type': 'pipican:resource', 'percent': -1.0}]}
        REACTIONS.setdefault(cid, {}).setdefault('sorcerous outburst', r)
        return [r, {'type': 'pipican:reaction_mod', 'name': 'sorcerous outburst', 'radius': 0.0,
                    'scaling': [{'source': 'special_stat', 'per': round(1 / float(m.group(3)), 6)}]}]
    if cid == 'sorcerer':
        m = re.fullmatch(rf'sorcerous outburst explosion damage \+{N} per ([a-z]+) points?', low)
        if m and m.group(2) == sp:
            return [{'type': 'pipican:reaction_mod', 'name': 'sorcerous outburst', 'damage': 0.0, 'scaling': [{'source': 'special_stat', 'per': float(m.group(1))}]}]
        m = re.fullmatch(rf'outburst radius \+{N} blocks?', low)
        if m:
            # Explosion power and radius grow together (radius is about twice the power).
            return [{'type': 'pipican:reaction_mod', 'name': 'sorcerous outburst', 'radius': round(float(m.group(1)) / 2, 6)}]
        m = re.fullmatch(rf'outbursts deal double damage, but you take {N}% of the outburst damage yourself', low)
        if m:
            return [{'type': 'pipican:reaction_mod', 'name': 'sorcerous outburst', 'strength': 1.0}, bonus_kind('explode_self_share', pct(m.group(1)))[0]]
    # Destroyer: the spend shockwave ("destruction released") and the backlash.
    if cid == 'destroyer':
        m = re.fullmatch(rf'shockwave radius \+{N} per {N} ([a-z]+)', low)
        if m and m.group(3) == sp:
            return [{'type': 'pipican:reaction_mod', 'name': 'destruction released', 'radius': 0.0,
                     'scaling': [{'source': 'special_stat', 'per': round(float(m.group(1)) / float(m.group(2)), 6)}]}]
        m = re.fullmatch(rf'shockwave damage \+{N} per ([a-z]+) points?', low)
        if m and m.group(2) == sp:
            return [{'type': 'pipican:reaction_mod', 'name': 'destruction released', 'damage': 0.0, 'scaling': [{'source': 'special_stat', 'per': float(m.group(1))}]}]
        if re.fullmatch(r'shockwave knocks mobs back', low):
            return [{'type': 'pipican:reaction_mod', 'name': 'destruction released',
                     'actions': [{'type': 'pipican:push', 'target': 'hostiles', 'radius': 3.0, 'strength': 0.6}]}]
        m = re.fullmatch(rf'every time you spend ([a-z ]+), take {N} damage', low)
        if m and m.group(1) == rn:
            return [{'type': 'pipican:on', 'event': 'spend', 'name': 'backlash', 'actions': [{'type': 'pipican:hurt_self', 'amount': float(m.group(2))}]}]
        m = re.fullmatch(r'no self-damage when spending ([a-z ]+)', low)
        if m and m.group(1) == rn:
            return [{'type': 'pipican:reaction_mod', 'name': 'backlash', 'damage': -1000.0}]
    # Traveler Thunderclap: the arrival slow also knocks mobs back.
    if re.fullmatch(r'arrivals knock mobs back', low) and 'spatial disturbance' in REACTIONS.get(cid, {}):
        push = dict(_first_targets(REACTIONS[cid]['spatial disturbance']) or {'target': 'hostiles', 'radius': 4.0}, type='pipican:push', strength=0.6)
        return [{'type': 'pipican:reaction_mod', 'name': 'spatial disturbance', 'actions': [push]}]
    # Hexer: "while a curse you placed is active, you take 10% more damage"
    m = re.fullmatch(r'while a curse you placed is active, (.+)', low)
    if m and (inner := _rest(m.group(1), ctx)):
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'marking'}, 'effects': inner}]
    # Gladiator: "Bloodlust only fills while another player is within 32 blocks"
    m = re.fullmatch(rf'([a-z ]+) only fills while another player is within {N} blocks', low)
    if m and m.group(1) == rn and (inner := _rest(f'-100% {ctx.get("resource_name")} regen', ctx)):
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:nearby', 'entities': 'players', 'radius': float(m.group(2)), 'min': 0, 'max': 0},
                 'effects': inner}]
    # Conjurer Steady Gate: the periodic minion comes every N seconds.
    m = re.fullmatch(rf'minion spawn every {N}s', low)
    if m:
        for name, r in REACTIONS.get(cid, {}).items():
            if r.get('cooldown_ticks') and any(a.get('type') == 'pipican:summon' for a in r.get('actions', [])):
                return [{'type': 'pipican:reaction_mod', 'name': r.get('name', name),
                         'cooldown': round(float(m.group(1)) * 20 / r['cooldown_ticks'] - 1, 6)}]
    # Technomancer gadgets: stored next-hit charges from spending Voltage.
    m = re.fullmatch(rf"each time you spend ([a-z ]+), you get a '?gadget'? charge - your next attack or check \(dodge, crit\) is improved \(\+{N}%\)", low)
    if m and m.group(1) == rn:
        r = {'type': 'pipican:on', 'event': 'spend', 'name': 'invention', 'actions': [{'type': 'pipican:empower', 'target': 'self',
             'multiplier': round(1 + float(m.group(2)) / 100, 6), 'seconds': 600.0, 'charges': 1}]}
        REACTIONS.setdefault(cid, {}).setdefault('invention', r)
        return [r]
    m = re.fullmatch(rf'per {N} ([a-z]+),? \+{N} gadget charges? stored|\+{N} gadget charges? stored per {N} ([a-z]+)', low)
    if m:
        n, stat, k = (m.group(1), m.group(2), m.group(3)) if m.group(1) else (m.group(5), m.group(6), m.group(4))
        if stat == sp:
            return bonus_kind('empower_charges', 0.0, round(float(k) / float(n), 6))
    m = re.fullmatch(rf'\+{N} gadget charges?', low)
    if m:
        return bonus_kind('empower_charges', float(m.group(1)))
    m = re.fullmatch(rf'lightning arcs to {N} more mobs', low)
    # (the on-hit lightning reactions are named "lightning")
    if m:
        return [{'type': 'pipican:reaction_mod', 'name': 'lightning', 'actions': [{'type': 'pipican:lightning', 'others': int(float(m.group(1)))}]}]
    if re.fullmatch(r'burning mobs spread fire to adjacent mobs', low):
        return bonus_kind('wildfire', 1.0)
    m = re.fullmatch(rf'mobs within {N} blocks prefer to target you', low)
    if m:
        return bonus_kind('taunt_radius', float(m.group(1)))
    # Quarries on the move: "When your marked target moves away, you get Speed II for 2s", "when prey flees, ..."
    m = re.fullmatch(r'when (?:your marked target moves away|prey flees|your prey flees), you get (.+)', low)
    if m and (out := combat.outcome('get ' + m.group(1), 'self')):
        r = {'type': 'pipican:on', 'event': 'quarry_flee', 'name': 'pursuit', 'actions': out}
        REACTIONS.setdefault(cid, {}).setdefault('pursuit', r)
        return [r]
    m = re.fullmatch(rf'speed boost lasts {N}s longer', low)
    if m and 'pursuit' in REACTIONS.get(cid, {}):
        return [{'type': 'pipican:reaction_mod', 'name': 'pursuit', 'seconds': float(m.group(1))}]
    m = re.fullmatch(rf'\+{N}% movement speed toward (?:the marked target|your prey|your marked target)', low)
    if m:
        return bonus_kind('quarry_pursuit', pct(m.group(1)))
    m = re.fullmatch(rf'per {N} ([a-z]+),? \+{N}% attack speed against the foe', low)
    if m and m.group(2) == sp:
        return bonus_kind('quarry_attack_speed', 0.0, round(float(m.group(3)) / 100 / float(m.group(1)), 6))
    m = re.fullmatch(rf'\+{N} blocks? reach against (?:the )?challenged foe', low)
    if m:
        return bonus_kind('quarry_reach', float(m.group(1)))
    # Runesmith sigils. The definition itself is built in (renamed weapons, trimmed armor).
    if re.fullmatch(r"armor with a trim and weapons you'?ve renamed at an anvil count as sigil-marked", low):
        return []
    sigil_weapon = {'type': 'pipican:item_rule', 'rule': 'sigil_weapon'}
    sigil_armor = {'type': 'pipican:item_rule', 'rule': 'sigil_armor', 'min': 1}
    m = re.fullmatch(rf'sigil weapon damage \+{N}% per ([a-z]+)', low)
    if m and m.group(2) == sp:
        return [{'type': 'pipican:conditional', 'when': sigil_weapon, 'effects': [{'type': 'pipican:damage_dealt', 'multiplier': 1.0,
                 'scaling': [{'source': 'special_stat', 'per': pct(m.group(1))}]}]}]
    m = re.fullmatch(rf'per {N} ([a-z]+),? \+{N} armou?r toughness from sigil armou?r', low)
    if m and m.group(2) == sp:
        return [{'type': 'pipican:conditional', 'when': sigil_armor, 'effects': [{'type': 'pipican:attribute', 'attribute': 'minecraft:armor_toughness',
                 'amount': 0.0, 'scaling': [{'source': 'special_stat', 'per': round(float(m.group(3)) / float(m.group(1)), 6)}]}]}]
    m = re.fullmatch(rf'\+{N}% damage with sigil-marked weapons and \+{N} armou?r with sigil-marked armou?r', low)
    if m and ctx.get('implicit_per_point'):
        return [{'type': 'pipican:conditional', 'when': sigil_weapon, 'effects': [{'type': 'pipican:damage_dealt', 'multiplier': 1.0,
                 'scaling': [{'source': 'special_stat', 'per': pct(m.group(1))}]}]},
                {'type': 'pipican:conditional', 'when': sigil_armor, 'effects': [{'type': 'pipican:attribute', 'attribute': 'minecraft:armor',
                 'amount': 0.0, 'scaling': [{'source': 'special_stat', 'per': float(m.group(2))}]}]}]
    m = re.fullmatch(rf'-{N}% damage with unmarked weapons and -{N} armou?r per unmarked armou?r piece', low)
    if m:
        armed = {'type': 'pipican:any_of', 'conditions': [{'type': 'pipican:holding', 'items': '#pipican:weapons/melee'},
                                                          {'type': 'pipican:holding', 'items': '#pipican:weapons/ranged'}]}
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:all_of', 'conditions': [armed, {'type': 'pipican:not', 'condition': sigil_weapon}]},
                 'effects': [{'type': 'pipican:damage_dealt', 'multiplier': round(1 - float(m.group(1)) / 100, 6)}]},
                {'type': 'pipican:attribute', 'attribute': 'minecraft:armor', 'amount': 0.0, 'scaling': [{'source': 'unmarked_armor', 'per': -float(m.group(2))}]}]
    m = re.fullmatch(rf'mighty sigils triggers at {N}% ([a-z ]+)', low)
    if m and m.group(2) == rn and float(m.group(1)) < 100:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:all_of', 'conditions': [
                    {'type': 'pipican:resource', 'min': pct(m.group(1)), 'max': 0.9999}, sigil_weapon]},
                 'effects': [{'type': 'pipican:damage_dealt', 'multiplier': 1.1}]}]
    # Weapon limits: "+25% staff damage, but you can't use any other weapon at better than clumsy",
    # "+30% favored damage, but other weapons are forbidden-tier", Dervish's shields and bows.
    staff = {'type': 'pipican:holding', 'items': ['minecraft:blaze_rod', 'minecraft:stick']}
    m = re.fullmatch(rf"\+{N}% staff damage, but you can'?t use any other weapon at better than clumsy", low)
    if m:
        return [{'type': 'pipican:conditional', 'when': staff, 'effects': [{'type': 'pipican:damage_dealt', 'multiplier': round(1 + float(m.group(1)) / 100, 6)}]},
                bonus_kind('staff_only', 1.0)[0]]
    m = re.fullmatch(rf'\+{N}% favou?red damage, but other weapons are forbidden-tier', low)
    if m:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:holding_favored'},
                 'effects': [{'type': 'pipican:damage_dealt', 'multiplier': round(1 + float(m.group(1)) / 100, 6)}]}, bonus_kind('favored_only', 1.0)[0]]
    m = re.fullmatch(rf'shields count as forbidden and -{N}% damage with bows and crossbows', low)
    if m:
        k = round(1 - float(m.group(1)) / 100, 6)
        return [bonus_kind('no_shield', 1.0)[0], {'type': 'pipican:damage_dealt', 'weapons': '#pipican:weapons/bows', 'multiplier': k},
                {'type': 'pipican:damage_dealt', 'weapons': '#pipican:weapons/crossbows', 'multiplier': k}]
    m = re.fullmatch(rf'staff hits sweep {N} extra mobs?', low)
    if m:
        return [{'type': 'pipican:cleave', 'fraction': 0.5, 'radius': 2.5, 'max_targets': int(float(m.group(1))), 'weapons': '#pipican:weapons/staff'}]
    m = re.fullmatch(rf'(?:a blaze rod or stick held in the main hand counts as your staff - )?\+{N} attack damage, \+{N}% of it as magic damage', low)
    if m:
        return [{'type': 'pipican:conditional', 'when': staff, 'effects': [{'type': 'pipican:attribute', 'attribute': 'minecraft:attack_damage', 'amount': float(m.group(1))},
                 bonus_kind('magic_portion', pct(m.group(2)))[0]]}]
    # Mace smash
    m = re.fullmatch(rf'\+{N}% fall-smash damage', low)
    if m:
        return [{'type': 'pipican:damage_dealt', 'damage_types': '#pipican:is_mace_smash', 'multiplier': round(1 + float(m.group(1)) / 100, 6)}]
    # Range / cooldown upgrades of named reactions: "Frightful Wrath range +3 blocks", "goat horn cooldown -20s"
    labels = {v.strip('"').lower() for v in LABELS.get(cid, {}).values()}
    m = re.fullmatch(rf"([a-z' ]+) range \+{N} blocks?", low)
    if m and (m.group(1) in labels or m.group(1) in REACTIONS.get(cid, {})):
        return [{'type': 'pipican:reaction_mod', 'name': m.group(1), 'radius': float(m.group(2))}]
    m = re.fullmatch(rf'goat horn cooldown -{N}s', low)
    if m and 'attack command' in REACTIONS.get(cid, {}) and REACTIONS[cid]['attack command'].get('cooldown_ticks'):
        base = REACTIONS[cid]['attack command']['cooldown_ticks'] / 20
        return [{'type': 'pipican:reaction_mod', 'name': 'attack command', 'cooldown': round(-float(m.group(1)) / base, 6)}]
    m = re.fullmatch(rf'absorption \+{N} more hearts', low)
    if m and "nature's resilience" in REACTIONS.get(cid, {}):
        return [{'type': 'pipican:reaction_mod', 'name': "nature's resilience", 'amplify': {'minecraft:absorption': int(float(m.group(1)) // 2)}}]
    # Tongues: chat already reaches every player on the server.
    if re.fullmatch(r"your chat messages can be read by all players within 1000 blocks when you start them with '!'|tongues messages carry server-wide", low):
        return []
    # Healer: Far Healing pulses heal players near you while the bar is above a share.
    m = re.fullmatch(rf'other players within {N} blocks regenerate {N} hp every {N}s while you have over {N}% ([a-z ]+)(?: \(drains {N} per heal\))?', low)
    if m and m.group(5) == rn:
        actions = [{'type': 'pipican:heal', 'target': 'players', 'radius': float(m.group(1)), 'amount': float(m.group(2)), 'require_target': True}]
        if m.group(6):
            actions.append({'type': 'pipican:resource', 'amount': -float(m.group(6))})
        r = {'type': 'pipican:on', 'event': 'second', 'name': 'far healing', 'cooldown_ticks': int(float(m.group(3)) * 20), 'actions': actions}
        REACTIONS.setdefault(cid, {}).setdefault('far healing', r)
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:resource', 'min': pct(m.group(4))}, 'effects': [r]}]
    m = re.fullmatch(rf'far healing tick -{N}% interval per ([a-z ]+)', low)
    if m and m.group(2) == sp:
        return [{'type': 'pipican:reaction_mod', 'name': 'far healing', 'cooldown': 0.0, 'scaling': [{'source': 'special_stat', 'per': -pct(m.group(1))}]}]
    # Chaplain: Succor - crouch still next to a player low on health.
    m = re.fullmatch(rf'crouching next to a downed-ish player \(below {N} hp\) for {N}s heals them {N} hp \({N}s cooldown\)', low)
    if m:
        r = {'type': 'pipican:on', 'event': 'second', 'name': 'succor', 'cooldown_ticks': int(float(m.group(4)) * 20),
             'actions': [{'type': 'pipican:heal', 'target': 'players', 'radius': 2.5, 'state': {'health': {'max': round(float(m.group(1)) / 20, 6)}},
                          'amount': float(m.group(3)), 'require_target': True}]}
        REACTIONS.setdefault(cid, {}).setdefault('succor', r)
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:all_of', 'conditions': [
            {'type': 'pipican:state', 'state': 'sneaking'}, {'type': 'pipican:still', 'seconds': float(m.group(2))}]}, 'effects': [r]}]
    if cid == 'chaplain':
        m = re.fullmatch(rf'succor heal \+{N} per {N} ([a-z]+)', low)
        if m and m.group(3) == sp:
            return [{'type': 'pipican:reaction_mod', 'name': 'succor', 'heal': 0.0, 'scaling': [{'source': 'special_stat', 'per': round(float(m.group(1)) / float(m.group(2)), 6)}]}]
        m = re.fullmatch(rf'succor cooldown -{N}s', low)
        if m and REACTIONS.get(cid, {}).get('succor', {}).get('cooldown_ticks'):
            return [{'type': 'pipican:reaction_mod', 'name': 'succor', 'cooldown': round(-float(m.group(1)) * 20 / REACTIONS[cid]['succor']['cooldown_ticks'], 6)}]
        m = re.fullmatch(r'succor also removes poison and wither', low)
        if m:
            return [{'type': 'pipican:reaction_mod', 'name': 'succor', 'actions': [{'type': 'pipican:cleanse', 'target': 'players', 'radius': 2.5,
                     'effects': ['minecraft:poison', 'minecraft:wither']}]}]
    # Crowds: "-5% damage and -1 armor for each hostile mob beyond the first within 6 blocks"
    m = re.fullmatch(rf'-{N}% damage and -{N} armou?r for each hostile mob beyond the first within 6 blocks', low)
    if m:
        return [{'type': 'pipican:damage_dealt', 'multiplier': 1.0, 'scaling': [{'source': 'extra_hostiles', 'per': -pct(m.group(1))}]},
                {'type': 'pipican:attribute', 'attribute': 'minecraft:armor', 'amount': 0.0, 'scaling': [{'source': 'extra_hostiles', 'per': -float(m.group(2))}]}]
    # Acrobat: "+25% damage taken while Slowed or standing in a 1-block-wide space"
    m = re.fullmatch(rf'\+{N}% damage taken while slowed or standing in a 1-block-wide space', low)
    if m:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:any_of', 'conditions': [
            {'type': 'pipican:effect', 'effects': 'minecraft:slowness'}, {'type': 'pipican:state', 'state': 'narrow'}]},
            'effects': [{'type': 'pipican:damage_taken', 'multiplier': round(1 + float(m.group(1)) / 100, 6)}]}]
    m = re.fullmatch(rf'first round of combat: \+{N}% damage(?: and speed)?', low)
    if m:
        k = pct(m.group(1))
        eff = [{'type': 'pipican:damage_dealt', 'multiplier': round(1 + k, 6)}]
        if 'speed' in low:
            eff.append({'type': 'pipican:attribute', 'attribute': 'minecraft:movement_speed', 'amount': k, 'operation': 'add_multiplied_base'})
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'combat_start'}, 'effects': eff}]
    # Killing the innocent: villagers, iron golems, tamed animals.
    m = re.fullmatch(rf'killing (?:a villager, iron golem or tamed animal|villagers, golems or tamed animals) (empties|locks) ([a-z ]+?)(?: regen)?(?: and gives (.+?))?(?: for {N} minutes)?', low)
    if m and m.group(2) == rn:
        if m.group(1) == 'empties':
            actions = [{'type': 'pipican:resource', 'percent': -1.0}]
            if m.group(3):
                out = combat.outcome(f'get {m.group(3)} for {float(m.group(4) or 1) * 60:g}s', 'self')
                if not out:
                    return None
                actions += out
        else:
            regen = _rest(f'-100% {ctx.get("resource_name")} regen', ctx)
            if not regen or not m.group(4):
                return None
            actions = [{'type': 'pipican:boost', 'seconds': float(m.group(4)) * 60, 'effects': regen}]
        return [{'type': 'pipican:on', 'event': 'kill_innocent', 'actions': actions}]
    # Village behaviour
    m = re.fullmatch(rf"villager trades cost {N}% more and hero of the village doesn'?t apply to you", low)
    if m:
        return [bonus_kind('trade_price', pct(m.group(1)))[0], {'type': 'pipican:immune', 'effects': ['minecraft:hero_of_the_village']}]
    m = re.fullmatch(rf'villager trades cost {N}% more, and iron golems turn hostile if they see you open a chest inside a village', low)
    if m:
        return [bonus_kind('trade_price', pct(m.group(1)))[0], bonus_kind('golem_chest', 1.0)[0]]
    if cid == 'thief' and re.fullmatch(r'cancels the villager price penalty from your weakness', low):
        return bonus_kind('trade_price', -0.2)
    m = re.fullmatch(r'villagers refuse to trade with you, animals flee from you(?:, and (.+))?', low)
    if m:
        out = [bonus_kind('no_trade', 1.0)[0], bonus_kind('animals_flee', 1.0)[0]]
        if m.group(1):
            rest = _rest(m.group(1), ctx)
            if not rest:
                return None
            out += rest
        return out
    m = re.fullmatch(rf'-{N}% potion effect duration on you, and enchanting costs \+{N} levels?', low)
    if m:
        return [bonus_kind('potion_duration', -pct(m.group(1)))[0], bonus_kind('enchant_levels', -float(m.group(2)))[0]]
    m = re.fullmatch(r'passive animals follow you and hostile spiders ignore you', low)
    if m:
        return [bonus_kind('animals_follow', 1.0)[0], {'type': 'pipican:ignored_by', 'mobs': ['minecraft:spider', 'minecraft:cave_spider']}]
    m = re.fullmatch(rf'takes {N}% more fire damage and burns for {N}% longer', low)
    if m:
        return [{'type': 'pipican:damage_taken', 'damage_types': '#minecraft:is_fire', 'multiplier': round(1 + float(m.group(1)) / 100, 6)},
                bonus_kind('burn_longer', pct(m.group(2)))[0]]
    # Mob memory: "mobs that lose sight of you forget you 50% faster", "... forget you after 2s", "mobs forget you 1s faster"
    m = re.fullmatch(rf'mobs (?:that lose sight of you forget you|lose track of you) {N}% faster', low)
    if m:
        return bonus_kind('forget_faster', pct(m.group(1)))
    m = re.fullmatch(rf'breaking line of sight with a mob makes it forget you after {N}s', low)
    if m:
        return bonus_kind('forget_delay_max', float(m.group(1)))
    m = re.fullmatch(rf'mobs forget you {N}s faster', low)
    if m:
        return bonus_kind('forget_sooner', float(m.group(1)))
    m = re.fullmatch(rf'mobs you hit while sneaking forget you {N}% faster', low)
    if m:
        return bonus_kind('forget_hit_sneaking', pct(m.group(1)))
    # Infiltrator: Faceless
    m = re.fullmatch(rf"while sneaking and not attacking, hostile mobs beyond {N} blocks can'?t see you at all", low)
    if m:
        return bonus_kind('faceless_range', float(m.group(1)))
    if re.fullmatch(r"while wearing any medium or heavy armou?r, faceless doesn'?t work", low):
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:any_of', 'conditions': [
            {'type': 'pipican:wearing', 'items': '#pipican:armor/medium'}, {'type': 'pipican:wearing', 'items': '#pipican:armor/heavy'}]},
            'effects': bonus_kind('faceless_range', -100.0)}]
    m = re.fullmatch(rf'faceless distance -{N} blocks per ([a-z]+)', low)
    if m and m.group(2) == sp:
        return bonus_kind('faceless_range', 0.0, -float(m.group(1)))
    m = re.fullmatch(rf'faceless works within {N} blocks', low)
    if m and cid == 'infiltrator':
        # The base range is 4 blocks.
        return bonus_kind('faceless_range', float(m.group(1)) - 4)
    # Woodwose Forest Hide, Tenebrist Shadow Form
    m = re.fullmatch(r'in foliage(?: \(leaves, tall grass, ferns\))?, sneaking makes you invisible', low)
    if m:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:all_of', 'conditions': [
            {'type': 'pipican:state', 'state': 'sneaking'}, {'type': 'pipican:state', 'state': 'in_foliage'}]},
            'effects': [{'type': 'pipican:mob_effect', 'effect': 'minecraft:invisibility'}]}]
    shadow = lambda: [{'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'sneaking'},
                       'effects': [{'type': 'pipican:mob_effect', 'effect': 'minecraft:invisibility'}]},
                      {'type': 'pipican:damage_dealt', 'multiplier': 1.15}]
    m = re.fullmatch(r"in light level 7 or lower, you'?re invisible while sneaking and \+15% damage", low)
    if m and cid == 'tenebrist':
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:light', 'max': 8}, 'effects': shadow()}]
    if cid == 'tenebrist' and re.fullmatch(r'shadow form works in any light', low):
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:not', 'condition': {'type': 'pipican:light', 'max': 8}}, 'effects': shadow()}]
    m = re.fullmatch(rf'forest armou?r \+{N} per {N} ([a-z]+)', low)
    if m and m.group(3) == sp and (forest := cond.parse('in forests', ctx)):
        return [{'type': 'pipican:conditional', 'when': forest, 'effects': [{'type': 'pipican:attribute', 'attribute': 'minecraft:armor', 'amount': 0.0,
                 'scaling': [{'source': 'special_stat', 'per': round(float(m.group(1)) / float(m.group(2)), 6)}]}]}]
    # Seeing: Visions, Primal Bond, No Hiding, Sentry, Lookout, Alertness
    m = re.fullmatch(rf'you see the outline of hostile mobs through walls within {N} blocks while sneaking', low)
    if m:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'sneaking'},
                 'effects': [{'type': 'pipican:on', 'event': 'second', 'actions': [_glow('hostiles', float(m.group(1)))]}]}]
    if re.fullmatch(r'your tamed animals within 32 blocks share your night vision and you see them through walls', low):
        return bonus_kind('pet_share_sight', 1.0)
    if re.fullmatch(r"mobs you see can'?t become invisible to anyone near you", low):
        return bonus_kind('no_hiding', 16.0)
    if re.fullmatch(r'players within 8 blocks also see what glows for you', low):
        return []  # glowing already shows to every player
    if re.fullmatch(r"can'?t be hit by sneak-attack bonuses \(you are never 'unaware'\)", low):
        return bonus_kind('never_unaware', 1.0)
    m = re.fullmatch(rf"you can'?t be backstabbed \(no bonus damage from mobs hitting you from behind\) and hostile mobs within {N} blocks make a sound cue", low)
    if m:
        return [bonus_kind('never_unaware', 1.0)[0], bonus_kind('danger_sense', float(m.group(1)))[0]]
    # Savant focus schools (picked with /pipican focus).
    m = re.fullmatch(rf'pick two elemental focus schools when you choose the class - .*damage and effects of those elements are \+{N}%.*', low)
    if m:
        return [bonus_kind('focus_slots', 2.0)[0], bonus_kind('focus_bonus', pct(m.group(1)))[0]]
    m = re.fullmatch(rf'focus bonus \+{N}%(?: per ([a-z]+))?', low)
    if m and cid == 'savant' and (not m.group(2) or m.group(2) == sp):
        return bonus_kind('focus_bonus', 0.0 if m.group(2) else pct(m.group(1)), pct(m.group(1)) if m.group(2) else None)
    if cid == 'savant' and re.fullmatch(r'focus bonus doubled', low):
        return bonus_kind('focus_bonus', 0.2)
    m = re.fullmatch(rf'non-focus schools get \+{N}%', low)
    if m:
        return bonus_kind('non_focus_bonus', pct(m.group(1)))
    m = re.fullmatch(rf'\+{N} more focus schools?', low)
    if m:
        return bonus_kind('focus_slots', float(m.group(1)))
    # Enchanting tables and cheaper crafting stations.
    m = re.fullmatch(rf'\+{N} (?:counted )?bookshel(?:f|ves)(?: counted)?(?: around your (?:enchanting )?table)?(?: per {N} ([a-z]+))?', low)
    if m and (not m.group(3) or m.group(3) == sp):
        if m.group(3):
            return bonus_kind('bookshelf_bonus', 0.0, round(float(m.group(1)) / float(m.group(2)), 6))
        return bonus_kind('bookshelf_bonus', float(m.group(1)))
    m = re.fullmatch(rf'\+{N} counted bookshelf, -{N} enchanting cost', low)
    if m:
        return [bonus_kind('bookshelf_bonus', float(m.group(1)))[0], bonus_kind('enchant_levels', float(m.group(2)))[0]]
    m = re.fullmatch(rf'\+{N}% chance of a better enchantment roll', low)
    if m:
        return bonus_kind('better_roll', pct(m.group(1)))
    m = re.fullmatch(rf'-{N} lapis cost on level-3 enchants', low)
    if m:
        return bonus_kind('enchant_lapis_top', float(m.group(1)))
    m = re.fullmatch(rf'\+{N}% chance per ([a-z]+) point to keep the lapis when enchanting', low)
    if m and m.group(2) == sp:
        return bonus_kind('enchant_lapis_keep', 0.0, pct(m.group(1)))
    m = re.fullmatch(r'(brewing and enchanting|enchanting, brewing and anvil use all) cost slightly less \(-1 level\s*[,/]\s*10% faster(?: brew)?\)', low)
    if m:
        out = [bonus_kind('enchant_levels', 1.0)[0], bonus_kind('brew_speed', 0.1)[0]]
        if 'anvil' in m.group(1):
            out.append(bonus_kind('anvil_levels', 1.0)[0])
        return out
    # Engineer eidolon
    m = re.fullmatch(rf'eidolon armou?r \+{N} per {N} ([a-z]+)', low)
    if m and m.group(3) == sp:
        return [{'type': 'pipican:eidolon', 'attribute': 'minecraft:armor', 'amount': 0.0,
                 'scaling': [{'source': 'special_stat', 'per': round(float(m.group(1)) / float(m.group(2)), 6)}]}]
    m = re.fullmatch(rf'eidolon becomes size 2 \(bigger, \+{N}% stats\) but it moves {N}% slower', low)
    if m:
        k = pct(m.group(1))
        out = [{'type': 'pipican:eidolon', 'attribute': 'minecraft:scale', 'amount': 1.0, 'operation': 'add_multiplied_base'}]
        out += [{'type': 'pipican:eidolon', 'attribute': a, 'amount': k, 'operation': 'add_multiplied_base'}
                for a in ('minecraft:max_health', 'minecraft:attack_damage', 'minecraft:armor')]
        out.append({'type': 'pipican:eidolon', 'attribute': 'minecraft:movement_speed', 'amount': -pct(m.group(2)), 'operation': 'add_multiplied_base'})
        return out
    # Shapeshifter: forms
    forms = [w['ability'] for n, (w, _) in NAMED.get(cid, {}).items() if n.endswith(' form') and w.get('type') == 'pipican:ability_active']
    m = re.fullmatch(rf'\+{N}% bonus from your current form', low)
    if m and forms and ctx.get('implicit_per_point'):
        return [{'type': 'pipican:ability_mod', 'ability': a, 'strength': 0.0, 'scaling': [{'source': 'special_stat', 'per': pct(m.group(1))}]} for a in forms]
    if forms and re.fullmatch(r'changing form is instant and cleanses slowness', low):
        # Forms are already instant toggles; switching also clears Slowness.
        return [{'type': 'pipican:ability_mod', 'ability': a, 'actions': [{'type': 'pipican:cleanse', 'target': 'self', 'effects': ['minecraft:slowness']}]} for a in forms]
    if forms and re.fullmatch(r"while transformed you can'?t use weapons, bows, shields or items \(only bare-handed attacks\)", low):
        any_form = forms and {'type': 'pipican:any_of', 'conditions': [{'type': 'pipican:ability_active', 'ability': a} for a in forms]}
        return [{'type': 'pipican:conditional', 'when': any_form, 'effects': [bonus_kind('bare_handed', 1.0)[0], bonus_kind('no_shield', 1.0)[0]]}]
    # Dreadnaught: heavy armor
    m = re.fullmatch(rf'heavy armou?r gives \+{N} armou?r per piece and no movement penalty', low)
    if m:
        # Heavy armor is mastery-tier for the class, which already has no movement penalty.
        return [{'type': 'pipican:attribute', 'attribute': 'minecraft:armor', 'amount': 0.0, 'scaling': [{'source': 'heavy_armor', 'per': float(m.group(1))}]}]
    heavy4 = {'type': 'pipican:item_rule', 'rule': 'heavy_armor', 'min': 4}
    m = re.fullmatch(rf'weapon resistance \+{N}% per ([a-z]+)', low)
    if m and m.group(2) == sp:
        sc = [{'source': 'special_stat', 'per': -pct(m.group(1))}]
        return [{'type': 'pipican:conditional', 'when': heavy4, 'effects': [
            {'type': 'pipican:damage_taken', 'damage_types': '#pipican:is_melee', 'multiplier': 1.0, 'scaling': sc},
            {'type': 'pipican:damage_taken', 'damage_types': '#minecraft:is_projectile', 'multiplier': 1.0, 'scaling': sc}]}]
    m = re.fullmatch(rf'\+{N}% more weapon resistance', low)
    if m:
        k = round(1 - float(m.group(1)) / 100, 6)
        return [{'type': 'pipican:conditional', 'when': heavy4, 'effects': [
            {'type': 'pipican:damage_taken', 'damage_types': '#pipican:is_melee', 'multiplier': k},
            {'type': 'pipican:damage_taken', 'damage_types': '#minecraft:is_projectile', 'multiplier': k}]}]
    # Assassin: Assassinate finishes what the ambush starts.
    unaware = {'not_targeting_you': True}
    m = re.fullmatch(rf"your first hit on a mob that isn'?t targeting you deals \+{N}% damage; if that brings it below {N}% health, it dies", low)
    if m:
        return [{'type': 'pipican:damage_dealt', 'victim': unaware, 'multiplier': round(1 + float(m.group(1)) / 100, 6)},
                {'type': 'pipican:execute', 'threshold': pct(m.group(2)), 'victim': unaware}]
    m = re.fullmatch(rf'assassinate threshold rises to {N}% health, but -{N}% damage to mobs that are targeting you', low)
    if m:
        return [{'type': 'pipican:execute', 'threshold': pct(m.group(1)), 'victim': unaware},
                {'type': 'pipican:damage_dealt', 'victim': {'targeting_you': True}, 'multiplier': round(1 - float(m.group(2)) / 100, 6)}]
    # Jack of All Trades: Aptitude
    m = re.fullmatch(rf'once every {N}s, your next hit is a guaranteed critical with \+{N}% damage', low)
    if m:
        # A critical is +50%; the extra damage multiplies on top.
        # Named apart from the Aptitude stat, so "+1 Aptitude" stays a stat.
        r = {'type': 'pipican:on', 'event': 'second', 'name': 'aptitude crit', 'cooldown_ticks': int(float(m.group(1)) * 20),
             'actions': [{'type': 'pipican:empower', 'target': 'self', 'multiplier': round(1.5 * (1 + float(m.group(2)) / 100), 6),
                          'seconds': float(m.group(1)) * 10, 'charges': 1}]}
        REACTIONS.setdefault(cid, {}).setdefault('aptitude crit', r)
        return [r]
    m = re.fullmatch(rf'aptitude crit triggers every {N}s', low)
    if m and REACTIONS.get(cid, {}).get('aptitude crit', {}).get('cooldown_ticks'):
        return [{'type': 'pipican:reaction_mod', 'name': 'aptitude crit', 'cooldown': round(float(m.group(1)) * 20 / REACTIONS[cid]['aptitude crit']['cooldown_ticks'] - 1, 6)}]
    m = re.fullmatch(rf'\+{N} luck and no gear is worse than neutral for you', low)
    if m and (luck := _rest(f'+{m.group(1)} luck', ctx)):
        return luck + [bonus_kind('affinity_floor', 1.0)[0]]
    # Thaumaturge: chaos luck
    m = re.fullmatch(rf'per {N} ([a-z]+),? bad chaos outcomes are {N}% rarer', low)
    if m and m.group(2) == sp:
        return bonus_kind('chaos_luck', 0.0, round(float(m.group(3)) / 100 / float(m.group(1)), 6))
    m = re.fullmatch(rf'bad (?:chaos )?outcomes {N}% rarer', low)
    if m:
        return bonus_kind('chaos_luck', pct(m.group(1)))
    m = re.fullmatch(rf'\+{N}% to all random effects in your favou?r \(chaos, crits, loot\)', low)
    if m and ctx.get('implicit_per_point'):
        sc = [{'source': 'special_stat', 'per': pct(m.group(1))}]
        return [{'type': 'pipican:bonus', 'kind': 'chaos_luck', 'amount': 0.0, 'scaling': sc},
                {'type': 'pipican:critical', 'chance': 0.0, 'scaling': sc},
                {'type': 'pipican:bonus', 'kind': 'extra_loot', 'amount': 0.0, 'scaling': sc}]
    if re.fullmatch(r'seize chaos chance doubled, but outcomes are fully random', low):
        return [{'type': 'pipican:reaction_chance', 'name': 'seize chaos', 'multiplier': 2.0}, bonus_kind('chaos_luck', -10.0)[0]]
    # Cavalier
    if re.fullmatch(r'you never fall off when your mount is hit', low):
        return []  # being hit never throws a rider off
    m = re.fullmatch(rf'sprinting or riding at full speed into a mob adds \+{N}% damage to the hit \(\+{N}% mounted\)', low)
    if m:
        mounted = {'type': 'pipican:state', 'state': 'mounted'}
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:all_of', 'conditions': [{'type': 'pipican:state', 'state': 'sprinting'},
                 {'type': 'pipican:not', 'condition': mounted}]}, 'effects': [{'type': 'pipican:damage_dealt', 'damage_types': '#pipican:is_melee',
                 'multiplier': round(1 + float(m.group(1)) / 100, 6)}]},
                {'type': 'pipican:conditional', 'when': mounted, 'effects': [{'type': 'pipican:damage_dealt', 'damage_types': '#pipican:is_melee',
                 'multiplier': round(1 + float(m.group(2)) / 100, 6)}]}]
    if re.fullmatch(r'your mount damages small mobs it runs into', low):
        return bonus_kind('trample', 2.0)
    if re.fullmatch(r"taking damage doesn'?t drain ([a-z ]+)", low):
        return []  # nothing drains the bar on damage
    m = re.fullmatch(rf'illagers are {N}% slower to notice you', low)
    if m:
        return bonus_kind('illager_detection', -pct(m.group(1)))
    # --- Bulk 39
    m = re.fullmatch(r'immune to knockback while ([a-z ]+) is above half', low)
    if m and m.group(1) == rn:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:resource', 'min': 0.5},
                 'effects': [{'type': 'pipican:attribute', 'attribute': 'minecraft:knockback_resistance', 'amount': 1.0}]}]
    m = re.fullmatch(rf"landing from any fall can'?t break your legs below {N} hearts", low)
    if m:
        return bonus_kind('fall_floor', float(m.group(1)) * 2)
    if re.fullmatch(r"no slowdown after being knocked back|your movement never triggers mob 'attack of opportunity' style knockback", low):
        return []  # neither slowdown nor attacks of opportunity exist in the game
    m = re.fullmatch(rf"\+{N}% wind charge knockback(?: and they don'?t hurt you)?", low)
    if m and not ctx.get('implicit_per_point'):
        return bonus_kind('wind_knockback', pct(m.group(1)))  # your own wind charges never hurt you
    m = re.fullmatch(rf'\+{N}% movement speed and \+{N}% wind charge knockback', low)
    if m and ctx.get('implicit_per_point'):
        return [{'type': 'pipican:attribute', 'attribute': 'minecraft:movement_speed', 'amount': 0.0, 'operation': 'add_multiplied_base',
                 'scaling': [{'source': 'special_stat', 'per': pct(m.group(1))}]}, bonus_kind('wind_knockback', 0.0, pct(m.group(2)))[0]]
    m = re.fullmatch(r'(?:unlocks insight: )?after you dodge, your next hit is a guaranteed crit|a successful parry makes your next hit a crit', low)
    if m:
        # A crit is +50%, like vanilla's.
        return [{'type': 'pipican:on', 'event': 'dodge', 'actions': [{'type': 'pipican:empower', 'target': 'self', 'multiplier': 1.5, 'hits': 1, 'seconds': 10.0}]}]
    if re.fullmatch(r'no fall damage when landing on stone or dirt', low):
        return bonus_kind('fall_safe_ground', 1.0)
    if re.fullmatch(r'dirty fighting can also apply weakness', low):
        return [{'type': 'pipican:reaction_mod', 'name': 'dirty fighting', 'actions': [{'type': 'pipican:random', 'choices': [
            [{'type': 'pipican:effect', 'target': 'victim', 'effects': [{'effect': 'minecraft:weakness', 'seconds': 2.0}]}], [], []]}]}]
    m = re.fullmatch(rf'once per day, a player who dies near you is saved at {N} hp', low)
    if m:
        return bonus_kind('save_ally', float(m.group(1)))
    m = re.fullmatch(rf"after a decoy, you'?re invisible for {N}s", low)
    if m:
        return [{'type': 'pipican:reaction_mod', 'name': 'decoy', 'actions': [{'type': 'pipican:effect', 'target': 'self',
                 'effects': [{'effect': 'minecraft:invisibility', 'seconds': float(m.group(1))}]}]}]
    m = re.fullmatch(rf'decoys never fool wardens, endermen or the wither, and you take {N}% more damage from them', low)
    if m:
        return [bonus_kind('decoy_proof', 1.0)[0], {'type': 'pipican:damage_taken', 'against': ['minecraft:warden', 'minecraft:enderman', 'minecraft:wither'],
                'multiplier': round(1 + float(m.group(1)) / 100, 6)}]
    m = re.fullmatch(rf'per {N} ([a-z ]+?),? \+{N} escalating hits?', low)
    if m and m.group(2) == sp:
        return [{'type': 'pipican:reaction_mod', 'name': 'escalating violence', 'hits': 0.0,
                 'scaling': [{'source': 'special_stat', 'per': round(float(m.group(3)) / float(m.group(1)), 6)}]}]
    m = re.fullmatch(rf'per {N} ([a-z]+),? \+{N}% bound weapon critical damage', low)
    if m and m.group(2) == sp:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:item_rule', 'rule': 'bound_weapon'}, 'effects': [
            {'type': 'pipican:critical', 'damage': 0.0, 'scaling': [{'source': 'special_stat', 'per': round(float(m.group(3)) / 100 / float(m.group(1)), 6)}]}]}]
    m = re.fullmatch(rf'your lightning chance works in any rain, but you take {N}% more fire damage', low)
    if m and cid == 'stormbringer':
        rain = {'type': 'pipican:all_of', 'conditions': [{'type': 'pipican:state', 'state': 'raining'},
                                                         {'type': 'pipican:not', 'condition': {'type': 'pipican:state', 'state': 'thundering'}}]}
        return [{'type': 'pipican:conditional', 'when': rain, 'effects': [{'type': 'pipican:on', 'event': 'hit', 'chance': 0.05, 'name': 'lightning',
                 'actions': [{'type': 'pipican:lightning'}]}]},
                {'type': 'pipican:damage_taken', 'damage_types': '#minecraft:is_fire', 'multiplier': round(1 + float(m.group(1)) / 100, 6)}]
    m = re.fullmatch(rf"powered by storm lasts {N} minutes; you can'?t wear iron or netherite armou?r", low)
    if m and 'struck by lightning' in REACTIONS.get(cid, {}):
        base = next((a['seconds'] for a in REACTIONS[cid]['struck by lightning']['actions'] if a.get('type') == 'pipican:boost'), 60.0)
        return [{'type': 'pipican:reaction_mod', 'name': 'struck by lightning', 'seconds': float(m.group(1)) * 60 - base}, bonus_kind('no_metal_armor', 1.0)[0]]
    m = re.fullmatch(rf'you count as being in darkness at all times - (?:mobs detect you {N}% later|-{N}% detection range)', low)
    if m:
        return [bonus_kind('always_dark', 1.0)[0], bonus_kind('detection_range', -pct(m.group(1) or m.group(2)))[0]]
    if re.fullmatch(r'you can re-optimize once per day without sleeping', low):
        return bonus_kind('reoptimize', 1.0)
    if re.fullmatch(r'you can change favou?red weapon once per day without sleeping', low):
        return bonus_kind('swap_favored', 1.0)
    m = re.fullmatch(rf'steal spell also steals beneficial effects from mobs you hit \(witches!\), but -{N}% max health', low)
    if m and (hp := _rest(f'-{m.group(1)}% max health', ctx)):
        return [{'type': 'pipican:on', 'event': 'hit', 'chance': 0.15, 'name': 'steal spell', 'actions': [{'type': 'pipican:steal_effects', 'target': 'victim'}]}] + hp
    m = re.fullmatch(rf'zeal triggers at {N} hearts; ([a-z ]+) fills twice as fast', low)
    if m and m.group(2) == rn and float(m.group(1)) >= 2:
        return [bonus_kind('low_health_extra', (float(m.group(1)) - 2) * 2)[0], bonus_kind('resource_gain', 1.0)[0]]
    m = re.fullmatch(rf'your tnt has \+{N}% radius', low)
    if m:
        return bonus_kind('tnt_power', pct(m.group(1)))
    m = re.fullmatch(r'([a-z ]+) regen stops while wet and gadgets fizzle in the nether', low)
    if m and m.group(1) == rn and (wet := _rest(f'no {ctx.get("resource_name")} regen while wet', ctx)):
        return wet + [bonus_kind('charges_fizzle_nether', 1.0)[0]]
    m = re.fullmatch(rf'your melee hits and arrows apply poison i for {N}s \(poison ii if you hold a pufferfish or poison potion in the off-hand\)', low)
    if m:
        off = {'type': 'pipican:item_rule', 'rule': 'poison_offhand'}
        hit = lambda amp: {'type': 'pipican:on', 'event': 'hit', 'name': 'poison mastery', 'actions': [{'type': 'pipican:effect', 'target': 'victim',
                           'effects': [dict({'effect': 'minecraft:poison', 'seconds': float(m.group(1))}, **({'amplifier': amp} if amp else {}))]}]}
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:not', 'condition': off}, 'effects': [hit(0)]},
                {'type': 'pipican:conditional', 'when': off, 'effects': [hit(1)]}]
    m = re.fullmatch(rf'brewing poison potions is {N}% faster', low)
    if m:
        return bonus_kind('poison_brew_speed', pct(m.group(1)))
    m = re.fullmatch(rf'fire you start burns {N}% longer', low)
    if m:
        return bonus_kind('fire_longer', pct(m.group(1)))
    m = re.fullmatch(rf'per {N} ([a-z]+),? marked targets within 4 blocks get weakness i', low)
    if m and m.group(2) == sp:
        return bonus_kind('marked_weakness', 0.0, round(1 / float(m.group(1)), 6))
    m = re.fullmatch(rf'villager trades cost {N}% less and villagers restock twice as often for you', low)
    if m:
        return [bonus_kind('trade_price', -pct(m.group(1)))[0], bonus_kind('extra_restock', 1.0)[0]]
    m = re.fullmatch(rf'\+{N} villager restocks?', low)
    if m:
        # Villagers restock about twice a day: one more is +50%.
        return bonus_kind('extra_restock', round(float(m.group(1)) / 2, 6))
    m = re.fullmatch(rf'\+{N} tamed-animal regen per {N} ([a-z ]+)', low)
    if m and m.group(3) == sp:
        return bonus_kind('pet_regen', 0.0, round(float(m.group(1)) / float(m.group(2)), 6))
    m = re.fullmatch(rf'per {N} ([a-z]+),? \+{N} minions? at a time', low)
    if m and m.group(2) == sp:
        return [{'type': 'pipican:summon_mod', 'extra': 0.0, 'scaling': [{'source': 'special_stat', 'per': round(float(m.group(3)) / float(m.group(1)), 6)}]}]
    # --- Bulk 40
    m = re.fullmatch(rf'-{N}% ([a-z ]+) regen and ([a-z\' ]+) is disabled when no other player is within {N} blocks', low)
    if m and m.group(2) == rn and (regen := _rest(f'-{m.group(1)}% {ctx.get("resource_name")} regen', ctx)):
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:nearby', 'entities': 'players', 'radius': float(m.group(4)), 'min': 0, 'max': 0},
                 'effects': regen + [{'type': 'pipican:aura_mod', 'name': m.group(3), 'strength': -100.0}]}]
    m = re.fullmatch(r"in the nether and the end: ([a-z ]+) does not regenerate and ([a-z' ]+) is disabled", low)
    if m and m.group(1) == rn and (regen := _rest(f'-100% {ctx.get("resource_name")} regen', ctx)):
        where = {'type': 'pipican:any_of', 'conditions': [{'type': 'pipican:dimension', 'dimensions': ['minecraft:the_nether']},
                                                          {'type': 'pipican:dimension', 'dimensions': ['minecraft:the_end']}]}
        return [{'type': 'pipican:conditional', 'when': where, 'effects': regen + [{'type': 'pipican:aura_mod', 'name': m.group(2), 'strength': -100.0}]}]
    m = re.fullmatch(rf"([a-z' ]+) switches off for 10s whenever you take more than {N} damage in one hit", low)
    if m and m.group(1) in AURAS.get(cid, {}):
        return bonus_kind('aura_break_at', float(m.group(2)))
    m = re.fullmatch(rf"([a-z' ]+) no longer stops from big hits below {N} damage", low)
    if m and m.group(1) in AURAS.get(cid, {}):
        # The base threshold is 6 damage.
        return bonus_kind('aura_break_at', float(m.group(2)) - 6)
    m = re.fullmatch(rf'\+{N}% more damage in chant', low)
    if m:
        return [{'type': 'pipican:aura_mod', 'name': 'chant', 'effects': [{'type': 'pipican:damage_dealt', 'multiplier': round(1 + float(m.group(1)) / 100, 6)}]}]
    m = re.fullmatch(rf"([a-z' ]+) range \+{N}(?: blocks?)?", low)
    if m and m.group(1) in AURAS.get(cid, {}):
        return [{'type': 'pipican:aura_mod', 'name': m.group(1), 'radius': float(m.group(2))}]
    # Myrmidon: shield bash (blocking a hit while sneaking bashes the attacker) and Forceful Shield knockback.
    m = re.fullmatch(rf'sneak-attacking with a raised shield bashes for {N} damage', low)
    if m:
        r = {'type': 'pipican:on', 'event': 'block', 'name': 'shield bash', 'actions': [{'type': 'pipican:damage', 'target': 'victim', 'amount': float(m.group(1)), 'kind': 'melee'}]}
        REACTIONS.setdefault(cid, {}).setdefault('shield bash', r)
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'sneaking'}, 'effects': [r]}]
    m = re.fullmatch(rf'shield bash \+{N} damage', low)
    if m:
        return [{'type': 'pipican:reaction_mod', 'name': 'shield bash', 'damage': float(m.group(1))}]
    m = re.fullmatch(rf'\+{N}% shield bash damage and -{N}% damage taken while blocking', low)
    if m and ctx.get('implicit_per_point'):
        return [{'type': 'pipican:reaction_mod', 'name': 'shield bash', 'damage': 0.0, 'scaling': [{'source': 'special_stat', 'per': round(4 * float(m.group(1)) / 100, 6)}]},
                {'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'blocking'}, 'effects': [{'type': 'pipican:damage_taken', 'multiplier': 1.0,
                 'scaling': [{'source': 'special_stat', 'per': -pct(m.group(2))}]}]}]
    m = re.fullmatch(rf'knockback \+{N} blocks per ([a-z]+)', low)
    if m and m.group(2) == sp and 'forceful shield' in REACTIONS.get(cid, {}):
        return [{'type': 'pipican:reaction_mod', 'name': 'forceful shield', 'radius': 0.0, 'scaling': [{'source': 'special_stat', 'per': float(m.group(1))}]}]
    m = re.fullmatch(rf'\+{N} knockback blocks', low)
    if m and 'forceful shield' in REACTIONS.get(cid, {}):
        return [{'type': 'pipican:reaction_mod', 'name': 'forceful shield', 'radius': float(m.group(1))}]
    # Sentinel: Perfect Awareness reach
    m = re.fullmatch(rf'\+{N} blocks? (?:hostile-mob )?detection range', low)
    if m and 'perfect awareness' in REACTIONS.get(cid, {}):
        if ctx.get('implicit_per_point'):
            return [{'type': 'pipican:reaction_mod', 'name': 'perfect awareness', 'radius': 0.0, 'scaling': [{'source': 'special_stat', 'per': float(m.group(1))}]}]
        return [{'type': 'pipican:reaction_mod', 'name': 'perfect awareness', 'radius': float(m.group(1))}]
    # Druid, Warrior, Witch, senses
    if re.fullmatch(r'passive animals within 16 blocks never flee from you', low):
        return bonus_kind('calm_animals', 1.0)
    m = re.fullmatch(rf'\+{N}% shield-disable time when you break a shield', low)
    if m:
        return bonus_kind('shield_break_time', pct(m.group(1)))
    m = re.fullmatch(rf'your instant health/damage splash potions are {N}% stronger', low)
    if m:
        return bonus_kind('splash_power', pct(m.group(1)))
    m = re.fullmatch(rf'\+{N}% chance per {N} ([a-z]+) to brew one extra potion per brewing cycle', low)
    if m and m.group(3) == sp:
        return bonus_kind('extra_brew', 0.0, round(float(m.group(1)) / 100 / float(m.group(2)), 6))
    m = re.fullmatch(rf'enchanted items and mobs with potion effects within {N} blocks glow for you while sneaking', low)
    if m:
        return bonus_kind('sense_magic', float(m.group(1)))
    m = re.fullmatch(rf'chests with loot within {N} blocks sparkle while you sneak', low)
    if m:
        return bonus_kind('loot_sense', float(m.group(1)))
    m = re.fullmatch(rf'ores within {N} blocks glow while you sneak', low)
    if m:
        return bonus_kind('ore_sense', float(m.group(1)))
    m = re.fullmatch(rf'structures within {N} blocks are hinted in chat when you look at a compass', low)
    if m:
        return bonus_kind('structure_sense', float(m.group(1)))
    m = re.fullmatch(rf"per {N} ([a-z]+),? prey can'?t become invisible to you", low)
    if m and m.group(2) == sp:
        return bonus_kind('prey_reveal', 0.0, round(1 / float(m.group(1)), 6))
    # --- Bulk 41
    half = {'type': 'pipican:resource', 'min': 0.5}
    m = re.fullmatch(rf'you emit light \(dynamic light level {N}\) while ([a-z ]+) is above half', low)
    if m and m.group(2) == rn:
        return [{'type': 'pipican:conditional', 'when': half, 'effects': bonus_kind('emit_light', float(m.group(1)))}]
    m = re.fullmatch(rf'light radius \+{N} per {N} ([a-z]+)', low)
    if m and m.group(3) == sp and cid == 'astromancer':
        # A light block's level is its reach: +1 level per step.
        return [{'type': 'pipican:conditional', 'when': half, 'effects': bonus_kind('emit_light', 0.0, round(float(m.group(1)) / float(m.group(2)), 6))}]
    if re.fullmatch(r'you glow and light the area', low):
        return [{'type': 'pipican:mob_effect', 'effect': 'minecraft:glowing'}, bonus_kind('emit_light', 15.0)[0]]
    if re.fullmatch(r"your staff returns to you if dropped and can'?t be destroyed", low):
        # A staff (blaze rod or stick) has no durability; dropping it - or dying - brings it back.
        return bonus_kind('staff_return', 1.0)
    if re.fullmatch(r'grimoire bonus also works with a book in any hotbar slot', low):
        return bonus_kind('book_hotbar', 1.0)
    if re.fullmatch(r'your pearls can carry a nearby player with you', low):
        return bonus_kind('pearl_carry', 1.0)
    if re.fullmatch(r'your mount respawns at your bed a day after dying', low):
        return bonus_kind('mount_return', 1.0)
    m = re.fullmatch(rf'mobs below {N}% health that you sweep die instantly', low)
    if m:
        return bonus_kind('sweep_execute', pct(m.group(1)))
    m = re.fullmatch(rf'gadget bonus doubled, but {N}% chance gadgets misfire and damage you', low)
    if m and 'invention' in REACTIONS.get(cid, {}):
        return [{'type': 'pipican:reaction_mod', 'name': 'invention', 'strength': 1.0},
                {'type': 'pipican:on', 'event': 'spend', 'name': 'misfire', 'chance': pct(m.group(1)), 'actions': [{'type': 'pipican:hurt_self', 'amount': 2.0}]}]
    # --- Bulk 42: Gunslinger (crossbows are the firearms until guns exist)
    gun = '#pipican:weapons/crossbows'
    m = re.fullmatch(rf'a pistol you modify at a smithing table holds {N} shots before reloading and never misfires', low)
    if m:
        return bonus_kind('cylinder', float(m.group(1)))
    m = re.fullmatch(rf'\+{N}% firearm damage and \+{N}% accuracy(?: \(less spread\))?', low)
    if m:
        return [{'type': 'pipican:damage_dealt', 'weapons': gun, 'multiplier': round(1 + float(m.group(1)) / 100, 6)}, bonus_kind('accuracy', pct(m.group(2)))[0]]
    m = re.fullmatch(rf'\+{N}% accuracy', low)
    if m:
        return bonus_kind('accuracy', pct(m.group(1)))
    m = re.fullmatch(rf'per {N} ([a-z ]+?),? \+{N} shots? per cylinder', low)
    if m and m.group(2) == sp:
        return bonus_kind('cylinder', 0.0, round(float(m.group(3)) / float(m.group(1)), 6))
    m = re.fullmatch(rf'every shot alerts hostile mobs within {N} blocks, and -{N}% melee damage', low)
    if m:
        return [bonus_kind('shot_alert', float(m.group(1)))[0], {'type': 'pipican:damage_dealt', 'damage_types': '#pipican:is_melee',
                'multiplier': round(1 - float(m.group(2)) / 100, 6)}]
    m = re.fullmatch(rf'first shot after swapping to a gun deals \+{N}%', low)
    if m:
        return bonus_kind('quick_draw', pct(m.group(1)))
    m = re.fullmatch(rf'\+{N}% fire rate', low)
    if m:
        return bonus_kind('reload_speed', pct(m.group(1)))
    m = re.fullmatch(rf"\+{N}% firearm damage, but you can'?t wear heavy armou?r without -{N}% accuracy", low)
    if m:
        return [{'type': 'pipican:damage_dealt', 'weapons': gun, 'multiplier': round(1 + float(m.group(1)) / 100, 6)},
                {'type': 'pipican:conditional', 'when': {'type': 'pipican:item_rule', 'rule': 'heavy_armor', 'min': 1},
                 'effects': bonus_kind('accuracy', -pct(m.group(2)))}]
    if re.fullmatch(r'6th shot in a cylinder always crits', low):
        return bonus_kind('last_shot_crit', 1.0)
    m = re.fullmatch(rf'bullets bounce to a second mob within 4 blocks for {N}%', low)
    if m:
        return bonus_kind('ricochet', pct(m.group(1)))
    # --- Bulk 43
    # "+10% max Witchery, Guidance range +2": a plain bonus and a named upgrade split by a comma.
    if ', ' in low and ';' not in low and low.count(', ') == 1:
        first, second = low.split(', ')
        tail = world(second, ctx)
        if tail and (head := _rest(first, ctx)):
            return head + tail
    if re.fullmatch(r'when a hostile mob enters your temple, you get speed ii for 2s', low):
        return bonus_kind('temple_alarm', 1.0)
    m = re.fullmatch(r'pain curse also gives (weakness|slowness|blindness)', low)
    if m and 'curse' in REACTION_NAMES.get(cid, set()):
        # Pain is the curse that bleeds the mob.
        return [{'type': 'pipican:reaction_mod', 'name': 'curse', 'actions': [{'type': 'pipican:effect', 'target': 'victim', 'state': {'bleeding': True},
                 'effects': [{'effect': 'minecraft:' + m.group(1), 'seconds': 8.0}]}]}]
    # Berserk: lockout after it ends, and Reckless Strike lowering its threshold.
    if 'berserk' in NAMED.get(cid, {}):
        base = NAMED[cid]['berserk'][0]

        m = re.fullmatch(rf"can'?t go berserk again for {N}s", low)
        if m:
            PATCHES.setdefault(cid, []).append(_berserk(cid, base, float(m.group(1))))
            return []
        m = re.fullmatch(rf'berserk triggers at {N}% ([a-z ]+) instead of {N}%; fatigue lasts twice as long', low)
        if m and m.group(2) == rn:
            PATCHES.setdefault(cid, []).append(_berserk(cid, base, None))
            return [bonus_kind('threshold_drop', round((float(m.group(3)) - float(m.group(1))) / 100, 6))[0],
                    {'type': 'pipican:reaction_mod', 'name': 'berserk after-effects', 'duration': 1.0}]
    # --- Bulk 44
    if re.fullmatch(r'chaos outcomes can hit 2 mobs', low):
        return bonus_kind('chaos_double', 1.0)
    if re.fullmatch(r'battle stance can be held while walking slowly', low):
        return bonus_kind('stance_walk', 1.0)
    m = re.fullmatch(rf'while wearing 2 or more pieces of heavy armou?r: -{N}% movement speed and ([a-z\' ]+) is disabled', low)
    if m and m.group(2) in REACTION_NAMES.get(cid, set()):
        heavy2 = {'type': 'pipican:item_rule', 'rule': 'heavy_armor', 'min': 2}
        PATCHES.setdefault(cid, []).append(_gate_reaction(m.group(2), {'type': 'pipican:not', 'condition': heavy2}))
        return [{'type': 'pipican:conditional', 'when': heavy2, 'effects': [{'type': 'pipican:attribute', 'attribute': 'minecraft:movement_speed',
                 'amount': -pct(m.group(1)), 'operation': 'add_multiplied_base'}]}]
    # --- Bulk 45: movement (the client moves the player; the server only sends which abilities it has)
    eff_attr = lambda a: {'type': 'pipican:attribute', 'attribute': a, 'amount': 1.0}
    m = re.fullmatch(r'no slowdown from ((?:powder snow|soul sand|cobwebs|honey|when swimming)(?:(?:, |,? or )(?:powder snow|soul sand|cobwebs|honey|when swimming))*)', low)
    if m:
        what = m.group(1)
        out = []
        if 'soul sand' in what or 'honey' in what:
            out.append(eff_attr('minecraft:movement_efficiency'))
        if 'swimming' in what:
            out.append(eff_attr('minecraft:water_movement_efficiency'))
        if 'cobwebs' in what:
            out.append(bonus_kind('no_cobweb', 1.0)[0])
        if 'powder snow' in what:
            out.append(bonus_kind('no_powder_snow', 1.0)[0])
        return out
    if re.fullmatch(r"cobwebs don'?t slow you", low):
        return bonus_kind('no_cobweb', 1.0)
    m = re.fullmatch(rf"while wet(?: \(rain or water\))?, you can'?t be slowed or caught by cobwebs/powder snow and \+{N}% damage", low)
    if m:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'wet'}, 'effects': [
            {'type': 'pipican:immune', 'effects': ['minecraft:slowness']}, bonus_kind('no_cobweb', 1.0)[0], bonus_kind('no_powder_snow', 1.0)[0],
            {'type': 'pipican:damage_dealt', 'multiplier': round(1 + float(m.group(1)) / 100, 6)}]}]
    if re.fullmatch(r'sprinting into a wall lets you run up it for 2 blocks', low):
        return bonus_kind('wall_run', 1.0)
    m = re.fullmatch(rf'double-jump in mid-air once per 5s, but -{N} armou?r', low)
    if m:
        return [bonus_kind('double_jump', 1.0)[0], {'type': 'pipican:attribute', 'attribute': 'minecraft:armor', 'amount': -float(m.group(1))}]
    m = re.fullmatch(rf'you can glide like with an elytra without one, but -{N} armou?r', low)
    if m:
        return [bonus_kind('glide', 1.0)[0], {'type': 'pipican:attribute', 'attribute': 'minecraft:armor', 'amount': -float(m.group(1))}]
    if re.fullmatch(r'you can climb any wall like a ladder while sneaking and holding jump', low):
        return bonus_kind('wall_climb', 1.0)
    # --- Bulk 46
    if re.fullmatch(r'your nameplate is hidden from other players while sneaking or invisible', low):
        return bonus_kind('hide_nameplate', 1.0)
    if re.fullmatch(r'desire-cursed mobs fight other mobs', low) and 'curse' in REACTION_NAMES.get(cid, set()):
        # Desire is the curse that charms the mob; charmed ones turn on other mobs.
        return [{'type': 'pipican:reaction_mod', 'name': 'curse', 'actions': [{'type': 'pipican:control', 'target': 'victim', 'mode': 'charmed_ally', 'seconds': 8.0}]}]
    if re.fullmatch(r'icon of faith also accepts any shield with a banner pattern', low):
        return bonus_kind('icon_shield', 1.0)
    m = re.fullmatch(rf'guarded casting also gives \+{N} armou?r to players within {N} blocks', low)
    if m and 'guarded casting' in REACTIONS.get(cid, {}):
        secs = next((a.get('seconds') for a in REACTIONS[cid]['guarded casting'].get('actions', []) if a.get('type') == 'pipican:boost'), 10.0)
        return [{'type': 'pipican:reaction_mod', 'name': 'guarded casting', 'actions': [{'type': 'pipican:boost', 'seconds': secs, 'others': float(m.group(2)),
                 'effects': [{'type': 'pipican:attribute', 'attribute': 'minecraft:armor', 'amount': float(m.group(1))}]}]}]
    m = re.fullmatch(rf'per {N} ([a-z]+),? staff hits give ({eff}) (i|ii) for {N}s', low)
    if m and m.group(2) == sp:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:special_stat', 'min': float(m.group(1))}, 'effects': [
            {'type': 'pipican:on', 'event': 'melee_hit', 'weapons': '#pipican:weapons/staff', 'actions': [{'type': 'pipican:effect', 'target': 'victim',
             'effects': [{'effect': 'minecraft:' + combat.EFFECT_NAMES[m.group(3)], 'seconds': float(m.group(5)), 'amplifier': combat.ROMAN[m.group(4)]}]}]}]}]
    if re.fullmatch(r'lost staffs can be remade at a crafting table(?: \(blaze rod \+ amethyst shard\))?', low):
        return []  # any blaze rod or stick is a staff
    if re.fullmatch(r'you can keep one charmed mob as a pet until you sleep', low):
        return bonus_kind('puppet', 1.0)
    m = re.fullmatch(rf'\+{N}% damage for players near you', low)
    if m and ctx.get('implicit_per_point'):
        return [{'type': 'pipican:aura', 'radius': 8.0, 'include_self': False, 'effects': [{'type': 'pipican:damage_dealt', 'multiplier': 1.0,
                 'scaling': [{'source': 'special_stat', 'per': pct(m.group(1))}]}]}]
    if re.fullmatch(r"when you'?d die, you keep fighting for 5s at 1 hp, then die; once per day", low):
        return bonus_kind('glorious_death', 1.0)
    m = re.fullmatch(r'every (\d+)(?:st|nd|rd|th) ability is free', low)
    if m:
        return bonus_kind('free_ability_every', float(m.group(1)))
    if re.fullmatch(r'you get a warning message 30s before rain/thunder starts', low):
        return bonus_kind('weather_warning', 1.0)
    m = re.fullmatch(rf'you take {N}% of the damage nearby allies take instead of them', low)
    if m:
        return bonus_kind('guard_allies', pct(m.group(1)))
    if re.fullmatch(r'players within 3 blocks behind you are protected from projectiles', low):
        return bonus_kind('projectile_ward', 1.0)
    m = re.fullmatch(rf'executed mobs drop \+{N} looting', low)
    if m:
        return bonus_kind('execute_looting', float(m.group(1)))
    if re.fullmatch(r'taming always succeeds on the first try', low):
        return bonus_kind('sure_tame', 1.0)
    m = re.fullmatch(rf'fire charges you throw deal \+{N}% damage', low)
    if m:
        return bonus_kind('fire_charge_damage', pct(m.group(1)))
    m = re.fullmatch(rf'wind charges you throw deal \+{N} damage', low)
    if m:
        return bonus_kind('wind_charge_damage', float(m.group(1)))
    m = re.fullmatch(rf'\+{N}% damage per 16 blocks of distance', low)
    if m:
        return bonus_kind('distance_damage', pct(m.group(1)))
    if re.fullmatch(r'players below 30% health heal twice as fast from you', low):
        return bonus_kind('triage', 1.0)
    # Reactions to what happens around you: "when a mob within 3 blocks hits another player, your next hit on it deals +30%"
    m = re.fullmatch(rf'when a mob within {N} blocks (hits another player|takes damage from another player), your next hit on it (?:deals \+{N}%|is a guaranteed critical)', low)
    if m:
        event = 'mob_hits_ally' if m.group(2).startswith('hits') else 'ally_hits_mob'
        # A guaranteed critical is +50%, like a vanilla crit.
        mult = 1 + float(m.group(3)) / 100 if m.group(3) else 1.5
        name = (ctx.get('node_name') or ('retributive strike' if event == 'mob_hits_ally' else 'opportunist')).lower()
        r = {'type': 'pipican:on', 'event': event, 'name': name, 'victim': {'within': float(m.group(1))},
             'actions': [{'type': 'pipican:mark', 'target': 'victim', 'multiplier': round(mult, 6), 'next_hit': True, 'seconds': 10.0, 'glow': False}]}
        REACTIONS.setdefault(cid, {}).setdefault(name, r)
        return [r]
    m = re.fullmatch(rf'when you hit a mob, players within {N} blocks deal \+{N}% damage to it for {N}s', low)
    if m:
        r = {'type': 'pipican:on', 'event': 'hit', 'name': 'battlefield leadership',
             'actions': [{'type': 'pipican:mark', 'target': 'victim', 'multiplier': round(1 + float(m.group(2)) / 100, 6), 'shared': True,
                          'seconds': float(m.group(3)), 'glow': False}]}
        REACTIONS.setdefault(cid, {}).setdefault('battlefield leadership', r)
        return [r]
    # "Leadership bonus +0.5% per Command point", "retribution bonus +20%", "+10% more Divine Might damage",
    # "Smite bonus damage +1% per 2 Sanctity": a reaction's mark / empower / damage boost grows by that many points.
    def reaction_base(r):
        for a in r.get('actions', []) + EXTRA.get(cid, {}).get(r.get('name', ''), []):
            if a.get('type') in ('pipican:mark', 'pipican:empower') and a.get('multiplier', 1.3) > 1:
                return a.get('multiplier', 1.3) - 1
            if a.get('type') == 'pipican:boost':
                for e in a.get('effects', []):
                    if e.get('type') == 'pipican:damage_dealt' and e.get('multiplier', 1) > 1:
                        return e['multiplier'] - 1
        return None
    names = {}
    for name, r in REACTIONS.get(cid, {}).items():
        for alias in {name, name.split()[0], name.split()[-1]}:
            names.setdefault(alias, (r.get('name', name), r))
    for label, rname in {('zealot', 'divine might'): 'zeal', ('technomancer', 'gadget'): 'invention'}.items():
        if label[0] == cid and rname in REACTIONS.get(cid, {}):
            names[label[1]] = (rname, REACTIONS[cid][rname])
    node = (ctx.get('node_name') or '').lower()
    if node in REACTIONS.get(cid, {}):
        names.setdefault(node, (REACTIONS[cid][node].get('name', node), REACTIONS[cid][node]))
    alias = '|'.join(sorted((re.escape(n) for n in names if len(n) > 3), key=len, reverse=True))
    if alias:
        V = r'(?P<v>\d+(?:\.\d+)?)'
        P = r'(?: per (?:(?P<n>\d+(?:\.\d+)?) )?(?P<s>[a-z]+)(?: points?)?)?'
        m = re.fullmatch(rf'\+{V}% more (?P<w>{alias}) damage', low) or \
            re.fullmatch(rf'(?P<w>{alias}) bonus(?: damage)? \+{V}%{P}', low) or \
            re.fullmatch(rf'(?P<w>{alias}) \+{V}%{P}', low) or \
            re.fullmatch(rf'per (?P<n>\d+(?:\.\d+)?) (?P<s>[a-z]+),? (?P<w>{alias}) \+{V}%', low) or \
            re.fullmatch(rf'(?P<w>[a-z]+) bonus \+{V}%', low)
        if m:
            d = m.groupdict()
            word, value, n, stat = d['w'], d['v'], d.get('n'), d.get('s')
        if m and (word in names or (word not in names and node in names)):
            rname, r = names.get(word) or names[node]
            base = reaction_base(r)
            if base and (not stat or stat in (sp, 'point')):
                k = float(value) / 100 / base
                e = {'type': 'pipican:reaction_mod', 'name': rname, 'strength': 0.0 if stat else round(k, 6)}
                if stat:
                    e['scaling'] = [{'source': 'special_stat', 'per': round(k / float(n or 1), 6)}]
                return [e]
    # Avenger: "Retribution fills by 3 every time you or a nearby ally takes damage"
    m = re.fullmatch(rf'([a-z ]+) fills by {N} every time you or a nearby ally takes damage', low)
    if m and m.group(1) == rn:
        act = [{'type': 'pipican:resource', 'amount': float(m.group(2))}]
        return [{'type': 'pipican:on', 'event': 'hurt', 'actions': act}, {'type': 'pipican:on', 'event': 'ally_hurt', 'actions': act}]
    m = re.fullmatch(rf'every magic hit you land adds {N} ([a-z ]+)', low)
    if m and m.group(2) == rn:
        return [{'type': 'pipican:on', 'event': 'magic_hit', 'actions': [{'type': 'pipican:resource', 'amount': float(m.group(1))}]}]
    # Astromancer: under the open sky, by night or by day.
    m = re.fullmatch(r'([a-z ]+) regenerates twice as fast under the open night sky and in daylight', low)
    if m and m.group(1) == rn and (inner := _rest(f'+100% {ctx.get("resource_name")} regen', ctx)):
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:state', 'state': 'sees_sky'}, 'effects': inner}]
    # Spellbinder: the bound weapon's magic portion (and Magic Weapon raising it from 15% to 35%).
    bound = {'type': 'pipican:item_rule', 'rule': 'bound_weapon'}
    m = re.fullmatch(rf'your bound weapon counts as a magic focus: its hits deal {N}% of their damage as magic damage(?: \(ignores armou?r\))?', low)
    if m:
        return [{'type': 'pipican:conditional', 'when': bound, 'effects': bonus_kind('magic_portion', pct(m.group(1)))}]
    m = re.fullmatch(rf'the magic damage portion of your hits rises to {N}%', low)
    if m and cid == 'spellbinder' and float(m.group(1)) > 15:
        return [{'type': 'pipican:conditional', 'when': bound, 'effects': bonus_kind('magic_portion', pct(float(m.group(1)) - 15))}]
    eff = '|'.join(sorted((re.escape(k) for k in combat.EFFECT_NAMES), key=len, reverse=True))
    # Chosen targets (pipican:quarry): Vow of Vengeance, Hunt Prey, Challenge, and their upgrades.
    m = re.fullmatch(rf'the next mob to damage you or a player within 16 blocks becomes marked for {N}s(?: \(one at a time\))?', low)
    if m:
        return [{'type': 'pipican:quarry', 'name': 'vow of vengeance', 'pick': 'hurt_by', 'seconds': float(m.group(1)), 'glow': True}]
    m = re.fullmatch(rf"-{N}% damage against anything that isn'?t your marked target while a mark is active", low)
    if m and cid == 'avenger':
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:quarry', 'name': 'vow of vengeance'},
                 'effects': [{'type': 'pipican:damage_dealt', 'victim': {'marked': True}, 'except': True, 'multiplier': round(1 - float(m.group(1)) / 100, 6)}]}]
    if cid == 'avenger' and re.fullmatch(r'marks last until the target dies, but you can never mark a new one until it does', low):
        # One vow at a time is already the rule; it now only ends when the mob dies.
        return [{'type': 'pipican:quarry_mod', 'name': 'vow of vengeance', 'forever': True}]
    m = re.fullmatch(rf'the last mob you hit with an arrow becomes your prey for {N}s; you deal \+{N}% damage to it and it glows for you', low)
    if m:
        return [{'type': 'pipican:quarry', 'name': 'hunt prey', 'pick': 'arrow_hit', 'seconds': float(m.group(1)), 'replace': True, 'glow': True,
                 'multiplier': round(1 + float(m.group(2)) / 100, 6)}]
    m = re.fullmatch(rf'\+{N}% hunt prey duration per ([a-z]+) points?', low)
    if m and m.group(2) == sp:
        return [{'type': 'pipican:quarry_mod', 'name': 'hunt prey', 'duration': 0.0, 'scaling': [{'source': 'special_stat', 'per': pct(m.group(1))}]}]
    m = re.fullmatch(rf'you can mark prey in melee too and prey bonus \+{N}%; hunt prey ends instantly if you hit another mob', low)
    if m:
        return [{'type': 'pipican:quarry_mod', 'name': 'hunt prey', 'melee': True, 'multiplier': pct(m.group(1)), 'end_on_other_hit': True}]
    m = re.fullmatch(rf'the first mob you hit becomes your challenged foe for {N}s; it deals {N}% less damage to anyone but you', low)
    if m:
        return [{'type': 'pipican:quarry', 'name': 'challenge', 'pick': 'first_hit', 'seconds': float(m.group(1)), 'glow': True,
                 'others_damage': pct(m.group(2))}]
    if cid == 'duelist' and re.fullmatch(r'when your challenged foe attacks you and misses or is blocked, you counterattack instantly', low):
        r = {'type': 'pipican:on', 'event': 'quarry_miss', 'name': 'riposte', 'actions': [{'type': 'pipican:counter', 'target': 'victim'}]}
        REACTIONS.setdefault(cid, {}).setdefault('riposte', r)
        return [r]
    m = re.fullmatch(rf'riposte chance on any hit from the foe: \+{N}% per ([a-z]+)', low)
    if m and cid == 'duelist' and m.group(2) == sp:
        return [{'type': 'pipican:on', 'event': 'quarry_hurt', 'name': 'riposte', 'chance': 0.0, 'actions': [{'type': 'pipican:counter', 'target': 'victim'}]},
                {'type': 'pipican:reaction_chance', 'name': 'riposte', 'scaling': [{'source': 'special_stat', 'per': pct(m.group(1))}]}]
    if cid == 'duelist' and re.fullmatch(r'riposte hits are always critical', low):
        return [{'type': 'pipican:reaction_mod', 'name': 'riposte', 'strength': 0.5}]
    m = re.fullmatch(rf'challenged foe gets ({eff}) (i|ii|iii)', low)
    if m:
        return [{'type': 'pipican:quarry_mod', 'name': 'challenge', 'effects': {'minecraft:' + combat.EFFECT_NAMES[m.group(1)]: combat.ROMAN[m.group(2)]}}]
    m = re.fullmatch(rf"while the foe hasn'?t hit you, \+{N}% damage", low)
    if m and cid == 'duelist':
        return [{'type': 'pipican:quarry_mod', 'name': 'challenge', 'untouched': pct(m.group(1))}]
    # Hit counters
    m = re.fullmatch(rf'every (\d+)(?:st|nd|rd|th) hit on the same mob deals \+{N}% damage', low)
    if m:
        return [{'type': 'pipican:nth_hit', 'every': int(m.group(1)), 'same_target': True, 'multiplier': round(1 + float(m.group(2)) / 100, 6)}]
    m = re.fullmatch(r'(?:(spears/tridents|spears|tridents): )?every (\d+)(?:st|nd|rd|th) hit strikes twice', low)
    if m:
        e = {'type': 'pipican:nth_hit', 'every': int(m.group(2)), 'strike_twice': True}
        if m.group(1):
            e['weapons'] = {'spears/tridents': '#pipican:weapons/polearms', 'spears': '#pipican:weapons/spears', 'tridents': '#pipican:weapons/tridents'}[m.group(1)]
        return [e]
    m = re.fullmatch(r'every (\d+)(?:st|nd|rd|th) hit (?:while holding your|with your|with) favou?red weapon is a crit', low)
    if m:
        # A crit is +50%, like vanilla's.
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:holding_favored'},
                 'effects': [{'type': 'pipican:nth_hit', 'every': int(m.group(1)), 'multiplier': 1.5}]}]
    m = re.fullmatch(rf'hitting the same mob with both hands in a row deals \+{N}% on the second hit', low)
    if m:
        return bonus_kind('offhand_combo', pct(m.group(1)))
    m = re.fullmatch(rf'combo bonus \+{N}%', low)
    if m and cid == 'dervish':
        return bonus_kind('offhand_combo', pct(m.group(1)))
    # "tamed animals +1 armor and +10% damage; you can have at most 3 tamed wolves": every part must translate.
    if ';' in low:
        parts = [p.strip() for p in low.split(';') if p.strip()]
        own = [world(p, ctx) for p in parts]
        if any(own):
            out = []
            for p, o in zip(parts, own):
                o = o or pets(p, lambda v: (v, None)) or _rest(p, ctx)
                if not o:
                    return None
                out += o
            return out
    m = re.fullmatch(rf'\+{N}% chance per ([a-z]+) point to get one extra item when a mob drops loot', low)
    if m and m.group(2) == sp:
        return bonus_kind('extra_loot', 0.0, pct(m.group(1)))
    m = re.fullmatch(rf'you can have at most {N} tamed wolves', low)
    if m:
        return bonus_kind('max_wolves', float(m.group(1)))
    # Warlord: "Attack Command gives Strength II" - the reaction's effect one level higher.
    eff = '|'.join(sorted((re.escape(k) for k in combat.EFFECT_NAMES), key=len, reverse=True))
    m = re.fullmatch(rf"([a-z' ]+?) gives ({eff}) (ii|iii|iv)", low)
    if m and m.group(1) in REACTIONS.get(cid, {}):
        return [{'type': 'pipican:reaction_mod', 'name': m.group(1),
                 'amplify': {'minecraft:' + combat.EFFECT_NAMES[m.group(2)]: combat.ROMAN[m.group(3)]}}]
    m = re.fullmatch(rf'tamed (cats|wolves|parrots) near you give \+{N} armou?r', low)
    if m:
        inner = _rest(f'+{m.group(2)} armor', ctx)
        if inner:
            kind = {'cats': 'minecraft:cat', 'wolves': 'minecraft:wolf', 'parrots': 'minecraft:parrot'}[m.group(1)]
            return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:nearby', 'entities': 'pets', 'entity': kind, 'radius': 8.0}, 'effects': inner}]
    m = re.fullmatch(rf'splash potions you throw deal \+{N} damage to undead', low)
    if m:
        return bonus_kind('splash_undead', float(m.group(1)))
    m = re.fullmatch(rf'\+{N}% projectile speed for thrown potions and fire charges', low)
    if m:
        return bonus_kind('thrown_speed', pct(m.group(1)))
    m = re.fullmatch(rf'\+{N}% damage to mobs your (?:animals|pets|tamed animals) are attacking', low)
    if m:
        return [{'type': 'pipican:damage_dealt', 'victim': {'pet_target': True}, 'multiplier': round(1 + float(m.group(1)) / 100, 6)}]
    m = re.fullmatch(rf'\+{N}% damage against shields and armou?red mobs', low)
    if m:
        return [{'type': 'pipican:damage_dealt', 'victim': {'armored': True}, 'multiplier': round(1 + float(m.group(1)) / 100, 6)}]
    m = re.fullmatch(rf'the first time you open a naturally generated loot chest, {N}% chance of one extra roll of loot', low)
    if m:
        return bonus_kind('open_locks', pct(m.group(1)))
    m = re.fullmatch(rf'open locks chance \+{N}%(?: per ([a-z]+) points?)?', low)
    if m and (not m.group(2) or m.group(2) == sp):
        return bonus_kind('open_locks', pct(m.group(1)), pct(m.group(1)) if m.group(2) else None)
    m = re.fullmatch(rf'{N}% chance an incoming hit is redirected to a random nearby mob', low)
    if m:
        return bonus_kind('redirect_hit', pct(m.group(1)))
    m = re.fullmatch(rf'mobs within 8 blocks have {N}% chance to miss everyone', low)
    if m:
        return bonus_kind('ill_fortune', pct(m.group(1)))
    # brewing: "takes 25% less time" = a third faster
    m = re.fullmatch(rf'brewing takes {N}% less time', low)
    if m and float(m.group(1)) < 100:
        return bonus_kind('brew_speed', round(1 / (1 - float(m.group(1)) / 100) - 1, 6))
    m = re.fullmatch(rf'brewing is {N}% faster', low)
    if m:
        return bonus_kind('brew_speed', pct(m.group(1)))
    if re.fullmatch(r'you can never reach tier v in any skill(?: \(max tier iv\))?', low):
        return bonus_kind('no_top_tier', 1.0)
    m = re.fullmatch(rf'every shared skill trains {N}% faster for you', low)
    if m:
        return bonus_kind('skill_training', pct(m.group(1)))
    m = re.fullmatch(rf'potion effects you drink also apply to your nearest tamed animal', low)
    if m:
        return bonus_kind('share_drink', 1.0)
    # Reclaim Arcana: a chance that what an ability cost comes back.
    m = re.fullmatch(rf'whenever you spend [a-z ]+, 1-in-(\d+) chance to get it all back', low)
    if m:
        return bonus_kind('resource_refund', round(1 / float(m.group(1)), 6))
    m = re.fullmatch(rf'\+{N}% reclaim chance(?: per ([a-z]+) points?)?|reclaim chance \+{N}%()', low)
    if m and (not m.group(2) or m.group(2) == sp):
        v = pct(m.group(1) or m.group(3))
        return bonus_kind('resource_refund', v, v if m.group(2) else None)
    m = re.fullmatch(rf'reclaim refunds go to {N}%', low)
    if m and float(m.group(1)) > 100:
        return bonus_kind('refund_bonus', round(float(m.group(1)) / 100 - 1, 6))
    # Command Undead: hit mobs become servants (ally control with a cap).
    m = re.fullmatch(rf"hitting (?:a )?(zombie or skeleton|zombie|skeleton|undead mob) that isn'?t targeting you has a {N}% chance to make it your servant for {N}s(?: \(max {N}\))?", low)
    if m:
        victims = combat.mobs({'zombie or skeleton': 'zombies and skeletons', 'zombie': 'zombies', 'skeleton': 'skeletons',
                               'undead mob': 'undead'}[m.group(1)])
        if not victims:
            return None
        control = {'type': 'pipican:control', 'target': 'victim', 'mode': 'ally', 'seconds': float(m.group(3))}
        if m.group(4):
            control['max'] = int(float(m.group(4)))
        r = {'type': 'pipican:on', 'event': 'hit', 'chance': pct(m.group(2)), 'name': 'command', 'victims': victims,
             'victim': {'not_targeting_you': True}, 'actions': [control]}
        REACTIONS.setdefault(cid, {}).setdefault('command undead', r)
        return [r]
    m = re.fullmatch(rf'per {N} ([a-z]+),? \+{N} servants?', low)
    if m and m.group(2) == sp:
        return [{'type': 'pipican:summon_mod', 'cap': 0.0, 'scaling': [{'source': 'special_stat', 'per': round(float(m.group(3)) / float(m.group(1)), 6)}]}]
    if re.fullmatch(r'double servant cap', low):
        return [{'type': 'pipican:summon_mod', 'cap_multiplier': 1.0}]
    # Disarming Charm: "per 10 Resonance, +3% chance a hostile mob you hit forgets you" (it stops targeting you for a while).
    m = re.fullmatch(rf'per {N} ([a-z]+),? \+{N}% chance a (?:hostile )?mob you hit forgets you', low)
    if m and m.group(2) == sp:
        r = {'type': 'pipican:on', 'event': 'hit', 'chance': 0.0, 'name': 'forget',
             'actions': [{'type': 'pipican:control', 'target': 'victim', 'mode': 'pacify', 'seconds': 5.0}]}
        return [r, {'type': 'pipican:reaction_chance', 'name': 'forget',
                    'scaling': [{'source': 'special_stat', 'per': round(float(m.group(3)) / 100 / float(m.group(1)), 6)}]}]
    # Blade: "-20% damage against mobs with natural armor (iron golems, ravagers, wardens, the Wither) and with any weapon that isn't a sword"
    m = re.fullmatch(rf"-{N}% damage against mobs with natural armou?r \(iron golems, ravagers, wardens, the wither\) and with any weapon that isn'?t a sword", low)
    if m:
        k = round(1 - float(m.group(1)) / 100, 6)
        return [{'type': 'pipican:damage_dealt', 'against': ['minecraft:iron_golem', 'minecraft:ravager', 'minecraft:warden', 'minecraft:wither'], 'multiplier': k},
                {'type': 'pipican:conditional', 'when': {'type': 'pipican:not', 'condition': {'type': 'pipican:holding', 'items': '#minecraft:swords'}},
                 'effects': [{'type': 'pipican:damage_dealt', 'multiplier': k}]}]
    # Exorcist: everything that isn't unclean (undead, nether mobs, illager magic...).
    m = re.fullmatch(rf'-{N}% damage against animals, illagers and other ordinary mobs', low)
    if m:
        return [{'type': 'pipican:damage_dealt', 'against': '#pipican:mobs/unclean', 'except': True, 'multiplier': round(1 - float(m.group(1)) / 100, 6)}]
    # Geomancer: "-25% movement speed while swimming and takes 25% more freezing damage"
    m = re.fullmatch(rf'(-{N}% movement speed while swimming) and takes {N}% more (\w+) damage', low)
    if m:
        a, b = _rest(m.group(1), ctx), _rest(f'+{m.group(3)}% {m.group(4)} damage taken', ctx)
        if a and b:
            return a + b
    # Beastmaster: "when one of your tamed animals dies, you get Weakness I and Slowness I for 30s"
    m = re.fullmatch(rf'when one of your tamed animals dies, you get (.+?) for {N}s', low)
    if m:
        actions = []
        for part in re.split(r'\s+and\s+|,\s*', m.group(1)):
            out = combat.outcome(f'get {part} for {m.group(2)}s', 'self')
            if not out:
                return None
            actions += out
        return [{'type': 'pipican:on', 'event': 'pet_death', 'actions': actions}]
    # Perfect Shot (sharpshooter): the node raises the passive's arrow armor ignore from 20% to the new number.
    m = re.fullmatch(rf'armor ignore {N}%', low)
    if m and (ctx.get('node_name') or '').lower() == 'perfect shot' and float(m.group(1)) > 20:
        return bonus_kind('ranged_armor_pierce', pct(float(m.group(1)) - 20))
    # --- Bulk 47
    m = re.fullmatch(rf"sinks in water,? (?:and )?can'?t swim upward while wearing {N}\+ heavy pieces", low)
    if m:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:item_rule', 'rule': 'heavy_armor', 'min': int(float(m.group(1)))},
                 'effects': bonus_kind('sink', 1.0)}]
    if re.fullmatch(r'blocking also stops arrows from the sides', low):
        return bonus_kind('side_block', 1.0)
    m = re.fullmatch(rf'poison you apply also works on undead \(as wither instead\) and deals \+{N}% damage', low)
    if m:
        return bonus_kind('poison_undead', 1.0) + bonus_kind('poison_damage', pct(m.group(1)))
    m = re.fullmatch(rf'\+{N}% poison damage and duration you apply', low)
    if m and ctx.get('implicit_per_point'):
        return bonus_kind('poison_damage', 0.0, pct(m.group(1))) + [
            {'type': 'pipican:applied_effect', 'effect': 'minecraft:poison', 'duration': 0.0, 'scaling': [{'source': 'special_stat', 'per': pct(m.group(1))}]}]
    m = re.fullmatch(rf'copper golems near you move and work {N}% faster', low)
    if m:
        return bonus_kind('copper_heart', pct(m.group(1)))
    m = re.fullmatch(rf"magic damage you deal ignores {N}% of the target'?s armou?r", low)
    if m:
        return bonus_kind('magic_armor_pierce', pct(m.group(1)))
    if re.fullmatch(r'lethargy also stops jumping', low) and 'curse' in REACTION_NAMES.get(cid, set()):
        # Lethargy is the curse that slows: the hobble only takes on a mob the curse just slowed.
        return [{'type': 'pipican:reaction_mod', 'name': 'curse', 'actions': [{'type': 'pipican:hobble', 'seconds': 8.0, 'if_effect': 'minecraft:slowness'}]}]
    m = re.fullmatch(rf'slowness becomes immobilise for {N}s', low)
    if m and 'temple guardian' in REACTION_NAMES.get(cid, set()):
        return [{'type': 'pipican:reaction_mod', 'name': 'temple guardian', 'actions': [{'type': 'pipican:hobble', 'seconds': float(m.group(1)), 'root': True}]}]
    m = re.fullmatch(rf'per {N} ([a-z]+),? ride the lightning works in clear weather at half strength', low)
    if m and m.group(2) == sp:
        wet = [{'type': 'pipican:state', 'state': 'raining'}, {'type': 'pipican:state', 'state': 'thundering'}]
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:all_of', 'conditions': [
                    {'type': 'pipican:not', 'condition': {'type': 'pipican:any_of', 'conditions': wet}},
                    {'type': 'pipican:special_stat', 'min': float(m.group(1))}]},
                 'effects': [{'type': 'pipican:attribute', 'attribute': 'minecraft:movement_speed', 'amount': 0.1, 'operation': 'add_multiplied_base'},
                             {'type': 'pipican:damage_dealt', 'multiplier': 1.075}]}]
    # Arch-Hexer / Grand Inquisitor: one curse / scrutinised mob at a time otherwise.
    m = re.fullmatch(rf'you can curse {N} mobs at once(?:; (.+))?', low)
    if m and 'curse' in REACTION_NAMES.get(cid, set()):
        rest = _rest(m.group(2), ctx) if m.group(2) else []
        if rest is not None:
            return bonus_kind('mark_targets', float(m.group(1)) - 1) + rest
    m = re.fullmatch(rf'scrutini[sz]e {N} mobs at once(?:; (.+))?', low)
    if m and cid == 'inquisitor':
        rest = _rest(m.group(2), ctx) if m.group(2) else []
        if rest is not None:
            return bonus_kind('scrutiny_targets', float(m.group(1)) - 1) + rest
    if re.fullmatch(r'first escalating hit also knocks back', low) and cid == 'mage_knight':
        return bonus_kind('empower_knockback', 0.8)
    m = re.fullmatch(rf'per {N} ([a-z]+),? wandering traders offer {N} extra trades?', low)
    if m and m.group(2) == sp:
        return bonus_kind('wandering_trades', 0.0, round(float(m.group(3)) / float(m.group(1)), 6))
    # Violent Madness (zealot): a frenzy when the bar fills, then it empties.
    m = re.fullmatch(rf'when ([a-z ]+) is full: violent madness - for {N}s you attack anything near you \(including allies\), \+{N}% damage, then \1 resets', low)
    if m and m.group(1) == rn:
        secs = float(m.group(2))
        return [{'type': 'pipican:on', 'event': 'resource_full', 'name': 'violent madness', 'actions': [
            {'type': 'pipican:flag', 'name': 'frenzy', 'seconds': secs},
            {'type': 'pipican:boost', 'seconds': secs, 'effects': [{'type': 'pipican:damage_dealt', 'multiplier': round(1 + pct(m.group(3)), 6)}]},
            {'type': 'pipican:resource', 'percent': -1.0}]}]
    if re.fullmatch(r'violent madness no longer targets players', low):
        return bonus_kind('frenzy_spares_players', 1.0)
    # Low Blow (scout): "revealed" mobs are the ones you marked with Reveal Weakness.
    m = re.fullmatch(rf'when a revealed mob takes damage from an ally, you get a free {N}% damage hit on it(?:; (.+))?', low)
    if m:
        rest = _rest(m.group(2), ctx) if m.group(2) else []
        if rest is not None:
            # A short cooldown so two players with it can't trade free hits forever.
            return [{'type': 'pipican:on', 'event': 'ally_hits_mob', 'victim': {'marked': True}, 'cooldown_ticks': 10, 'name': 'low blow',
                     'actions': [{'type': 'pipican:counter', 'target': 'victim', 'multiplier': pct(m.group(1))}]}] + rest
    # Transmuter's points: whole steps only, counted after the other stats.
    m = re.fullmatch(rf'\+{N} to every stat per {N} points(?: \(rounded down\))?', low)
    if m and ctx.get('implicit_per_point'):
        import effects as fx
        return [{'type': 'pipican:stat', 'stat': 'pipican:' + st, 'per_special': float(m.group(1)), 'step': float(m.group(2))} for st in fx.CORE_STATS]
    # Living Weapon (fighter): the downside is to magic you deal and potions you drink.
    m = re.fullmatch(rf'\+{N}% weapon damage and attack speed, but -{N}% magic damage and potion strength', low)
    if m:
        up = _rest(f'+{m.group(1)}% weapon damage and attack speed', ctx)
        if up:
            return up + [{'type': 'pipican:damage_dealt', 'damage_types': '#pipican:is_magic', 'multiplier': round(1 - pct(m.group(2)), 6)}] + \
                bonus_kind('drunk_potion_strength', -pct(m.group(2)))
    # Templar: the temple itself changes (Fortress Temple, Living Temple, Sacred Ground).
    if cid == 'templar':
        if re.fullmatch(r'temple bonuses doubled', low):
            return bonus_kind('temple_strength', 1.0)
        m = re.fullmatch(rf'-{N}% temple bonuses', low)
        if m:
            return bonus_kind('temple_strength', -pct(m.group(1)))
        if re.fullmatch(r'outside it you lose all class passives', low):
            return bonus_kind('temple_only_passives', 1.0)
        m = re.fullmatch(rf'your temple follows you(?: \(radius {N}\))?', low)
        if m:
            return bonus_kind('temple_mobile', float(m.group(1) or 6))
        m = re.fullmatch(rf'players in your temple regenerate {N}% faster', low)
        if m:
            return bonus_kind('temple_healing', pct(m.group(1)))
    # Lone Wolf (duelist): the challenge is the class's quarry.
    if re.fullmatch(r'duel bonuses doubled', low) and cid == 'duelist':
        return bonus_kind('quarry_double', 1.0)
    m = re.fullmatch(rf"you can'?t damage non-challenged mobs for {N}s after challenging", low)
    if m and cid == 'duelist':
        return bonus_kind('quarry_lockout', float(m.group(1)))
    m = re.fullmatch(r'while ([a-z ]+) is full, your bound weapon glows and lights the area around you', low)
    if m and m.group(1) == rn:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:all_of', 'conditions': [
                    {'type': 'pipican:resource', 'min': 0.999}, {'type': 'pipican:item_rule', 'rule': 'bound_weapon'}]},
                 'effects': bonus_kind('emit_light', 15.0)}]
    return None



def _berserk(cid, base, lock):
    """A patch for the finished class JSON: Berserk's resource condition becomes lowerable (threshold_drop) and, with
    `lock` seconds, can't start again while the "berserk lockout" flag set when it ends is running."""
    def patch(rpg):
        def walk(o):
            if isinstance(o, dict):
                w = o.get('when')
                if o.get('type') == 'pipican:conditional' and isinstance(w, dict):
                    if w.get('type') == 'pipican:all_of' and w.get('conditions') and isinstance(w['conditions'][0], dict) \
                            and {k: v for k, v in w['conditions'][0].items() if k != 'lowerable'} == base:
                        core, rest = w['conditions'][0], w['conditions'][1:]
                    elif {k: v for k, v in w.items() if k != 'lowerable'} == base:
                        core, rest = w, []
                    else:
                        core = None
                    if core is not None:
                        core = dict(core, lowerable=True)
                        flag = {'type': 'pipican:not', 'condition': {'type': 'pipican:flag', 'name': 'berserk lockout'}}
                        if lock and flag not in rest:
                            rest = rest + [flag]
                        o['when'] = {'type': 'pipican:all_of', 'conditions': [core] + rest} if rest else core
                if lock and o.get('type') == 'pipican:on' and o.get('name') == 'berserk after-effects' \
                        and not any(a.get('type') == 'pipican:flag' for a in o.get('actions', [])):
                    o['actions'].append({'type': 'pipican:flag', 'name': 'berserk lockout', 'seconds': lock})
                for v in list(o.values()):
                    walk(v)
            elif isinstance(o, list):
                for v in o:
                    walk(v)
        walk(rpg)
    return patch



def _gate_reaction(name, when):
    """A patch for the finished class JSON: the passive reaction called `name` only works while `when` holds."""
    def patch(rpg):
        for key in ('passives', 'special_passives'):
            out = []
            for e in rpg.get(key, []):
                if e.get('type') == 'pipican:on' and e.get('name') == name:
                    e = {'type': 'pipican:conditional', 'when': when, 'effects': [e]}
                out.append(e)
            if key in rpg:
                rpg[key] = out
    return patch


def whole_line(text, ctx):
    """Lines whose brackets or quotes carry the meaning, matched before notes are dropped. -> effects or None."""
    import combat
    t = text.strip().rstrip('.')
    # "Divine Ecstasy lasts longer: drain -1% per 2 Communion" reads like a label but names the ability.
    am = ability_mods(t.lower(), ctx) or reaction_mods(re.sub(r'^([a-z\' -]+): ', r'\1 ', t.lower()), ctx)
    if am:
        return am
    t = re.sub(r"^[A-Z][\w' -]{1,30}:\s+", '', t)
    low = t.lower()
    w = world(low, ctx)
    if w is not None:
        return w
    # "immune to Nausea, and Blindness/Darkness last half as long": the ", and" ends the immunity list.
    m = re.fullmatch(r'(immune to [a-z ]+), and (.+)', low)
    if m and not re.search(r'\bbut\b', low):
        a, b = _rest(m.group(1), ctx), _rest(m.group(2), ctx)
        if a and b:
            return a + b
    # "breathe underwater, swim 40% faster, no mining slowdown underwater": parts whose wording
    # ("underwater") would otherwise read as a condition on the whole line.
    if ',' in low and not ctx.get('implicit_per_point') and not re.search(r'\bbut\b|;', low):
        import bonus
        parts = [bonus.misc(p.strip(), lambda v: (v, None)) for p in low.split(',')]
        if all(parts):
            return [e for p in parts for e in p]
    # Exacting Strike: "critical hits deal maximum damage (+25% critical damage)" - the bracket is the number.
    m = re.fullmatch(r'critical hits deal maximum damage \(\+(\d+)% critical damage\)', low)
    if m:
        return [{'type': 'pipican:critical', 'damage': round(float(m.group(1)) / 100, 6)}]
    # "+20% damage and knockback immunity", "mobs deal 10% less damage to you and your potion debuffs last 20% shorter":
    # two whole phrases joined by "and", with no condition that could belong to only one of them.
    if ' and ' in low and not ctx.get('implicit_per_point') and not re.search(r'\b(?:but|per|while|when|whenever|if|during|after|in|on|for|within|near|blocks?|get|gets|gain)\b|[;,()]', low) \
            and not re.match(r'(?:other )?(?:players|allies|tamed|your animals|animals|minions|servants|summons|pets|eidolon|(?:your )?mount)\b', low):
        for i in [mm.start() for mm in re.finditer(r' and ', low)]:
            a, b = _rest(low[:i], ctx), _rest(low[i + 5:], ctx)
            if a and b:
                return a + b
    # "mount gets Regeneration I while you ride": the "while" is part of the phrase, not a condition to split off.
    if re.fullmatch(r'(?:your )?mount gets .+ while you ride', low):
        import bonus
        r = bonus.misc(low, lambda v: (v, None))
        if r:
            return r
    # "for 2s after a fully charged hit, you take 20% more damage"
    m = re.fullmatch(rf'for {N}s after a fully charged hit, you take {N}% more damage', low)
    if m:
        return [{'type': 'pipican:on', 'event': 'melee_hit', 'victim': {'charged_hit': True}, 'actions': [
            {'type': 'pipican:boost', 'seconds': float(m.group(1)), 'effects': [{'type': 'pipican:damage_taken', 'multiplier': round(1 + float(m.group(2)) / 100, 6)}]}]}]
    # "every 60s in combat, a random harmful effect hits you for 3s"
    m = re.fullmatch(rf'every {N}s in combat, a random harmful effect hits you for {N}s', low)
    if m:
        secs = float(m.group(2))
        choices = [[{'type': 'pipican:effect', 'target': 'self', 'effects': [{'effect': 'minecraft:' + e, 'seconds': secs}]}]
                   for e in ('poison', 'slowness', 'weakness', 'blindness', 'nausea', 'hunger', 'mining_fatigue')]
        return [{'type': 'pipican:on', 'event': 'in_combat', 'cooldown_ticks': int(float(m.group(1)) * 20), 'actions': [{'type': 'pipican:random', 'choices': choices}]}]
    # "Air Walk: after throwing a wind charge, Slow Falling for 3s"
    m = re.fullmatch(r'after (?:throwing|using) (?:a |an )?(wind charge|ender pearl|snowball|trident|firework rocket), (.+)', low)
    if m:
        import combat
        out = combat.outcome('gain ' + m.group(2), 'self')
        if out:
            item = 'minecraft:' + m.group(1).replace(' ', '_')
            return [{'type': 'pipican:on', 'event': 'use_item', 'weapons': item, 'actions': out}]
    # "Feral Soul: forms drain 50% slower"
    m = re.fullmatch(rf'(?:your )?(?:animal )?forms drain {N}% (slower|faster)', low)
    if m:
        forms = [w['ability'] for n, (w, _) in NAMED.get(ctx.get('class_id'), {}).items() if n.endswith(' form') and w.get('type') == 'pipican:ability_active']
        if forms:
            k = float(m.group(1)) / 100 * (-1 if m.group(2) == 'slower' else 1)
            return [{'type': 'pipican:ability_mod', 'ability': f, 'drain': round(k, 6)} for f in forms]
    # "+20% damage to your marked target and it glows for you"
    m = re.fullmatch(r'(.+ (?:your )?(?:marked target|prey)) and it glows(?: for you)?', low)
    if m and (first := _rest(m.group(1), ctx)):
        return first + glows('marked targets glow', ctx)
    # Stay the Hand: "15% chance that a melee hit against you is cancelled and the attacker stops targeting you for 3s"
    m = re.fullmatch(rf'{N}% chance that a melee hit against you is cancelled(?: and the attacker stops targeting you for {N}s)?', low)
    if m:
        acts = [{'type': 'pipican:shrug'}]
        if m.group(2):
            acts.insert(0, {'type': 'pipican:control', 'target': 'victim', 'mode': 'pacify', 'seconds': float(m.group(2))})
        return [{'type': 'pipican:on', 'event': 'attacked_melee', 'chance': round(float(m.group(1)) / 100, 6), 'actions': acts}]
    # Hasty Escape: "when you take a hit below half health, 25% chance to blink 6 blocks away (20s cooldown)"
    m = re.fullmatch(rf'when you take a hit below half health, {N}% chance to blink {N} blocks away \({N}s cooldown\)', low)
    if m:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:health', 'max': 0.5}, 'effects': [
            {'type': 'pipican:on', 'event': 'hurt', 'chance': round(float(m.group(1)) / 100, 6), 'cooldown_ticks': int(float(m.group(3)) * 20),
             'actions': [{'type': 'pipican:blink', 'distance': float(m.group(2))}]}]}]
    # Brutal Swing: "when you kill a mob with an axe or mace, your attack cooldown resets instantly"
    m = re.fullmatch(r'when you kill a mob(?: with an? (axe or mace|axe|mace|sword|spear))?, your attack cooldown resets instantly', low)
    if m:
        r = {'type': 'pipican:on', 'event': 'kill', 'actions': [{'type': 'pipican:reset_attack'}]}
        if m.group(1):
            r['weapons'] = {'axe or mace': '#pipican:weapons/axes_maces', 'axe': '#pipican:weapons/axes', 'mace': '#pipican:weapons/maces',
                            'sword': '#pipican:weapons/swords', 'spear': '#pipican:weapons/spears'}[m.group(1)]
        return [r]
    # Exploit Opportunity: "critical hits have a 10% chance to instantly reset your attack cooldown"
    m = re.fullmatch(rf'critical hits have a {N}% chance to instantly reset your attack cooldown', low)
    if m:
        return [{'type': 'pipican:on', 'event': 'crit', 'chance': round(float(m.group(1)) / 100, 6), 'actions': [{'type': 'pipican:reset_attack'}]}]
    # Quickening: "whenever you spend Sands, gain Speed II for 3s and your movement doesn't provoke knockback"
    m = re.fullmatch(rf"whenever you spend [a-z ]+, gain speed (i|ii|iii) for {N}s and your movement doesn'?t provoke knockback", low)
    if m:
        secs = float(m.group(2))
        return [{'type': 'pipican:on', 'event': 'spend', 'actions': [
            {'type': 'pipican:effect', 'target': 'self', 'effects': [{'effect': 'minecraft:speed', 'seconds': secs, 'amplifier': {'i': 0, 'ii': 1, 'iii': 2}[m.group(1)]}]},
            {'type': 'pipican:boost', 'seconds': secs, 'effects': [{'type': 'pipican:attribute', 'attribute': 'minecraft:knockback_resistance', 'amount': 1.0}]}]}]
    # Prodigy: "each time you sleep, one random stat gets +2 until you sleep again"
    m = re.fullmatch(rf'each time you sleep, one random stat gets \+{N} until you sleep again', low)
    if m:
        import effects as fx
        amount = int(float(m.group(1)))
        choices = [[{'type': 'pipican:boost', 'seconds': 1200.0, 'effects': [{'type': 'pipican:stat', 'stat': 'pipican:' + st, 'amount': amount}]}]
                   for st in fx.CORE_STATS]
        return [{'type': 'pipican:on', 'event': 'wake', 'actions': [{'type': 'pipican:random', 'choices': choices}]}]
    # Nature Bond: "you can rest in a forest (sneak still for 10s) to heal 1 HP every 2s"
    m = re.fullmatch(rf'you can rest in a forest \(sneak still for {N}s\) to heal {N} hp every {N}s', low)
    if m:
        import conditions as cnd
        forest = cnd.parse('in a forest', ctx)
        if forest:
            when = {'type': 'pipican:all_of', 'conditions': [{'type': 'pipican:state', 'state': 'sneaking'},
                                                            {'type': 'pipican:still', 'seconds': float(m.group(1))}, forest]}
            return [{'type': 'pipican:conditional', 'when': when, 'effects': [{'type': 'pipican:on', 'event': 'second', 'cooldown_ticks': int(float(m.group(3)) * 20),
                                                                                'actions': [{'type': 'pipican:heal', 'target': 'self', 'amount': float(m.group(2))}]}]}]
    # Worker of Miracles: "sneaking + right-clicking a player with an empty hand heals them 6 HP (spends 20 Miracle)"
    m = re.fullmatch(rf'sneaking \+ right-clicking a player with an empty hand heals them {N} hp(?: \(spends {N} [a-z ]+\))?', low)
    if m:
        r = {'type': 'pipican:on', 'event': 'interact', 'victims': 'minecraft:player',
             'actions': [{'type': 'pipican:heal', 'target': 'victim', 'amount': float(m.group(1))}]}
        if m.group(2):
            r['cost'] = float(m.group(2))
        when = {'type': 'pipican:all_of', 'conditions': [{'type': 'pipican:state', 'state': 'sneaking'},
                                                        {'type': 'pipican:holding', 'items': 'minecraft:air', 'hand': 'main'}]}
        return [{'type': 'pipican:conditional', 'when': when, 'effects': [r]}]
    # Spare Parts: "right-clicking the eidolon with an iron ingot heals it 10 HP"
    m = re.fullmatch(rf'right-clicking (?:the|your) eidolon with an? ([a-z ]+?) heals it {N} hp', low)
    if m:
        return [{'type': 'pipican:on', 'event': 'interact', 'victims': 'pipican:eidolon', 'weapons': 'minecraft:' + m.group(1).replace(' ', '_'),
                 'actions': [{'type': 'pipican:heal', 'target': 'victim', 'amount': float(m.group(2))}]}]
    # Earthen Defense: "your resource bar absorbs damage first while you stand on stone, dirt, sand or gravel (1 point per damage)"
    m = re.fullmatch(rf'your resource bar absorbs damage first(?: while (you stand on [a-z ,]+?))?(?: \({N} points? per damage\))?', low)
    if m:
        per_point = 1 / float(m.group(2)) if m.group(2) else 1.0
        shield = {'type': 'pipican:resource_shield', 'absorb': round(per_point, 6)}
        if not m.group(1):
            return [shield]
        import conditions as cnd
        when = cnd.parse(m.group(1), ctx)
        if when:
            return [{'type': 'pipican:conditional', 'when': when, 'effects': [shield]}]
    # Deadly Aim: "holding a fully drawn bow for 2 extra seconds makes the shot deal +50% damage and never miss (no spread)"
    m = re.fullmatch(rf'holding a fully drawn bow for {N} extra seconds makes the shot deal \+{N}% damage and never miss(?: \(no spread\))?', low)
    if m:
        REACTION_NAMES.setdefault(ctx.get('class_id'), set()).add('deadly aim')
        return [{'type': 'pipican:deadly_aim', 'base': True, 'hold_seconds': float(m.group(1)), 'damage': round(float(m.group(2)) / 100, 6)}]
    # Living God: "Stigmata lasts twice as long; you can't sleep while it's active" - "it" is the ability just named.
    m = re.fullmatch(r"(.+?) lasts (twice|three times) as long; you can'?t sleep while it'?s active", low)
    if m:
        first = ability_mods(f'{m.group(1)} lasts {m.group(2)} as long', ctx)
        if first:
            return first + [{'type': 'pipican:conditional', 'when': {'type': 'pipican:ability_active', 'ability': first[0]['ability']},
                             'effects': [{'type': 'pipican:bonus', 'kind': 'no_sleep', 'amount': 1.0}]}]
    # Weapon Mastery: "15% chance to reroll a 'glancing' (non-critical) hit into a critical hit" - a crit chance.
    m = re.fullmatch(rf"{N}% chance to reroll a '?glancing'? \(non-critical\) hit into a critical hit", low)
    if m:
        return [{'type': 'pipican:critical', 'chance': round(float(m.group(1)) / 100, 6)}]
    # Exorcism: "hitting a zombie villager has 10% chance to cure it instantly"
    m = re.fullmatch(rf'hitting a zombie villager has (?:a )?{N}% chance to cure it(?: instantly)?', low)
    if m:
        return [{'type': 'pipican:on', 'event': 'hit', 'victims': 'minecraft:zombie_villager', 'chance': round(float(m.group(1)) / 100, 6),
                 'actions': [{'type': 'pipican:cure'}]}]
    # Purge the Unclean: "unclean mobs you hit can't teleport for 5s (endermen, shulkers)"
    m = re.fullmatch(rf"(.+?) you hit can'?t teleport for {N}s(?: \(.*\))?", low)
    if m:
        import combat
        group = combat.mobs(m.group(1))
        if group:
            return [{'type': 'pipican:on', 'event': 'hit', 'victims': group, 'actions': [{'type': 'pipican:pin', 'seconds': float(m.group(2))}]}]
    # Banishing: "unclean mobs you hit are knocked back 50% farther"
    m = re.fullmatch(rf'(.+?) you hit are knocked back {N}% farther', low)
    if m:
        import combat
        group = combat.mobs(m.group(1))
        if group:
            return [{'type': 'pipican:on', 'event': 'melee_hit', 'victims': group,
                     'actions': [{'type': 'pipican:push', 'target': 'victim', 'strength': round(float(m.group(2)) / 100, 2)}]}]
    # Shrapnel: "explosions you cause give Slowness I"
    m = re.fullmatch(r'explosions you cause (give .+)', low)
    if m:
        import combat
        out = combat.outcome(m.group(1), 'victim')
        if out:
            return [{'type': 'pipican:on', 'event': 'explosion_hit', 'actions': out}]
    # "After an Execute kill, mobs within 6 blocks get Weakness I for 3s"
    m = re.fullmatch(rf'after an execute kill, (?:hostile )?mobs within {N} blocks get (.+)', low)
    if m:
        import combat
        out = combat.outcome('give ' + m.group(2), 'victim')
        if out and out[0].get('type') == 'pipican:effect':
            out[0].update({'target': 'hostiles', 'radius': float(m.group(1))})
            r = {'type': 'pipican:on', 'event': 'execute', 'name': 'execute aftermath', 'actions': out}
            REACTIONS.setdefault(ctx.get('class_id'), {}).setdefault('execute aftermath', r)
            return [r]
    # "Phantom Strike: first hit after a blink deals +25%"
    m = re.fullmatch(rf'(?:your )?first hit after a (?:blink|teleport) deals \+{N}%(?: damage)?', low)
    if m:
        return [{'type': 'pipican:on', 'event': 'teleport', 'actions': [{'type': 'pipican:empower', 'target': 'self',
                 'multiplier': round(1 + float(m.group(1)) / 100, 6), 'hits': 1, 'seconds': 10.0}]}]
    # "Nature's Resilience: bone-mealing a plant gives you +2 absorption hearts for 30s (60s cooldown)"
    m = re.fullmatch(rf'bone-mealing a plant gives you \+{N} absorption hearts for {N}s(?: \({N}s cooldown\))?', low)
    if m:
        level = max(0, int(float(m.group(1)) / 2) - 1)
        r = {'type': 'pipican:on', 'event': 'bone_meal', 'actions': [{'type': 'pipican:effect', 'target': 'self',
             'effects': [{'effect': 'minecraft:absorption', 'seconds': float(m.group(2)), 'amplifier': level}]}]}
        if m.group(3):
            r['cooldown_ticks'] = int(float(m.group(3)) * 20)
        return [r]
    # kill reactions on a kind of victim: "killing a marked target heals you 25%", "killing a bleeding mob refills 20 X",
    # "cursed mobs that die explode in a small Instant Damage cloud", "after a stealth kill, you are invisible for 3s",
    # "poison spreads to mobs within 2 blocks on death"
    m = re.fullmatch(rf'killing a marked target heals you {N}%', low)
    if m:
        return [{'type': 'pipican:on', 'event': 'kill', 'victim': {'marked': True},
                 'actions': [{'type': 'pipican:heal', 'target': 'self', 'percent': round(float(m.group(1)) / 100, 6)}]}]
    m = re.fullmatch(rf'killing a bleeding mob refills {N} [a-z ]+', low)
    if m:
        return [{'type': 'pipican:on', 'event': 'kill', 'victim': {'bleeding': True}, 'actions': [{'type': 'pipican:resource', 'amount': float(m.group(1))}]}]
    m = re.fullmatch(r'cursed mobs that die explode in a small instant damage cloud', low)
    if m:
        return [{'type': 'pipican:on', 'event': 'kill', 'victim': {'marked': True},
                 'actions': [{'type': 'pipican:cloud', 'effect': 'minecraft:instant_damage', 'radius': 2.0}]}]
    m = re.fullmatch(rf"after a stealth kill, you(?:'re| are) invisible for {N}s", low)
    if m:
        return [{'type': 'pipican:on', 'event': 'kill', 'victim': {'not_targeting_you': True},
                 'actions': [{'type': 'pipican:effect', 'target': 'self', 'effects': [{'effect': 'minecraft:invisibility', 'seconds': float(m.group(1))}]}]}]
    m = re.fullmatch(rf'poison spreads to mobs within {N} blocks on death', low)
    if m:
        return [{'type': 'pipican:on', 'event': 'kill', 'victim': {'effects': 'minecraft:poison'},
                 'actions': [{'type': 'pipican:effect', 'target': 'hostiles', 'radius': float(m.group(1)), 'effects': [{'effect': 'minecraft:poison', 'seconds': 5.0}]}]}]
    # "Hotter: burning mobs take +1 damage per second" (near you)
    m = re.fullmatch(rf'burning mobs take \+{N} damage per second', low)
    if m:
        return [{'type': 'pipican:on', 'event': 'second', 'actions': [{'type': 'pipican:damage', 'target': 'hostiles', 'radius': 16.0,
                 'amount': float(m.group(1)), 'state': {'on_fire': True}}]}]
    # weapon-group prefixes: "Powerful Attack: axes/maces: fully charged hits +15% damage"
    m = re.fullmatch(r'(swords|axes|maces|axes/maces|spears/tridents|spears|tridents|bows|crossbows): (.+)', low)
    if m:
        tag = {'swords': '#pipican:weapons/swords', 'axes': '#pipican:weapons/axes', 'maces': '#pipican:weapons/maces',
               'axes/maces': '#pipican:weapons/axes_maces', 'spears': '#pipican:weapons/spears', 'tridents': '#pipican:weapons/tridents',
               'bows': '#pipican:weapons/bows', 'crossbows': '#pipican:weapons/crossbows'}.get(m.group(1))
        rest = re.sub(rf'^fully charged hits \+{N}% damage$', lambda mm: f'fully charged hits deal +{mm.group(1)}%', m.group(2))
        inner = _rest(rest, ctx)
        if tag and inner:
            return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:holding', 'items': tag}, 'effects': inner}]
    # Driven: "10% chance that a hit that would leave you under 2 hearts leaves you at 2 hearts instead"
    m = re.fullmatch(rf'{N}% chance that a hit that would leave you under 2 hearts leaves you at 2 hearts instead', low)
    if m:
        return [{'type': 'pipican:on', 'event': 'low_health', 'chance': round(float(m.group(1)) / 100, 6), 'actions': [{'type': 'pipican:shrug'}]}]
    # Fluid Magic: "potions you drink have 20% chance to also give a random positive effect"
    m = re.fullmatch(rf'potions you drink have {N}% chance to also give a random positive effect', low)
    if m:
        choices = [[{'type': 'pipican:effect', 'target': 'self', 'effects': [{'effect': 'minecraft:' + e, 'seconds': 30.0}]}]
                   for e in ('speed', 'strength', 'regeneration', 'resistance', 'haste', 'jump_boost', 'fire_resistance', 'night_vision')]
        return [{'type': 'pipican:on', 'event': 'drink', 'chance': round(float(m.group(1)) / 100, 6), 'actions': [{'type': 'pipican:random', 'choices': choices}]}]
    # "Driven chance 20%", "random potion effect chance 35%": a new total for the class's named chance.
    m = re.fullmatch(rf'([a-z][a-z ]+?) chance {N}%', low)
    if m:
        name = {'random potion effect': 'fluid magic'}.get(m.group(1), m.group(1))
        base = REACTIONS.get(ctx.get('class_id'), {}).get(name)
        if base and 'chance' in base and float(m.group(2)) / 100 > base['chance']:
            return [{'type': 'pipican:reaction_chance', 'name': name, 'amount': round(float(m.group(2)) / 100 - base['chance'], 6)}]
    # "Crusader: +30% damage to undead, but -10% damage to everything else"
    m = re.fullmatch(rf'(.+?),? but ([+-]{N})% (?:damage )?to everything else', low)
    if m and (first := _rest(m.group(1), ctx)):
        aimed = [e for e in first if e.get('type') == 'pipican:damage_dealt' and ('against' in e or 'victim' in e)]
        if len(aimed) == 1:
            rest = {k: v for k, v in aimed[0].items() if k not in ('multiplier', 'scaling')}
            rest['multiplier'] = round(1 + float(m.group(2)) / 100, 6)
            rest['except'] = True
            return first + [rest]
    # "-25% damage to mobs that aren't poisoned or withered"
    m = re.fullmatch(rf'([+-]{N})% damage to mobs that aren\'?t (poisoned|withered|slowed|burning|bleeding)(?: or (poisoned|withered|slowed))?', low)
    if m:
        names = {'poisoned': 'minecraft:poison', 'withered': 'minecraft:wither', 'slowed': 'minecraft:slowness'}
        if m.group(3) in ('burning', 'bleeding'):
            victim = {'on_fire': True} if m.group(3) == 'burning' else {'bleeding': True}
        else:
            ids = [names[g] for g in (m.group(3), m.group(4)) if g]
            victim = {'effects': ids if len(ids) > 1 else ids[0]}
        return [{'type': 'pipican:damage_dealt', 'victim': victim, 'multiplier': round(1 + float(m.group(1)) / 100, 6), 'except': True}]
    # Stacking boosts: "Blade Dance: each hit gives +1% speed for 3s (stacks 10)",
    # "Rampage: consecutive kills within 5s add +5% damage (stacks 5)"
    m = re.fullmatch(rf'(?:each|every) (hit|melee hit|kill|crit|critical hit) gives (.+) for {N}s \(stacks {N}\)', low) or \
        re.fullmatch(rf'consecutive (kills|hits) within {N}s add (.+) \(stacks {N}\)()', low)
    if m:
        if m.re.pattern.startswith('consecutive'):
            what, secs, bonus_text, stacks = m.group(1), float(m.group(2)), m.group(3), int(float(m.group(4)))
        else:
            what, bonus_text, secs, stacks = m.group(1), m.group(2), float(m.group(3)), int(float(m.group(4)))
        event = {'hit': 'hit', 'hits': 'hit', 'melee hit': 'melee_hit', 'kill': 'kill', 'kills': 'kill', 'crit': 'crit', 'critical hit': 'crit'}[what]
        inner = _rest(re.sub(r'([+-]\d+(?:\.\d+)?%) speed$', r'\1 movement speed', bonus_text), ctx)
        if inner:
            return [{'type': 'pipican:on', 'event': event, 'actions': [{'type': 'pipican:boost', 'seconds': secs, 'effects': inner, 'max_stacks': stacks}]}]
    # Dirty Fighting: "15% chance on hit to give Blindness or Slowness for 2s" (named so "+5% Dirty Fighting chance" finds it)
    m = re.fullmatch(r"dirty fighting: (.+)", text.strip().rstrip('.').lower())
    if m:
        r = combat.reaction(m.group(1))
        if r and len(r) == 1 and r[0].get('type') == 'pipican:on':
            r[0]['name'] = 'dirty fighting'
            return r
    # "+1% damage and +1% max health for your tamed animals": the "and" belongs to the animals, not you.
    if ' and ' in low and not re.search(r'\bbut\b|\bper\b|;', low):
        pet = pets(low, (lambda v: (0.0, [('special_stat', v)])) if ctx.get('implicit_per_point') else (lambda v: (v, None)))
        if pet:
            return pet
    # Evil Eye: "12% chance on hit to curse a mob for 8s with one of: Desire (stops attacking), Lethargy (Slowness II) or Pain (2 damage per second)"
    m = re.fullmatch(rf'{N}% chance on hit to curse a mob for {N}s with one of: (.+)', low)
    if m:
        secs = float(m.group(2))
        choices = []
        for part in re.split(r',\s*(?:or\s+)?|\s+or\s+', m.group(3)):
            mm = re.fullmatch(r'[a-z]+ \((.+)\)', part.strip())
            if not mm:
                return None
            what = mm.group(1)
            if re.fullmatch(r'stops attacking', what):
                acts = [{'type': 'pipican:control', 'target': 'victim', 'mode': 'pacify', 'seconds': secs}]
            elif re.fullmatch(rf'{N} damage per second', what):
                acts = [{'type': 'pipican:dot', 'damage': float(re.match(N, what).group(1)), 'seconds': secs}]
            else:
                entries = _effects_list(what, secs)
                if not entries:
                    return None
                acts = [{'type': 'pipican:effect', 'target': 'victim', 'effects': entries}]
            choices.append(acts)
        return [{'type': 'pipican:on', 'event': 'hit', 'chance': round(float(m.group(1)) / 100, 6), 'name': 'curse', 'actions': [
            {'type': 'pipican:mark', 'target': 'victim', 'seconds': secs, 'multiplier': 1.0, 'glow': False, 'max': 1},
            {'type': 'pipican:random', 'choices': choices}]}]
    # Illusory Duplicates: "12% chance an attack against you hits a decoy instead; you blink 4 blocks away"
    m = re.fullmatch(rf'{N}% chance an attack against you hits a decoy instead(?:; you blink {N} blocks away)?', low)
    if m:
        acts = [{'type': 'pipican:decoy', 'seconds': 3.0}]
        if m.group(2):
            acts.append({'type': 'pipican:blink', 'distance': float(m.group(2))})
        acts.append({'type': 'pipican:shrug'})
        return [{'type': 'pipican:on', 'event': 'attacked', 'chance': round(float(m.group(1)) / 100, 6), 'name': 'decoy', 'actions': acts}]
    # Seize Chaos: "10% of your hits trigger a random effect - double damage, ignite, Levitation on the target, a heal for you,
    # or (rarely) a small explosion on yourself"
    m = re.fullmatch(rf'{N}% of your hits trigger a random effect - double damage, ignite, levitation on the target, a heal for you, or \(rarely\) a small explosion on yourself', low)
    if m:
        good = [[{'type': 'pipican:damage', 'target': 'victim', 'amount': 5.0}],
                [{'type': 'pipican:damage', 'target': 'victim', 'amount': 0, 'fire_seconds': 4.0}],
                [{'type': 'pipican:effect', 'target': 'victim', 'effects': [{'effect': 'minecraft:levitation', 'seconds': 3.0}]}],
                [{'type': 'pipican:heal', 'target': 'self', 'amount': 4.0}]]
        bad = [[{'type': 'pipican:explode', 'power': 1.0}, {'type': 'pipican:damage', 'target': 'self', 'amount': 3.0}]]
        return [{'type': 'pipican:on', 'event': 'hit', 'chance': round(float(m.group(1)) / 100, 6), 'name': 'seize chaos',
                 'actions': [{'type': 'pipican:random', 'choices': good * 3 + bad}]}]
    # Temple of Faith: "an area within 10 blocks of a lodestone, beacon or bed you've set becomes your temple - inside it, +15% damage and +1 armor"
    m = re.fullmatch(r"an area within (\d+) blocks of a lodestone, beacon or bed you've set becomes your temple - inside it, (.+)", low)
    if m:
        eff = _rest(m.group(2), ctx)
        if eff:
            out = [{'type': 'pipican:conditional', 'when': {'type': 'pipican:temple'}, 'effects': eff}]
            if float(m.group(1)) != 10:
                out.append({'type': 'pipican:bonus', 'kind': 'temple_radius', 'amount': float(m.group(1)) - 10})
            return out
    # Off-hand swings: "weapons in your off-hand also swing (off-hand hit every 2nd attack)",
    # "holding a sword in your off-hand lets you hit with it too (off-hand swing every 2nd attack)"
    m = re.fullmatch(r'(?:weapons in your off-hand also swing|holding a (sword|weapon) in your off-hand lets you hit with it too) \(off-hand (?:hit|swing) every (2nd|3rd|other) attack\)', low)
    if m:
        share = {'2nd': 0.5, 'other': 0.5, '3rd': 1 / 3}[m.group(2)]
        return [{'type': 'pipican:bonus', 'kind': 'offhand_swing', 'amount': round(share, 6)}]
    # Divine Smite: "your first melee hit every 5s deals +20% damage (+20% more against undead, Nether mobs, vexes and phantoms)"
    m = re.fullmatch(rf'your first melee hit every {N}s deals \+{N}% damage(?: \(\+{N}% more against (.+)\))?', low)
    if m:
        import combat
        secs, pct = float(m.group(1)), float(m.group(2))
        acts = [{'type': 'pipican:empower', 'target': 'self', 'multiplier': round(1 + pct / 100, 6), 'hits': 1, 'seconds': secs}]
        if m.group(3):
            group = combat.mobs(m.group(4))
            if group is None:
                return None
            acts.append({'type': 'pipican:empower', 'target': 'self', 'multiplier': round(1 + float(m.group(3)) / 100, 6), 'hits': 1,
                         'seconds': secs, 'against': group})
        return [{'type': 'pipican:on', 'event': 'second', 'cooldown_ticks': int(secs * 20), 'actions': acts}]
    # Utter Destruction: "your TNT and explosive damage rolls high (+25% explosion damage)"
    m = re.fullmatch(r'your tnt and explosive damage rolls high \(\+(\d+)% explosion damage\)', low)
    if m:
        return [{'type': 'pipican:damage_dealt', 'damage_types': '#minecraft:is_explosion', 'multiplier': round(1 + float(m.group(1)) / 100, 6)}]
    # Premonitions: "after sleeping, you get 3 'premonitions' - each automatically turns a hit against you into a miss"
    m = re.fullmatch(rf"after sleeping, you get {N} '?premonitions'? - each automatically turns a hit against you into a miss", low)
    if m:
        return [{'type': 'pipican:on', 'event': 'attacked', 'name': 'premonition', 'uses_per_rest': int(float(m.group(1))),
                 'actions': [{'type': 'pipican:shrug'}]}]
    return None


def pulses(t, ctx):
    """Every-N-seconds effects, block follow-ups, sleep heals, glowing, per-hit refills, heal on kill."""
    rn = (ctx.get('resource_name') or '').lower()
    m = re.fullmatch(rf'(?:hostile )?mobs near you take {N} damage every {N}s', t)
    if m:
        return [{'type': 'pipican:on', 'event': 'second', 'cooldown_ticks': int(float(m.group(2)) * 20),
                 'actions': [{'type': 'pipican:damage', 'target': 'hostiles', 'radius': 4.0, 'amount': float(m.group(1))}]}]
    m = re.fullmatch(rf'you (?:slowly )?take {N} damage every {N}s', t)
    if m:
        return [{'type': 'pipican:on', 'event': 'second', 'cooldown_ticks': int(float(m.group(2)) * 20),
                 'actions': [{'type': 'pipican:damage', 'target': 'self', 'amount': float(m.group(1))}]}]
    m = re.fullmatch(rf'(?:hostile )?mobs within {N} blocks of you move {N}% slower', t)
    if m:
        return [{'type': 'pipican:on', 'event': 'second', 'actions': [{'type': 'pipican:effect', 'target': 'hostiles', 'radius': float(m.group(1)),
                 'effects': [{'effect': 'minecraft:slowness', 'seconds': 2.0}]}]}]
    m = re.fullmatch(rf'(?:after a block,? (?:your )?next hit \+{N}%(?: damage)?|blocking an attack makes your next hit within {N}s deal \+{N}% damage)', t)
    if m:
        pct = float(m.group(1) or m.group(3))
        return [{'type': 'pipican:on', 'event': 'block', 'actions': [{'type': 'pipican:empower', 'target': 'self',
                 'multiplier': round(1 + pct / 100, 6), 'hits': 1, 'seconds': float(m.group(2) or 5)}]}]
    m = re.fullmatch(rf'sleeping heals you(?: and players within {N} blocks)? fully', t)
    if m:
        acts = [{'type': 'pipican:heal', 'target': 'self', 'percent': 1.0}]
        if m.group(1):
            acts.append({'type': 'pipican:heal', 'target': 'players', 'radius': float(m.group(1)), 'percent': 1.0})
        return [{'type': 'pipican:on', 'event': 'wake', 'actions': acts}]
    m = re.fullmatch(rf'you glow(?: and give glowing to every mob within {N} blocks)?', t)
    if m:
        out = [{'type': 'pipican:mob_effect', 'effect': 'minecraft:glowing'}]
        if m.group(1):
            out.append({'type': 'pipican:on', 'event': 'second', 'actions': [{'type': 'pipican:effect', 'target': 'hostiles',
                        'radius': float(m.group(1)), 'effects': [{'effect': 'minecraft:glowing', 'seconds': 2.0}]}]})
        return out
    m = re.fullmatch(rf'heal {N} hp on each kill|each kill heals you {N} hp', t)
    if m:
        return [{'type': 'pipican:on', 'event': 'kill', 'actions': [{'type': 'pipican:heal', 'target': 'self', 'amount': float(m.group(1) or m.group(2))}]}]
    if rn:
        r = '(?:' + re.escape(rn) + ('|' + re.escape(rn.split()[-1]) if ' ' in rn else '') + ')'
        m = re.fullmatch(rf'\+{N} (?:more )?{r} per (?:melee )?hit', t)
        if m:
            return [{'type': 'pipican:on', 'event': 'melee_hit', 'actions': [{'type': 'pipican:resource', 'amount': float(m.group(1))}]}]
    return None


CLEAVE_BASE = {}


def strikes(t, ctx):
    cid = ctx.get('class_id')
    # "fully charged axe/mace hits damage all mobs within 2 blocks for 40% (sweep)" (bracket already dropped)
    m = re.fullmatch(rf'(fully[- ]charged )?(?:(axe/mace|axe|mace|sword|melee) )?hits damage all mobs within {N} blocks for {N}%', t)
    if m:
        e = {'type': 'pipican:cleave', 'fraction': round(float(m.group(4)) / 100, 6), 'radius': float(m.group(3))}
        if m.group(1):
            e['charged_only'] = True
        w = {'axe/mace': '#pipican:weapons/axes_maces', 'axe': '#pipican:weapons/axes', 'mace': '#pipican:weapons/maces',
             'sword': '#pipican:weapons/swords'}.get(m.group(2) or '')
        if w:
            e['weapons'] = w
        CLEAVE_BASE[cid] = e['fraction']
        return [e]
    m = re.fullmatch(rf'your fully[- ]charged melee hits also strike one other mob within {N} blocks for {N}% damage', t)
    if m:
        return [{'type': 'pipican:cleave', 'fraction': round(float(m.group(2)) / 100, 6), 'radius': float(m.group(1)), 'max_targets': 1, 'charged_only': True}]
    m = re.fullmatch(rf'sweep radius \+{N} blocks?', t)
    if m and cid in CLEAVE_BASE:
        return [{'type': 'pipican:cleave', 'radius': float(m.group(1))}]
    m = re.fullmatch(rf'sweep damage {N}%', t)
    if m and cid in CLEAVE_BASE:
        return [{'type': 'pipican:cleave', 'fraction': round(float(m.group(1)) / 100 - CLEAVE_BASE[cid], 6)}]
    m = re.fullmatch(rf'sweep damage \+{N}% per ([a-z]+)(?: points?)?', t)
    if m and cid in CLEAVE_BASE and _stat_source(m.group(2), ctx):
        return [{'type': 'pipican:cleave', 'scaling': [{'source': 'special_stat', 'per': round(float(m.group(1)) / 100, 6)}]}]
    # off-hand swing upgrades: "off-hand swing every attack instead of every 2nd", "off-hand swings every attack"
    m = re.fullmatch(r'off-hand swings? every attack(?: instead of every 2nd)?', t)
    if m:
        return [{'type': 'pipican:bonus', 'kind': 'offhand_swing', 'amount': 0.5}]
    if re.fullmatch(r"you can'?t use shields|you cannot use shields", t):
        return [{'type': 'pipican:bonus', 'kind': 'no_shield', 'amount': 1.0}]
    # Trickery: "once every 4 seconds, your next hit deals +15% damage"
    m = re.fullmatch(rf'once every {N} seconds?,? your next hit deals \+{N}% damage', t)
    if m:
        secs = float(m.group(1))
        return [{'type': 'pipican:on', 'event': 'second', 'cooldown_ticks': int(secs * 20), 'actions': [
            {'type': 'pipican:empower', 'target': 'self', 'multiplier': round(1 + float(m.group(2)) / 100, 6), 'hits': 1, 'seconds': secs}]}]
    # banners: "a banner in your off-hand gives players within 8 blocks +1 armor", "banner in off-hand: allies +1 armor"
    m = re.fullmatch(rf'(?:a )?banner in (?:your )?off-hand(?: gives players within {N} blocks|: allies) (.+)', t)
    if m:
        eff = _rest(m.group(2), ctx)
        if eff:
            return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:holding', 'items': '#minecraft:banners', 'hand': 'off'},
                     'effects': [{'type': 'pipican:aura', 'radius': float(m.group(1) or 8), 'effects': eff, 'include_self': False}]}]
    return None


def mechanics(t, ctx):
    cid = ctx.get('class_id')
    st = strikes(t, ctx)
    if st:
        return st
    pu = pulses(t, ctx)
    if pu:
        return pu
    # premonition / curse / decoy counts and add-ons
    m = re.fullmatch(rf'temple radius \+{N} blocks? per ([a-z]+)(?: points?)?|\+{N} temple radius', t)
    if m:
        if m.group(3):
            return [{'type': 'pipican:bonus', 'kind': 'temple_radius', 'amount': float(m.group(3))}]
        if _stat_source(m.group(2), ctx):
            return [{'type': 'pipican:bonus', 'kind': 'temple_radius', 'amount': 0.0, 'scaling': [{'source': 'special_stat', 'per': float(m.group(1))}]}]
    m = re.fullmatch(rf'hostile mobs hit inside your temple get (.+)', t)
    if m:
        entries = _effects_list(m.group(1), 3.0)
        if entries:
            return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:temple'}, 'effects': [{'type': 'pipican:on', 'event': 'hit',
                     'actions': [{'type': 'pipican:effect', 'target': 'victim', 'effects': entries}]}]}]
    m = re.fullmatch(rf'\+{N} premonitions?', t)
    if m:
        return [{'type': 'pipican:reaction_chance', 'name': 'premonition', 'uses': float(m.group(1))}]
    m = re.fullmatch(rf'per {N} ([a-z]+),? \+{N} premonitions?|\+{N} premonitions? per {N} ([a-z]+)', t)
    if m:
        per, stat, n = (m.group(1), m.group(2), m.group(3)) if m.group(1) else (m.group(5), m.group(6), m.group(4))
        if _stat_source(stat, ctx):
            return [{'type': 'pipican:reaction_chance', 'name': 'premonition', 'uses_scaling': [{'source': 'special_stat', 'per': round(float(n) / float(per), 6)}]}]
    if re.fullmatch(r'premonitions also work on arrows', t):
        return []  # they already do
    m = re.fullmatch(rf"after (?:a|an) (decoy),? you(?:'re| are) invisible for {N}s", t)
    if m and ctx.get('passive'):
        EXTRA.setdefault(cid, {}).setdefault('decoy', []).append({'type': 'pipican:effect', 'effects': [{'effect': 'minecraft:invisibility', 'seconds': float(m.group(2))}]})
        return []
    tm = triggers_more(t, ctx)
    if tm:
        return tm
    rm = reactions_more(t, ctx)
    if rm:
        return rm
    du = durability(t, ctx)
    if du:
        return du
    # "sigil weapon hits ignite for 2s", "bound weapon hits set mobs on fire for 2s while Charge is above half"
    m = re.fullmatch(rf'(bound weapon|sigil weapon|staff) hits (?:ignite|set mobs on fire)(?: mobs)? for {N}s(?: (while .+))?', t)
    if m:
        import conditions as cond
        whens = [ITEM_CONDS[m.group(1)]]
        if m.group(3):
            extra = cond.parse(m.group(3), ctx)
            if extra is None:
                return None
            whens.append(extra)
        when = whens[0] if len(whens) == 1 else {'type': 'pipican:all_of', 'conditions': whens}
        return [{'type': 'pipican:conditional', 'when': when, 'effects': [{'type': 'pipican:on', 'event': 'melee_hit',
                 'actions': [{'type': 'pipican:damage', 'target': 'victim', 'amount': 0, 'fire_seconds': float(m.group(2))}]}]}]
    au = auras(t, ctx)
    if au:
        return au
    m = re.fullmatch(rf'(?:abilities have {N}% shorter cooldowns|-{N}% ability cooldowns?|ability cooldowns -{N}%)', t)
    if m:
        return [{'type': 'pipican:bonus', 'kind': 'ability_cooldown', 'amount': -round(float(m.group(1) or m.group(2) or m.group(3)) / 100, 6)}]
    m = re.fullmatch(rf'execute threshold {N}%', t)
    if m:
        return [{'type': 'pipican:execute', 'threshold': round(float(m.group(1)) / 100, 6)}]
    # --- "every 30s in combat, a holy pulse hits mobs within 4 blocks - Weakness I for 3s"
    m = re.fullmatch(rf'every {N}s (?:while )?in combat,? (.+)', t)
    if m:
        what = m.group(2)
        acts = None
        mm = re.fullmatch(rf'.*?(?:hits|affects) (?:hostile )?mobs within {N} blocks - (.+)', what)
        if mm:
            entries = _effects_list(mm.group(2))
            if entries:
                acts = [{'type': 'pipican:effect', 'target': 'hostiles', 'radius': float(mm.group(1)), 'effects': entries}]
        mm = re.fullmatch(rf'a friendly .*?minion(?: \(vex model\))? appears to fight for {N}s', what)
        if mm:
            acts = [{'type': 'pipican:summon', 'entity': 'minecraft:vex', 'seconds': float(mm.group(1)), 'ally': True}]
        if acts:
            return [{'type': 'pipican:on', 'event': 'in_combat', 'cooldown_ticks': int(float(m.group(1)) * 20), 'actions': acts}]
    # --- add-ons to a named reaction: "after Zeal triggers, immune to ... for 5s", "hits within 5s of a Zeal trigger deal +20% damage"
    for word, rname in TRIGGERED.items():
        m = re.fullmatch(rf'after {word} triggers,? (.+)', t)
        if m:
            b = _immune_boost(m.group(1))
            if b is None:
                mm = re.fullmatch(rf'(.+?) for {N}s', m.group(1))
                import bonus
                b = bonus._boost(mm.group(1), mm.group(2), ctx) if mm else None
            if b and ctx.get('passive'):
                EXTRA.setdefault(cid, {}).setdefault(rname, []).append(b)
                return []
        m = re.fullmatch(rf'hits within {N}s of an? {word} trigger deal \+{N}% damage', t)
        if m and ctx.get('passive'):
            EXTRA.setdefault(cid, {}).setdefault(rname, []).append({'type': 'pipican:boost', 'seconds': float(m.group(1)),
                'effects': [{'type': 'pipican:damage_dealt', 'multiplier': round(1 + float(m.group(2)) / 100, 6)}]})
            return []
    # --- "when Berserk ends: Weakness I and Slowness I for 8s"
    for name, (when, _) in NAMED.get(cid, {}).items():
        m = re.fullmatch(rf'when {re.escape(name)} ends:? (.+)', t)
        if m:
            entries = _effects_list(m.group(1))
            if entries:
                r = {'type': 'pipican:on', 'event': 'end', 'name': f'{name} after-effects', 'actions': [{'type': 'pipican:effect', 'effects': entries}]}
                REACTIONS.setdefault(cid, {}).setdefault(f'{name} after-effects', r)
                return [{'type': 'pipican:conditional', 'when': when, 'effects': [r]}]
    # --- "when you would die": once per rest / per day / per N minutes
    m = re.fullmatch(rf'(?:once per (rest|day|{N} minutes?),? )?when you(?:\'d| would) die,? (.+)', t)
    if m:
        limit = {}
        if m.group(1) == 'rest':
            limit['once_per_rest'] = True
        elif m.group(1) == 'day':
            limit['cooldown_ticks'] = 24000
        elif m.group(2):
            limit['cooldown_ticks'] = int(float(m.group(2)) * 1200)
        what = m.group(3)
        acts = None
        mm = re.fullmatch(rf'go (.+?) instead with {N} hp', what)
        if mm and mm.group(1) in NAMED.get(cid, {}):
            when = NAMED[cid][mm.group(1)][0]
            if when.get('type') == 'pipican:resource':
                acts = [{'type': 'pipican:resource', 'percent': 1.0}, {'type': 'pipican:cheat_death', 'health': float(mm.group(2))}]
        mm = re.fullmatch(rf'(?:explode in flames?(?: and )?)?heal {N} ?hp', what)
        if mm:
            acts = ([{'type': 'pipican:damage', 'target': 'hostiles', 'radius': 4.0, 'amount': 4.0, 'fire_seconds': 4.0}] if 'explode' in what else []) + \
                   [{'type': 'pipican:cheat_death', 'health': float(mm.group(1))}]
        if acts:
            return [dict({'type': 'pipican:on', 'event': 'death', 'actions': acts}, **limit)]
    # --- "once per 5 minutes, when you'd drop below 2 hearts, heal 6 HP and gain +20% damage for 5s"
    m = re.fullmatch(rf'once per {N} minutes?,? when you(?:\'d| would) drop below 2 hearts,? (.+)', t)
    if m:
        acts = _heal_and_boost(m.group(2), ctx)
        if acts:
            return [{'type': 'pipican:on', 'event': 'low_health', 'cooldown_ticks': int(float(m.group(1)) * 1200), 'actions': acts}]
    # --- execute
    m = re.fullmatch(rf'if (?:a|your) hit (?:takes|leaves|brings) (?:a mob|it|the mob) below {N}% health,? it dies', t) or \
re.fullmatch(rf'if a hit takes a mob below {N}% health, it dies', t)
    if m:
        return [{'type': 'pipican:execute', 'threshold': round(float(m.group(1)) / 100, 6)}]
    m = re.fullmatch(rf'(.+?) below {N}% health die from your hits', t)
    if m:
        import combat
        group = combat.mobs(re.sub(r'\s+mobs$', '', m.group(1)))
        if group:
            return [{'type': 'pipican:execute', 'threshold': round(float(m.group(2)) / 100, 6), 'victims': group}]
    m = re.fullmatch(rf'execute threshold \+{N}% per ([a-z]+)(?: points?)?', t)
    if m and _stat_source(m.group(2), ctx):
        return [{'type': 'pipican:execute', 'scaling': [{'source': 'special_stat', 'per': round(float(m.group(1)) / 100, 6)}]}]
    m = re.fullmatch(rf'fully charged hits deal \+{N}%(?: damage)?', t)
    if m:
        return [{'type': 'pipican:damage_dealt', 'victim': {'charged_hit': True}, 'multiplier': round(1 + float(m.group(1)) / 100, 6)}]
    # --- auras: "players within 10 blocks (including you) get +10% damage", "players within 4 blocks of you take 15% less damage"
    # "players within 6 blocks also immune to Poison", "players near you are also immune to Nausea"
    m = re.fullmatch(rf'(?:other )?players (?:within {N} blocks(?: of you)?|near you) (?:are )?(?:also )?(immune to .+|lose .+ (?:twice|three times) as fast)', t)
    if m:
        import combat, bonus
        eff = combat.immune(m.group(2)) or bonus.misc(m.group(2), lambda v: (v, None))
        if eff:
            a = {'type': 'pipican:aura', 'radius': float(m.group(1) or 8), 'effects': eff, 'include_self': False}
            label = LABELS.get(ctx.get('class_id'), {}).get(t)
            if label:
                a['name'] = label
            return [a]
    m = re.fullmatch(rf'(?:other )?players within {N} blocks(?: of you)?( \(including you\)| including you)?,? (?:get|gain|have|take|deal) (.+)', t)
    if m:
        verb = re.search(r'\b(get|gain|have|take|deal)\b', t).group(1)
        rest = m.group(3)
        rest = ('takes ' + rest) if verb == 'take' else rest
        # "+10% damage and immunity to Darkness, Nausea and Blindness": the immunity list is one part.
        im = re.search(r',?\s+and (immun(?:e|ity) to .+)$', rest)
        if im:
            first, second = _rest(rest[:im.start()], ctx), _rest(im.group(1), ctx)
            eff = first + second if first and second else None
        else:
            eff = _rest(rest, ctx)
        if eff:
            a = {'type': 'pipican:aura', 'radius': float(m.group(1)), 'effects': eff}
            label = LABELS.get(ctx.get('class_id'), {}).get(t)
            if label:
                a['name'] = label
            if not m.group(2):
                a['include_self'] = False
            return [a]
    # --- challenge: "the first mob you hit becomes your challenged foe for 30s"
    m = re.fullmatch(rf'the first mob you hit becomes your challenged foe for {N}s(?:; .+)?', t)
    if m and not m.group(0).count(';'):
        secs = float(m.group(1))
        return [{'type': 'pipican:on', 'event': 'hit', 'cooldown_ticks': int(secs * 20),
                 'actions': [{'type': 'pipican:mark', 'target': 'victim', 'seconds': secs, 'multiplier': 1.0}]}]
    # --- scrutiny
    m = re.fullmatch(rf'the mob you look at for {N}s becomes scrutinised - it glows, (?:you see its health, )?and you deal \+{N}% damage to it', t)
    if m:
        return [{'type': 'pipican:scrutiny', 'look_seconds': float(m.group(1)), 'seconds': 30.0},
                {'type': 'pipican:damage_dealt', 'victim': {'marked': True}, 'multiplier': round(1 + float(m.group(2)) / 100, 6)}]
    m = re.fullmatch(rf'per {N} ([a-z]+),? scrutinise {N}s faster', t)
    if m and _stat_source(m.group(2), ctx):
        return [{'type': 'pipican:scrutiny', 'scaling': [{'source': 'special_stat', 'per': round(-float(m.group(3)) / float(m.group(1)), 6)}]}]
    # --- favored weapon
    if re.fullmatch(r'when you sleep, the weapon type in your main hand becomes your favored weapon type(?: \(.*\))?', t):
        return [{'type': 'pipican:favored_weapon'}]
    m = re.fullmatch(r'with your favored weapon,? (.+)', t)
    if m:
        eff = _rest(m.group(1), ctx)
        if eff:
            return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:holding_favored'}, 'effects': eff}]
    m = re.fullmatch(rf'favored weapon critical hits deal \+{N}%', t)
    if m:
        return [{'type': 'pipican:conditional', 'when': {'type': 'pipican:holding_favored'},
                 'effects': [{'type': 'pipican:critical', 'damage': round(float(m.group(1)) / 100, 6)}]}]
    # --- Zeal: "when a hit would bring you below 2 hearts, 30% chance to shrug it off entirely (+10 Madness)"
    m = re.fullmatch(rf'when a hit would bring you below 2 hearts, {N}% chance to shrug it off entirely(?: and gain {N} ([a-z ]+))?', t)
    if m:
        rn = (ctx.get('resource_name') or '').lower()
        actions = []
        if m.group(2):
            if m.group(3).strip() != rn:
                return None
            actions.append({'type': 'pipican:resource', 'amount': float(m.group(2))})
        actions.append({'type': 'pipican:shrug'})
        return [{'type': 'pipican:on', 'event': 'low_health', 'chance': round(float(m.group(1)) / 100, 6), 'name': 'zeal', 'actions': actions}]
    return None


PETS = r'(?:your )?(?:tamed animals|tamed wolves, cats, horses and parrots|animals you tame|animals|pets)(?: near you| within \d+ blocks)?'


def pets(t, scal):
    """Tamed-animal upgrades -> pipican:pet_mod ("tamed animals +5% damage", "+1 heart to animals you tame")."""
    def mod(parts):
        e = {'type': 'pipican:pet_mod'}
        scaling = None
        for key, raw in parts:
            v, s = scal(raw)
            if s:
                pct = key in ('damage', 'health', 'damage_taken')
                sc = [{'source': src, 'per': round(per / (100 if pct else 1), 6)} for src, per in s]
                if scaling is not None and sc != scaling:
                    return None  # one scaling list drives every amount
                scaling = sc
                e['scaling'] = sc
                e[key] = 0.0
                continue
            e[key] = round(v / 100, 6) if key in ('damage', 'health', 'damage_taken') else v
        return [e]

    def part(p):
        p = p.strip()
        m = re.fullmatch(rf'\+?{N}% (?:more )?damage', p)
        if m:
            return 'damage', float(m.group(1))
        m = re.fullmatch(rf'\+?{N}% (?:max )?health', p)
        if m:
            return 'health', float(m.group(1))
        m = re.fullmatch(rf'\+{N} armou?r', p)
        if m:
            return 'armor', float(m.group(1))
        m = re.fullmatch(rf'\+{N} hearts?', p)
        if m:
            return 'hearts', float(m.group(1))
        m = re.fullmatch(rf'\+{N} max health', p)
        if m:
            return 'hearts', float(m.group(1)) / 2
        return None

    def parts(text):
        out = [part(p) for p in re.split(r'\s+and\s+|,\s*', text)]
        return None if not out or None in out else out

    m = re.fullmatch(rf'{PETS} takes? ([+-]?{N})% (?:less |more )?damage(?: taken)?', t)
    if m:
        v = float(m.group(1))
        if 'less' in t and v > 0:
            v = -v
        return mod([('damage_taken', v)])
    m = re.fullmatch(rf'{PETS} (?:get |gain |have )?(.+)', t)
    if m and (ps := parts(m.group(1))):
        return mod(ps)
    m = re.fullmatch(rf'(.+?) (?:for|to) {PETS}', t)
    if m and (ps := parts(m.group(1))):
        return mod(ps)
    return None
