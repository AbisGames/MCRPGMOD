package com.pipican.boss;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Altar stays in place. The boss appears six blocks north on the same floor level. */
public final class RitualAltarBlock extends Block {
    public static final MapCodec<RitualAltarBlock> CODEC=simpleCodec(RitualAltarBlock::new);
    public RitualAltarBlock(Properties properties){ super(properties); }
    @Override public MapCodec<RitualAltarBlock> codec(){ return CODEC; }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var definition=RitualSigils.find(stack);
        if(definition.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if(level.isClientSide()) return InteractionResult.SUCCESS;
        if(!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        var sigil=definition.get();
        if(!sigil.boss().equals(BossCatalog.MORVATH.id()) || pos.getY()<sigil.minY() || pos.getY()>sigil.maxY()
                || !BossEncounterService.morvathBiome(serverPlayer.level(),pos)){
            serverPlayer.sendSystemMessage(Component.translatable("ritual.pipican.wrong_biome"),true); return InteractionResult.FAIL;
        }
        var result=BossEncounterService.summonMorvath(serverPlayer,pos.north(6));
        if(result!=BossEncounterService.Result.SUCCESS){
            serverPlayer.sendSystemMessage(Component.translatable("ritual.pipican.failed",result.name()),true); return InteractionResult.FAIL;
        }
        stack.shrink(1); // Exactly once, including creative; all validation/spawn work has succeeded.
        serverPlayer.sendSystemMessage(Component.translatable("ritual.pipican.summoned"),true);
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer serverPlayer){
            if(player.isShiftKeyDown()){
                var encounter=BossEncounters.get(serverPlayer.level()).ownedNear(player.getUUID(),pos);
                if(encounter.isPresent() && BossEncounterService.cancel(serverPlayer.level(),encounter.get().boss(),player.getUUID())){
                    serverPlayer.sendSystemMessage(Component.translatable("ritual.pipican.cancelled"),true); return InteractionResult.SUCCESS;
                }
            }
            serverPlayer.sendSystemMessage(Component.translatable("ritual.pipican.instructions"),true);
        }
        return InteractionResult.SUCCESS;
    }
}
