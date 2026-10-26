# Configurable stat effects and passive tree nodes

The eight core stats remain a fixed `Stat` enum by design.

## Configuration model
`StatRules` is the stat conversion definition. It maps each primary stat to
any number of `EffectModifier` descriptors, plus baseline derived effects.
Each entry can use `FLAT` or `PERCENT`. Class-specific rates are supplied
by `StatProfile.scaling(stat)`. The default rules are placeholders, not final balance.

`StatCalculator.effects(...)` returns a map of all derived effects including
regeneration, attack speed, healing power and critical stats. The older
`derive(...)` method remains for the common HP/speed/i-frame/damage values.
External passive effects (from gear or class skills) use exactly the same
`EffectModifier` structure.

Tree nodes now support `cost`, `attributeBonuses`, `passives`, and
`prerequisites`. Passive-only nodes cost points without forcibly raising
primary stats. Example (not a registered class or node):

```java
var node = new StatTree.Node("fortitude", 2, Map.of(),
    List.of(new EffectModifier(Effect.HEALTH_REGEN,
        EffectModifier.Operation.PERCENT, 5)), Set.of("vitality_root"));
```

Build the calculator from `new StatCalculator(StatRules.defaults())`, or
supply a custom `StatRules` from configuration. Use
`tree.activePassives(unlockedNodeIds)` to pass unlocked bonuses to
`calculator.effects(...)` or `calculator.derive(...)`.

Movement remains capped at 2x. Dodge i-frames are integral and based on
agility bonus scaled by class; the roll implementation must apply the returned
extra ticks. Armor and magic resistance cannot be stat-tree effects and remain
equipment-only.

## Incomplete integration
`StatRules` and tree-node definitions are injectable Java data, but parsing
JSON/server configs is not yet implemented. The NeoForge project is still absent
on this branch, so event handlers, persistence, GUI and in-game balancing cannot
be tested yet. The class team should define how skill passives feed modifiers
into the shared calculator. The 8 base stats remain fixed.
