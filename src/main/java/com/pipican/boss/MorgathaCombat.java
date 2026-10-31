package com.pipican.boss;

import java.util.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Bog bubbles, a single-target mosquito cloud and charged decoy hexes. */
public final class MorgathaCombat {
    public static final String BUBBLES="muck_bubbles", VEIL="mosquito_veil", DECOYS="bog_decoys";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO,cloud=Vec3.ZERO,decoy=Vec3.ZERO,waveOrigin=Vec3.ZERO;
    private double heading;
    private List<Vec3> bubbles=List.of(),pools=List.of();
    private List<Boolean> burst=List.of(false,false,false);
    private UUID veilTarget;
    private int phase=1,poolTicks,veilTicks,veilPulse,phaseWaveDelay;

    public MorgathaCombat(RegionalBossEntity boss){this.boss=boss;var d=BossDesigns.require("morgatha_swamp_queen");clock=new RegionalCombatClock(Map.of(
        BUBBLES,time(d,0),VEIL,time(d,1),DECOYS,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,48)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}
    public int phaseWaveDelay(){return phaseWaveDelay;}
    public Vec3 phaseWaveTarget(){return waveOrigin.add(Math.cos(heading),0,Math.sin(heading));}
    public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;
        origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();
        if(id.equals(BUBBLES)){Vec3 side=new Vec3(-Math.sin(heading),0,Math.cos(heading));bubbles=List.of(mark,mark.add(side.scale(3.2)),mark.subtract(side.scale(3.2)));burst=List.of(false,false,false);}
        if(id.equals(VEIL)){veilTarget=target.getUUID();cloud=origin.add(0,1.1,0);veilTicks=0;veilPulse=0;}
        if(id.equals(DECOYS)){decoy=origin.add(Math.cos(heading)*7,0,Math.sin(heading)*7);waveOrigin=decoy;}
        return true;}
    public void tick(ServerLevel level){
        tickPools(level);tickVeil(level);
        if(phaseWaveDelay>0){telegraphWave(level);if(--phaseWaveDelay==0)resolveWave(level);}
        String active=clock.active();if(!active.isEmpty()&&!clock.resolved()){if(active.equals(BUBBLES))checkBurst(level);if(clock.remaining()%5==0)telegraph(level,active);}
        if(active.isEmpty()){clock.tick();return;}String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case BUBBLES->{var live=new ArrayList<Vec3>();for(int i=0;i<bubbles.size();i++)if(i>=burst.size()||!burst.get(i))live.add(bubbles.get(i));pools=List.copyOf(live);poolTicks=100;boss.playSound(SoundEvents.SLIME_BLOCK_BREAK,1.2f,0.7f);}
            case VEIL->{veilTicks=100;veilPulse=0;boss.playSound(SoundEvents.BEE_STING,1.4f,0.55f);}
            case DECOYS->{boss.teleportTo(decoy.x,decoy.y,decoy.z);waveOrigin=decoy;if(phase==2)phaseWaveDelay=14;else resolveWave(level);boss.playSound(SoundEvents.ENDERMAN_TELEPORT,1.1f,0.7f);}
            case "basic"->strike(level,List.of(mark),0.68f);default->{}}
    }
    private void checkBurst(ServerLevel level){var next=new ArrayList<>(burst);for(int i=0;i<bubbles.size();i++){if(next.get(i))continue;var p=bubbles.get(i);var found=level.getEntitiesOfClass(Projectile.class,new net.minecraft.world.phys.AABB(p.add(-1.1,-1.1,-1.1),p.add(1.1,1.1,1.1)),Projectile::isAlive);
            if(!found.isEmpty()){next.set(i,true);found.forEach(Projectile::discard);level.sendParticles(ParticleTypes.POOF,p.x,p.y+0.3,p.z,10,0.3,0.2,0.3,0.02);}}
        burst=List.copyOf(next);}
    private void tickPools(ServerLevel level){if(poolTicks<=0||pools.isEmpty())return;poolTicks--;for(Vec3 p:pools){level.sendParticles(poolTicks%12<6?ParticleTypes.BUBBLE:ParticleTypes.SPORE_BLOSSOM_AIR,p.x,p.y+0.08,p.z,4,0.75,0.04,0.75,0.005);
            if(poolTicks%20==0)for(Player player:players(level))if(Math.abs(player.getY()-p.y)<2.5&&player.position().distanceToSqr(p)<=2.6*2.6){damage(level,player,0.42f);player.addEffect(new MobEffectInstance(MobEffects.POISON,50,0));}}if(poolTicks==0)pools=List.of();}
    private void tickVeil(ServerLevel level){if(veilTicks<=0)return;Player target=players(level).stream().filter(p->p.getUUID().equals(veilTarget)).findFirst().orElse(null);if(target!=null){Vec3 wanted=target.position().add(0,0.7,0);Vec3 delta=wanted.subtract(cloud);double len=delta.length();if(len>0.001)cloud=cloud.add(delta.scale(Math.min(0.14/len,1)));}
        level.sendParticles(ParticleTypes.MYCELIUM,cloud.x,cloud.y,cloud.z,8,0.7,0.45,0.7,0.01);level.sendParticles(ParticleTypes.BUBBLE,cloud.x,cloud.y,cloud.z,4,0.45,0.35,0.45,0.005);veilTicks--;
        if(++veilPulse>=16){veilPulse=0;if(target!=null&&target.position().distanceToSqr(cloud)<=2.3*2.3){damage(level,target,0.35f);target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,35,0));}}}
    private void telegraph(ServerLevel level,String id){if(id.equals(BUBBLES)){for(int i=0;i<bubbles.size();i++){Vec3 p=bubbles.get(i);level.sendParticles(burst.get(i)?ParticleTypes.POOF:ParticleTypes.BUBBLE,p.x,p.y+0.12,p.z,10,0.45,0.15,0.45,0.005);}}
        else if(id.equals(VEIL)){level.sendParticles(ParticleTypes.ANGRY_VILLAGER,mark.x,mark.y+1,mark.z,2,0.15,0.25,0.15,0);level.sendParticles(ParticleTypes.MYCELIUM,cloud.x,cloud.y,cloud.z,8,0.4,0.3,0.4,0.01);}
        else{level.sendParticles(ParticleTypes.WITCH,decoy.x,decoy.y+0.5,decoy.z,12,0.45,0.6,0.45,0.01);}}
    private void telegraphWave(ServerLevel level){List<Vec3> line=wave();for(int i=0;i<line.size();i+=2){Vec3 p=line.get(i);level.sendParticles(ParticleTypes.WITCH,p.x,p.y+0.12,p.z,2,0.12,0.08,0.12,0.002);}}
    private List<Vec3> wave(){var out=new ArrayList<Vec3>();Vec3 side=new Vec3(-Math.sin(heading),0,Math.cos(heading));for(int i=0;i<=30;i++){double d=i*0.42;Vec3 center=waveOrigin.add(Math.cos(heading)*d,0,Math.sin(heading)*d);for(int lane=-1;lane<=1;lane++){Vec3 p=center.add(side.scale(lane*0.9));if(boss.insideArena(p))out.add(p);}}return out;}
    private void resolveWave(ServerLevel level){strike(level,wave(),0.72f);}
    private void strike(ServerLevel level,List<Vec3> points,float scale){for(Player player:players(level))if(points.stream().anyMatch(p->Math.abs(player.getY()-p.y)<2.8&&player.position().distanceToSqr(p)<=1.8*1.8)){damage(level,player,scale);player.addEffect(new MobEffectInstance(MobEffects.POISON,60,0));}}
    private List<Player> players(ServerLevel level){return level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(24),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    private void damage(ServerLevel level,Player player,float scale){player.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    public boolean updatePhase(float health,float max){boolean changed=clock.updatePhase(health,max);if(changed)phase=2;return changed;}
    public void save(ValueOutput out){clock.save(out);out.store("MorgathaOrigin",Vec3.CODEC,origin);out.store("MorgathaMark",Vec3.CODEC,mark);out.store("MorgathaCloud",Vec3.CODEC,cloud);out.store("MorgathaDecoy",Vec3.CODEC,decoy);out.store("MorgathaWave",Vec3.CODEC,waveOrigin);out.store("MorgathaBubbles",Vec3.CODEC.listOf(),bubbles);out.store("MorgathaPools",Vec3.CODEC.listOf(),pools);out.store("MorgathaBurst",com.mojang.serialization.Codec.BOOL.listOf(),burst);if(veilTarget!=null)out.store("MorgathaTarget",UUIDUtil.CODEC,veilTarget);out.putDouble("MorgathaHeading",heading);out.putInt("MorgathaPhase",phase);out.putInt("MorgathaPoolTicks",poolTicks);out.putInt("MorgathaVeilTicks",veilTicks);out.putInt("MorgathaVeilPulse",veilPulse);out.putInt("MorgathaWaveDelay",phaseWaveDelay);}
    public void load(ValueInput in){clock.load(in);origin=in.read("MorgathaOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=in.read("MorgathaMark",Vec3.CODEC).orElse(Vec3.ZERO);cloud=in.read("MorgathaCloud",Vec3.CODEC).orElse(Vec3.ZERO);decoy=in.read("MorgathaDecoy",Vec3.CODEC).orElse(Vec3.ZERO);waveOrigin=in.read("MorgathaWave",Vec3.CODEC).orElse(Vec3.ZERO);bubbles=in.read("MorgathaBubbles",Vec3.CODEC.listOf()).orElse(List.of()).stream().limit(3).toList();pools=in.read("MorgathaPools",Vec3.CODEC.listOf()).orElse(List.of()).stream().limit(3).toList();burst=in.read("MorgathaBurst",com.mojang.serialization.Codec.BOOL.listOf()).orElse(List.of(false,false,false)).stream().limit(3).toList();veilTarget=in.read("MorgathaTarget",UUIDUtil.CODEC).orElse(null);heading=in.getDoubleOr("MorgathaHeading",0);phase=in.getIntOr("MorgathaPhase",clock.phase())==2?2:1;poolTicks=Math.clamp(in.getIntOr("MorgathaPoolTicks",0),0,100);veilTicks=Math.clamp(in.getIntOr("MorgathaVeilTicks",0),0,100);veilPulse=Math.clamp(in.getIntOr("MorgathaVeilPulse",0),0,15);phaseWaveDelay=Math.clamp(in.getIntOr("MorgathaWaveDelay",0),0,14);}
}
