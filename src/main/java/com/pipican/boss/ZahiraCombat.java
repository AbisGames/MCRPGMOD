package com.pipican.boss;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Marked corsair boarding routes, breakable broadside cannons and independent fuse bombs. */
public final class ZahiraCombat {
    public static final String BOARDING="boarding_lines", BROADSIDE="broadside", FUSES="fuse_dance";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO;
    private double heading;
    private List<Boolean> cannonDisabled=List.of(false,false,false),bombKicked=List.of(false,false,false);
    private List<Integer> fuses=List.of(0,0,0);
    private List<Vec3> bombPositions=List.of(Vec3.ZERO,Vec3.ZERO,Vec3.ZERO);
    private int phase=1,laneShift,boardingIndex=-1,boardingDelay,exposureTicks;
    public ZahiraCombat(RegionalBossEntity boss){this.boss=boss;var d=BossDesigns.require("zahira_crimson_corsair");clock=new RegionalCombatClock(Map.of(BOARDING,time(d,0),BROADSIDE,time(d,1),FUSES,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,50)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}
    public int boardingProgress(){return boardingIndex;}
    public int disabledCannons(){return (int)cannonDisabled.stream().filter(Boolean::booleanValue).count();}
    public int activeBombs(){return (int)fuses.stream().filter(f->f>0).count();}
    public int fuse(int index){return fuses.get(index);}
    public int exposureTicks(){return exposureTicks;}
    public float damageMultiplier(net.minecraft.world.damagesource.DamageSource source){return phase==2&&exposureTicks>0&&disabledCannons()==3?1.4f:1f;}
    public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();if(phase==2){laneShift=(laneShift+1)%3;heading+=0.14;boss.playSound(SoundEvents.TRIDENT_RETURN,1.1f,0.7f);}if(id.equals(BOARDING)){boardingIndex=0;boardingDelay=0;}if(id.equals(BROADSIDE))cannonDisabled=List.of(false,false,false);if(id.equals(FUSES)){fuses=List.of(0,0,0);bombKicked=List.of(false,false,false);bombPositions=bombSpots();}return true;}
    public void tick(ServerLevel level){if(exposureTicks>0)exposureTicks--;String active=clock.active();if(active.equals(BROADSIDE)&&!clock.resolved())checkCannonHits(level);if(active.equals(FUSES))checkBombKicks(level);if(!active.isEmpty()&&!clock.resolved()&&clock.remaining()%5==0)telegraph(level,active);
        if(active.equals(BOARDING)&&clock.resolved()&&boardingIndex>=0&&boardingIndex<3){if(boardingDelay>0)boardingDelay--;else{resolveBoarding(level,boardingIndex);boardingIndex++;boardingDelay=8;}}
        if(active.equals(FUSES)&&fuses.stream().anyMatch(f->f>0))tickBombs(level);
        if(active.isEmpty()){clock.tick();return;}String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case BOARDING->{boardingIndex=0;boardingDelay=1;boss.playSound(SoundEvents.CHAIN_PLACE,1.25f,0.65f);}
            case BROADSIDE->{resolveBroadside(level);boss.playSound(SoundEvents.CROSSBOW_SHOOT,1.35f,0.7f);}
            case FUSES->{fuses=List.of(16,30,44);boss.playSound(SoundEvents.TNT_PRIMED,1.2f,0.72f);}
            case "basic"->strike(level,List.of(mark),0.68f);default->{}}
    }
    private Vec3 forward(){return new Vec3(Math.cos(heading),0,Math.sin(heading));}
    private Vec3 side(){Vec3 f=forward();return new Vec3(-f.z,0,f.x);}
    private double laneOffset(int lane){return (lane-1)*4.8;}
    private int boardingLane(int index){return phase==2?Math.floorMod(index+laneShift,3):index;}
    private List<Vec3> boardingLanes(int lane){var path=new ArrayList<Vec3>();Vec3 f=forward(),s=side();double offset=laneOffset(boardingLane(lane));for(int i=0;i<=24;i++){double d=-8+i*16/24.0;Vec3 p=origin.add(f.scale(d)).add(s.scale(offset));if(boss.insideArena(p))path.add(p);}return path;}
    public Vec3 boardingLandingPosition(int index){if(index<0||index>=3)throw new IndexOutOfBoundsException("Zahira boarding index must be in [0,2]");return origin.add(forward().scale(7)).add(side().scale(laneOffset(boardingLane(index))));}
    public Vec3 cannonPosition(int index){if(index<0||index>=3)throw new IndexOutOfBoundsException("Zahira cannon index must be in [0,2]");int lane=phase==2?Math.floorMod(index+laneShift,3):index;return origin.add(forward().scale(-6)).add(side().scale(laneOffset(lane)));}
    private List<Vec3> broadsideLane(int index){int lane=phase==2?Math.floorMod(index+laneShift,3):index;Vec3 start=origin.add(forward().scale(-6)).add(side().scale(laneOffset(lane)));var points=new ArrayList<Vec3>();for(int n=0;n<=32;n++){Vec3 p=start.add(forward().scale(n*0.5));if(boss.insideArena(p))points.add(p);}return points;}
    public Vec3 bombPosition(int index){if(index<0||index>=3)throw new IndexOutOfBoundsException("Zahira bomb index must be in [0,2]");return bombPositions.get(index);}
    private List<Vec3> bombSpots(){var points=new ArrayList<Vec3>();Vec3 f=forward(),s=side();points.add(mark);points.add(mark.add(s.scale(4.2)).add(f.scale(2)));points.add(mark.add(s.scale(-4.2)).add(f.scale(4)));for(int i=0;i<points.size();i++)if(!boss.insideArena(points.get(i)))points.set(i,origin.add(f.scale(4+i*2)).add(s.scale(laneOffset(i))));return List.copyOf(points);}
    private void checkCannonHits(ServerLevel level){var next=new ArrayList<>(cannonDisabled);for(int i=0;i<3;i++)if(!next.get(i)){Vec3 p=cannonPosition(i);var box=new net.minecraft.world.phys.AABB(p.add(-1,-1,-1),p.add(1,2,1));var shots=level.getEntitiesOfClass(Projectile.class,box,Projectile::isAlive);if(!shots.isEmpty()){next.set(i,true);shots.forEach(Projectile::discard);level.sendParticles(ParticleTypes.POOF,p.x,p.y,p.z,18,0.35,0.25,0.35,0.02);}}cannonDisabled=List.copyOf(next);}
    private void checkBombKicks(ServerLevel level){var kicked=new ArrayList<>(bombKicked);var positions=new ArrayList<>(bombPositions);for(int i=0;i<3;i++)if(!kicked.get(i)&&fuses.get(i)>0){Vec3 p=positions.get(i);var box=new net.minecraft.world.phys.AABB(p.add(-1,-1,-1),p.add(1,1.5,1));var shots=level.getEntitiesOfClass(Projectile.class,box,Projectile::isAlive);if(!shots.isEmpty()){Projectile shot=shots.get(0);Vec3 dir=shot.getDeltaMovement();if(dir.lengthSqr()<0.001)dir=side().scale(i%2==0?1:-1);else dir=dir.normalize();Vec3 moved=p.add(dir.scale(4));if(!boss.insideArena(moved))moved=p.add(side().scale(i%2==0?-4:4));positions.set(i,moved);kicked.set(i,true);shot.discard();level.sendParticles(ParticleTypes.CRIT,moved.x,moved.y+0.15,moved.z,12,0.18,0.08,0.18,0.02);}}bombKicked=List.copyOf(kicked);bombPositions=List.copyOf(positions);}
    private void telegraph(ServerLevel level,String id){if(id.equals(BOARDING)){for(int lane=0;lane<3;lane++){var path=boardingLanes(lane);for(int n=0;n<path.size();n+=3){Vec3 p=path.get(n);level.sendParticles(ParticleTypes.CRIMSON_SPORE,p.x,p.y+0.1,p.z,1,0.05,0.02,0.05,0);}Vec3 land=boardingLandingPosition(lane);level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,land.x,land.y+0.1,land.z,10,0.25,0.05,0.25,0.005);}}
        else if(id.equals(BROADSIDE)){for(int i=0;i<3;i++){Vec3 gun=cannonPosition(i);level.sendParticles(cannonDisabled.get(i)?ParticleTypes.POOF:ParticleTypes.FLAME,gun.x,gun.y+0.35,gun.z,10,0.12,0.08,0.12,0);var line=broadsideLane(i);for(int n=0;n<line.size();n+=3){Vec3 p=line.get(n);level.sendParticles(ParticleTypes.CRIMSON_SPORE,p.x,p.y+0.08,p.z,1,0.03,0.02,0.03,0);}}}
        else{for(int i=0;i<3;i++){Vec3 p=bombPositions.get(i);int fuse=fuses.get(i);level.sendParticles(fuse==0?ParticleTypes.FLAME:ParticleTypes.SMOKE,p.x,p.y+0.12,p.z,fuse==0?10:Math.max(2,12-fuse/5),0.18,0.05,0.18,0.003);}}}
    private void resolveBoarding(ServerLevel level,int index){var path=boardingLanes(index);for(int i=0;i<path.size();i+=2){Vec3 p=path.get(i);level.sendParticles(ParticleTypes.CRIT,p.x,p.y,p.z,1,0.15,0.04,0.15,0.01);}strike(level,path,0.5f);Vec3 land=boardingLandingPosition(index);strike(level,List.of(land),0.25f);}
    private void resolveBroadside(ServerLevel level){int live=0;for(int i=0;i<3;i++)if(!cannonDisabled.get(i)){live++;var lane=broadsideLane(i);for(int n=0;n<lane.size();n+=2){Vec3 p=lane.get(n);level.sendParticles(ParticleTypes.FLAME,p.x,p.y,p.z,2,0.15,0.06,0.15,0.02);}strike(level,lane,0.57f);}if(live==0&&phase==2)exposureTicks=60;}
    private void tickBombs(ServerLevel level){var next=new ArrayList<>(fuses);for(int i=0;i<3;i++)if(next.get(i)>0){int left=next.get(i)-1;next.set(i,left);if(left==0){Vec3 p=bombPositions.get(i);level.sendParticles(ParticleTypes.EXPLOSION,p.x,p.y,p.z,6,0.35,0.12,0.35,0.025);strike(level,List.of(p),0.72f);boss.playSound(SoundEvents.GENERIC_EXPLODE.value(),0.8f,0.8f+i*0.12f);}}fuses=List.copyOf(next);}
    private void strike(ServerLevel level,List<Vec3> points,float scale){for(Player player:players(level))if(points.stream().anyMatch(p->Math.abs(player.getY()-p.y)<2.8&&p.distanceToSqr(player.position())<=1.75*1.75))player.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    private List<Player> players(ServerLevel level){return level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(28),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    public boolean updatePhase(float health,float max){boolean changed=clock.updatePhase(health,max);if(changed)phase=2;return changed;}
    public void save(ValueOutput o){clock.save(o);o.store("ZahiraOrigin",Vec3.CODEC,origin);o.store("ZahiraMark",Vec3.CODEC,mark);o.store("ZahiraBombs",Vec3.CODEC.listOf(),bombPositions);o.store("ZahiraFuses",com.mojang.serialization.Codec.INT.listOf(),fuses);o.store("ZahiraBombKicked",com.mojang.serialization.Codec.BOOL.listOf(),bombKicked);o.store("ZahiraCannons",com.mojang.serialization.Codec.BOOL.listOf(),cannonDisabled);o.putDouble("ZahiraHeading",heading);o.putInt("ZahiraPhase",phase);o.putInt("ZahiraLaneShift",laneShift);o.putInt("ZahiraBoardingIndex",boardingIndex);o.putInt("ZahiraBoardingDelay",boardingDelay);o.putInt("ZahiraExposure",exposureTicks);}
    public void load(ValueInput i){clock.load(i);origin=i.read("ZahiraOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("ZahiraMark",Vec3.CODEC).orElse(Vec3.ZERO);heading=i.getDoubleOr("ZahiraHeading",0);phase=clock.phase();laneShift=Math.floorMod(i.getIntOr("ZahiraLaneShift",0),3);boardingIndex=Math.clamp(i.getIntOr("ZahiraBoardingIndex",-1),-1,3);boardingDelay=Math.clamp(i.getIntOr("ZahiraBoardingDelay",0),0,8);exposureTicks=Math.clamp(i.getIntOr("ZahiraExposure",0),0,60);bombPositions=normalize(i.read("ZahiraBombs",Vec3.CODEC.listOf()).orElse(List.of()),Vec3.ZERO);fuses=normalize(i.read("ZahiraFuses",com.mojang.serialization.Codec.INT.listOf()).orElse(List.of()),0);bombKicked=normalize(i.read("ZahiraBombKicked",com.mojang.serialization.Codec.BOOL.listOf()).orElse(List.of()),false);cannonDisabled=normalize(i.read("ZahiraCannons",com.mojang.serialization.Codec.BOOL.listOf()).orElse(List.of()),false);}
    private static <T> List<T> normalize(List<T> values,T fallback){var result=new ArrayList<T>(3);for(int i=0;i<3;i++)result.add(i<values.size()?values.get(i):fallback);return List.copyOf(result);}
}
