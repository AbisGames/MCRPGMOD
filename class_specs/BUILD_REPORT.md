# Class build report

Generated from `class_specs/*.md` by the class builder. Everything listed here is **not in the game yet**:
tree nodes and skill tiers still show their text in-game, marked "(not in game yet)" or "(partly not in game yet)".

- Classes built: **84**, skills: **109**, active abilities: **114** (of 114 named in the specs)
- Bonus phrases: **2250**. Fully working: **2209**, partly working: **2**, not yet: **39**

Passives and weaknesses have no tooltip of their own, so their missing parts are only listed here.

## Missing features, biggest first

| Feature | Lines |
|---|---|
| [Skill trigger approximated](#skill-trigger-approximated) | 28 |
| [Other mechanics](#other-mechanics) | 15 |
| [Active abilities (named abilities, cooldowns, auras, summons)](#active-abilities-named-abilities-cooldowns-auras-summons) | 8 |
| [Resource bar: filling, refills, spending](#resource-bar-filling-refills-spending) | 7 |
| [Conditional bonus (while / when / where)](#conditional-bonus-while-when-where) | 5 |
| [Potions, XP, loot, trading, crafting, food](#potions-xp-loot-trading-crafting-food) | 5 |
| [Immunities](#immunities) | 4 |
| [Resource bar that fills from use (only the drain works)](#resource-bar-that-fills-from-use-only-the-drain-works) | 2 |
| [Chance on hit / proc effects](#chance-on-hit-proc-effects) | 1 |
| [Bonus against a kind of mob / marked target](#bonus-against-a-kind-of-mob-marked-target) | 1 |

## Skill trigger approximated

- **abjurer**
  - Abjuration: "taking damage while Ward is above half" -> pipican:hurt_wearing #pipican:armor/any
- **aeromancer**
  - Windcalling: "throwing wind charges and falling long distances safely" -> pipican:jump
- **astromancer**
  - Stargazing: "spending time outdoors at night and killing mobs while glowing" -> pipican:kill
- **bard**
  - Repertoire: "allies killing mobs inside your Song" -> pipican:kill
  - Performance: "using goat horns, note blocks and jukeboxes near other players" -> pipican:kill
- **chaplain**
  - Liturgy: "allies killing mobs inside your Battle Chant" -> pipican:kill
- **conqueror**
  - Generalship: "allies killing mobs you've hit in the last 4s" -> pipican:kill
- **death_dealer**
  - Reaping: "killing 3+ mobs within 10s" -> pipican:kill
- **defender**
  - Protector: "blocking hits aimed at mobs targeting other players / taking damage near allies" -> pipican:hurt_wearing #pipican:armor/any
- **dervish**
  - Whirling: "landing both-hand combos" -> pipican:melee_hit
- **engineer**
  - Mechanics: "your eidolon dealing damage and repairing it" -> pipican:kill
- **explorer**
  - Pathfinding: "discovering new chunks / travelling long distances" -> pipican:move
- **fighter**
  - Drill: "killing mobs with 3 or more different weapon types in the same day" -> pipican:kill
- **gunslinger**
  - Gunplay: "hitting mobs with firearms" -> pipican:ranged_hit #pipican:weapons/crossbows
  - Gunsmithing: "modifying and repairing firearms" -> pipican:kill
- **infiltrator**
  - Espionage: "sneaking past hostile mobs within 4 blocks unnoticed" -> pipican:kill
- **jack_of_all_trades**
  - Dabbling: "using 4+ different item types in combat in one day" -> pipican:kill
- **mage_knight**
  - War Magic: "hitting mobs while Escalating Violence is active" -> pipican:melee_hit
- **priest**
  - Devout: "other players within 8 blocks killing mobs while your Prayer aura is active" -> pipican:kill
- **pyromancer**
  - Firestarting: "setting mobs on fire and killing burning mobs" -> pipican:filtered
- **rogue**
  - Stealth: "sneaking within 8 blocks of a hostile mob without being targeted" -> pipican:kill
- **runesmith**
  - Runecarving: "applying armor trims and renaming weapons at anvils" -> pipican:kill
- **savant**
  - Broad Study: "using magic of both focus schools in one fight" -> pipican:kill
- **scout**
  - Pathfinding: "discovering new chunks / travelling long distances" -> pipican:move
- **sorcerer**
  - Overchannel: "triggering Sorcerous Outbursts and landing magic hits while Strain is above half" -> pipican:reaction
- **thief**
  - Thievery: "opening naturally generated loot chests for the first time and killing mobs with Looting" -> pipican:kill
- **transmuter**
  - Alterations: "drinking potions and using Optimization" -> pipican:did
- **witch**
  - Coven Lore: "brewing potions and throwing them at mobs" -> pipican:did

## Other mechanics

- **destroyer**
  - general tree: Annihilator: +30% explosion and magic damage, but explosions you make damage blocks more
- **executioner**
  - special tree: Steady: exposure after charged hits lasts 1s
- **explorer**
  - general tree: Cartographer: maps you make explore 25% faster
- **illusionist**
  - special tree: Unreal: you can pass through mobs and they lose track every 10s; -10% damage
- **inquisitor**
  - special passive: Scrutiny bonus +0.5% per point
- **jack_of_all_trades**
  - special tree: Prodigy: Prodigy picks the stat you most recently used
- **miracle_worker**
  - weakness: each Stigmata or Worker of Miracles use lowers your max health by 2 until you sleep (stacks)
  - general tree: Saint Among Us: miracles cost half
  - general tree: Saint Among Us: max health penalty is 3 per use
- **technomancer**
  - passive: Redstone you place powers 2 blocks farther
- **templar**
  - general tree: Fortress Temple: outside it you lose all class passives
- **tenebrist**
  - special tree: Snuff: torches within 4 blocks flicker - mobs near you can't see you
- **transmuter**
  - special stat per point: +0.2 to every stat per 10 points (rounded down)
- **warlock**
  - special tree: Expert Spell Thief: stolen effects can be re-applied (see section 12)
- **zealot**
  - special tree: Violent Madness: Violent Madness no longer targets players

## Active abilities (named abilities, cooldowns, auras, summons)

- **bard**
  - special tree: Swift Song: Inspiring Song refreshes twice as fast
- **diviner**
  - passive: while you have a premonition left, mobs can't surprise-hit you and you see their outlines within 8 blocks
- **engineer**
  - special tree: Mecha: while in the cockpit, eidolon damage +25%
- **runesmith**
  - general tree: Runelord: sigil bonuses doubled
- **shapeshifter**
  - special passive: Form bonuses +1% per Mutability
- **templar**
  - general tree: Fortress Temple: temple bonuses doubled
  - special tree: Living Temple: your temple follows you
  - special tree: Living Temple: -15% temple bonuses

## Resource bar: filling, refills, spending

- **assassin**
  - special tree: The Black Hand: Assassinate can trigger again 10s after a kill; -10% max health
- **diviner**
  - special tree: Oracle of Ages: premonitions refill every 10 minutes; Omens regen -30%
- **savant**
  - weakness: -10% max Insight regen and your focus schools are the only ones above neutral
- **scout**
  - special tree: Low Blow: when a revealed mob takes damage from an ally, you get a free 50% damage hit on it; -10% max Stride
- **shapeshifter**
  - special tree: Thousand Faces: you can disguise as a villager to be ignored by illagers; -10% max health
- **technomancer**
  - special tree: Clockwork Legion: Animate Object can keep 3 at once; -15% max health
- **templar**
  - general tree: Sacred Ground: players in your temple regenerate 20% faster

## Conditional bonus (while / when / where)

- **acrobat**
  - passive: You can walk through other players and pass by mobs without being pushed
- **aeromancer**
  - special tree: Cyclone: wind charges pull light mobs in before pushing them out
- **engineer**
  - special tree: Mecha: you can't use your own weapons outside it for 10s after leaving
- **hydromancer**
  - general tree: Riptide: tridents with Riptide work without rain while Current is above half
- **zealot**
  - weakness: when Madness is full: Violent Madness - for 10s you attack anything near you (including allies), +30% damage, then Madness resets

## Potions, XP, loot, trading, crafting, food

- **artificer**
  - special tree: Store Spell: potions you brew can be loaded into crafted gear (see section 12)
- **diplomat**
  - special passive: Per 10 Rapport, wandering traders offer 1 extra trade
  - skill Bartering: +5% piglin barter quality
- **diviner**
  - general tree: Read the Signs: loot chests show their contents' rarity colour
- **fighter**
  - general tree: Living Weapon: +15% weapon damage and attack speed, but -30% magic damage and potion strength

## Immunities

- **druid**
  - special tree: Trackless: your steps never trigger sculk sensors or pressure plates
- **hydromancer**
  - special tree: Slippery: immune to being held (guardians' beam slow too)
- **scout**
  - passive: Trackless: your footsteps never trigger sculk sensors, shriekers or tripwires
- **thief**
  - passive: you never trigger tripwires or pressure plates

## Resource bar that fills from use (only the drain works)

- **sorcerer**
  - resource: Strain drains 0.2/s; filling it from fighting needs the active-skill system
- **zealot**
  - resource: Madness drains 0.3/s; filling it from fighting needs the active-skill system

## Chance on hit / proc effects

- **mage_knight**
  - special tree: Charged Blade: first escalating hit also knocks back

## Bonus against a kind of mob / marked target

- **runesmith**
  - general tree: Runelord: unmarked gear is forbidden-tier

## Other notes per class

- **abjurer**: ability Sanctuary: pushes mobs out of 4 blocks once and slows them, instead of a 5s barrier
- **abjurer**: kit: "splash potion of resistance" doesn't exist in vanilla; gave Turtle Master instead
- **abjurer**: special passive: cap not enforced (scaling has no maximum): "-0.2% damage taken per Warding point (max -20%)"
- **apocalyptist**: ability Hasten the Apocalypse: "fall prone" and slowing ground become Slowness III
- **artificer**: ability Assemble: an iron sword (iron chestplate while sneaking) that vanishes after 10 minutes or when you sleep
- **artificer**: ability Mechanical Servants: one iron golem for 30s instead of small copper golems
- **artificer**: ability Store Spell: uses up the potion in your off hand: your next 5 hits within 60s apply it
- **assassin**: ability Disguise: Invisibility for 10s
- **assassin**: ability Mark Target: works on any mob in sight, not only ones you are hidden from
- **avenger**: ability Declare Vengeance: +25% damage to the marked mob for 60s
- **beastmaster**: ability Beast Within: +2 attack damage with anything, not only unarmed
- **cavalier**: ability Charge: hits mobs in front when the charge starts, not on impact
- **chronomancer**: ability Time Stop: mobs are stunned and harmless for 3s rather than frozen in time
- **cleric**: ability Holy Strike: your next hit on an undead mob within 10s: +25% and +3.5 damage
- **conjurer**: icon "zombie_spawner_egg" isn't an item id; used zombie_spawn_egg
- **conqueror**: ability Attack Command: the ally you look at (or you) deals +50% on their next hit within 10s
- **conqueror**: ability Direct the Troops: you and nearby players charge the way you look
- **dervish**: ability Whirl: one hit on every mob around you (not one per weapon)
- **druid**: ability Tree Walk: steps out beside the log you look at, up to 24 blocks away
- **enchanter**: ability Countercharm: removes harmful potion effects (charm and fear don't exist yet)
- **enchanter**: kit: "random level-1 enchanted book" isn't possible; gave a plain book
- **engineer**: ability Build Eidolon: uses 4 iron blocks; if your eidolon exists it comes to you instead
- **engineer**: ability Overdrive: Speed II and Strength II on your eidolon (and other companions) for 10s
- **exorcist**: ability Exorcise: cleanses the player you look at, or stuns the mob you look at (any mob, not only unclean ones)
- **fighter**: special passive: cap not enforced (scaling has no maximum): "Fighter's Resolve: -0.25% damage taken per Discipline point (max -15%)"
- **geomancer**: ability Earth Glide: moves you up to 8 blocks through stone at once
- **gladiator**: ability Coup de Grace: a heavy hit on the mob you look at (any mob, not only affected ones)
- **gunslinger**: ability Deadeye: +50% ranged damage for 3s instead of slowed time and guaranteed crits
- **gunslinger**: gear: Firearms don't exist yet; crossbows stand in (already proficient in the table).
- **gunslinger**: kit: bullets (planned) -> arrows
- **gunslinger**: kit: pistol (planned) -> crossbow
- **hexer**: ability Hex: a fixed curse (Weakness, Slowness, Glowing) instead of choosing one
- **hydromancer**: ability Watery Form: half damage, fast swimming and invisibility underwater; no squeezing through gaps
- **inquisitor**: ability Dreadful Threat: any mob you look at (scrutiny isn't tracked yet)
- **jack_of_all_trades**: weakness: cap not enforced (scaling has no maximum): "you can never reach Tier V in any skill (max Tier IV)"
- **magician**: ability Spell Recovery: refills half the Mana
- **magician**: gear: Stick/blaze rod are proficient (they don't become spears: there is no "counts as" rule).
- **necromancer**: ability Raise Dead: for 30s, every mob you kill rises as an allied zombie for 30s
- **necromancer**: passive: cap not enforced (scaling has no maximum): "Command Undead: hitting a zombie or skeleton that isn't targeting you has a 20% chance to make it your servant for 60s (max 2)"
- **oracle**: ability Commune: points you toward the nearest village
- **paladin**: ability Holy Radiance: slows and weakens all hostile mobs, not only undead
- **priest**: ability Shared Recovery: heals you and every player within 6 blocks
- **priest**: gear: Shovels are neutral (they don't become maces: there is no "counts as" rule).
- **runesmith**: ability Rune Blast: a blast around you (sigils don't exist yet)
- **runesmith**: icon "smithing_template" isn't an item id; used coast_armor_trim_smithing_template
- **runesmith**: kit: "any trim" template -> coast armor trim
- **shapeshifter**: ability Bird Form: slow fall instead of gliding
- **sorcerer**: ability Sorcerous Blast: free to cast on a 1.5s cooldown; doesn't build Strain yet
- **spellbinder**: ability Invest Power: +30% damage with anything (bound weapons don't exist yet)
- **spellbinder**: ability Recall Weapon: calls back the nearest weapon you dropped or threw (weapons aren't bound yet)
- **technomancer**: ability Animate Object: an allied iron golem for 60s (you don't need to pick a block)
- **templar**: ability Consecrate Ground: a 6-block circle where you stand for 60s: allies regenerate, mobs are slowed, undead are burned
- **tenebrist**: ability Shadow Form: Invisibility and -20% damage taken; no passing through mobs
- **tenebrist**: ability Shadow Step: blink 12 blocks, usable in light level 7 or lower
- **thaumaturge**: ability Reroll: removes your harmful effects (no new random effect)
- **thaumaturge**: kit: "random potion" isn't possible; gave awkward potions
- **thief**: ability Escape: removes Slowness and hops free
- **transmuter**: ability Transmute: turns the held stack into a related material (cobblestone to stone, sand to glass, raw ores to ingots...)
- **traveler**: ability Recall: sneak + use to mark the spot (free), use to return
- **warlock**: ability Return to Sender: moves your harmful effects onto the mob you look at
- **weapon_master**: ability Masterstroke: your next hit within 10s deals double damage, with any weapon
- **witch**: ability Flying Broom: flies while holding a stick (or blaze rod)
- **witch**: ability Witch Fire: invisible while on; every second burns the mob you look at (no steering)
- **witch**: gear: Stick/blaze rod are proficient (there is no "staff" category).
- **wizard**: ability Grimoire Casting: write a spell name (e.g. "Firebolt", "Blink", "Haste") on the first page of a book and quill
- **wizard**: gear: Book/enchanted book counts as proficient in any hand (no off-hand-only rule exists).
- **wizard**: kit: "random level-1 enchanted book" isn't possible; gave a plain book
- **woodwose**: ability Treeform: hidden and rooted while on, regenerating fast
- **zealot**: general tree: Flagellant: cap not enforced (scaling has no maximum): "+5% damage per missing heart (max +25%)"
