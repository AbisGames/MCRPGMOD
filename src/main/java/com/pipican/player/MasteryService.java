package com.pipican.player;

import com.pipican.effect.type.BonusEffect;
import com.pipican.core.ClassLookup;
import com.pipican.mastery.Skill;
import com.pipican.mastery.SkillAction;
import com.pipican.rpgclass.RpgClass;
import com.pipican.skill.TreeKind;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Skill mastery: turns player actions into mastery, and mastery tiers into bonuses + skill points.
 *
 * <p>Flow: an event handler builds a {@link SkillAction} and calls {@link #onAction}. Every skill
 * whose trigger matches gains mastery (subject to its cooldown and the class's affinity for the
 * item used). Crossing a threshold announces the new tier and re-applies class bonuses - the new
 * tier's effects and the extra skill point are both derived from mastery, nothing else to store.
 */
public final class MasteryService {

    /** Game time of the last counted use, per player and skill. Not saved (cooldowns are short). */
    private static final Map<UUID, Map<ResourceKey<Skill>, Long>> LAST_USE = new ConcurrentHashMap<>();
    private static final String[] NUMERALS = {"0", "I", "II", "III", "IV", "V"};

    private MasteryService() {}

    /** Feeds an action to every matching skill. Players without a class don't gain mastery. */
    public static void onAction(ServerPlayer player, SkillAction action) {
        PlayerClassData data = ClassService.data(player);
        Optional<RpgClass> rpgClass = ClassService.activeClass(player);
        if (rpgClass.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        Map<ResourceKey<Skill>, Long> lastUse = LAST_USE.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        for (var entry : ClassLookup.skills(player.level().registryAccess()).entrySet()) {
            Skill skill = entry.getValue();
            if (!skill.allowedFor(data.activeClass())) {
                continue;
            }
            double uses = skill.trigger().uses(player, action);
            if (uses <= 0) {
                continue;
            }
            if (!action.continuous() && skill.cooldownTicks() > 0) {
                Long last = lastUse.get(entry.getKey());
                if (last != null && now - last < skill.cooldownTicks()) {
                    continue;
                }
                lastUse.put(entry.getKey(), now);
            }
            double gain = uses * skill.gainPerUse() * ClassEffectApplier.state(player).bonus(BonusEffect.Kind.SKILL_TRAINING);
            if (skill.affinityScaled() && !action.item().isEmpty()) {
                gain *= rpgClass.get().affinities().resolve(action.item()).tuning().mastery();
            }
            addMastery(player, entry.getKey(), skill, gain);
        }
    }

    /** Adds mastery (may be negative). Announces and applies tier changes. */
    public static void addMastery(ServerPlayer player, ResourceKey<Skill> key, Skill skill, double amount) {
        PlayerClassData data = ClassService.data(player);
        double before = data.mastery(key);
        double after = Math.max(0, before + amount);
        data.allMastery().put(key, after);
        int oldTier = skill.tierFor(before);
        int newTier = skill.tierFor(after);
        if (newTier > oldTier) {
            for (int tier = oldTier + 1; tier <= newTier; tier++) {
                player.sendSystemMessage(Component.translatable("pipican.mastery.tier_up", skill.name(), numeral(tier),
                        Component.translatable("pipican.tree." + skill.pointsTo().getSerializedName())).withStyle(ChatFormatting.AQUA));
            }
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.6f);
        }
        if (newTier != oldTier) {
            ClassService.changed(player);
        } else {
            data.setMasteryDirty(true);
        }
    }

    /** Admin: sets a skill to exactly the start of {@code tier} (0 = no mastery). */
    public static void setTier(ServerPlayer player, ResourceKey<Skill> key, Skill skill, int tier) {
        double target = tier <= 0 ? 0 : skill.thresholds().get(Math.min(tier, Skill.TIERS) - 1);
        addMastery(player, key, skill, target - ClassService.data(player).mastery(key));
    }

    /**
     * Admin: clears all mastery, and with it the general and special trees (their points are gone).
     * The stat tree is kept: it is paid with stat points, not mastery.
     */
    public static void resetAll(ServerPlayer player) {
        ClassService.data(player).allMastery().clear();
        ClassService.data(player).activeProgress().ifPresent(progress -> {
            progress.nodes(TreeKind.GENERAL).clear();
            progress.nodes(TreeKind.SPECIAL).clear();
        });
        ClassService.changed(player);
    }

    /** Sends batched mastery changes to the client (once a second at most). Called every tick. */
    public static void tick(ServerPlayer player) {
        PlayerClassData data = ClassService.data(player);
        if (data.masteryDirty() && player.tickCount % 20 == 0) {
            data.setMasteryDirty(false);
            player.syncData(PipicanAttachments.CLASS_DATA);
        }
    }

    public static void forget(ServerPlayer player) {
        LAST_USE.remove(player.getUUID());
    }

    public static String numeral(int tier) {
        return NUMERALS[Math.clamp(tier, 0, NUMERALS.length - 1)];
    }
}
