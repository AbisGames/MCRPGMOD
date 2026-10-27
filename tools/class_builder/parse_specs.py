"""Parse class_specs/*.md (the plain-language template) into structured dicts."""
import os
import re


def _field(block, label):
    m = re.search(r'^- \*\*' + re.escape(label) + r':\*\*\s*(.*)$', block, re.M)
    return m.group(1).strip() if m else None


def _sections(text):
    """{'1': body, '2': body, ...} keyed by the leading number of each '## N.' heading."""
    out = {}
    parts = re.split(r'^## (\d+)\.[^\n]*\n', text, flags=re.M)
    for i in range(1, len(parts), 2):
        out[parts[i]] = parts[i + 1]
    return out


def _table(body):
    rows = []
    for line in body.splitlines():
        line = line.strip()
        if not line.startswith('|') or line.startswith('|---') or re.match(r'^\|\s*-+\s*\|', line):
            continue
        cells = [c.strip() for c in line.strip('|').split('|')]
        rows.append(cells)
    return rows[1:] if rows else rows  # drop header


def _bullets(body):
    return [l[2:].strip() for l in body.splitlines() if l.startswith('- ') and l[2:].strip() not in ('', '-', 'none')]


def parse(path):
    text = open(path, encoding='utf-8').read()
    s = _sections(text)
    c = {'file': os.path.basename(path)}
    b = s['1']
    c['id'] = _field(b, 'Id')
    c['name'] = _field(b, 'Display name')
    c['description'] = _field(b, 'Description')
    c['icon'] = _field(b, 'Icon item')
    c['selectable'] = (_field(b, 'Selectable by players') or 'yes').lower().startswith('y')
    c['health'] = float(re.match(r'[\d.]+', _field(b, 'Base health')).group())

    c['stats'] = {}
    for row in _table(s['2']):
        if len(row) >= 2 and re.match(r'^-?\d+$', row[1]):
            key = 'SPECIAL' if row[0].lower().startswith('special') else row[0].lower()
            c['stats'][key] = int(row[1])

    sp = s['3']
    res_split = re.split(r'^### Resource bar[^\n]*\n', sp, flags=re.M)
    head = res_split[0]
    c['special'] = {
        'name': _field(head, 'Name'),
        'description': _field(head, 'Description'),
        'per_point': _field(head, 'What each point gives'),
        'max': int(re.match(r'\d+', _field(head, 'Max points') or '100').group()),
    }
    c['resource'] = None
    if len(res_split) > 1:
        r = res_split[1]
        num = lambda label: float(re.search(r'-?[\d.]+', _field(r, label)).group())
        colour = _field(r, 'Colour') or ''
        hexm = re.search(r'#([0-9A-Fa-f]{6})', colour)
        c['resource'] = {
            'name': _field(r, 'Name'),
            'base_max': num('Max at 0 points'),
            'max_per_point': num('Extra max per special-stat point'),
            'regen_per_second': num('Regen per second'),
            'regen_per_point': num('Extra regen per second per special-stat point'),
            'starts_full': (_field(r, 'Starts full when the class is chosen') or 'yes').lower().startswith('y'),
            'color': hexm.group(1) if hexm else None,
            'raw_regen': _field(r, 'Regen per second'),
            'starts_note': _field(r, 'Starts full when the class is chosen') or '',
        }

    c['kit'] = []
    for line in _bullets(s['4']):
        m = re.match(r'^(\d+)\s*[×x]\s*(\S+)(.*)$', line)
        c['kit'].append((int(m.group(1)), m.group(2), m.group(3).strip()) if m else (None, None, line))

    pas = re.split(r'^### Special passives[^\n]*\n', s['5'], flags=re.M)
    c['passives'] = _bullets(pas[0])
    c['special_passives'] = _bullets(pas[1]) if len(pas) > 1 else []

    c['weakness'] = {'read': _field(s['6'], 'What players read'), 'does': _field(s['6'], 'What it actually does')}

    c['affinity'] = {}
    c['affinity_exceptions'] = 'none'
    for row in _table(s['7']):
        if len(row) < 2:
            continue
        if row[0].lower().startswith('specific'):
            c['affinity_exceptions'] = row[1]
        else:
            c['affinity'][row[0].split('(')[0].strip().lower()] = row[1].strip().lower()

    c['trinkets'] = {row[0].lower(): int(row[1]) for row in _table(s['8']) if len(row) >= 2 and row[1].isdigit()}

    lv = s['9']
    c['max_level'] = int(_field(lv, 'Max class level'))
    c['free_general'] = int(_field(lv, 'Free skill points at start (general tree)'))
    c['free_special'] = int(_field(lv, 'Free skill points at start (special tree)'))

    c['skills'] = []
    for blk in re.split(r'^### Skill:\s*', s['10'], flags=re.M)[1:]:
        name, _, body = blk.partition('\n')
        ref = re.search(r'full definition in `([^`]+)`', body)
        sk = {'name': name.strip()}
        if ref:
            sk['ref'] = ref.group(1)
        else:
            sk.update({
                'shared': (_field(body, 'Shared or class-only') or '').lower().startswith('shared'),
                'trigger': _field(body, 'Trained by'),
                'points_to': 'special' if 'special' in (_field(body, 'Tier points go to') or '').lower() else 'general',
                'speed': _field(body, 'How fast it levels') or 'normal',
                'tiers': [_field(body, f'Tier {t} bonus') for t in ('I', 'II', 'III', 'IV', 'V')],
            })
        c['skills'].append(sk)

    trees = {}
    for blk in re.split(r'^### 11([abc])\.[^\n]*\n', s['11'], flags=re.M)[1:]:
        pass
    parts = re.split(r'^### 11([abc])\.[^\n]*\n', s['11'], flags=re.M)
    for i in range(1, len(parts), 2):
        kind = {'a': 'general', 'b': 'special', 'c': 'stat'}[parts[i]]
        body = parts[i + 1]
        nodes = []
        for row in _table(body):
            if len(row) < 7 or not row[0]:
                continue
            nodes.append({
                'id': row[0], 'name': row[1], 'size': row[2].lower(),
                'cost': int(row[3]) if row[3].isdigit() else 1,
                'level': int(row[4]) if row[4].isdigit() else 1,
                'links': [l.strip() for l in row[5].split(',') if l.strip() and l.strip() != '-'],
                'gives': row[6] if row[6] not in ('-', '') else '',
            })
        trees[kind] = {
            'nodes': nodes,
            'where': _field(body, 'Where this class starts'),
            'shared': (_field(body, 'Shared with other classes, or its own?') or 'shared').lower().startswith('shared'),
        }
    c['trees'] = trees
    c['notes'] = s.get('12', '').strip()
    return c


def parse_dir(d):
    out = []
    for f in sorted(os.listdir(d)):
        path = os.path.join(d, f)
        if f.endswith('.md') and not f.startswith(('_', 'example_')) and open(path, encoding='utf-8').read().startswith('# Class:'):
            out.append(parse(path))
    return out
