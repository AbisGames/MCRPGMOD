"""Phase 6: generates the Tamashigane soul gear (Soul Blade and soul armor), its tags, names and pipican/soul_paths.json
from soul_paths.py. Called by build_ores.py; the items themselves are registered in Java (com.pipican.soul)."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import soul_paths  # noqa: E402

NS = 'pipican'
BLADE_DEFAULT = 0xC8CCD4     # bare steel until stage 3
ROBE_DEFAULT = 0x1C1C22      # the plain black kimono
ARMOR = {'helmet': ('Soul Hood', 'Ruh Başlığı', 'minecraft:head_armor'),
         'chestplate': ('Soul Kimono', 'Ruh Kimonosu', 'minecraft:chest_armor'),
         'leggings': ('Soul Hakama', 'Ruh Hakaması', 'minecraft:leg_armor'),
         'boots': ('Soul Sandals', 'Ruh Sandaletleri', 'minecraft:foot_armor')}


def _signed(rgb):
    c = 0xFF000000 | rgb
    return c - (1 << 32)


def build(out, tag, lang_en, lang_tr):
    # --- items: models tinted with the stack's dye colour (set to the path's colour from stage 3)
    out.json(f'assets/{NS}/models/item/soul_blade.json', {'parent': 'minecraft:item/handheld', 'textures': {
        'layer0': f'{NS}:item/soul/hilt', 'layer1': f'{NS}:item/soul/blade'}})
    out.json(f'assets/{NS}/items/soul_blade.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/soul_blade', 'tints': [
        {'type': 'minecraft:constant', 'value': -1}, {'type': 'minecraft:dye', 'default': _signed(BLADE_DEFAULT)}]}})
    lang_en[f'item.{NS}.soul_blade'] = 'Soul Blade'
    lang_tr[f'item.{NS}.soul_blade'] = 'Ruh Kılıcı'
    tag('item', 'minecraft:swords', f'{NS}:soul_blade')
    tag('item', 'minecraft:sharp_weapon_enchantable', f'{NS}:soul_blade')
    for kind, (en, tr, vanilla) in ARMOR.items():
        item = f'soul_{kind}'
        out.json(f'assets/{NS}/models/item/{item}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/gear/{kind}'}})
        out.json(f'assets/{NS}/items/{item}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{item}', 'tints': [
            {'type': 'minecraft:dye', 'default': _signed(ROBE_DEFAULT)}]}})
        lang_en[f'item.{NS}.{item}'] = en
        lang_tr[f'item.{NS}.{item}'] = tr
        tag('item', vanilla, f'{NS}:{item}')
        tag('item', 'minecraft:dyeable', f'{NS}:{item}')
        tag('item', f'{NS}:soul/armor', f'{NS}:{item}')
    for item in ['soul_blade'] + [f'soul_{k}' for k in ARMOR]:
        tag('item', f'{NS}:traits/soulbound', f'{NS}:{item}')
        tag('item', f'{NS}:soul/gear', f'{NS}:{item}')
    layer = {'texture': f'{NS}:soul_robe', 'dyeable': {'color_when_undyed': ROBE_DEFAULT}}
    out.json(f'assets/{NS}/equipment/soul_robe.json', {'layers': {'humanoid': [layer], 'humanoid_leggings': [layer]}})

    # --- the one vein: no Silk Touch, so the ore block itself never leaves the spring
    for block in ('tamashigane_ore', 'deepslate_tamashigane_ore'):
        out.json(f'data/{NS}/loot_table/blocks/{block}.json', {'type': 'minecraft:block', 'random_sequence': f'{NS}:blocks/{block}', 'pools': [{
            'rolls': 1.0, 'bonus_rolls': 0.0, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:raw_tamashigane'}]}]})

    # --- which deaths carry a soul, and how big
    for e in ('minecraft:iron_golem', 'minecraft:snow_golem', 'minecraft:copper_golem', 'minecraft:slime', 'minecraft:magma_cube',
              'minecraft:shulker', 'minecraft:armor_stand', 'minecraft:creaking'):
        tag('entity_type', f'{NS}:soul/soulless', e)
    for e in ('minecraft:elder_guardian', 'minecraft:ravager', 'minecraft:evoker', 'minecraft:piglin_brute', 'minecraft:breeze'):
        tag('entity_type', f'{NS}:soul/mini_bosses', e)
    for e in ('minecraft:ender_dragon', 'minecraft:wither', 'minecraft:warden'):
        tag('entity_type', f'{NS}:soul/world_bosses', e)

    # --- names and descriptions
    for n, (souls, en, tr, does_en, does_tr) in enumerate(soul_paths.STAGES, start=1):
        lang_en[f'{NS}.soul.stage.{n}'] = en
        lang_tr[f'{NS}.soul.stage.{n}'] = tr
        lang_en[f'{NS}.soul.stage.{n}.desc'] = does_en
        lang_tr[f'{NS}.soul.stage.{n}.desc'] = does_tr
    for pid, (en, tr, color, text) in soul_paths.PATHS.items():
        lang_en[f'{NS}.soul.path.{pid}'] = en
        lang_tr[f'{NS}.soul.path.{pid}'] = tr
        for key, (t_en, t_tr) in text.items():
            lang_en[f'{NS}.soul.path.{pid}.{key}'] = t_en
            lang_tr[f'{NS}.soul.path.{pid}.{key}'] = t_tr
    lang_en[f'{NS}.ore.tamashigane_ingot.note'] = 'Use it to forge your Soul Blade and armor and choose your path - once, for life.'
    lang_tr[f'{NS}.ore.tamashigane_ingot.note'] = 'Kullanarak Ruh Kılıcını ve zırhını döv, yolunu seç - bir kez, ömür boyu.'
    for key, (en, tr) in MESSAGES.items():
        lang_en[f'{NS}.soul.{key}'] = en
        lang_tr[f'{NS}.soul.{key}'] = tr

    out.json(f'{NS}/soul_paths.json', {
        'stages': [s[0] for s in soul_paths.STAGES],
        'souls': soul_paths.SOULS,
        'paths': [{'id': pid, 'color': color} for pid, (en, tr, color, text) in soul_paths.PATHS.items()]})


MESSAGES = {
    'choose': ('Choose the path of your soul - this can never be changed:', 'Ruhunun yolunu seç - bu asla değiştirilemez:'),
    'choose_hover': ('Forge your blade on this path', 'Kılıcını bu yolda döv'),
    'forged': ('Your Soul Blade and armor are forged on the path of %s. They are bound to you alone.',
               'Ruh Kılıcın ve zırhın %s yolunda dövüldü. Yalnızca sana bağlılar.'),
    'already_forged': ('Your soul is already bound to a blade.', 'Ruhun zaten bir kılıca bağlı.'),
    'need_ingot': ('Hold a Tamashigane ingot to forge.', 'Dövmek için elinde bir Tamashigane külçesi tut.'),
    'unknown_path': ('No such path.', 'Böyle bir yol yok.'),
    'mined_once': ('The vein refuses you: a soul may take Tamashigane only once.', 'Damar seni reddediyor: bir ruh Tamashigane\'yi yalnızca bir kez alabilir.'),
    'mined': ('You took your one piece of Tamashigane. The vein closes behind you.', 'Tek Tamashigane parçanı aldın. Damar arkandan kapanıyor.'),
    'spring': ('The River of Souls murmurs nearby...', 'Ruhlar Nehri yakınlarda mırıldanıyor...'),
    'not_owner': ('This blade is bound to another soul.', 'Bu kılıç başka bir ruha bağlı.'),
    'souls': ('+%s souls (%s)', '+%s ruh (%s)'),
    'stage_up': ('Your blade reaches stage %s: %s', 'Kılıcın %s. aşamaya ulaştı: %s'),
    'trial_ready': ('Your blade\'s spirit waits for you. Sneak and use the blade to face it.', 'Kılıcının ruhu seni bekliyor. Yüzleşmek için eğilip kılıcı kullan.'),
    'trial_start': ('You enter your inner world: defeat the spirit of your blade!', 'İç dünyana giriyorsun: kılıcının ruhunu yen!'),
    'trial_won': ('You have overcome your blade\'s spirit.', 'Kılıcının ruhunu yendin.'),
    'trial_failed': ('The spirit fades - the trial is lost. Try again when you are ready.', 'Ruh siliniyor - sınav kaybedildi. Hazır olduğunda tekrar dene.'),
    'spirit': ('Spirit of %s', '%s Ruhu'),
    'whisper': ('Your blade whispers its name: %s. Rename it on an anvil to name it yourself.',
                'Kılıcın adını fısıldıyor: %s. Kendin adlandırmak için örste yeniden adlandır.'),
    'no_name_yet': ('The blade has no name to take yet (stage 4).', 'Kılıç henüz bir isim alamaz (4. aşama).'),
    'bankai': ('Bankai!', 'Bankai!'),
    'bankai_end': ('Your Bankai fades.', 'Bankai\'n sönüyor.'),
    'bankai_wait': ('Bankai recovers in %s s.', 'Bankai %s sn sonra hazır.'),
    'cooldown': ('Recovering: %s s', 'Toplanıyor: %s sn'),
    'locked': ('Your blade has nothing to show yet (stage 3).', 'Kılıcının henüz gösterecek bir şeyi yok (3. aşama).'),
    'ultimate': ('Absolute Form!', 'Mutlak Form!'),
    'ultimate_used': ('The Absolute Form is spent until tomorrow.', 'Mutlak Form yarına kadar tükendi.'),
    'saved': ('Your soul refuses to die!', 'Ruhun ölmeyi reddediyor!'),
    'info': ('%s - stage %s (%s), %s souls', '%s - %s. aşama (%s), %s ruh'),
    'info_next': ('Next stage at %s souls.', 'Sonraki aşama %s ruhta.'),
    'info_none': ('Your soul is bound to no blade.', 'Ruhun hiçbir kılıca bağlı değil.'),
    'tooltip_stage': ('Stage %s - %s', '%s. aşama - %s'),
    'tooltip_souls': ('%s souls', '%s ruh'),
    'tooltip_path': ('Path: %s', 'Yol: %s'),
    'tooltip_bound': ('Bound to %s', '%s kişisine bağlı'),
}
