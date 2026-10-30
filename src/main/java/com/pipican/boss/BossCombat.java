package com.pipican.boss;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Encounter-owned behavior. Implementations persist pending telegraphs and hazards. */
public interface BossCombat {
    boolean begin(BossCombatState clock,BossCombatState.Ability ability,LivingEntity target);
    void tick(ServerLevel level,BossCombatState clock);
    void clear();
    void save(ValueOutput output);
    void load(ValueInput input);
}
