# Class: Enchanter

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Enchanter' (p. 86). -->

## 1. Basics

- **Id:** enchanter
- **Display name:** Enchanter
- **Description:** A controller who makes enemies dance on eldritch strings - charming, compelling and confusing them.
- **Icon item:** enchanted_book
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 3 |
| Strength     | 0 |
| Dexterity    | 2 |
| Agility      | 2 |
| Intelligence | 8 |
| Wisdom       | 7 |
| Endurance    | 1 |
| Luck         | 3 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Glamour
- **Description:** The pull of your charms on other minds.
- **What each point gives:** +0.5% chance your hits charm a mob
- **Max points:** 100

### Resource bar

- **Name:** Allure
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** lilac (#C27BE0)

## 4. Starting kit

- 1 × enchanted_book (random level-1 enchant)
- 4 × lapis_lazuli
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Subtle Charm: 10% chance a hostile mob you hit is charmed - it stops attacking you for 5s and wanders off
- Enchantment Defense: immune to Nausea, and Blindness/Darkness last half as long
- Persistent Enchantment: enchanting at a table gives a 15% chance of an extra enchantment on the item

### Special passives

- Charm duration +0.1s per Glamour
- Per 10 Glamour, +3% extra-enchantment chance

## 6. Weakness

- **What players read:** Charms need a mind to work on.
- **What it actually does:** -30% damage against mindless mobs (golems, slimes, magma cubes, shulkers, the Wither)

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | clumsy |
| Spears | proficient |
| Maces | clumsy |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | clumsy |
| Shields | clumsy |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | clumsy |
| Heavy armor (iron, diamond, netherite) | forbidden |
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 2 |
| Ring | 1 |
| Cloak | 1 |
| Shoulderpad | 0 |
| Belt | 0 |
| Charm | 1 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Enchanting

- Shared skill - full definition in `magician.md`

### Skill: Beguiling

- **Shared or class-only:** class-only
- **Trained by:** charming mobs
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Glamour
- **Tier II bonus:** +1 Glamour
- **Tier III bonus:** +10% Allure regen
- **Tier IV bonus:** +1 Glamour
- **Tier V bonus:** +2 Glamour

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Witch

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| enchanter_start | Enchanter | start | - | - | - | +3% charm chance |
| enchanter_captivate | Captivate | small | 1 | 1 | enchanter_start | +5% charm chance |
| enchanter_spellwright | Spellwright | small | 1 | 1 | enchanter_start | +10% extra-enchantment chance |
| enchanter_countercharm | Countercharm | notable | 2 | 12 | enchanter_captivate | players near you are also immune to Nausea |
| enchanter_puppeteer | Puppeteer | notable | 2 | 15 | enchanter_spellwright | charmed mobs attack other hostile mobs |
| enchanter_mind_thief | Mind Thief | keystone | 3 | 30 | enchanter_countercharm, enchanter_puppeteer | charm chance doubled, but you deal -30% damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Subtle Charm | start | - | - | - | - |
| bewitch | Bewitch | small | 1 | 1 | start | +2 Glamour |
| lasting_charm | Lasting Charm | small | 1 | 5 | start | charms last 3s longer |
| countercharm | Countercharm | notable | 2 | 15 | bewitch | unlocks Countercharm ability |
| compel | Compel | notable | 2 | 20 | lasting_charm | unlocks Compel ability |
| puppet_master | Puppet Master | keystone | 3 | 35 | countercharm, compel | you can keep one charmed mob as a pet until you sleep; -15% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| enchanter_stat_start | Enchanter | start | - | - | - | - |
| enchanter_captivating | Captivating | small | 2 | 1 | enchanter_stat_start | +2 Wisdom, +1 Luck |
| enchanter_clever | Clever | small | 2 | 5 | enchanter_captivating | +2 Intelligence, +1 Agility |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Compel** (mob obeys you for 10s, 40 Allure), **Countercharm** (cleanse charm/fear from players nearby, 20 Allure).
- Book talents: Enchantment Defense, Subtle Charm, Countercharm, Persistent Enchantment. Tradition: Enchantment.
- Fits Minecraft enchanting nicely - Enchanter is also the best item enchanter.
