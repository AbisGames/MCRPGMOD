package com.pipican.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code barter_quality} (Diplomat's Bartering): a piglin bartering near you may roll its loot twice. */
@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {

    @Inject(method = "getBarterResponseItems", at = @At("RETURN"), cancellable = true)
    private static void pipican$barterQuality(Piglin body, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!(body.level() instanceof ServerLevel level)) {
            return;
        }
        double chance = 0;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, body.getBoundingBox().inflate(16))) {
            chance = Math.max(chance, com.pipican.player.ClassEffectApplier.state(player).chance(com.pipican.effect.type.BonusEffect.Kind.BARTER_QUALITY));
        }
        if (chance <= 0 || body.getRandom().nextDouble() >= chance) {
            return;
        }
        List<ItemStack> more = new ArrayList<>(cir.getReturnValue());
        more.addAll(level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.PIGLIN_BARTERING)
                .getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, body).create(LootContextParamSets.PIGLIN_BARTER)));
        cir.setReturnValue(more);
    }
}
