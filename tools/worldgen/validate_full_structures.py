#!/usr/bin/env python3
"""Check every generated structure pool, assembly, NBT header and referenced file.

Static validation, not a substitute for loading this pack on Minecraft 26.1.2.
"""
import gzip, json, struct, sys
from pathlib import Path

HERE=Path(__file__).resolve().parent
OUT=HERE/'output'
BASE=OUT/'data/pipican'
# In the repository consume the canonical author data; standalone builds can
# keep the JSON files next to this validator.
ASSETS=HERE if (HERE/'region_map.json').is_file() else HERE.parents[1]/'src/main/resources/data/pipican/worldgen'
POOLS=json.loads((ASSETS/('asset_manifest.json' if (ASSETS/'asset_manifest.json').is_file() else 'structure_pools_manifest.json')).read_text())['pools']
SITES=json.loads((ASSETS/'settlements.json').read_text())['settlements']
REGIONS=json.loads((ASSETS/'region_map.json').read_text())['macro_regions']

# Lightweight parser: only inspect the root structure tags, then sanity-check
# lists and coordinate payload sizes without building an entire NBT tree.
def inspect_nbt(file, payload=None):
    data=gzip.decompress(file.read_bytes() if payload is None else payload)
    offset=0
    def take(n):
        nonlocal offset
        assert offset+n<=len(data),f'Truncated tag: {file}'
        out=data[offset:offset+n];offset+=n;return out
    def read_string():
        n=struct.unpack('>H',take(2))[0]
        return take(n).decode('utf-8')
    def read_int():return struct.unpack('>i',take(4))[0]
    assert take(1)==b'\x0a'
    assert read_string()==''
    sizes=None;palette_count=None;blocks_count=None;has_entities=False
    while offset<len(data):
        t=take(1)[0]
        if t==0:break
        key=read_string()
        if key=='size' and t==9:
            assert take(1)==b'\x03';assert read_int()==3
            sizes=tuple(read_int() for _ in range(3))
        elif key=='DataVersion' and t==3:
            assert read_int()>100
        elif key=='palette' and t==9:
            assert take(1)==b'\x0a';n=read_int();palette_count=n
            # Each palette entry is a compound containing Name string and
            # optionally Properties (compound of string tags).
            for _ in range(n):
                block_name=None
                while True:
                    sub=take(1)[0]
                    if sub==0:break
                    name=read_string()
                    if sub==8:
                        v=read_string()
                        if name=='Name':block_name=v
                    elif sub==10:
                        while True:
                            tt=take(1)[0]
                            if tt==0:break
                            assert tt==8
                            read_string();read_string()
                    else:raise AssertionError(f'Unexpected palette element tag {sub} {name}')
                assert block_name is not None and ':' in block_name
        elif key=='blocks' and t==9:
            assert take(1)==b'\x0a'
            n=read_int();blocks_count=n
            # Most blocks are fixed-length compounds. Dungeon chests add a
            # block-entity NBT compound with a regional LootTable identifier.
            first=offset
            expected=b'\x09\x00\x03pos'
            assert data[first:first+6]==expected, f'Invalid block payload in {file}'
            if 'dungeon' not in file.name:
                record_length=36
                assert first+n*record_length<=len(data)
                for ix in [0,n//2,n-1]:
                    pos=first+ix*record_length
                    assert data[pos:pos+6]==expected
                    coords=struct.unpack_from('>iii',data,pos+11)
                    assert sizes and all(0<=v<b for v,b in zip(coords,sizes)),(file,coords,sizes)
                    state=struct.unpack_from('>i',data,pos+31)[0]
                    assert 0<=state<palette_count,(file,state,palette_count)
                    assert data[pos+35]==0
                offset+=n*record_length
            else:
                chest_tags=0
                trial_tags=0
                for ix in range(n):
                    pos=offset
                    assert data[pos:pos+6]==expected,(file,ix)
                    coords=struct.unpack_from('>iii',data,pos+11)
                    assert all(0<=v<b for v,b in zip(coords,sizes)),(file,coords,sizes)
                    state=struct.unpack_from('>i',data,pos+31)[0]
                    assert 0<=state<palette_count,(file,state,palette_count)
                    offset+=35
                    if data[offset]==10:  # The optional block-entity compound.
                        offset+=1
                        assert read_string()=='nbt'
                        fields={}
                        while True:
                            field_type=take(1)[0]
                            if field_type==0:break
                            field_name=read_string()
                            if field_type==8:fields[field_name]=read_string()
                            elif field_type==3:fields[field_name]=read_int()
                            else:raise AssertionError(('unknown block entity NBT',field_type,field_name))
                        if 'LootTable' in fields:
                            assert fields['LootTable'].startswith('pipican:chests/')
                            chest_tags+=1
                        if 'normal_config' in fields:
                            assert fields['normal_config'].startswith('pipican:')
                            assert fields.get('target_cooldown_length',0)>0
                            trial_tags+=1
                    assert take(1)==b'\x00'
                assert chest_tags==3,(file,chest_tags)
                assert trial_tags==1,(file,trial_tags)
        elif key=='entities' and t==9:
            assert take(1)==b'\x0a';assert read_int()==0
            has_entities=True
        else:raise AssertionError((file,key,t))
    assert sizes and all(1<=v<=128 for v in sizes), (file,sizes)
    assert palette_count and blocks_count and has_entities
    assert offset==len(data),f'Trailing NBT bytes: {file}'
    return sizes,blocks_count

def main():
    all_nbt=list((BASE/'structure').rglob('*.nbt'))
    catalog=json.loads((OUT/'STRUCTURE_CATALOG.json').read_text())
    assert len(POOLS)==331 and len(SITES)==70 and len(REGIONS)==22
    assert len(catalog)==len(all_nbt), (len(catalog),len(all_nbt))
    expected={r['pool'] for r in POOLS}
    have={p.relative_to(BASE/'worldgen/template_pool').as_posix()[:-5] for p in (BASE/'worldgen/template_pool').rglob('*.json')}
    assert expected<=have, list(expected-have)[:10]
    total=0
    for entry in catalog:
        path=BASE/'structure'/(entry['id']+'.nbt')
        assert path.is_file(),path
        sz,n=inspect_nbt(path)
        assert sz==tuple(entry['size']) and n==entry['blocks']
        total+=n
    for pool in (BASE/'worldgen/template_pool').rglob('*.json'):
        item=json.loads(pool.read_text())
        for child in item['elements']:
            loc=child['element']['location']
            ns,ident=loc.split(':',1)
            assert ns=='pipican' and (BASE/'structure'/(ident+'.nbt')).exists(),loc
    capitals=[x for x in SITES if x['tier']=='capital']
    for s in capitals:
        ass=json.loads((BASE/'worldgen/capital_assemblies'/(s['id']+'.json')).read_text())
        assert len(ass['relative_positions'])==25
        assert len({(x['x'],x['z']) for x in ass['relative_positions']})==25
        for row in ass['relative_positions']:
            loc=row['template'].split(':',1)[1]
            assert (BASE/'structure'/(loc+'.nbt')).is_file(),loc
        fn=(BASE/'function/build_capital'/(s['id']+'.mcfunction')).read_text().splitlines()
        assert len(fn)==25+len(ass.get('connectors',[]))
    print(f'PASS {len(all_nbt)} NBT structure templates, {len(expected)} authored pool IDs, '
          f'{len(capitals)} complete 5x5 capital assemblies, {total:,} block records.')
    print('NOTE: static format verification only; Minecraft runtime and detailed collision/path QA remain outstanding.')

if __name__=='__main__':main()
