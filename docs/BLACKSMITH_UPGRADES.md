# Blacksmith progression

The regional blacksmith backend is server authoritative and has an inventory screen available from the Survival inventory. No NPC entity architecture exists in the project; a future compatible NPC can open the same screen by sending `OpenBlacksmithPayload` to the interacting player.

## Profiles and survival materials

`data/pipican/blacksmith_profiles.json` contains one active profile for each of the 27 catalog regions. Each profile is derived from that boss's guaranteed regional material, its existing trial boss-core catalyst (except Ashlands, which keeps the existing Ashen King's Heart progression), and its listed rare reward where present. Other regions use their guaranteed boss trophy as the rare-tier catalyst. Regional materials and boss cores retain their existing mine/trial loot sources, while trophies and listed rare drops come from the corresponding encounter rewards.

## Transactions and screen

The screen displays the selected inventory item, its current level/tier and bound region, the chosen region before binding, required item costs and the resulting armor or attack modifier. It supports armor, weapons and tools in inventory slots 0–35. Once bound, a piece stays in that region and the region selector locks.

Every upgrade payload includes a fresh nonce, slot, full expected item snapshot, expected revision, action and profile. `BlacksmithUpgrades.execute` revalidates all fields, materials and caps on the server thread. Failed requests leave gear and materials unchanged; duplicate/stale requests are rejected. The server remains authoritative if the client changes any displayed value.

## Tiers, costs and attributes

Four tiers each support +1 through +10: Regional, Elite, Legendary and Mythic. At +10, evolution consumes materials and advances one tier at +0; total progression points are retained, so evolution does not weaken gear. The item identity, appearance and all unrelated components/modifiers remain intact.

- Upgrade to +n: `(2+n)` regional materials, multiplied by 1/2/4/8 for the current tier.
- From +6: 1/2/3/4 regional boss catalysts by tier.
- From +8: 1/2/4/8 rare catalysts by tier.
- Evolution: 32 regional +2 boss +2 rare catalysts, multiplied by the current tier.
- Armor: +0.05 armor per progression point, up to +2 per piece.
- Weapons/tools: +0.2 attack damage per progression point, up to +8.

## Set bonuses and compatibility

Two or more armor pieces bound to the same region grant a small knockback-resistance bonus (2%–2.75%, scaled by tier). Four pieces also grant 1.5%–3% base movement speed. Mixed progression tiers still count; mixed regions do not. These are unique transient modifiers, recalculated from equipped armor and removed when the set is broken. Existing class/skill attributes are left untouched.

## Tests

GameTests cover all 27 profiles and their trial-catalyst loot sources, transaction spending, stale/duplicate and invalid requests, metadata preservation, all 40 level steps, three tier evolutions, caps, mixed-tier set activation, mixed-region rejection, and preservation of class modifiers. The target-version client screen still requires interactive visual QA.
