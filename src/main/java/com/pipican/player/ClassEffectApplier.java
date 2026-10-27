package com.pipican.player;

import com.pipican.Pipican;
import com.pipican.condition.ClassCondition;
import com.pipican.core.ClassLookup;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.EffectContext;
import com.pipican.effect.type.ResourceEffect;
import com.pipican.mastery.Skill;
import com.pipican.rpgclass.Affinity;
import com.pipican.rpgclass.RpgClass;
import com.pipican.rpgclass.SpecialStat;
import com.pipican.skill.SkillTree;
import com.pipican.skill.TreeKind;
import com.pipican.stat.StatDefinition;
import com.pipican.trinket.TrinketSlotType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Turns "player has class X at level Y with these nodes, stats and items" into real game effects.
 *
 * <p>{@link #refresh} is the single entry point: it throws away everything the class system
 * applied before and rebuilds it from scratch. That keeps the logic simple and means data edits,
 * class changes, level-ups, equipment swaps etc. all go through the same code path.
 *
 * <p>Order of work:
 * <ol>
 *   <li>stats = class base_stats + bought points + {@code pipican:stat} effects</li>
 *   <li>base health, per-stat-point attribute bonuses</li>
 *   <li>passives, special passives, weakness, allocated tree nodes, reached skill-mastery tiers</li>
 *   <li>equipment affinity: held weapon attack speed, armor pieces, trinkets</li>
 * </ol>
 * All attribute contributions are summed into at most three transient modifiers per attribute
 * (one per operation), with ids {@code pipican:class/<operation>}.
 */
public final class ClassEffectApplier {

    private static final Map<UUID, ClassState> STATES = new ConcurrentHashMap<>();
    private static final Map<AttributeModifier.Operation, Identifier> MODIFIER_IDS = Map.of(
            AttributeModifier.Operation.ADD_VALUE, Pipican.id("class/add_value"),
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, Pipican.id("class/add_multiplied_base"),
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Pipican.id("class/add_multiplied_total"));
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final double VANILLA_BASE_HEALTH = 20.0;

    private ClassEffectApplier() {}

    /** Last computed state for a player (empty if they have no class or were never refreshed). */
    public static ClassState state(ServerPlayer player) {
        return STATES.getOrDefault(player.getUUID(), ClassState.EMPTY);
    }

    public static void forget(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    /** Recomputes and re-applies every class bonus/penalty for this player. Server side only. */
    public static void refresh(ServerPlayer player) {
        ClassState old = state(player);
        for (Holder<Attribute> attribute : old.touchedAttributes()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                MODIFIER_IDS.values().forEach(instance::removeModifier);
            }
        }

        ClassState next = compute(player);
        STATES.put(player.getUUID(), next);

        applyMobEffects(player, old.mobEffects(), next.mobEffects());

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        PlayerClassData data = player.getData(PipicanAttachments.CLASS_DATA);
        data.setResourceMax(next.resourceMax());
        data.setResource(Math.min(data.resource(), next.resourceMax()));
        player.syncData(PipicanAttachments.CLASS_DATA);
    }

    /** Re-adds class potion effects that were removed (milk, /effect clear...). Called periodically. */
    public static void maintainMobEffects(ServerPlayer player) {
        applyMobEffects(player, Map.of(), state(player).mobEffects());
    }

    private static void applyMobEffects(ServerPlayer player, Map<Holder<MobEffect>, Integer> previous, Map<Holder<MobEffect>, Integer> current) {
        for (Holder<MobEffect> effect : previous.keySet()) {
            MobEffectInstance active = player.getEffect(effect);
            if (!current.containsKey(effect) && active != null && active.isInfiniteDuration()) {
                player.removeEffect(effect);
            }
        }
        current.forEach((effect, amplifier) -> {
            MobEffectInstance active = player.getEffect(effect);
            if (active != null && active.isInfiniteDuration() && active.getAmplifier() != amplifier) {
                player.removeEffect(effect);
                active = null;
            }
            // A temporary effect from elsewhere (e.g. a potion) is left alone; we re-add ours when it ends.
            if (active == null) {
                player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, true, false, true));
            }
        });
    }

    private static ClassState compute(ServerPlayer player) {
        PlayerClassData data = player.getData(PipicanAttachments.CLASS_DATA);
        RegistryAccess access = player.level().registryAccess();
        Optional<RpgClass> maybeClass = data.activeClass().flatMap(key -> ClassLookup.rpgClass(access, key));
        if (maybeClass.isEmpty()) {
            if (data.hasClass()) {
                Pipican.LOGGER.warn("Player {} has unknown class {} (was its JSON removed?)", player.getName().getString(), data.activeClass().get().identifier());
            }
            return ClassState.EMPTY;
        }
        RpgClass rpgClass = maybeClass.get();
        ClassProgress progress = data.activeProgress().orElseThrow();
        Collector c = new Collector(rpgClass, progress.level(), player);

        // Every effect source with the rank it applies at.
        List<Map.Entry<ClassEffect, Integer>> effects = new ArrayList<>();
        rpgClass.passives().forEach(e -> effects.add(Map.entry(e, 1)));
        rpgClass.specialPassives().forEach(e -> effects.add(Map.entry(e, 1)));
        rpgClass.weakness().ifPresent(w -> w.effects().forEach(e -> effects.add(Map.entry(e, 1))));
        for (TreeKind kind : TreeKind.values()) {
            Optional<SkillTree> tree = ClassLookup.tree(access, rpgClass, kind);
            if (tree.isEmpty()) {
                continue;
            }
            // The start node's effects count too (it is owned for free).
            Set<String> owned = new HashSet<>(progress.nodes(kind));
            rpgClass.tree(kind).ifPresent(ref -> owned.add(ref.start()));
            for (String nodeId : owned) {
                tree.get().node(nodeId).ifPresent(node -> node.effects().forEach(e -> effects.add(Map.entry(e, 1))));
            }
        }
        // Skill mastery: every reached tier's effects (tiers stack).
        ClassLookup.skills(access).entrySet().forEach(entry -> {
            Skill skill = entry.getValue();
            if (!skill.allowedFor(data.activeClass())) {
                return;
            }
            int tier = skill.tierFor(data.mastery(entry.getKey()));
            for (int t = 0; t < Math.min(tier, skill.tiers().size()); t++) {
                skill.tiers().get(t).effects().forEach(e -> effects.add(Map.entry(e, 1)));
            }
        });

        // Pass 1: stats.
        rpgClass.baseStats().forEach((stat, points) -> c.addStat(stat, points));
        data.boughtStats().forEach((stat, points) -> c.addStat(stat, points));
        for (Map.Entry<ClassEffect, Integer> e : effects) {
            c.rank = e.getValue();
            e.getKey().collectStats(c);
        }

        // Pass 2: everything else.
        c.addAttribute(Attributes.MAX_HEALTH, rpgClass.baseHealth() - VANILLA_BASE_HEALTH, AttributeModifier.Operation.ADD_VALUE);
        c.stats.forEach((statKey, points) -> {
            StatDefinition stat = ClassLookup.stats(access).getValue(statKey);
            if (stat != null) {
                stat.perPoint().forEach(b -> c.addAttribute(b.attribute(), b.amount() * points, b.operation()));
            }
        });
        for (Map.Entry<ClassEffect, Integer> e : effects) {
            c.rank = e.getValue();
            e.getKey().apply(c);
        }
        applyEquipmentAffinity(player, rpgClass, data, c);

        // Push attribute modifiers onto the player.
        Set<Holder<Attribute>> touched = new HashSet<>();
        c.attributes.forEach((attribute, byOp) -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                return;
            }
            touched.add(attribute);
            byOp.forEach((op, amount) -> {
                if (amount != 0) {
                    instance.addTransientModifier(new AttributeModifier(MODIFIER_IDS.get(op), amount, op));
                }
            });
        });

        double resourceMax = 0;
        double regenPerTick = 0;
        if (rpgClass.specialStat().flatMap(SpecialStat::resource).isPresent()) {
            SpecialStat.Resource resource = rpgClass.specialStat().get().resource().get();
            double points = c.sourceValue("special_stat");
            resourceMax = Math.max(0, (resource.max(points) + c.resourceFlat.get(ResourceEffect.Target.MAX))
                    * (1 + c.resourcePercent.get(ResourceEffect.Target.MAX)));
            regenPerTick = (resource.regenPerSecond(points) + c.resourceFlat.get(ResourceEffect.Target.REGEN))
                    * Math.max(0, 1 + c.resourcePercent.get(ResourceEffect.Target.REGEN)) / 20.0;
        }

        List<ClassState.ConditionCheck> checks = new ArrayList<>();
        c.conditions.forEach((condition, result) -> checks.add(new ClassState.ConditionCheck(condition, result)));
        return new ClassState(Optional.of(rpgClass), Map.copyOf(c.stats), List.copyOf(c.taken), List.copyOf(c.dealt),
                Map.copyOf(c.mobEffects), touched, resourceMax, regenPerTick, List.copyOf(checks));
    }

    /**
     * Re-tests every condition the last refresh depended on, and refreshes if any answer changed.
     * Called twice a second per player. Returns true if it refreshed.
     */
    public static boolean recheckConditions(ServerPlayer player) {
        for (ClassState.ConditionCheck check : state(player).conditions()) {
            if (check.condition().test(player) != check.result()) {
                refresh(player);
                return true;
            }
        }
        return false;
    }

    /**
     * Equipment rules. Damage dealt by weapons is handled in the damage event (it needs the actual
     * weapon used); here we handle what can be expressed as attributes.
     */
    private static void applyEquipmentAffinity(ServerPlayer player, RpgClass rpgClass, PlayerClassData data, Collector c) {
        // Held item: attack speed.
        ItemStack mainHand = player.getMainHandItem();
        Affinity held = rpgClass.affinities().resolve(mainHand);
        c.addAttribute(Attributes.ATTACK_SPEED, held.tuning().attackSpeed() - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        // Armor: scale the armor/toughness each piece gives, and slow the player per bad piece.
        double speedFactor = 1;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            var tier = rpgClass.affinities().resolve(stack).tuning();
            double[] armor = new double[2];
            stack.forEachModifier(slot, (attribute, modifier) -> {
                if (modifier.operation() != AttributeModifier.Operation.ADD_VALUE) {
                    return;
                }
                if (attribute.equals(Attributes.ARMOR)) {
                    armor[0] += modifier.amount();
                } else if (attribute.equals(Attributes.ARMOR_TOUGHNESS)) {
                    armor[1] += modifier.amount();
                }
            });
            c.addAttribute(Attributes.ARMOR, armor[0] * (tier.armor() - 1), AttributeModifier.Operation.ADD_VALUE);
            c.addAttribute(Attributes.ARMOR_TOUGHNESS, armor[1] * (tier.armor() - 1), AttributeModifier.Operation.ADD_VALUE);
            speedFactor *= tier.movePenaltyPerArmorPiece();
        }
        c.addAttribute(Attributes.MOVEMENT_SPEED, speedFactor - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        // Trinkets: every attribute modifier on the item counts, scaled by its affinity tier.
        Map<ResourceKey<TrinketSlotType>, Integer> allowed = rpgClass.trinketSlots();
        data.allTrinkets().forEach((slotType, stacks) -> {
            int count = Math.min(stacks.size(), allowed.getOrDefault(slotType, 0));
            for (int i = 0; i < count; i++) {
                ItemStack stack = stacks.get(i);
                if (stack.isEmpty()) {
                    continue;
                }
                double mult = rpgClass.affinities().resolve(stack).tuning().trinket();
                for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
                    c.addAttribute(entry.attribute(), entry.modifier().amount() * mult, entry.modifier().operation());
                }
            }
        });
    }

    /** The {@link EffectContext} implementation: just accumulates everything into maps. */
    private static final class Collector implements EffectContext {
        private final RpgClass rpgClass;
        private final int level;
        private final ServerPlayer player;
        int rank = 1;
        /** Every condition tested during this refresh and its answer (identity: the same JSON object). */
        final Map<ClassCondition, Boolean> conditions = new IdentityHashMap<>();
        final Map<ResourceEffect.Target, Double> resourceFlat = new EnumMap<>(Map.of(ResourceEffect.Target.MAX, 0.0, ResourceEffect.Target.REGEN, 0.0));
        final Map<ResourceEffect.Target, Double> resourcePercent = new EnumMap<>(Map.of(ResourceEffect.Target.MAX, 0.0, ResourceEffect.Target.REGEN, 0.0));
        final Map<ResourceKey<StatDefinition>, Double> stats = new HashMap<>();
        final Map<Holder<Attribute>, Map<AttributeModifier.Operation, Double>> attributes = new HashMap<>();
        final Map<Holder<MobEffect>, Integer> mobEffects = new HashMap<>();
        final List<ClassState.DamageRule> taken = new ArrayList<>();
        final List<ClassState.DamageRule> dealt = new ArrayList<>();

        Collector(RpgClass rpgClass, int level, ServerPlayer player) {
            this.rpgClass = rpgClass;
            this.level = level;
            this.player = player;
        }

        @Override
        public double sourceValue(String source) {
            return switch (source) {
                case "class_level" -> level;
                case "special_stat" -> rpgClass.specialStat().map(s -> stats.getOrDefault(s.stat(), 0.0)).orElse(0.0);
                default -> {
                    Identifier id = Identifier.tryParse(source);
                    yield id == null ? 0 : stats.getOrDefault(ResourceKey.create(com.pipican.core.PipicanRegistries.STAT, id), 0.0);
                }
            };
        }

        @Override
        public int rank() {
            return rank;
        }

        @Override
        public void addStat(ResourceKey<StatDefinition> stat, double amount) {
            stats.merge(stat, amount, Double::sum);
        }

        @Override
        public void addAttribute(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
            if (amount != 0) {
                attributes.computeIfAbsent(attribute, a -> new HashMap<>()).merge(operation, amount, Double::sum);
            }
        }

        @Override
        public void addMobEffect(Holder<MobEffect> effect, int amplifier) {
            mobEffects.merge(effect, amplifier, Math::max);
        }

        @Override
        public void addDamageTakenMultiplier(Optional<TagKey<DamageType>> types, Optional<HolderSet<Item>> weapons, double multiplier) {
            taken.add(new ClassState.DamageRule(types, weapons, multiplier));
        }

        @Override
        public void addDamageDealtMultiplier(Optional<TagKey<DamageType>> types, Optional<HolderSet<Item>> weapons, double multiplier) {
            dealt.add(new ClassState.DamageRule(types, weapons, multiplier));
        }

        @Override
        public void addResource(ResourceEffect.Target target, double amount, AttributeModifier.Operation operation) {
            // add_multiplied_total is treated like add_multiplied_base: there is only one resource "base".
            (operation == AttributeModifier.Operation.ADD_VALUE ? resourceFlat : resourcePercent).merge(target, amount, Double::sum);
        }

        @Override
        public boolean test(ClassCondition condition) {
            return conditions.computeIfAbsent(condition, c -> c.test(player));
        }
    }
}
