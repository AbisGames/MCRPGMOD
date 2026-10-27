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
    for name, (when, _) in sorted(NAMED.get(ctx.get('class_id'), {}).items(), key=lambda kv: -len(kv[0])):
        pat = rf'(?:while |when |in |during )(?:in )?(?:your )?{re.escape(name)}(?: is active| is on)?'
        out.append((pat, lambda m, w=when: w, 'other'))
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


def segment(t, ctx):
    """Whole-segment phrases. `t` is lowercase, label already stripped."""
    import bonus
    import combat
    import effects as fx
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
    m = re.fullmatch(rf'{words} range ([+-]{N}) blocks?(?: per (?:{N} )?([a-z]+)(?: points?)?)?', t) or \
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
    m = re.fullmatch(rf'\+?{N}% damage and health for your {who}', t)
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
            return [{'type': 'pipican:on', 'event': 'lightning', 'cooldown_ticks': 40, 'actions': [
                {'type': 'pipican:resource', 'percent': 1.0}, {'type': 'pipican:boost', 'seconds': float(m.group(3)), 'effects': eff},
                {'type': 'pipican:effect', 'effects': entries}]}]
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


def whole_line(text, ctx):
    """Lines whose brackets or quotes carry the meaning, matched before notes are dropped. -> effects or None."""
    import combat
    t = text.strip().rstrip('.')
    t = re.sub(r"^[A-Z][\w' -]{1,30}:\s+", '', t)
    low = t.lower()
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
            {'type': 'pipican:mark', 'target': 'victim', 'seconds': secs, 'multiplier': 1.0, 'glow': False},
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


def mechanics(t, ctx):
    cid = ctx.get('class_id')
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
                return [{'type': 'pipican:conditional', 'when': when,
                         'effects': [{'type': 'pipican:on', 'event': 'end', 'actions': [{'type': 'pipican:effect', 'effects': entries}]}]}]
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
