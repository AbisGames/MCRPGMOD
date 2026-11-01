package com.pipican.soul;

import com.pipican.Pipican;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * What each of the 15 soul paths does (descriptions: tools/ore_builder/soul_paths.py). Every path has a stage-3
 * ability, a Shikai form (stage 5), what releasing Bankai does and its Bankai ability (stage 7), an Absolute Form
 * (stage 10), an armor passive (stage 6, full set worn) and an aura (stage 9). Abilities hit harder with the stages:
 * +25% at Mastery (6, also +25% range), +25% at Soul Union (9), +50% during Bankai.
 */
public final class SoulAbilities {

    /** One use of the blade: who, where, how strong. */
    public record Ctx(ServerPlayer player, ServerLevel level, String path, int stage, double dmg, double range, boolean bankai) {}

    /** Timed states: player -> state -> game time it ends. */
    private static final Map<UUID, Map<String, Long>> STATES = new ConcurrentHashMap<>();
    /** Marks left on creatures: owner -> kind -> creature -> game time it fades (Scorpion stings, bound foes, shards). */
    private static final Map<UUID, Map<String, Map<UUID, Long>>> MARKS = new ConcurrentHashMap<>();
    /** True while an ability deals its own damage, so blade-hit effects don't fire twice. */
    private static boolean abilityHit;

    private static final net.minecraft.resources.Identifier ARMOR_ID = Pipican.id("soul_armor");
    private static final net.minecraft.resources.Identifier TOUGH_ID = Pipican.id("soul_toughness");
    private static final net.minecraft.resources.Identifier LIMITLESS_ID = Pipican.id("soul_limitless");
    private static final net.minecraft.resources.Identifier REACH_ID = Pipican.id("soul_reach");
    private static final net.minecraft.resources.Identifier KNOCKBACK_ID = Pipican.id("soul_knockback");

    private SoulAbilities() {}

    public static Ctx ctx(ServerPlayer player) {
        SoulData data = Soul.get(player);
        int stage = data.stage();
        boolean bankai = Soul.bankai(player);
        double dmg = 4 + 1.2 * stage;
        double range = 1;
        if (stage >= 6) {
            dmg *= 1.25;
            range *= 1.25;
        }
        if (stage >= 9) {
            dmg *= 1.25;
            range *= 1.1;
        }
        if (bankai) {
            dmg *= 1.5;
        }
        return new Ctx(player, (ServerLevel) player.level(), data.path(), stage, dmg, range, bankai);
    }

    // ------------------------------------------------------------------ states and marks

    public static boolean on(Player p, String state) {
        Long end = STATES.getOrDefault(p.getUUID(), Map.of()).get(state);
        return end != null && end > p.level().getGameTime();
    }

    static void start(Player p, String state, int ticks) {
        STATES.computeIfAbsent(p.getUUID(), k -> new ConcurrentHashMap<>()).put(state, p.level().getGameTime() + ticks);
    }

    static void stop(Player p, String state) {
        STATES.getOrDefault(p.getUUID(), new HashMap<>()).remove(state);
    }

    private static void mark(Player owner, String kind, LivingEntity target, int ticks) {
        MARKS.computeIfAbsent(owner.getUUID(), k -> new ConcurrentHashMap<>()).computeIfAbsent(kind, k -> new ConcurrentHashMap<>())
                .put(target.getUUID(), owner.level().getGameTime() + ticks);
    }

    private static boolean marked(Player owner, String kind, LivingEntity target) {
        Long end = MARKS.getOrDefault(owner.getUUID(), Map.of()).getOrDefault(kind, Map.of()).get(target.getUUID());
        return end != null && end > owner.level().getGameTime();
    }

    private static void unmark(Player owner, String kind, LivingEntity target) {
        MARKS.getOrDefault(owner.getUUID(), Map.of()).getOrDefault(kind, new HashMap<>()).remove(target.getUUID());
    }

    /** The living creatures the owner marked with {@code kind} that are near (and clears the marks). */
    private static List<LivingEntity> takeMarked(Ctx c, String kind, double r) {
        Map<UUID, Long> marks = MARKS.getOrDefault(c.player().getUUID(), Map.of()).getOrDefault(kind, Map.of());
        long now = c.level().getGameTime();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, c.player().getBoundingBox().inflate(r))) {
            Long end = marks.get(e.getUUID());
            if (end != null && end > now) {
                out.add(e);
            }
        }
        out.forEach(e -> marks.remove(e.getUUID()));
        return out;
    }

    public static void clear(ServerPlayer player) {
        STATES.remove(player.getUUID());
        MARKS.remove(player.getUUID());
        SoulMinions.clear(player);
    }

    // ------------------------------------------------------------------ targeting

    /** Something the owner's abilities may hit: not the owner, other players, their pets or summons, or armor stands. */
    public static boolean foe(Player owner, LivingEntity e) {
        if (e == owner || !e.isAlive() || e instanceof Player || e instanceof ArmorStand) {
            return false;
        }
        if (e instanceof OwnableEntity pet && pet.getOwner() == owner) {
            return false;
        }
        return !SoulMinions.summonOf(e, owner) && !(SoulMinions.isSummoned(e) && !e.entityTags().contains("pipican_soul_spirit"));
    }

    /** A foe that is actually hostile - what area effects and auras hit (not villagers and cows). */
    public static boolean hostile(Player owner, LivingEntity e) {
        return foe(owner, e) && (e instanceof Enemy || (e instanceof Mob m && m.getTarget() == owner) || e.entityTags().contains("pipican_soul_spirit"));
    }

    private static boolean ally(Player owner, LivingEntity e) {
        return e == owner || (e instanceof Player && e.isAlive()) || (e instanceof OwnableEntity pet && pet.getOwner() == owner) || SoulMinions.summonOf(e, owner);
    }

    /** Creatures within {@code width} of the line the owner looks along, nearest first. */
    static List<LivingEntity> line(Ctx c, double length, double width, Predicate<LivingEntity> which) {
        ServerPlayer p = c.player();
        Vec3 from = p.getEyePosition();
        Vec3 to = from.add(p.getLookAngle().scale(length));
        AABB box = new AABB(from, to).inflate(width + 1);
        List<LivingEntity> out = new ArrayList<>(c.level().getEntitiesOfClass(LivingEntity.class, box, e -> which.test(e)
                && (e.getBoundingBox().inflate(width).clip(from, to).isPresent() || e.getBoundingBox().inflate(width).contains(from))));
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        return out;
    }

    static List<LivingEntity> line(Ctx c, double length, double width) {
        return line(c, length, width, e -> foe(c.player(), e));
    }

    static LivingEntity lookTarget(Ctx c, double range) {
        List<LivingEntity> hit = line(c, range, 0.6);
        return hit.isEmpty() ? null : hit.getFirst();
    }

    /** Where the owner looks: the creature, else the block, else the end of the range. */
    static Vec3 lookPoint(Ctx c, double range) {
        LivingEntity t = lookTarget(c, range);
        if (t != null) {
            return t.position();
        }
        return c.player().pick(range, 0, false).getLocation();
    }

    static List<LivingEntity> around(Ctx c, Vec3 at, double r) {
        return c.level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r),
                e -> hostile(c.player(), e) && e.position().distanceTo(at) <= r + e.getBbWidth() / 2);
    }

    static List<LivingEntity> allies(Ctx c, double r) {
        return c.level().getEntitiesOfClass(LivingEntity.class, c.player().getBoundingBox().inflate(r), e -> ally(c.player(), e));
    }

    /** Hostile creatures in front of the owner (within {@code r}, in a cone). */
    static List<LivingEntity> cone(Ctx c, double r, double dot) {
        Vec3 look = c.player().getLookAngle();
        Vec3 eye = c.player().getEyePosition();
        return c.level().getEntitiesOfClass(LivingEntity.class, c.player().getBoundingBox().inflate(r), e -> foe(c.player(), e)
                && e.distanceTo(c.player()) <= r && e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) >= dot);
    }

    static void hit(Ctx c, LivingEntity e, double amount) {
        abilityHit = true;
        try {
            e.invulnerableTime = 0;
            e.hurtServer(c.level(), c.level().damageSources().playerAttack(c.player()), (float) amount);
        } finally {
            abilityHit = false;
        }
    }

    static void effect(LivingEntity e, Holder<MobEffect> effect, int ticks, int amp) {
        e.addEffect(new MobEffectInstance(effect, ticks, amp, false, true));
    }

    static void freeze(LivingEntity e, int ticks) {
        effect(e, MobEffects.SLOWNESS, ticks, 4);
        e.setTicksFrozen(Math.max(e.getTicksFrozen(), 140 + ticks));
    }

    // ------------------------------------------------------------------ particles and sound

    private static void beam(Ctx c, ParticleOptions particle, double length) {
        Vec3 from = c.player().getEyePosition().subtract(0, 0.3, 0);
        Vec3 dir = c.player().getLookAngle();
        for (double d = 1; d <= length; d += 0.6) {
            Vec3 at = from.add(dir.scale(d));
            c.level().sendParticles(particle, at.x, at.y, at.z, 2, 0.15, 0.15, 0.15, 0.0);
        }
    }

    private static void burst(Ctx c, ParticleOptions particle, Vec3 at, double r, int count) {
        c.level().sendParticles(particle, at.x, at.y + 0.5, at.z, count, r / 2, 0.5, r / 2, 0.02);
    }

    private static void ring(Ctx c, ParticleOptions particle, Vec3 at, double r) {
        for (int i = 0; i < 36; i++) {
            double a = Math.PI * 2 * i / 36;
            c.level().sendParticles(particle, at.x + Math.cos(a) * r, at.y + 0.2, at.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    private static DustParticleOptions dust(Ctx c, float size) {
        return new DustParticleOptions(Math.max(Soul.PATHS.getOrDefault(c.path(), 0xFFFFFF), 0x202020), size);
    }

    private static void sound(Ctx c, net.minecraft.sounds.SoundEvent s, float pitch) {
        c.level().playSound(null, c.player().blockPosition(), s, SoundSource.PLAYERS, 1.0f, pitch);
    }

    private static void sound(Ctx c, Holder<net.minecraft.sounds.SoundEvent> s, float pitch) {
        sound(c, s.value(), pitch);
    }

    // ------------------------------------------------------------------ stage 3: the first ability

    /** Returns the cooldown in ticks. */
    public static int whisper(Ctx c) {
        ServerPlayer p = c.player();
        double r = c.range();
        switch (c.path()) {
            case "black_crescent" -> {
                line(c, 12 * r, 1.2).forEach(e -> hit(c, e, c.dmg()));
                beam(c, ParticleTypes.SWEEP_ATTACK, 12 * r);
                sound(c, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f);
            }
            case "ash_sovereign" -> {
                start(p, "fire_blade", 300);
                burst(c, ParticleTypes.FLAME, p.position(), 1, 30);
                sound(c, SoundEvents.FIRECHARGE_USE, 1.0f);
                return 400;
            }
            case "perfect_lie" -> {
                effect(p, MobEffects.INVISIBILITY, 100, 0);
                for (Mob m : c.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16), m -> m.getTarget() == p)) {
                    m.setTarget(null);
                }
                burst(c, ParticleTypes.POOF, p.position(), 1, 25);
                return 300;
            }
            case "cherry_storm" -> {
                cone(c, 6 * r, 0.7).forEach(e -> hit(c, e, c.dmg() * 0.8));
                beam(c, ParticleTypes.CHERRY_LEAVES, 6 * r);
            }
            case "snow_dragon" -> {
                LivingEntity t = lookTarget(c, 16 * r);
                if (t != null) {
                    Vec3 pull = p.position().subtract(t.position()).normalize().scale(1.4);
                    t.setDeltaMovement(pull.x, 0.4, pull.z);
                    t.hurtMarked = true;
                    effect(t, MobEffects.SLOWNESS, 60, 1);
                    hit(c, t, c.dmg() * 0.5);
                }
                beam(c, ParticleTypes.SNOWFLAKE, 16 * r);
            }
            case "unchained_beast" -> {
                start(p, "rage", 300);
                burst(c, new DustParticleOptions(0x8B0000, 1.5f), p.position(), 1, 30);
                return 500;
            }
            case "playful_shadow" -> {
                start(p, "twin", 200);
                burst(c, ParticleTypes.SMOKE, p.position(), 1, 25);
                return 400;
            }
            case "crimson_healer" -> {
                start(p, "lifesteal", 300);
                allies(c, 8).forEach(a -> a.heal(4));
                burst(c, ParticleTypes.HEART, p.position(), 3, 12);
                return 400;
            }
            case "shadow_inventor" -> {
                effect(p, MobEffects.ABSORPTION, 400, 2);
                burst(c, new DustParticleOptions(0xC62828, 1.2f), p.position(), 1, 30);
                return 500;
            }
            case "scorpion_sting" -> {
                effect(p, MobEffects.SPEED, 160, 1);
                effect(p, MobEffects.INVISIBILITY, 160, 0);
                burst(c, ParticleTypes.SMOKE, p.position(), 1, 20);
                return 400;
            }
            case "decay_experiment" -> {
                start(p, "poison_blade", 300);
                burst(c, ParticleTypes.ITEM_SLIME, p.position(), 1, 20);
                return 400;
            }
            case "moonlight_dance" -> {
                around(c, p.position(), 4 * r).forEach(e -> {
                    hit(c, e, c.dmg() * 0.6);
                    freeze(e, 80);
                });
                ring(c, ParticleTypes.SNOWFLAKE, p.position(), 4 * r);
            }
            case "bone_serpent" -> {
                start(p, "reach", 200);
                return 300;
            }
            case "lethal_reach" -> {
                line(c, 10 * r, 0.8).forEach(e -> hit(c, e, c.dmg() * 1.2));
                beam(c, ParticleTypes.CRIT, 10 * r);
                sound(c, SoundEvents.TRIDENT_THROW, 1.5f);
            }
            case "wolf_guardian" -> smash(c, lookPoint(c, 16 * r), 3, c.dmg() * 1.5);
            default -> { }
        }
        return 160;
    }

    private static void smash(Ctx c, Vec3 at, double r, double dmg) {
        around(c, at, r).forEach(e -> {
            hit(c, e, dmg);
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
            e.hurtMarked = true;
        });
        burst(c, ParticleTypes.EXPLOSION, at, r, 6);
        burst(c, ParticleTypes.SOUL, at, r, 30);
        c.level().playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.2f, 0.6f);
    }

    // ------------------------------------------------------------------ stage 5: Shikai

    public static int shikai(Ctx c) {
        ServerPlayer p = c.player();
        double r = c.range();
        switch (c.path()) {
            case "black_crescent" -> {
                line(c, 18 * r, 2.2).forEach(e -> hit(c, e, c.dmg() * 1.6));
                beam(c, ParticleTypes.SWEEP_ATTACK, 18 * r);
                beam(c, ParticleTypes.SOUL_FIRE_FLAME, 18 * r);
                sound(c, SoundEvents.PLAYER_ATTACK_SWEEP, 0.5f);
            }
            case "ash_sovereign" -> {
                cone(c, 10 * r, 0.6).forEach(e -> {
                    hit(c, e, c.dmg() * 1.4);
                    e.igniteForSeconds(6);
                });
                beam(c, ParticleTypes.FLAME, 10 * r);
                burst(c, ParticleTypes.FLAME, p.position().add(p.getLookAngle().scale(5)), 4, 60);
                sound(c, SoundEvents.BLAZE_SHOOT, 0.7f);
            }
            case "perfect_lie" -> {
                List<LivingEntity> seen = around(c, p.position(), 12 * r);
                for (LivingEntity e : seen) {
                    effect(e, MobEffects.NAUSEA, 200, 0);
                    if (e instanceof Mob m && seen.size() > 1) {
                        LivingEntity other = seen.get((seen.indexOf(e) + 1) % seen.size());
                        m.setTarget(other);
                    }
                }
                ring(c, ParticleTypes.WITCH, p.position(), 12 * r);
                return 300;
            }
            case "cherry_storm" -> {
                Vec3 at = lookPoint(c, 16 * r);
                around(c, at, 3 * r).forEach(e -> hit(c, e, c.dmg() * 1.5));
                burst(c, ParticleTypes.CHERRY_LEAVES, at, 3 * r, 120);
                beam(c, ParticleTypes.CHERRY_LEAVES, p.getEyePosition().distanceTo(at));
            }
            case "snow_dragon" -> {
                line(c, 16 * r, 1.5).forEach(e -> {
                    hit(c, e, c.dmg() * 1.5);
                    freeze(e, 80);
                });
                beam(c, ParticleTypes.SNOWFLAKE, 16 * r);
                beam(c, ParticleTypes.ITEM_SNOWBALL, 16 * r);
                sound(c, SoundEvents.PLAYER_HURT_FREEZE, 0.6f);
            }
            case "unchained_beast" -> cleave(c, 4 * r, 2.0);
            case "playful_shadow" -> {
                LivingEntity t = lookTarget(c, 16 * r);
                if (t != null) {
                    Vec3 behind = t.position().add(t.getLookAngle().multiply(1, 0, 1).normalize().scale(-1.5));
                    burst(c, ParticleTypes.LARGE_SMOKE, p.position(), 1, 20);
                    p.teleportTo(behind.x, t.getY(), behind.z);
                    start(p, "next_hit", 100);
                    burst(c, ParticleTypes.LARGE_SMOKE, behind, 1, 20);
                }
            }
            case "crimson_healer" -> {
                SoulMinions.summon(p, EntityType.ALLAY, p.position().add(0, 1.5, 0), 200, 0, a -> { });
                for (LivingEntity a : allies(c, 8)) {
                    a.heal(8);
                    effect(a, MobEffects.REGENERATION, 100, 1);
                }
                burst(c, ParticleTypes.HEART, p.position(), 6, 30);
                return 400;
            }
            case "shadow_inventor" -> {
                Vec3 at = lookPoint(c, 16 * r);
                around(c, at, 2.5).forEach(e -> hit(c, e, c.dmg() * 1.6));
                burst(c, new DustParticleOptions(0xC62828, 2f), at, 2.5, 60);
                burst(c, ParticleTypes.EXPLOSION, at, 1, 2);
                for (LivingEntity a : allies(c, 6)) {
                    effect(a, MobEffects.RESISTANCE, 120, 1);
                }
            }
            case "scorpion_sting" -> {
                Vec3 dash = p.getLookAngle().multiply(1, 0.2, 1).normalize().scale(1.8);
                p.setDeltaMovement(dash);
                p.hurtMarked = true;
                burst(c, ParticleTypes.SMOKE, p.position(), 1, 20);
                return 60;
            }
            case "decay_experiment" -> {
                LivingEntity t = lookTarget(c, 16 * r);
                if (t != null) {
                    effect(t, MobEffects.SLOWNESS, 100, 9);
                    effect(t, MobEffects.WEAKNESS, 100, 1);
                    effect(t, MobEffects.MINING_FATIGUE, 100, 2);
                    effect(t, MobEffects.POISON, 160, 1);
                    burst(c, ParticleTypes.ITEM_SLIME, t.position(), 1, 30);
                }
            }
            case "moonlight_dance" -> {
                if (on(p, "second_dance")) {
                    stop(p, "second_dance");
                    line(c, 16 * r, 2).forEach(e -> {
                        hit(c, e, c.dmg() * 1.8);
                        freeze(e, 100);
                    });
                    beam(c, ParticleTypes.SNOWFLAKE, 16 * r);
                    beam(c, ParticleTypes.END_ROD, 16 * r);
                } else {
                    Vec3 at = lookPoint(c, 16 * r);
                    around(c, at, 4).forEach(e -> {
                        hit(c, e, c.dmg() * 1.2);
                        freeze(e, 100);
                    });
                    ring(c, ParticleTypes.SNOWFLAKE, at, 4);
                    ring(c, ParticleTypes.END_ROD, at, 3);
                    start(p, "second_dance", 100);
                    return 15;
                }
            }
            case "bone_serpent" -> {
                line(c, 8 * r, 1.2).forEach(e -> hit(c, e, c.dmg() * 1.4));
                beam(c, ParticleTypes.CRIT, 8 * r);
                sound(c, SoundEvents.CHAIN_BREAK, 0.8f);
            }
            case "lethal_reach" -> {
                line(c, 30 * r, 0.8).forEach(e -> hit(c, e, c.dmg() * 1.4));
                beam(c, ParticleTypes.CRIT, 30 * r);
                sound(c, SoundEvents.TRIDENT_RIPTIDE_1, 1.4f);
            }
            case "wolf_guardian" -> {
                SoulMinions.dismiss(p, EntityType.IRON_GOLEM);
                var at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(2));
                SoulMinions.summon(p, EntityType.IRON_GOLEM, at, 600, 0, g -> {
                    g.setPlayerCreated(true);
                    g.getAttribute(Attributes.SCALE).setBaseValue(1.3);
                });
                return 900;
            }
            default -> { }
        }
        return 120;
    }

    private static void cleave(Ctx c, double r, double mult) {
        for (LivingEntity e : cone(c, r, 0.3)) {
            hit(c, e, c.dmg() * mult * (e.getMaxHealth() >= 60 ? 2 : 1));
        }
        burst(c, ParticleTypes.SWEEP_ATTACK, c.player().position().add(c.player().getLookAngle().scale(2)), r / 2, 8);
        burst(c, new DustParticleOptions(0x8B0000, 2f), c.player().position().add(c.player().getLookAngle().scale(2)), r / 2, 40);
        sound(c, SoundEvents.PLAYER_ATTACK_STRONG, 0.5f);
    }

    // ------------------------------------------------------------------ stage 7: Bankai

    /** Releasing Bankai. */
    static void bankaiStart(Ctx c) {
        ServerPlayer p = c.player();
        int ticks = (int) (Soul.get(p).bankaiEnd() - c.level().getGameTime());
        burst(c, dust(c, 2.5f), p.position(), 3, 120);
        burst(c, ParticleTypes.SOUL_FIRE_FLAME, p.position(), 3, 60);
        switch (c.path()) {
            case "black_crescent" -> {
                effect(p, MobEffects.SPEED, ticks, 2);
                effect(p, MobEffects.HASTE, ticks, 1);
            }
            case "ash_sovereign" -> effect(p, MobEffects.FIRE_RESISTANCE, ticks, 0);
            case "perfect_lie", "unchained_beast" -> effect(p, MobEffects.STRENGTH, ticks, 0);
            case "snow_dragon" -> {
                effect(p, MobEffects.SLOW_FALLING, ticks, 0);
                effect(p, MobEffects.SPEED, ticks, 0);
            }
            case "decay_experiment" -> SoulMinions.summon(p, EntityType.SILVERFISH, p.position(), ticks, 0, s -> {
                s.getAttribute(Attributes.SCALE).setBaseValue(3.0);
                s.getAttribute(Attributes.MAX_HEALTH).setBaseValue(80);
                s.setHealth(80);
                s.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
            });
            case "wolf_guardian" -> {
                SoulMinions.dismiss(p, EntityType.IRON_GOLEM);
                var at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(3));
                SoulMinions.summon(p, EntityType.IRON_GOLEM, at, ticks, 0.25f, g -> {
                    g.setPlayerCreated(true);
                    g.getAttribute(Attributes.SCALE).setBaseValue(2.2);
                    g.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300);
                    g.setHealth(300);
                    g.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(30);
                });
            }
            case "moonlight_dance" -> effect(p, MobEffects.RESISTANCE, ticks, 1);
            case "shadow_inventor", "crimson_healer" -> effect(p, MobEffects.REGENERATION, ticks, 0);
            case "scorpion_sting", "lethal_reach" -> effect(p, MobEffects.SPEED, ticks, 1);
            case "bone_serpent" -> effect(p, MobEffects.STRENGTH, ticks, 0);
            case "cherry_storm", "playful_shadow" -> effect(p, MobEffects.SPEED, ticks, 0);
            default -> { }
        }
    }

    /** Every second of Bankai. */
    static void bankaiTick(Ctx c) {
        ServerPlayer p = c.player();
        burst(c, dust(c, 1.5f), p.position(), 1, 10);
        switch (c.path()) {
            case "ash_sovereign" -> around(c, p.position(), 5).forEach(e -> {
                hit(c, e, c.dmg() * 0.2);
                e.igniteForSeconds(3);
            });
            case "cherry_storm" -> {
                around(c, p.position(), 6).forEach(e -> hit(c, e, c.dmg() * 0.4));
                ring(c, ParticleTypes.CHERRY_LEAVES, p.position(), 6);
            }
            case "unchained_beast" -> burst(c, new DustParticleOptions(0xFF1010, 2f), p.position(), 1, 20);
            case "moonlight_dance" -> effect(p, MobEffects.SLOWNESS, 30, 0);   // absolute zero slows you too
            case "snow_dragon" -> {
                if (on(p, "unlimited")) {
                    Soul.set(p, Soul.get(p).withBankai(c.level().getGameTime() + 60, Soul.get(p).bankaiReady() + 20));
                }
            }
            default -> { }
        }
    }

    /** The Bankai ability (use during Bankai). Returns the cooldown in ticks. */
    public static int bankaiAbility(Ctx c) {
        ServerPlayer p = c.player();
        switch (c.path()) {
            case "black_crescent" -> {
                double mult = on(p, "beast") ? 3.0 : 2.0;
                line(c, 20, 2.0).forEach(e -> hit(c, e, c.dmg() * mult));
                beam(c, on(p, "beast") ? new DustParticleOptions(0xD00000, 2f) : ParticleTypes.SQUID_INK, 20);
                beam(c, ParticleTypes.SWEEP_ATTACK, 20);
                sound(c, SoundEvents.PLAYER_ATTACK_SWEEP, 0.4f);
                return 40;
            }
            case "ash_sovereign" -> {
                cone(c, 14, 0.5).forEach(e -> {
                    hit(c, e, c.dmg() * 2);
                    e.igniteForSeconds(8);
                });
                beam(c, ParticleTypes.FLAME, 14);
                burst(c, ParticleTypes.LAVA, p.position().add(p.getLookAngle().scale(6)), 5, 40);
            }
            case "perfect_lie" -> {
                LivingEntity t = lookTarget(c, 24);
                if (t != null) {
                    effect(t, MobEffects.SLOWNESS, 60, 9);
                    effect(t, MobEffects.BLINDNESS, 60, 0);
                    burst(c, ParticleTypes.SQUID_INK, t.position(), 1.5, 80);
                    double dmg = c.dmg() * 3;
                    SoulMinions.later(c.level(), 40, () -> {
                        if (t.isAlive()) {
                            hit(c, t, dmg);
                            burst(c, ParticleTypes.SQUID_INK, t.position(), 1.5, 60);
                        }
                    });
                }
                return 100;
            }
            case "cherry_storm" -> {
                around(c, p.position(), 8).forEach(e -> hit(c, e, c.dmg() * 1.5));
                burst(c, ParticleTypes.CHERRY_LEAVES, p.position(), 8, 200);
            }
            case "snow_dragon" -> {
                line(c, 20, 2).forEach(e -> {
                    hit(c, e, c.dmg() * 2);
                    freeze(e, 120);
                });
                beam(c, ParticleTypes.SNOWFLAKE, 20);
                beam(c, ParticleTypes.ITEM_SNOWBALL, 20);
            }
            case "unchained_beast" -> cleave(c, 5, 2.5);
            case "playful_shadow" -> {
                LivingEntity t = lookTarget(c, 20);
                if (t != null) {
                    mark(p, "bound", t, 20 * 60);
                    effect(t, MobEffects.SLOWNESS, 20 * 30, 2);
                    effect(t, MobEffects.GLOWING, 20 * 30, 0);
                    burst(c, ParticleTypes.ENCHANT, t.position(), 1, 40);
                }
            }
            case "crimson_healer" -> {
                around(c, p.position(), 6).forEach(e -> {
                    hit(c, e, c.dmg());
                    effect(e, MobEffects.WITHER, 100, 1);
                });
                p.heal(6);
                burst(c, new DustParticleOptions(0x8B0000, 2f), p.position(), 6, 80);
            }
            case "shadow_inventor" -> {
                for (LivingEntity a : allies(c, 8)) {
                    a.heal(10);
                }
                LivingEntity t = lookTarget(c, 16);
                if (t != null) {
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        ItemStack s = t.getItemBySlot(slot);
                        if (s.isDamageableItem()) {
                            s.setDamageValue(Math.min(s.getMaxDamage() - 1, s.getDamageValue() + s.getMaxDamage() / 4));
                        }
                    }
                    effect(t, MobEffects.WEAKNESS, 200, 1);
                    burst(c, ParticleTypes.ENCHANTED_HIT, t.position(), 1, 40);
                }
                burst(c, ParticleTypes.HAPPY_VILLAGER, p.position(), 8, 40);
                return 200;
            }
            case "scorpion_sting" -> {
                Vec3 at = lookPoint(c, 48);
                c.level().explode(p, at.x, at.y, at.z, 3.0f, Level.ExplosionInteraction.NONE);
                around(c, at, 4).forEach(e -> hit(c, e, c.dmg() * 2));
                beam(c, ParticleTypes.FIREWORK, p.getEyePosition().distanceTo(at));
                return 100;
            }
            case "decay_experiment" -> {
                Vec3 at = lookPoint(c, 16);
                var cloud = new net.minecraft.world.entity.AreaEffectCloud(c.level(), at.x, at.y, at.z);
                cloud.setOwner(p);
                cloud.setRadius(4);
                cloud.setDuration(200);
                cloud.setWaitTime(0);
                cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
                cloud.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
                c.level().addFreshEntity(cloud);
                return 200;
            }
            case "moonlight_dance" -> {
                around(c, p.position(), 6).forEach(e -> {
                    hit(c, e, c.dmg() * 1.5);
                    freeze(e, 140);
                });
                ring(c, ParticleTypes.SNOWFLAKE, p.position(), 6);
                ring(c, ParticleTypes.END_ROD, p.position(), 4);
            }
            case "bone_serpent" -> {
                line(c, 24, 2.5).forEach(e -> {
                    hit(c, e, c.dmg() * 2.5);
                    e.igniteForSeconds(4);
                });
                beam(c, ParticleTypes.FLAME, 24);
                beam(c, ParticleTypes.END_ROD, 24);
                sound(c, SoundEvents.ENDER_DRAGON_SHOOT, 0.8f);
                return 100;
            }
            case "lethal_reach" -> {
                for (LivingEntity e : line(c, 32, 1.0)) {
                    hit(c, e, c.dmg() * 1.6);
                    effect(e, MobEffects.POISON, 100, 0);
                    mark(p, "shard", e, 20 * 300);
                }
                beam(c, ParticleTypes.CRIT, 32);
                sound(c, SoundEvents.TRIDENT_RIPTIDE_3, 1.6f);
            }
            case "wolf_guardian" -> smash(c, lookPoint(c, 16), 4, c.dmg() * 2);
            default -> { }
        }
        return 60;
    }

    // ------------------------------------------------------------------ stage 10: the Absolute Form

    public static void ultimate(Ctx c) {
        ServerPlayer p = c.player();
        burst(c, dust(c, 3f), p.position(), 4, 200);
        sound(c, SoundEvents.WITHER_SPAWN, 0.5f);
        switch (c.path()) {
            case "black_crescent" -> {
                start(p, "beast", 600);
                effect(p, MobEffects.STRENGTH, 600, 2);
                effect(p, MobEffects.RESISTANCE, 600, 0);
            }
            case "ash_sovereign" -> {
                line(c, 40, 3).forEach(e -> {
                    hit(c, e, c.dmg() * 5);
                    e.igniteForSeconds(10);
                });
                beam(c, ParticleTypes.FLAME, 40);
                beam(c, ParticleTypes.LAVA, 40);
                beam(c, ParticleTypes.EXPLOSION, 40);
            }
            case "perfect_lie" -> {
                start(p, "cheat", 20 * 300);
                effect(p, MobEffects.REGENERATION, 20 * 300, 1);
                effect(p, MobEffects.SLOW_FALLING, 20 * 300, 0);
            }
            case "cherry_storm" -> {
                start(p, "white_blade", 1200);
                effect(p, MobEffects.STRENGTH, 1200, 1);
                effect(p, MobEffects.SLOW_FALLING, 1200, 0);
                effect(p, MobEffects.JUMP_BOOST, 1200, 1);
            }
            case "snow_dragon" -> start(p, "unlimited", 2400);
            case "unchained_beast" -> start(p, "limitless", 600);
            case "playful_shadow" -> {
                for (LivingEntity e : takeMarked(c, "bound", 48)) {
                    if (Soul.isBoss(e)) {
                        hit(c, e, e.getMaxHealth() * 0.3);
                    } else {
                        hit(c, e, e.getHealth() + 1000);
                    }
                    burst(c, ParticleTypes.ENCHANT, e.position(), 1, 60);
                }
            }
            case "crimson_healer" -> start(p, "first_beast", 1200);
            case "shadow_inventor" -> {
                for (LivingEntity e : around(c, p.position(), 12)) {
                    effect(e, MobEffects.SLOWNESS, 120, 9);
                    effect(e, MobEffects.MINING_FATIGUE, 120, 2);
                    effect(e, MobEffects.WITHER, 120, 0);
                    hit(c, e, c.dmg());
                }
                ring(c, new DustParticleOptions(0xC62828, 2f), p.position(), 12);
                ring(c, ParticleTypes.CRIT, p.position(), 8);
            }
            case "scorpion_sting" -> {
                start(p, "death_marks", 600);
                effect(p, MobEffects.INVISIBILITY, 600, 0);
            }
            case "decay_experiment" -> {
                start(p, "adaptive", 1200);
                start(p, "cheat", 1200);
            }
            case "moonlight_dance" -> {
                for (LivingEntity e : around(c, p.position(), 12)) {
                    hit(c, e, c.dmg() * 4);
                    freeze(e, 200);
                }
                ring(c, ParticleTypes.SNOWFLAKE, p.position(), 12);
                ring(c, ParticleTypes.END_ROD, p.position(), 9);
                ring(c, ParticleTypes.SNOWFLAKE, p.position(), 6);
            }
            case "bone_serpent" -> {
                start(p, "serpent_arms", 1200);
                effect(p, MobEffects.STRENGTH, 1200, 2);
            }
            case "lethal_reach" -> {
                for (LivingEntity e : takeMarked(c, "shard", 64)) {
                    hit(c, e, c.dmg() * 6);
                    burst(c, ParticleTypes.EXPLOSION, e.position(), 1, 3);
                }
            }
            case "wolf_guardian" -> {
                start(p, "immortal", 400);
                SoulMinions.later(c.level(), 400, () -> {
                    effect(p, MobEffects.WEAKNESS, 1200, 1);
                    effect(p, MobEffects.SLOWNESS, 1200, 1);
                });
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ hits, wounds, death

    /** The owner strikes with the blade (melee): the blade's bonus and the path's on-hit effects. */
    static float onHit(ServerPlayer p, LivingEntity target, float amount) {
        if (abilityHit) {
            return amount;
        }
        float out = Soul.bladeBonus(p, amount);
        boolean bankai = Soul.bankai(p);
        int stage = Soul.get(p).stage();
        float missing = 1 - p.getHealth() / p.getMaxHealth();
        switch (Soul.get(p).path()) {
            case "black_crescent" -> {
                if (on(p, "beast")) {
                    out *= 1.3f;
                }
            }
            case "ash_sovereign" -> {
                if (on(p, "fire_blade") || bankai) {
                    target.igniteForSeconds(5);
                }
            }
            case "unchained_beast" -> {
                if (on(p, "rage")) {
                    out *= 1 + missing;
                }
                if (bankai) {
                    out *= 1 + 1.5f * missing;
                }
                if (on(p, "limitless")) {
                    out *= 3;
                }
            }
            case "playful_shadow" -> {
                if (on(p, "twin")) {
                    out *= 1.5f;
                }
                if (on(p, "next_hit")) {
                    stop(p, "next_hit");
                    out *= 1.5f;
                }
            }
            case "crimson_healer" -> {
                if (on(p, "first_beast")) {
                    p.heal(out);
                } else if (bankai) {
                    effect(target, MobEffects.WITHER, 100, 1);
                    p.heal(out * 0.3f);
                } else if (on(p, "lifesteal")) {
                    p.heal(out * 0.2f);
                }
            }
            case "scorpion_sting" -> {
                if (stage >= 5) {
                    if (on(p, "death_marks") || marked(p, "sting", target)) {
                        unmark(p, "sting", target);
                        out = Soul.isBoss(target) ? out + target.getMaxHealth() * 0.2f : target.getHealth() + 1000;
                        target.level().playSound(null, target.blockPosition(), SoundEvents.BEE_STING, SoundSource.PLAYERS, 1.2f, 0.5f);
                    } else {
                        mark(p, "sting", target, 200);
                        if (target.level() instanceof ServerLevel level) {
                            level.sendParticles(new DustParticleOptions(0xD4AF37, 1.5f), target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 15, 0.3, 0.3, 0.3, 0);
                        }
                    }
                }
            }
            case "decay_experiment" -> {
                if (on(p, "adaptive")) {
                    effect(target, MobEffects.WITHER, 100, 1);
                    effect(target, MobEffects.POISON, 100, 1);
                } else if (on(p, "poison_blade")) {
                    effect(target, MobEffects.POISON, 100, 1);
                    effect(target, MobEffects.SLOWNESS, 60, 1);
                }
            }
            case "moonlight_dance" -> {
                if (bankai) {
                    freeze(target, 40);
                    effect(target, MobEffects.SLOWNESS, 40, 9);
                }
            }
            case "snow_dragon" -> {
                if (on(p, "unlimited")) {
                    freeze(target, 40);
                    effect(target, MobEffects.SLOWNESS, 40, 9);
                }
            }
            case "cherry_storm" -> {
                if (on(p, "white_blade")) {
                    out += (float) ctx(p).dmg() * 0.5f;
                }
            }
            case "bone_serpent" -> {
                if (on(p, "serpent_arms")) {
                    target.igniteForSeconds(4);
                    target.knockback(1.5, p.getX() - target.getX(), p.getZ() - target.getZ());
                    if (target.level() instanceof ServerLevel level) {
                        level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
                    }
                }
            }
            default -> { }
        }
        return out;
    }

    /** The owner is hurt: the armor passives and auras that answer it. */
    static void onHurt(ServerPlayer p, LivingIncomingDamageEvent event) {
        SoulData data = Soul.get(p);
        boolean armor = data.stage() >= 6 && Soul.piecesWorn(p) == 4;
        boolean aura = data.stage() >= 9;
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity l && l != p ? l : null;
        switch (data.path()) {
            case "perfect_lie" -> {
                if (armor && attacker != null && p.getRandom().nextFloat() < 0.15f) {
                    event.setCanceled(true);
                    ((ServerLevel) p.level()).sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1, p.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
                    return;
                }
            }
            case "ash_sovereign" -> {
                if (aura && attacker != null) {
                    attacker.igniteForSeconds(4);
                }
            }
            case "snow_dragon" -> {
                if (armor && attacker != null) {
                    effect(attacker, MobEffects.SLOWNESS, 60, 1);
                }
            }
            case "shadow_inventor" -> {
                if (armor && event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
                    event.setAmount(event.getAmount() * 0.67f);
                }
            }
            case "scorpion_sting" -> {
                if (armor && event.getSource().is(DamageTypeTags.IS_FALL)) {
                    event.setAmount(event.getAmount() * 0.5f);
                }
            }
            case "decay_experiment" -> {
                if (aura && attacker != null) {
                    effect(attacker, MobEffects.POISON, 80, 0);
                }
            }
            case "playful_shadow" -> {
                if (Soul.bankai(p) && !abilityHit) {
                    // The drama: bound foes share your wounds.
                    Ctx c = ctx(p);
                    float shared = event.getAmount();
                    Map<UUID, Long> bound = MARKS.getOrDefault(p.getUUID(), Map.of()).getOrDefault("bound", Map.of());
                    for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(32), e -> bound.containsKey(e.getUUID()))) {
                        if (marked(p, "bound", e)) {
                            hit(c, e, shared);
                        }
                    }
                }
            }
            case "wolf_guardian" -> {
                if (on(p, "immortal") && event.getAmount() >= p.getHealth()) {
                    event.setAmount(Math.max(0, p.getHealth() - 1));
                }
            }
            default -> { }
        }
    }

    /** A deadly blow: the Butterfly (Perfect Lie) or liquefying (Decay Experiment) brings you back, once. */
    static boolean saveFromDeath(ServerPlayer p) {
        if (on(p, "immortal")) {
            p.setHealth(1);
            return true;
        }
        if (!on(p, "cheat")) {
            return false;
        }
        stop(p, "cheat");
        p.setHealth(p.getMaxHealth() / 2);
        p.removeEffect(MobEffects.WITHER);
        p.removeEffect(MobEffects.POISON);
        effect(p, MobEffects.REGENERATION, 200, 2);
        effect(p, MobEffects.RESISTANCE, 100, 2);
        p.sendSystemMessage(Component.translatable("pipican.soul.saved").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1, 0.5, 0.3);
        return true;
    }

    /** A creature the owner killed: Ash Sovereign's Bankai raises it as a skeleton of its army. */
    static void onKill(ServerPlayer p, LivingEntity dead) {
        if (Soul.bankai(p) && Soul.get(p).path().equals("ash_sovereign") && dead instanceof Mob && !SoulMinions.isSummoned(dead)) {
            int ticks = (int) Math.min(600, Soul.get(p).bankaiEnd() - p.level().getGameTime() + 200);
            SoulMinions.summon(p, EntityType.SKELETON, dead.position(), ticks, 0, s -> {
                s.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.STONE_SWORD));
                s.setDropChance(EquipmentSlot.MAINHAND, 0);
            });
        }
    }

    // ------------------------------------------------------------------ every second

    public static void tick(ServerPlayer p) {
        SoulData data = Soul.get(p);
        Ctx c = ctx(p);
        int pieces = Soul.piecesWorn(p);
        boolean armor = data.stage() >= 6 && pieces == 4;
        boolean aura = data.stage() >= 9 && Soul.carriesBlade(p);

        // Stage 2: sense the souls around you (sneaking with the blade).
        if (data.stage() >= 2 && p.isShiftKeyDown() && Soul.holdsBlade(p)) {
            for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(Soul.RANGE), e -> e != p && Soul.soulValue(e) > 0)) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false));
            }
        }
        // The armor grows with the stages: up to +12 armor and +12 toughness at stage 10 with all four pieces.
        double perPiece = 0.3 * data.stage();
        modifier(p, Attributes.ARMOR, ARMOR_ID, pieces * perPiece, AttributeModifier.Operation.ADD_VALUE);
        modifier(p, Attributes.ARMOR_TOUGHNESS, TOUGH_ID, pieces * (perPiece + (c.bankai() ? 1 : 0)), AttributeModifier.Operation.ADD_VALUE);
        modifier(p, Attributes.ARMOR, LIMITLESS_ID, on(p, "limitless") ? -1 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        double reach = (on(p, "reach") ? 3 : 0) + (aura && data.path().equals("bone_serpent") ? 1 : 0);
        modifier(p, Attributes.ENTITY_INTERACTION_RANGE, REACH_ID, reach, AttributeModifier.Operation.ADD_VALUE);
        double knock = (armor && (data.path().equals("cherry_storm") || data.path().equals("bone_serpent")) ? 0.5 : 0)
                + (aura && data.path().equals("wolf_guardian") ? 0.5 : 0);
        modifier(p, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, knock, AttributeModifier.Operation.ADD_VALUE);
        if (on(p, "limitless")) {
            burst(c, new DustParticleOptions(0xFF0000, 2.5f), p.position(), 1, 20);
        }

        if (armor) {
            switch (data.path()) {
                case "black_crescent", "lethal_reach", "scorpion_sting" -> quiet(p, MobEffects.SPEED, 0);
                case "ash_sovereign" -> quiet(p, MobEffects.FIRE_RESISTANCE, 0);
                case "unchained_beast" -> {
                    if (p.getHealth() < p.getMaxHealth() / 2) {
                        quiet(p, MobEffects.STRENGTH, 0);
                    }
                }
                case "playful_shadow" -> {
                    if (p.isShiftKeyDown() && c.level().getMaxLocalRawBrightness(p.blockPosition()) < 8) {
                        quiet(p, MobEffects.INVISIBILITY, 0);
                    }
                }
                case "crimson_healer" -> quiet(p, MobEffects.REGENERATION, 0);
                case "snow_dragon", "moonlight_dance" -> p.setTicksFrozen(0);
                case "decay_experiment" -> p.removeEffect(MobEffects.POISON);
                case "wolf_guardian" -> quiet(p, MobEffects.RESISTANCE, 0);
                default -> { }
            }
        }
        if (aura) {
            c.level().sendParticles(dust(c, 1.0f), p.getX(), p.getY() + 1, p.getZ(), 6, 0.5, 0.8, 0.5, 0);
            switch (data.path()) {
                case "black_crescent", "lethal_reach" -> quiet(p, MobEffects.HASTE, 0);
                case "perfect_lie" -> quiet(p, MobEffects.REGENERATION, 0);
                case "cherry_storm", "shadow_inventor" -> {
                    if (!p.hasEffect(MobEffects.ABSORPTION)) {
                        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 0, true, false));
                    }
                }
                case "snow_dragon", "playful_shadow", "moonlight_dance" -> around(c, p.position(), 6).forEach(e -> effect(e, MobEffects.SLOWNESS, 40, 0));
                case "unchained_beast" -> around(c, p.position(), 8).forEach(e -> effect(e, MobEffects.WEAKNESS, 40, 0));
                case "crimson_healer" -> allies(c, 8).forEach(a -> a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false)));
                case "scorpion_sting" -> {
                    if (p.isShiftKeyDown()) {
                        quiet(p, MobEffects.INVISIBILITY, 0);
                    }
                }
                case "wolf_guardian" -> quiet(p, MobEffects.RESISTANCE, 0);
                default -> { }
            }
        }
        if (c.bankai()) {
            start(p, "in_bankai", 40);
            bankaiTick(c);
        } else if (STATES.getOrDefault(p.getUUID(), Map.of()).containsKey("in_bankai")) {
            stop(p, "in_bankai");
            p.sendSystemMessage(Component.translatable("pipican.soul.bankai_end").withStyle(ChatFormatting.GRAY));
            Soul.restampAll(p);
        }
    }

    private static void quiet(LivingEntity e, Holder<MobEffect> effect, int amp) {
        e.addEffect(new MobEffectInstance(effect, 60, amp, true, false));
    }

    private static void modifier(Player p, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, net.minecraft.resources.Identifier id,
                                 double amount, AttributeModifier.Operation op) {
        var inst = p.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        if (amount == 0) {
            inst.removeModifier(id);
        } else {
            inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
        }
    }
}
