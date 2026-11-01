"""Tamashigane (phase 6): soul steel. Stages and the 15 soul paths (data; the mechanics are in com.pipican.soul).

One piece of Tamashigane per player, ever. Using the ingot forges it into a Soul Blade and a set of soul armor, and the
player picks a path (for good). The blade absorbs the souls of creatures that die within 16 blocks and grows through
10 stages. Stage 5 (Shikai) and stage 7 (Bankai) are earned in a fight with the blade's own spirit.

Controls (blade in hand): use = the path's ability (stage 3; the Shikai form from stage 5; the Bankai form while Bankai
is released); sneak + use = start a waiting trial, else release Bankai (stage 7+), or during Bankai the Absolute Form
ultimate (stage 10, once a day).
"""

# stage -> (souls needed, English, Turkish, what the stage gives - English, Turkish)
STAGES = [
    (0, 'Empty Blade', 'Boş Kılıç', 'A plain, nameless blade and a plain black kimono.', 'İsimsiz, sade bir kılıç ve sade siyah bir kimono.'),
    (100, 'Awakening', 'Uyanış', 'More damage and protection; sneak with the blade to sense souls through walls.',
     'Hasar ve koruma artar; kılıçla eğilince duvar arkasındaki ruhları görürsün.'),
    (300, 'Whisper', 'Fısıltı', 'The path\'s first ability; the armor takes the path\'s colours.', 'Yolun ilk yeteneği açılır; zırh yolun renklerini alır.'),
    (700, 'Name', 'İsim', 'The blade whispers its name - rename it on an anvil to give it your own.',
     'Kılıç adını fısıldar - örste ona kendi verdiğin adı koyabilirsin.'),
    (1500, 'Shikai', 'Shikai', 'Trial: defeat the blade\'s spirit. The blade takes its first form and its main ability.',
     'Sınav: kılıcın ruhunu yen. Kılıç ilk formuna geçer ve ana yeteneği açılır.'),
    (3000, 'Mastery', 'Ustalaşma', 'Shikai hits harder and reaches farther; the armor\'s passive bonus.',
     'Shikai daha sert vurur, menzili artar; zırhın pasif bonusu açılır.'),
    (6000, 'Bankai', 'Bankai', 'Second trial. Bankai: 60 seconds, 10 minutes to recover; the armor takes its full form.',
     'İkinci sınav. Bankai: 60 saniye, 10 dakika bekleme; zırh tam formuna geçer.'),
    (12000, 'Bankai Mastery', 'Bankai Ustalığı', 'Bankai lasts 90 seconds and recovers in 5 minutes.',
     'Bankai 90 saniye sürer, 5 dakikada yenilenir.'),
    (25000, 'Soul Union', 'Ruh Birliği', 'Blade and wielder are one soul: +25% to every ability; the path\'s aura is always on.',
     'Kılıç ve sahibi tek ruh olur: tüm yeteneklere +%25; yolun aurası hep açık.'),
    (50000, 'Absolute Form', 'Mutlak Form', 'The path\'s ultimate, once a day (sneak + use during Bankai).',
     'Yolun nihai yeteneği, günde bir kez (Bankai sırasında eğilip kullan).'),
]

# Souls a death is worth (100% when you killed it, 50% when it died near you).
SOULS = {'common': 1, 'strong': 3, 'elite': 10, 'mini_boss': 50, 'player': 25, 'region_boss': 250, 'world_boss': 1000}

# id -> English, Turkish, colour, then per stage (3 / 5 Shikai / 7 Bankai / 10 Absolute Form), armor passive (stage 6)
# and aura (stage 9), each as (English, Turkish).
PATHS = {
    'black_crescent': ('Black Crescent', 'Siyah Hilal', 0x1A1A1A, {
        's3': ('Swing a crescent of energy that cuts everything in a line.', 'Bir çizgideki her şeyi kesen enerji hilali fırlatır.'),
        's5': ('The blade becomes a huge guardless cleaver: a wider, stronger crescent.', 'Kılıç dev, kabzasız bir satıra döner: daha geniş, güçlü hilal.'),
        's7': ('A slim black blade and blinding speed; black crescents in quick succession.', 'İnce siyah kılıç ve aşırı hız; peş peşe siyah hilaller.'),
        's10': ('The inner beast wakes: a bone mask, crimson crescents and great strength for 30 seconds.',
                'İçteki canavar uyanır: kemik maske, kızıl hilaller ve 30 saniye büyük güç.'),
        'armor': ('A long black coat: swiftness.', 'Uzun siyah kaban: hız.'),
        'aura': ('Black soul flames; you strike faster.', 'Siyah ruh alevleri; daha hızlı vurursun.')}),
    'ash_sovereign': ('Ash Sovereign', 'Kül Hükümdarı', 0xE0461C, {
        's3': ('The blade catches fire: your hits burn for 15 seconds.', 'Kılıç alev alır: 15 saniye vuruşların yakar.'),
        's5': ('A wave of flame that burns everything it touches.', 'Dokunduğu her şeyi yakan alev dalgası.'),
        's7': ('The flames draw into the blade: searing heat around you, and the foes you kill rise as a skeleton army.',
               'Alevler kılıca çekilir: etrafını kavuran ısı; öldürdüğün düşmanlar iskelet ordusu olarak kalkar.'),
        's10': ('The sun blade: one strike that turns everything before you to ash.', 'Güneş kılıcı: önündeki her şeyi kül eden tek vuruş.'),
        'armor': ('An ash-grey robe with burnt edges: fire can\'t hurt you.', 'Kenarları yanık kül grisi cüppe: ateş sana işlemez.'),
        'aura': ('Whoever strikes you catches fire.', 'Sana vuran alev alır.')}),
    'perfect_lie': ('Perfect Lie', 'Kusursuz Yalan', 0xE8E2F0, {
        's3': ('A false clone: you vanish for a moment and creatures lose sight of you.', 'Sahte klon: bir anlığına kaybolursun, yaratıklar seni kaybeder.'),
        's5': ('Those who see the blade open fall into illusion: lost, they turn on each other.', 'Kılıcın açılışını görenler yanılsamaya düşer: şaşırır, birbirine saldırır.'),
        's7': ('Black coffin: traps the creature you look at and crushes it.', 'Kara tabut: baktığın yaratığı hapseder ve ezer.'),
        's10': ('Butterfly evolution: wings, constant regeneration, and you come back once from a deadly blow.',
                'Kelebek evrimi: kanatlar, sürekli yenilenme ve ölümcül darbeden bir kez dönüş.'),
        'armor': ('A white commander\'s robe: 15% of attacks hit only an illusion.', 'Beyaz komutan cüppesi: saldırıların %15\'i yalnızca bir yanılsamaya isabet eder.'),
        'aura': ('Constant slow regeneration.', 'Sürekli yavaş yenilenme.')}),
    'cherry_storm': ('Cherry Storm', 'Kiraz Fırtınası', 0xF4A7C0, {
        's3': ('A thin cloud of petals from the blade\'s tip.', 'Kılıç ucundan ince bir yaprak bulutu.'),
        's5': ('The blade scatters into a thousand petal-blades, sent where you look.', 'Kılıç binlerce yaprak-bıçağa dağılır, baktığın yere yönelir.'),
        's7': ('Pillars of a thousand blades all around: everything near you is cut, every second.',
               'Etrafta bin kılıçlık sütunlar: yakınındaki her şey her saniye kesilir.'),
        's10': ('All petals gather into one white blade; wings of light on your back.', 'Bütün yapraklar tek beyaz kılıçta toplanır; sırtında ışık kanatları.'),
        'armor': ('A noble white scarf: you shrug off knockback.', 'Soylu beyaz atkı: geri itilmezsin.'),
        'aura': ('Petals shield you: absorption.', 'Yapraklar seni korur: emilim.')}),
    'snow_dragon': ('Snow Dragon', 'Kar Ejderi', 0x9AD8F4, {
        's3': ('An ice chain catches the creature you look at and drags it to you.', 'Buz zinciri baktığın yaratığı yakalar ve sana çeker.'),
        's5': ('A chained crescent blade: loose an ice dragon that freezes what it hits.', 'Zincirli hilal kılıç: vurduğunu donduran bir buz ejderi fırlatır.'),
        's7': ('Ice wings and tail; twelve petals count your time down.', 'Buz kanatları ve kuyruk; 12 çiçek yaprağı süreni sayar.'),
        's10': ('Adult form: no time limit for 2 minutes, and everything you touch freezes at once.',
                'Yetişkin form: 2 dakika süre sınırı yok, dokunduğun her şey anında donar.'),
        'armor': ('Winged ice-crystal armor: cold can\'t touch you and attackers are chilled.', 'Buz kristalli kanatlı zırh: soğuk sana işlemez, saldıranlar üşür.'),
        'aura': ('Frost around you slows your foes.', 'Etrafındaki ayaz düşmanlarını yavaşlatır.')}),
    'unchained_beast': ('Unchained Beast', 'Zincirsiz Canavar', 0x8B0000, {
        's3': ('For 15 seconds the more you are hurt, the harder you hit.', '15 saniye boyunca yaralandıkça daha sert vurursun.'),
        's5': ('The blade turns into a giant cleaver: one swing that mows down heavy creatures.', 'Kılıç devasa satıra döner: ağır yaratıkları biçen tek savuruş.'),
        's7': ('Beast form, a crimson aura: the lower your health, the more your damage multiplies.',
               'Canavar formu, kızıl aura: canın azaldıkça hasarın katlanır.'),
        's10': ('Every limit gone for 30 seconds: no protection at all, enormous damage.', '30 saniye tüm sınırlar kalkar: hiç koruma yok, devasa hasar.'),
        'armor': ('A torn haori with bells: strength while below half health.', 'Çanlı yırtık haori: canın yarının altındayken güç.'),
        'aura': ('Your presence weakens the creatures around you.', 'Varlığın etrafındaki yaratıkları zayıflatır.')}),
    'playful_shadow': ('Playful Shadow', 'Oyunbaz Gölge', 0x5B3A8C, {
        's3': ('Twin blades: for 10 seconds every hit strikes twice.', 'İkiz kılıç: 10 saniye her vuruş iki kez vurur.'),
        's5': ('Rules of the game: step through the shadows behind the creature you look at; your next hit deals half again.',
               'Oyunun kuralları: baktığın yaratığın arkasındaki gölgeye geçersin; sonraki vuruşun yarısı kadar fazla vurur.'),
        's7': ('A drama in four acts: the creature you look at is bound by threads, slowed, and shares every wound you take.',
               'Dört perdelik dram: baktığın yaratık iplerle bağlanır, yavaşlar ve aldığın her yarayı paylaşır.'),
        's10': ('Final act: every bound foe is finished in a single blow.', 'Son perde: bağlanan her rakip tek darbede biter.'),
        'armor': ('A straw hat over a flowered kimono: hidden in the dark while sneaking.', 'Çiçekli kimono üstünde hasır şapka: karanlıkta eğilirken gizlenirsin.'),
        'aura': ('Shadows slow the foes around you.', 'Gölgeler etrafındaki düşmanları yavaşlatır.')}),
    'crimson_healer': ('Crimson Healer', 'Kızıl Şifacı', 0xB01E3C, {
        's3': ('For 15 seconds your hits steal life; allies near you heal.', '15 saniye vuruşların can çalar; yakındaki müttefikler iyileşir.'),
        's5': ('A flying healing beast: it swallows your allies and heals them.', 'Uçan şifa canavarı: müttefiklerini yutup iyileştirir.'),
        's7': ('Blood flows from the blade: your hits rot the foe and mend you.', 'Kılıçtan kan akar: vuruşların düşmanı çürütür, seni onarır.'),
        's10': ('The first beast: for a minute every hit both cuts and heals you in full.', 'İlk canavar: bir dakika her vuruş hem keser hem seni tamamen iyileştirir.'),
        'armor': ('A white healer\'s robe: regeneration.', 'Beyaz şifacı cüppesi: yenilenme.'),
        'aura': ('Allies around you regenerate.', 'Etrafındaki müttefikler yenilenir.')}),
    'shadow_inventor': ('Shadow Inventor', 'Gölge Mucit', 0x2E7D32, {
        's3': ('A crimson shield: extra hearts for 20 seconds.', 'Kızıl kalkan: 20 saniye ek can.'),
        's5': ('A crimson blast where you look, and a web of shields over your allies.', 'Baktığın yere kızıl enerji darbesi ve müttefiklerine kalkan ağı.'),
        's7': ('Reshape: mend yourself and your allies, and break the gear of the creature you look at.',
               'Yeniden biçimlendir: kendini ve müttefiklerini onar, baktığın yaratığın ekipmanını boz.'),
        's10': ('Restructure everything around you: every foe is pinned in traps.', 'Etrafındaki her şeyi yeniden kur: her düşman tuzaklara saplanır.'),
        'armor': ('A green-black coat and striped hat: projectiles hurt you a third less.', 'Yeşil-siyah hırka ve çizgili şapka: mermiler üçte bir daha az acıtır.'),
        'aura': ('Your shield keeps renewing itself.', 'Kalkanın kendini hep yeniler.')}),
    'scorpion_sting': ('Scorpion Sting', 'Akrep İğnesi', 0xD4AF37, {
        's3': ('Speed and stealth for 8 seconds.', '8 saniye hız ve gizlilik.'),
        's5': ('The needle blade: hit the same creature twice within 10 seconds and it dies (bosses take a fifth of their health). Use to dash.',
               'İğne kılıç: aynı yaratığa 10 saniye içinde iki kez vur, ölür (boss\'lar canının beşte birini kaybeder). Kullanınca atılırsın.'),
        's7': ('A giant missile on your arm: a long-range blast where you look.', 'Koluna takılan dev füze: baktığın yere uzun menzilli patlama.'),
        's10': ('Invisible for 30 seconds, and every hit leaves a death mark.', '30 saniye görünmezlik ve her vuruş ölüm damgası bırakır.'),
        'armor': ('Light open-backed assassin armor: swiftness and soft landings.', 'Sırtı açık hafif suikastçı zırhı: hız ve yumuşak iniş.'),
        'aura': ('Invisible while sneaking.', 'Eğilirken görünmezsin.')}),
    'decay_experiment': ('Decay Experiment', 'Çürüme Deneyi', 0x7CB342, {
        's3': ('Paralysing poison: for 15 seconds your hits poison and slow.', 'Felç zehri: 15 saniye vuruşların zehirler ve yavaşlatır.'),
        's5': ('The creature you look at is paralysed where it stands.', 'Baktığın yaratık olduğu yerde felç olur.'),
        's7': ('A giant grub creature and a cloud of poison gas where you look.', 'Dev bir tırtıl yaratık ve baktığın yerde zehirli gaz bulutu.'),
        's10': ('The poison adapts: for a minute it withers what it touches, and you liquefy to escape death once.',
                'Zehir uyum sağlar: bir dakika dokunduğunu soldurur ve ölümden bir kez sıvılaşarak kaçarsın.'),
        'armor': ('A gold-black mask: poison can\'t touch you.', 'Altın-siyah maske: zehir sana işlemez.'),
        'aura': ('Whoever strikes you is poisoned.', 'Sana vuran zehirlenir.')}),
    'moonlight_dance': ('Moonlight Dance', 'Ay Işığı Dansı', 0xF0F8FF, {
        's3': ('A freezing circle on the ground around you.', 'Etrafında yeri donduran bir daire.'),
        's5': ('A white blade. First dance: a frozen circle where you look; second dance (use again soon): a wave of ice.',
               'Beyaz kılıç. İlk dans: baktığın yerde donmuş daire; ikinci dans (hemen tekrar kullan): buz dalgası.'),
        's7': ('Absolute zero: your body freezes and everything you strike crystallises - but the cold slows you too.',
               'Mutlak sıfır: bedenin donar, vurduğun her şey kristalleşir - ama soğuk seni de yavaşlatır.'),
        's10': ('Dance of death: everything moving around you is shattered.', 'Ölüm dansı: etrafında hareket eden her şey parçalanır.'),
        'armor': ('A white ice kimono: cold can\'t touch you.', 'Beyaz buz kimonosu: soğuk sana işlemez.'),
        'aura': ('Snow falls around you and slows your foes.', 'Etrafına kar yağar, düşmanların yavaşlar.')}),
    'bone_serpent': ('Bone Serpent', 'Kemik Yılan', 0xC62828, {
        's3': ('The blade grows longer for 10 seconds: you reach farther.', 'Kılıç 10 saniye uzar: daha uzağa erişirsin.'),
        's5': ('A segmented whip-blade that lashes everything in a line.', 'Bir çizgideki her şeyi kamçılayan bölümlü kamçı-kılıç.'),
        's7': ('A giant serpent skeleton: an energy beam from its jaws.', 'Dev yılan iskeleti: ağzından enerji ışını.'),
        's10': ('The bone serpent wraps your arms: for a minute your blows explode with fire.', 'Kemik yılan kollarına sarılır: bir dakika darbelerin ateşle patlar.'),
        'armor': ('Tattooed arms and bone pauldrons: you shrug off knockback.', 'Dövmeli kollar ve kemik omuzluklar: geri itilmezsin.'),
        'aura': ('You always reach a little farther.', 'Hep biraz daha uzağa erişirsin.')}),
    'lethal_reach': ('Lethal Reach', 'Ölümcül Uzanış', 0xB0BEC5, {
        's3': ('The blade shoots out for an instant and pierces what is ahead.', 'Kılıç bir anlığına uzayıp öndekini deler.'),
        's5': ('The blade stretches to a great length, piercing everything in its path.', 'Kılıç çok uzun menzile uzar, yolundaki her şeyi deler.'),
        's7': ('The blade stretches and returns at the speed of sound, leaving a poison shard inside whatever it pierces.',
               'Kılıç ses hızıyla uzayıp kısalır, deldiği her şeyin içinde zehir parçası bırakır.'),
        's10': ('Every shard you left bursts from within, melting its host.', 'Bıraktığın her parça içeriden patlar, taşıyıcısını eritir.'),
        'armor': ('A light white coat: swiftness.', 'Hafif beyaz kaban: hız.'),
        'aura': ('Your work goes faster.', 'İşin daha hızlı ilerler.')}),
    'wolf_guardian': ('Wolf Guardian', 'Kurt Muhafız', 0x6D4C41, {
        's3': ('A giant ghostly arm smashes down where you look.', 'Dev hayalet bir kol baktığın yere iner.'),
        's5': ('Summon a giant armored warrior that fights beside you.', 'Yanında savaşan dev zırhlı bir savaşçı çağırır.'),
        's7': ('The giant in full form - but the wounds it takes are partly yours.', 'Devin tam formu - ama aldığı yaraların bir kısmı senindir.'),
        's10': ('Immortal for 20 seconds, then long weakness.', '20 saniye ölümsüzlük, sonra uzun bir zayıflık.'),
        'armor': ('A wolf-headed helm and heavy plate: resistance.', 'Kurt başlı miğfer ve ağır plaka: direnç.'),
        'aura': ('You stand like a wall: knockback resistance and resistance.', 'Duvar gibi durursun: geri itilmeme ve direnç.')}),
}
