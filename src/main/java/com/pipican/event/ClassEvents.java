package com.pipican.event;

import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.common.Tags;
import com.pipican.ability.Charged;
import com.pipican.ability.Scrutiny;
import com.pipican.ability.Strikes;
import com.pipican.ability.Auras;
import com.pipican.mastery.SkillAction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.Map;
import com.pipican.ability.Bleeds;
import com.pipican.ability.Mounts;
import com.pipican.ability.Movement;
import com.pipican.ability.Boosts;
import com.pipican.rpgclass.SpecialStat;
import com.pipican.ability.Combat;
import com.pipican.Pipican;
import com.pipican.ability.AbilityService;
import com.pipican.ability.Bonds;
import com.pipican.ability.Empowers;
import com.pipican.ability.HitShields;
import com.pipican.ability.Marks;
import com.pipican.ability.MobControl;
import com.pipican.ability.Optimize;
import com.pipican.ability.Reactions;
import com.pipican.ability.Scry;
import com.pipican.ability.Summons;
import com.pipican.core.PipicanConfig;
import com.pipican.effect.type.ReactionEffect;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassService;
import com.pipican.player.ClassState;
import com.pipican.player.PipicanAttachments;
import com.pipican.player.PlayerClassData;
import com.pipican.rpgclass.RpgClass;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Game-event glue for the class system. Everything here is thin: it decides <i>when</i> to act and
 * delegates the <i>what</i> to {@link ClassService} / {@link ClassEffectApplier}.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class ClassEvents {

    /** The mob each player just crit (set by CriticalHitEvent, read when the damage lands). */
    private static final Map<UUID, Crit> CRITS = new ConcurrentHashMap<>();

    private record Crit(net.minecraft.world.entity.Entity target, long tick) {}

    private ClassEvents() {}

    // ------------------------------------------------------------------ lifecycle -> refresh

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassService.changed(player);
            if (PipicanConfig.openSelectionOnJoin()) {
                ClassService.promptSelection(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassEffectApplier.forget(player);
            Reactions.forget(player);
            Empowers.forget(player);
            Boosts.forget(player);
            Combat.forget(player);
            Movement.forget(player);
            Auras.forget(player);
            Scrutiny.forget(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassEffectApplier.refresh(player);
            player.setHealth(player.getMaxHealth());
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassEffectApplier.refresh(player);
        }
    }

    /** /reload can change item tags, which changes affinities and trinket slot rules. */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            event.getRelevantPlayers().forEach(ClassService::changed);
        }
    }

    // ------------------------------------------------------------------ equipment rules

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ClassEffectApplier.refresh(player);
        warnAboutAffinity(player, event.getSlot(), event.getTo());
    }

    /** Tells the player (action bar) when they equip something their class is bad with. */
    private static void warnAboutAffinity(ServerPlayer player, EquipmentSlot slot, ItemStack stack) {
        // Skip the first seconds after joining/respawning: equipment "changes" then are just syncing.
        if (stack.isEmpty() || slot == EquipmentSlot.OFFHAND || player.tickCount < 40) {
            return;
        }
        Optional<RpgClass> rpgClass = ClassService.activeClass(player);
        if (rpgClass.isEmpty()) {
            return;
        }
        var affinity = rpgClass.get().affinities().resolve(stack);
        if (affinity.ordinal() > com.pipican.rpgclass.Affinity.NEUTRAL.ordinal()) {
            player.sendOverlayMessage(Component.translatable("pipican.affinity.warning",
                    Component.translatable(affinity.translationKey()).withStyle(affinity.color()), stack.getHoverName()));
        }
    }

    // ------------------------------------------------------------------ damage

    /** Applies damage_taken / damage_dealt effects and weapon affinity. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // Twist Fate / Intercept: the next hit misses, or lands on the guard instead.
        if (!event.getEntity().level().isClientSide()) {
            var shield = HitShields.take(event.getEntity());
            if (shield.isPresent()) {
                event.setCanceled(true);
                shield.get().guard().map(id -> event.getEntity().level().getServer().getPlayerList().getPlayer(id))
                        .ifPresent(guard -> guard.hurtServer(guard.level(), event.getSource(), event.getAmount()));
                return;
            }
        }
        float amount = event.getAmount();
        LivingEntity source = event.getSource().getEntity() instanceof LivingEntity l ? l : null;
        if (event.getEntity() instanceof ServerPlayer victim) {
            ClassState state = ClassEffectApplier.state(victim);
            // Decoys and premonitions: "an attack against you hits a decoy instead".
            if (source != null && (Reactions.fire(victim, source, event.getSource().getWeaponItem(), ReactionEffect.Event.ATTACKED)
                    || event.getSource().getDirectEntity() == source
                    && Reactions.fire(victim, source, event.getSource().getWeaponItem(), ReactionEffect.Event.ATTACKED_MELEE))) {
                victim.sendOverlayMessage(Component.translatable("pipican.dodged"));
                event.setCanceled(true);
                return;
            }
            // Dodge: only hits from someone (mobs, players, their arrows), never falls or fire.
            if (source != null) {
                boolean melee = event.getSource().getDirectEntity() == source;
                double chance = state.dodgeChance() + (melee ? state.meleeDodgeChance() : state.rangedDodgeChance());
                if (chance > 0 && victim.getRandom().nextDouble() < chance) {
                    victim.sendOverlayMessage(Component.translatable("pipican.dodged"));
                    MasteryEvents.did(victim, SkillAction.Did.Kind.DODGE);
                    event.setCanceled(true);
                    return;
                }
            }
            amount *= (float) ClassState.multiplier(state.damageTaken(), event.getSource(), source, victim);
            // Zeal: "when a hit would bring you below 2 hearts".
            if (amount > 0 && victim.getHealth() - amount < 4 && victim.getHealth() > 0
                    && Reactions.fire(victim, source, event.getSource().getWeaponItem(), ReactionEffect.Event.LOW_HEALTH)) {
                victim.sendOverlayMessage(Component.translatable("pipican.shrugged"));
                event.setCanceled(true);
                return;
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            ClassState state = ClassEffectApplier.state(attacker);
            amount *= (float) ClassState.multiplier(state.damageDealt(), event.getSource(), event.getEntity(), attacker);
            ItemStack weapon = event.getSource().getWeaponItem();
            if (weapon != null && !weapon.isEmpty() && state.rpgClass().isPresent()) {
                amount *= (float) state.rpgClass().get().affinities().resolve(weapon)
                        .atLeastNeutral(state.chance(com.pipican.effect.type.BonusEffect.Kind.AFFINITY_FLOOR) > 0).tuning().damage();
            }
            // Ability marks on the target and "your next hit" buffs.
            amount *= (float) Marks.multiplier(attacker, event.getEntity(), event.getSource());
            amount = Empowers.apply(attacker, event.getEntity(), amount);
        }
        if (amount != event.getAmount()) {
            event.setAmount(amount);
        }
    }

    // ------------------------------------------------------------------ class XP, death

    /** "When you would die": a death reaction with {@code pipican:cheat_death} keeps the player alive. */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            LivingEntity killer = event.getSource().getEntity() instanceof LivingEntity l ? l : null;
            if (Reactions.fire(player, killer, ItemStack.EMPTY, ReactionEffect.Event.DEATH) && player.getHealth() > 0) {
                event.setCanceled(true);
                player.sendOverlayMessage(Component.translatable("pipican.cheated_death"));
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        // Killing a mob gives kill XP based on its max health: class XP and stat points.
        if (event.getSource().getEntity() instanceof ServerPlayer killer && !(event.getEntity() instanceof Player)) {
            long xp = Math.round(event.getEntity().getMaxHealth() * PipicanConfig.killXpPerMaxHealth());
            ClassService.addXp(killer, xp);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            Reactions.fire(killer, event.getEntity(), event.getSource().getWeaponItem(), ReactionEffect.Event.KILL);
        }
        Marks.forget(event.getEntity());
        // Running ability states end on death; so does Optimization.
        if (event.getEntity() instanceof ServerPlayer player) {
            AbilityService.clearActive(player);
            Boosts.clear(player);
            Empowers.forget(player);
            Optimize.onDeath(player);
        }
        // Trinkets drop on death like the rest of the inventory, unless keepInventory is on.
        if (event.getEntity() instanceof ServerPlayer player && !player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
            ClassService.dropTrinkets(player);
        }
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ClassState state = ClassEffectApplier.state(player);
        if (state.rpgClass().isEmpty()) {
            return;
        }
        // Special-stat resource regen (or drain, for bars with negative regen). While it changes, the
        // client is synced twice a second.
        PlayerClassData data = player.getData(PipicanAttachments.CLASS_DATA);
        double regen = state.resourceRegenPerTick();
        if (regen < 0 && Combat.inCombat(player) && state.rpgClass().flatMap(RpgClass::specialStat).flatMap(SpecialStat::resource)
                .map(r -> !r.drainsInCombat()).orElse(false)) {
            regen = 0;
        }
        if (state.resourceMax() > 0 && (regen > 0 && data.resource() < state.resourceMax() || regen < 0 && data.resource() > 0)) {
            data.setResource(Math.clamp(data.resource() + regen, 0, state.resourceMax()));
            if (player.tickCount % 10 == 0 || data.resource() >= state.resourceMax() || data.resource() <= 0) {
                player.syncData(PipicanAttachments.CLASS_DATA);
            }
        }
        AbilityService.tick(player);
        Movement.tick(player);
        Boosts.tick(player);
        if (player.tickCount % 20 == 0) {
            Mounts.tick(player);
            com.pipican.ability.Pets.tick(player);
            if (!com.pipican.player.ClassService.data(player).abilityActive().isEmpty()) {
                MasteryEvents.did(player, SkillAction.Did.Kind.ACTIVE_SECOND);
            }
            Auras.tick(player);
            if (Combat.inCombat(player)) {
                Reactions.fire(player, null, player.getMainHandItem(), ReactionEffect.Event.IN_COMBAT);
            }
            Reactions.fire(player, null, player.getMainHandItem(), ReactionEffect.Event.SECOND);
        }
        if (player.tickCount % 5 == 0) {
            Scrutiny.tick(player);
        }
        // Conditional effects ("while wet", "at night"...): rebuild bonuses when a condition flips.
        if (player.tickCount % 10 == 0 && ClassEffectApplier.recheckConditions(player)) {
            state = ClassEffectApplier.state(player);
        }
        if (player.tickCount % 40 == 0) {
            ClassEffectApplier.maintainMobEffects(player);
        }
    }

    // ------------------------------------------------------------------ ability summons

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Summons.tick(event.getServer());
        MobControl.tick(event.getServer());
        Scry.tick(event.getServer());
        Bleeds.tick(event.getServer());
    }

    /** Summons left over from before a restart are not loaded back in. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && Summons.isLeftover(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ ability reactions & control

    /** {@code pipican:on} reactions for hits dealt and taken (after armor, when the hit really landed). */
    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getInflictedDamage() <= 0) {
            return;
        }
        if (Bleeds.ticking() || event.getSource().is(DamageTypes.GENERIC_KILL)) {
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getEntity() != attacker) {
            Combat.mark(attacker);
            Reactions.onHit(attacker, event.getEntity(), event.getSource());
            Crit last = CRITS.remove(attacker.getUUID());
            boolean crit = last != null && last.target() == event.getEntity() && last.tick() == attacker.level().getGameTime();
            if (crit) {
                Reactions.fire(attacker, event.getEntity(), event.getSource().getWeaponItem(), ReactionEffect.Event.CRIT);
                MasteryEvents.did(attacker, new SkillAction.Did(SkillAction.Did.Kind.CRIT, attacker.getMainHandItem(), Optional.empty(),
                        Optional.of(event.getEntity()), event.getInflictedDamage()));
            }
            // Only real swings: magic from abilities also names the player as its direct entity.
            boolean swing = event.getSource().getDirectEntity() == attacker
                    && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK);
            if (swing) {
                Bleeds.onHit(attacker, event.getEntity(), ClassEffectApplier.state(attacker).bleed(), crit);
            }
            execute(attacker, event.getEntity());
            if (swing) {
                Strikes.after(attacker, event.getEntity(), event.getInflictedDamage());
            }
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity l ? l : null;
            if (attacker != null) {
                Combat.mark(victim);
            }
            Reactions.fire(victim, attacker, event.getSource().getWeaponItem(), ReactionEffect.Event.HURT);
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getBlocked() && event.getEntity() instanceof ServerPlayer player) {
            LivingEntity attacker = event.getDamageSource().getEntity() instanceof LivingEntity l ? l : null;
            Reactions.fire(player, attacker, player.getUseItem(), ReactionEffect.Event.BLOCK);
        }
    }

    /** Charmed, frightened and allied mobs may not pick some targets. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!MobControl.mayTarget(event.getEntity(), event.getNewAboutToBeSetTarget())) {
            event.setCanceled(true);
            return;
        }
        // pipican:ignored_by - "undead mobs ignore you unless you attack them".
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer player) {
            for (var ignore : ClassEffectApplier.state(player).ignoredBy()) {
                if (ignore.mobs().contains(event.getEntity().getType().builtInRegistryHolder())
                        && !(ignore.unlessAttacked() && (event.getEntity().getLastHurtByMob() == player || player.getLastHurtMob() == event.getEntity()))) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Bonds.onHeal(player, event.getAmount());
        }
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.wakeImmediately()) {
            AbilityService.onWake(player);
        }
    }

    /** Class crit bonuses: extra crit damage, and a chance to turn a normal hit into a crit. */
    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // Remember who was crit, for crit reactions and bleeding once the hit lands (LivingDamageEvent.Post).
        CRITS.remove(player.getUUID());
        var criticals = ClassEffectApplier.state(player).criticals();
        if (criticals.isEmpty()) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        double damage = 0;
        double chance = 0;
        for (ClassState.Critical c : criticals) {
            if ((c.weapons().isEmpty() || weapon.is(c.weapons().get()))
                    && (c.victim().isEmpty() || event.getTarget() instanceof LivingEntity target && c.victim().get().test(player, target))) {
                damage += c.damage();
                chance += c.chance();
            }
        }
        if (!event.isCriticalHit() && chance > 0 && player.getRandom().nextDouble() < chance) {
            event.setCriticalHit(true);
            event.setDamageMultiplier(Math.max(event.getDamageMultiplier(), 1.5f));
        }
        if (event.isCriticalHit() && damage != 0) {
            event.setDamageMultiplier((float) Math.max(1, event.getDamageMultiplier() * (1 + damage)));
        }
    }

    /** {@code pipican:execute}: the hit left the mob low enough to finish it. */
    private static void execute(ServerPlayer attacker, LivingEntity victim) {
        if (!victim.isAlive() || victim instanceof Player || victim.getType().builtInRegistryHolder().is(Tags.EntityTypes.BOSSES) || victim.getMaxHealth() <= 0) {
            return;
        }
        double fraction = victim.getHealth() / victim.getMaxHealth();
        for (ClassState.Execute e : ClassEffectApplier.state(attacker).executes()) {
            if (fraction < e.threshold() && (e.victims().isEmpty() || e.victims().get().contains(victim.getType().builtInRegistryHolder()))
                    && victim.level() instanceof ServerLevel level) {
                victim.hurtServer(level, level.damageSources().source(DamageTypes.GENERIC_KILL, attacker), Float.MAX_VALUE);
                MasteryEvents.did(attacker, new SkillAction.Did(SkillAction.Did.Kind.EXECUTE, attacker.getMainHandItem(), Optional.empty(), Optional.of(victim), 0));
                Reactions.fire(attacker, victim, attacker.getMainHandItem(), ReactionEffect.Event.EXECUTE);
                return;
            }
        }
    }

    /** Remembers how charged each attack was, for "fully charged hits" bonuses (the charge resets before damage). */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Charged.remember(player);
        }
    }

    /** Runs after {@link #onCriticalHit} so crits made by any mod count. */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void rememberCritical(CriticalHitEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.isCriticalHit()) {
            CRITS.put(player.getUUID(), new Crit(event.getTarget(), player.level().getGameTime()));
        }
    }

    /** Class immunities to potion effects. */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof ServerPlayer player
                && ClassEffectApplier.state(player).immunities().contains(event.getEffectInstance().getEffect())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
