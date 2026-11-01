# Class build report

Generated from `class_specs/*.md` by the class builder. Everything listed here is **not in the game yet**:
tree nodes and skill tiers still show their text in-game, marked "(not in game yet)" or "(partly not in game yet)".

- Classes built: **84**, skills: **109**, active abilities: **114** (of 114 named in the specs)
- Bonus phrases: **2082**. Fully working: **2060**, partly working: **1**, not yet: **21**

Passives and weaknesses have no tooltip of their own, so their missing parts are only listed here.

## Missing features, biggest first

| Feature | Lines |
|---|---|
| [Other mechanics](#other-mechanics) | 9 |
| [Resource bar: filling, refills, spending](#resource-bar-filling-refills-spending) | 5 |
| [Active abilities (named abilities, cooldowns, auras, summons)](#active-abilities-named-abilities-cooldowns-auras-summons) | 5 |
| [Potions, XP, loot, trading, crafting, food](#potions-xp-loot-trading-crafting-food) | 3 |
| [Conditional bonus (while / when / where)](#conditional-bonus-while-when-where) | 1 |
| [Immunities](#immunities) | 1 |
| [Bonus against a kind of mob / marked target](#bonus-against-a-kind-of-mob-marked-target) | 1 |

## Other mechanics

- **executioner**
  - special tree: Steady: exposure after charged hits lasts 1s
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
- **tenebrist**
  - special tree: Snuff: torches within 4 blocks flicker - mobs near you can't see you
- **warlock**
  - special tree: Expert Spell Thief: stolen effects can be re-applied (see section 12)

## Resource bar: filling, refills, spending

- **assassin**
  - special tree: The Black Hand: Assassinate can trigger again 10s after a kill; -10% max health
- **diviner**
  - special tree: Oracle of Ages: premonitions refill every 10 minutes; Omens regen -30%
- **savant**
  - weakness: -10% max Insight regen and your focus schools are the only ones above neutral
- **shapeshifter**
  - special tree: Thousand Faces: you can disguise as a villager to be ignored by illagers; -10% max health
- **technomancer**
  - special tree: Clockwork Legion: Animate Object can keep 3 at once; -15% max health

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

## Potions, XP, loot, trading, crafting, food

- **artificer**
  - special tree: Store Spell: potions you brew can be loaded into crafted gear (see section 12)
- **diplomat**
  - skill Bartering: +5% piglin barter quality
- **diviner**
  - general tree: Read the Signs: loot chests show their contents' rarity colour

## Conditional bonus (while / when / where)

- **engineer**
  - special tree: Mecha: you can't use your own weapons outside it for 10s after leaving

## Immunities

- **hydromancer**
  - special tree: Slippery: immune to being held (guardians' beam slow too)

## Bonus against a kind of mob / marked target

- **runesmith**
  - general tree: Runelord: unmarked gear is forbidden-tier

## Other notes per class

