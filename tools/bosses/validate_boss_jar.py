"""Check the actual mod JAR; standard library only. Normalize Git CRLF conversion."""
import hashlib,json,sys,zipfile
from pathlib import Path
from collections import Counter

def validate(path):
    expected=json.loads(Path(__file__).with_name('original_geometry_sha256.json').read_text())
    assert len(expected)==27
    with zipfile.ZipFile(path) as jar:
        names=set(jar.namelist())
        assert not any(n.startswith(('com/pipican/gametest/','data/pipican_test/')) for n in names),'Test content shipped'
        for name,digest in expected.items():
            assert hashlib.sha256(jar.read('assets/pipican/geo/bosses/'+name).replace(b'\r\n',b'\n')).hexdigest()==digest,name
        for name in ['RegionalBossEntity','MorvathCombat','FrostboundEntity','FrostboundCombat','SkollvarEntity','SkollvarCombat','EldarothEntity','EldarothCombat','FallenKingEntity','FallenKingCombat','KragmorEntity','KragmorCombat','NoctharEntity','NoctharCombat','AetherionEntity','AetherionCombat','RitualAltarBlock','RitualSigils','BossContent']:
            assert 'com/pipican/boss/'+name+'.class' in names,name
        for phase in ['armored','enraged']:
            for suffix in ['', '_glow']:
                data=jar.read(f'assets/pipican/textures/entity/boss/morvath_{phase}{suffix}.png')
                assert data[:8]==bytes([137,80,78,71,13,10,26,10]) and int.from_bytes(data[16:20],'big')==256
        for item in ['ritual_core','morvath_sigil','ashen_kings_heart','infernium','ritual_altar']:
            definition=json.loads(jar.read(f'assets/pipican/items/{item}.json'))
            namespace,model=definition['model']['model'].split(':')
            assert f'assets/{namespace}/models/{model}.json' in names,model
        for item in ['frostbound_colossus_sigil','frostbound_colossus_trophy','skollvar_winterfang_sigil','skollvar_winterfang_trophy','eldaroth_treant_sigil','eldaroth_treant_trophy','fallen_king_sigil','fallen_king_trophy','kragmor_canyon_warlord_sigil','kragmor_canyon_warlord_trophy','nocthar_void_sovereign_sigil','nocthar_void_sovereign_trophy','aetherion_sky_guardian_sigil','aetherion_sky_guardian_trophy']:
            assert f'assets/pipican/models/item/{item}.json' in names
            assert f'assets/pipican/textures/item/{item}.png' in names
        for name in ['ritual_altar','ritual_core','morvath_sigil']:
            recipe=json.loads(jar.read(f'data/pipican/recipe/{name}.json'))
            assert recipe['result']=={'id':'pipican:'+name,'count':1}
        assert json.loads(jar.read('data/pipican/recipe/frostbound_colossus_sigil.json'))['result']=={'id':'pipican:frostbound_colossus_sigil','count':1}
        counts=Counter(recipe['key'][symbol] for row in recipe['pattern'] for symbol in row)
        assert counts=={'pipican:cinderite':5,'pipican:ashlands_boss_core':2,'pipican:ritual_core':1,'minecraft:magma_cream':1},counts
        assert 'assets/pipican/items/ashlands_boss_core.json' in names
        for item in ['ritual_core','morvath_sigil','ashen_kings_heart','infernium']:
            assert f'assets/pipican/textures/item/{item}.png' in names
        assert 'assets/pipican/textures/block/ritual_altar.png' in names
        if 'com/pipican/boss/BossDesigns.class' in names:
            designs=json.loads(jar.read('assets/pipican/geo/bosses/combat_designs.json'))
            assert len(designs)==27 and sum(design['functional'] for design in designs)==8
            assert 'com/pipican/boss/FrostboundEntity.class' in names
            assert 'com/pipican/boss/SkollvarEntity.class' in names
            assert 'com/pipican/boss/EldarothEntity.class' in names
            assert 'com/pipican/boss/EldarothCombat.class' in names
            assert 'com/pipican/boss/FallenKingEntity.class' in names
            assert 'com/pipican/boss/FallenKingCombat.class' in names
            assert 'com/pipican/boss/KragmorEntity.class' in names
            assert 'com/pipican/boss/KragmorCombat.class' in names
            assert 'com/pipican/boss/NoctharEntity.class' in names
            assert 'com/pipican/boss/NoctharCombat.class' in names
            assert 'com/pipican/boss/AetherionEntity.class' in names
            assert 'com/pipican/boss/AetherionCombat.class' in names
            for phase in ['dormant','awakened']:
                data=jar.read(f'assets/pipican/textures/entity/boss/frostbound_{phase}.png')
                assert data[:8]==bytes([137,80,78,71,13,10,26,10]) and int.from_bytes(data[16:20],'big')==256
            for phase in ['winterfang','alpha']:
                data=jar.read(f'assets/pipican/textures/entity/boss/skollvar_{phase}.png')
                assert data[:8]==bytes([137,80,78,71,13,10,26,10]) and int.from_bytes(data[16:20],'big')==256
            assert 'data/pipican/recipe/skollvar_winterfang_sigil.json' in names
            assert 'data/pipican/recipe/eldaroth_treant_sigil.json' in names
            assert 'data/pipican/recipe/fallen_king_sigil.json' in names
            assert 'data/pipican/recipe/kragmor_canyon_warlord_sigil.json' in names
            assert 'data/pipican/recipe/nocthar_void_sovereign_sigil.json' in names
            assert 'data/pipican/recipe/aetherion_sky_guardian_sigil.json' in names
            for phase in ['heartwood','awakened']:
                data=jar.read(f'assets/pipican/textures/entity/boss/eldaroth_ancient_{phase}.png')
                assert data[:8]==bytes([137,80,78,71,13,10,26,10]) and int.from_bytes(data[16:20],'big')==256
            for phase in ['regalia','unbound']:
                data=jar.read(f'assets/pipican/textures/entity/boss/fallen_king_{phase}.png')
                assert data[:8]==bytes([137,80,78,71,13,10,26,10]) and int.from_bytes(data[16:20],'big')==256
            for phase in ['canyon_iron','awakened']:
                data=jar.read(f'assets/pipican/textures/entity/boss/kragmor_{phase}.png')
                assert data[:8]==bytes([137,80,78,71,13,10,26,10]) and int.from_bytes(data[16:20],'big')==256
            for phase in ['void_sovereign','eclipsed']:
                data=jar.read(f'assets/pipican/textures/entity/boss/nocthar_{phase}.png')
                assert data[:8]==bytes([137,80,78,71,13,10,26,10]) and int.from_bytes(data[16:20],'big')==256
            for phase in ['sky_guardian','radiant']:
                data=jar.read(f'assets/pipican/textures/entity/boss/aetherion_{phase}.png')
                assert data[:8]==bytes([137,80,78,71,13,10,26,10]) and int.from_bytes(data[16:20],'big')==256
        if 'com/pipican/blacksmith/BlacksmithUpgrades.class' in names:
            assert 'data/pipican/blacksmith_profiles.json' in names
        if 'com/pipican/resource/RareResources.class' in names:
            for resource in ['soulstone','dragonite','abyssium','runestone','astralite','sunforged','moonshard','primordial']:
                for path in [f'assets/pipican/textures/block/{resource}_ore.png',f'assets/pipican/items/{resource}.json',
                             f'data/pipican/worldgen/configured_feature/rare_{resource}.json',f'data/pipican/worldgen/placed_feature/rare_{resource}.json',
                             f'data/pipican/neoforge/biome_modifier/rare_{resource}.json',f'data/pipican/loot_table/blocks/{resource}_ore.json']:
                    assert path in names,path
            for material in ['umbrite','tideglass','runic_iron','skycrystal','sunglass','mooncrystal']:
                assert f'data/pipican/recipe/{material}_pickaxe.json' in names

    print('PASS: 27 original geometries, boss/altar classes, textures, models, exact sigil recipe; no GameTest content')
if __name__=='__main__':
    assert len(sys.argv)==2,'Expected one mod JAR'
    validate(sys.argv[1])
