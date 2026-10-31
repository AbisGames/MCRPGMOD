package com.pipican.gametest;

import com.pipican.Pipican;
import com.pipican.boss.*;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in client scene only. This entire source set is excluded from the distributable. */
@EventBusSubscriber(modid=Pipican.MOD_ID)
public final class MorvathClientQa {
    private static final Map<MinecraftServer,Session> SESSIONS=new WeakHashMap<>();
    private static final List<java.util.function.Supplier<net.minecraft.world.entity.EntityType<?>>> VISUAL_BOSSES=List.of(
        ()->BossRegistries.FROSTBOUND.get(),()->BossRegistries.NHALKORR.get(),()->BossRegistries.SKOLLVAR.get(),()->BossRegistries.HROTHGAR.get(),
        ()->BossRegistries.VALTHOR.get(),()->BossRegistries.TENGRI.get(),()->BossRegistries.THALASSOR.get(),()->BossRegistries.ELDAROTH.get(),
        ()->BossRegistries.FALLEN_KING.get(),()->BossRegistries.DURMAGON.get(),()->BossRegistries.LONGWEI.get(),()->BossRegistries.MORGATHA.get(),
        ()->BossRegistries.AURELIOS.get(),()->BossRegistries.LIANHUA.get(),()->BossRegistries.AKHMAR.get(),()->BossRegistries.NETHARIS.get(),
        ()->BossRegistries.ZAHIRA.get(),()->BossRegistries.XALTEK.get(),()->BossRegistries.CORALLITH.get(),()->BossRegistries.AZHAR.get(),
        ()->BossRegistries.KRAGMOR.get(),()->BossRegistries.AETHERION.get(),()->BossRegistries.SYLTHERA.get(),()->BossRegistries.NOCTHAR.get(),
        ()->BossRegistries.VERMILLION.get(),()->BossRegistries.LUMYNARA.get());
    private static final class Session { boolean prepared; int lastSlot=-1,angle,nextBoss; final Set<UUID> seen=new HashSet<>(); }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(!Boolean.getBoolean("pipican.clientQa")) return;
        var server=event.getServer(); if(!server.isSingleplayer() || server.getPlayerList().getPlayers().isEmpty()) return;
        String configured=System.getProperty("pipican.clientQaRoot",""); if(configured.isBlank()) return;
        Path permitted=Path.of(configured).toAbsolutePath().normalize().resolve("saves");
        if(!server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().startsWith(permitted)) return;
        var player=server.getPlayerList().getPlayers().getFirst(); var level=player.level();
        var session=SESSIONS.computeIfAbsent(server,key->new Session());
        if(!session.prepared){
            for(int x=-24;x<=24;x+=8) for(int z=-24;z<=24;z+=8) level.getChunkAt(new BlockPos(x,65,z));
            for(var boss:level.getEntitiesOfClass(RegionalBossEntity.class,new AABB(-24,60,-24,24,90,24))) boss.discard();
            for(var pos:BlockPos.betweenClosed(new BlockPos(-20,64,-20),new BlockPos(20,84,20))){
                var state=pos.getY()==64 ? (pos.getX()%5==0 || pos.getZ()%5==0 ? Blocks.POLISHED_ANDESITE : Blocks.SMOOTH_STONE).defaultBlockState() : Blocks.AIR.defaultBlockState();
                level.setBlock(pos,state,2);
            }
            var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("ashlands/ash_waste_plains")));
            var filled=net.minecraft.server.commands.FillBiomeCommand.fill(level,new BlockPos(-20,56,-20),new BlockPos(20,72,20),biome);
            if(filled.left().isEmpty()) throw new IllegalStateException("Client QA biome preparation failed");
            level.setBlock(new BlockPos(0,65,0),BossContent.ALTAR.get().defaultBlockState(),2);
            player.setGameMode(GameType.SURVIVAL);
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,360000,4,false,false));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,360000,0,false,false));
            player.getInventory().setItem(0,new ItemStack(BossContent.MORVATH_SIGIL.get(),4));
            player.getInventory().setItem(1,new ItemStack(Items.NETHERITE_SWORD));
            player.getInventory().setItem(4,new ItemStack(Items.SPYGLASS));
            player.getInventory().setItem(7,new ItemStack(Items.BLAZE_POWDER));
            player.getInventory().setItem(8,new ItemStack(Items.CLOCK)); player.getInventory().setSelectedSlot(0);
            player.teleportTo(0.5,65,3.5); player.resetFallDistance();
            player.lookAt(EntityAnchorArgument.Anchor.EYES,new Vec3(0.5,65.7,0.5));
            player.containerMenu.broadcastChanges();
            player.sendSystemMessage(Component.literal("QA ready: use sigil on altar. Boss starts paused. Hotbar 5: next boss model (26 remaining); 6: finish; 7: hurt; 8: phase two; 9: pause/resume AI. Visual immunity is active."));
            Pipican.LOGGER.info("CLIENT QA READY world={} altar=0,65,0 boss=0,65,-6 visualImmunity=true",server.getWorldPath(LevelResource.ROOT));
            session.prepared=true;
        }
        var bosses=level.getEntitiesOfClass(RegionalBossEntity.class,new AABB(-24,60,-24,24,90,24),RegionalBossEntity::isAlive);
        for(var boss:bosses) if(session.seen.add(boss.getUUID())){
            boss.setNoAi(true); boss.setTarget(null);
            boss.tickCombat(level); // Advance one server-authoritative combat tick before the model preview.
            boss.setYRot(0); boss.yBodyRot=0;
            player.lookAt(EntityAnchorArgument.Anchor.EYES,boss.position().add(0,boss.getBbHeight()*0.5,0));
            Pipican.LOGGER.info("CLIENT QA BOSS spawned={} sigils={} phase={} paused=true",boss.getUUID(),player.getInventory().getItem(0).getCount(),boss.phase());
        }
        int slot=player.getInventory().getSelectedSlot();
        if(slot!=session.lastSlot){
            if(slot==4){
                for(var previous:level.getEntitiesOfClass(RegionalBossEntity.class,new AABB(-24,60,-24,24,90,24))) previous.discard();
                if(session.nextBoss<VISUAL_BOSSES.size()){
                    var entityType=VISUAL_BOSSES.get(session.nextBoss++).get();
                    var spawned=entityType.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    if(spawned instanceof RegionalBossEntity boss){
                        boss.teleportTo(0.5,65,-5.5);
                        if(level.addFreshEntity(boss)) Pipican.LOGGER.info("CLIENT QA MODEL QUEUED index={} id={}",session.nextBoss,BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()));
                        else Pipican.LOGGER.error("CLIENT QA MODEL SPAWN REJECTED index={} id={}",session.nextBoss,BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()));
                    } else throw new IllegalStateException("QA visual boss is not a RegionalBossEntity: "+entityType);
                } else player.sendSystemMessage(Component.literal("All 26 regional boss models have been cycled."));
            }
            for(var boss:bosses){
                if(slot==1 || slot==2 || slot==3){
                    boss.setNoAi(true); boss.setTarget(null); boss.getNavigation().stop();
                    boss.teleportTo(0.5,65,-5.5); boss.setDeltaMovement(Vec3.ZERO);
                    boss.setYRot(0); boss.setYHeadRot(0); boss.yBodyRot=0;
                    if(slot==3) session.angle=(session.angle+1)%4;
                    double angle=session.angle*Math.PI/2;
                    player.teleportTo(0.5+Math.sin(angle)*10,65,-5.5+Math.cos(angle)*10);
                    player.setDeltaMovement(Vec3.ZERO); player.clearFire();
                    player.lookAt(EntityAnchorArgument.Anchor.EYES,boss.position().add(0,2.1,0));
                }
                if(slot==6) boss.hurtServer(level,boss.damageSources().playerAttack(player),1);
                if(slot==5) boss.hurtServer(level,boss.damageSources().genericKill(),10000);
                if(slot==7){ boss.setHealth(boss.getMaxHealth()*0.49f); boss.setNoAi(false); }
                if(slot==8) boss.setNoAi(!boss.isNoAi());
                Pipican.LOGGER.info("CLIENT QA CONTROL slot={} phase={} ability={} remaining={} paused={}",slot+1,boss.phase(),boss.ability(),boss.abilityTicksRemaining(),boss.isNoAi());
            }
        }
        session.lastSlot=slot;
    }
}
