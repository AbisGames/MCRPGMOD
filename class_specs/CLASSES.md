# Shadow of the Demon Lord classes - all 84 paths

Converted from *Shadow of the Demon Lord* (core rulebook, ch. 3-5) into the class template.
One file per class: 4 novice, 16 expert, 64 master.

## Power tiers (all selectable from the start)

| Tier | Classes | Stat points | Special points | Trinket slots | Free points (general/special) | Suggested gate (if ever) |
|---|---|---|---|---|---|---|
| Novice | 4 | 14 | 5 | 3 | 1 / 0 | none |
| Expert | 16 | 20 | 8 | 4 | 2 / 1 | any class lv 20 |
| Master | 64 | 26 | 10 | 5 | 3 / 1 | any class lv 35 |

Base health follows the book's Health-per-level: novice 18-24, expert 22-30, master 26-40
(master = 24 + 2 x book Health per level; Brute is the tankiest at 40).

## Decisions so far
- Reversed bars (fill from use, drain over time) are supported: Sorcerer Strain, Berserker Rage, Death Dealer Bloodshed, Zealot Madness.
- Berserk triggers automatically at 75% Rage.
- Fighters can buy as many fighting-style nodes as they like.
- Druid "no metal" and Thief "villager prices" weaknesses stay.
- Every class keeps its own resource-bar name.
- Gunslinger is written for the planned gun system (crossbows stand in until then).

## Book stat mapping
Strength -> Strength/Vitality, Agility -> Agility/Dexterity, Intellect -> Intelligence,
Will -> Wisdom, Health -> Vitality/Endurance, Perception/fortune -> Dexterity/Luck.
Book "boons" became % damage/crit/chance bonuses; book spells became passives now and
active-ability notes in section 12 of each file.

## General-tree layout (shared web)
Four quarters; ring 1 = novice, ring 2 = expert, ring 3 = master (each master sits just beyond its closest expert - see the file).
- Top - Power: Magician; Artificer, Sorcerer, Witch, Wizard
- Right - Faith: Priest; Cleric, Druid, Oracle, Paladin
- Bottom - Trickery: Rogue; Assassin, Scout, Thief, Warlock
- Left - War: Warrior; Berserker, Fighter, Ranger, Spellbinder

## Shared skills (defined once, referenced elsewhere)
- Swordsmanship - `warrior.md`
- Axemanship - `warrior.md`
- Archery - `warrior.md`
- Shield Wall - `warrior.md`
- Stealth - `rogue.md`
- Backstab - `rogue.md`
- Acrobatics - `rogue.md`
- Mace Training - `priest.md`
- Undead Bane - `priest.md`
- Mending - `priest.md`
- Enchanting - `magician.md`
- Alchemy - `magician.md`
- Crossbowmanship - `artificer.md`
- Smithing - `artificer.md`
- Herbalism - `witch.md`
- Spear Training - `druid.md`
- Beast Bond - `druid.md`
- Athletics - `scout.md`
- Survival - `ranger.md`
- Performance - `bard.md`
- Riding - `cavalier.md`
- Bartering - `diplomat.md`
- Mining - `geomancer.md`
- Gunplay - `gunslinger.md`
- Swimming - `hydromancer.md`

## All classes

| Tier | Class | File | Special stat | Resource | Health | Quarter |
|---|---|---|---|---|---|---|
| novice | Warrior | `warrior.md` | Grit | Stamina | 24 | left side of the web (the War quarter) |
| novice | Rogue | `rogue.md` | Cunning | Guile | 20 | bottom of the web (the Trickery quarter) |
| novice | Priest | `priest.md` | Faith | Devotion | 22 | right side of the web (the Faith quarter) |
| novice | Magician | `magician.md` | Power | Mana | 18 | top of the web (the Power quarter) |
| expert | Artificer | `artificer.md` | Ingenuity | Spare Parts | 22 | top of the web (Power quarter) |
| expert | Sorcerer | `sorcerer.md` | Potency | Strain | 22 | top of the web (Power quarter) |
| expert | Witch | `witch.md` | Craft | Witchery | 22 | top of the web (Power quarter) |
| expert | Wizard | `wizard.md` | Lore | Aether | 22 | top of the web (Power quarter) |
| expert | Cleric | `cleric.md` | Conviction | Grace | 26 | right side of the web (Faith quarter) |
| expert | Druid | `druid.md` | Wildness | Verdance | 26 | right side of the web (Faith quarter) |
| expert | Oracle | `oracle.md` | Communion | Ecstasy | 24 | right side of the web (Faith quarter) |
| expert | Paladin | `paladin.md` | Sanctity | Smite | 26 | right side of the web (Faith quarter) |
| expert | Assassin | `assassin.md` | Lethality | Shadow | 24 | bottom of the web (Trickery quarter) |
| expert | Scout | `scout.md` | Awareness | Stride | 24 | bottom of the web (Trickery quarter) |
| expert | Thief | `thief.md` | Finesse | Nerve | 24 | bottom of the web (Trickery quarter) |
| expert | Warlock | `warlock.md` | Larceny | Stolen Power | 22 | bottom of the web (Trickery quarter) |
| expert | Berserker | `berserker.md` | Fury | Rage | 30 | left side of the web (War quarter) |
| expert | Fighter | `fighter.md` | Discipline | Momentum | 28 | left side of the web (War quarter) |
| expert | Ranger | `ranger.md` | Instinct | Quarry | 28 | left side of the web (War quarter) |
| expert | Spellbinder | `spellbinder.md` | Binding | Eldritch Charge | 24 | left side of the web (War quarter) |
| master | Abjurer | `abjurer.md` | Warding | Ward | 28 | Power quarter |
| master | Acrobat | `acrobat.md` | Poise | Flow | 30 | Trickery quarter |
| master | Aeromancer | `aeromancer.md` | Gale | Breath of Wind | 28 | Power quarter |
| master | Apocalyptist | `apocalyptist.md` | Corruption | Doom | 28 | Power quarter |
| master | Arcanist | `arcanist.md` | Arcana | Mystic Flux | 28 | Power quarter |
| master | Astromancer | `astromancer.md` | Radiance | Starlight | 28 | Faith quarter |
| master | Avenger | `avenger.md` | Vengeance | Retribution | 34 | War quarter |
| master | Bard | `bard.md` | Resonance | Melody | 30 | Trickery quarter |
| master | Beastmaster | `beastmaster.md` | Primal Bond | Wild Spirit | 28 | Faith quarter |
| master | Blade | `blade.md` | Edge | Bloodletting | 32 | Trickery quarter |
| master | Brute | `brute.md` | Brawn | Might | 40 | War quarter |
| master | Cavalier | `cavalier.md` | Horsemanship | Charge | 34 | War quarter |
| master | Champion | `champion.md` | Resolve | Valor | 34 | War quarter |
| master | Chaplain | `chaplain.md` | Sermon | Chant | 32 | Faith quarter |
| master | Chronomancer | `chronomancer.md` | Tempo | Sands | 28 | Power quarter |
| master | Conjurer | `conjurer.md` | Summoning | Ether | 28 | Power quarter |
| master | Conqueror | `conqueror.md` | Command | Orders | 34 | War quarter |
| master | Death Dealer | `death_dealer.md` | Carnage | Bloodshed | 36 | War quarter |
| master | Defender | `defender.md` | Bulwark | Guard | 36 | War quarter |
| master | Dervish | `dervish.md` | Rhythm | Whirl | 34 | War quarter |
| master | Destroyer | `destroyer.md` | Ruin | Devastation | 30 | Power quarter |
| master | Diplomat | `diplomat.md` | Rapport | Persuasion | 30 | Trickery quarter |
| master | Diviner | `diviner.md` | Foresight | Omens | 26 | Power quarter |
| master | Dreadnaught | `dreadnaught.md` | Plate | Fortitude | 34 | War quarter |
| master | Duelist | `duelist.md` | Finesse | Panache | 32 | War quarter |
| master | Enchanter | `enchanter.md` | Glamour | Allure | 28 | Power quarter |
| master | Engineer | `engineer.md` | Engineering | Fuel | 30 | Power quarter |
| master | Executioner | `executioner.md` | Severance | Dread | 30 | Trickery quarter |
| master | Exorcist | `exorcist.md` | Purity | Sanctification | 32 | Faith quarter |
| master | Explorer | `explorer.md` | Wayfinding | Endurance | 30 | War quarter |
| master | Geomancer | `geomancer.md` | Bedrock | Earthen Defense | 32 | Power quarter |
| master | Gladiator | `gladiator.md` | Showmanship | Bloodlust | 34 | War quarter |
| master | Gunslinger | `gunslinger.md` | Trigger Finger | Grit | 30 | War quarter |
| master | Healer | `healer.md` | Vitality Flow | Lifeblood | 32 | Faith quarter |
| master | Hexer | `hexer.md` | Malice | Spite | 28 | Power quarter |
| master | Hydromancer | `hydromancer.md` | Tide | Current | 30 | Power quarter |
| master | Illusionist | `illusionist.md` | Phantasm | Mirage | 28 | Power quarter |
| master | Infiltrator | `infiltrator.md` | Subtlety | Cover | 30 | Trickery quarter |
| master | Inquisitor | `inquisitor.md` | Scrutiny | Judgment | 30 | Faith quarter |
| master | Jack-of-All-Trades | `jack_of_all_trades.md` | Aptitude | Knack | 30 | Trickery quarter |
| master | Mage Knight | `mage_knight.md` | Battle Arcana | Warfire | 28 | War quarter |
| master | Magus | `magus.md` | Staffcraft | Staff Charge | 28 | Power quarter |
| master | Marauder | `marauder.md` | Savagery | Warcry | 34 | War quarter |
| master | Miracle Worker | `miracle_worker.md` | Devotion | Miracle | 36 | Faith quarter |
| master | Myrmidon | `myrmidon.md` | Shieldcraft | Bulwark | 34 | War quarter |
| master | Necromancer | `necromancer.md` | Deathgrip | Necrosis | 26 | Power quarter |
| master | Poisoner | `poisoner.md` | Toxicity | Venom | 28 | Trickery quarter |
| master | Pyromancer | `pyromancer.md` | Inferno | Embers | 28 | Power quarter |
| master | Runesmith | `runesmith.md` | Runecraft | Rune Power | 28 | Power quarter |
| master | Savant | `savant.md` | Breadth | Insight | 28 | Power quarter |
| master | Sentinel | `sentinel.md` | Vigilance | Watch | 34 | War quarter |
| master | Shapeshifter | `shapeshifter.md` | Mutability | Change | 28 | Faith quarter |
| master | Sharpshooter | `sharpshooter.md` | Precision | Focus | 32 | War quarter |
| master | Stormbringer | `stormbringer.md` | Tempest | Charge | 28 | Power quarter |
| master | Technomancer | `technomancer.md` | Circuitry | Voltage | 28 | Power quarter |
| master | Templar | `templar.md` | Sanctum | Consecration | 32 | Faith quarter |
| master | Tenebrist | `tenebrist.md` | Umbra | Shade | 28 | Power quarter |
| master | Thaumaturge | `thaumaturge.md` | Entropy | Chaos | 28 | Power quarter |
| master | Theurge | `theurge.md` | Piety | Divinity | 28 | Faith quarter |
| master | Transmuter | `transmuter.md` | Flux | Alteration | 28 | Power quarter |
| master | Traveler | `traveler.md` | Displacement | Waypoint | 28 | Power quarter |
| master | Weapon Master | `weapon_master.md` | Mastery | Focus | 34 | War quarter |
| master | Woodwose | `woodwose.md` | Verdure | Sap | 28 | Faith quarter |
| master | Zealot | `zealot.md` | Fervor | Madness | 36 | Faith quarter |
