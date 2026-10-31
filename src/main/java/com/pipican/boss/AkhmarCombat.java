package com.pipican.boss;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Marked sand-spirit lanes, breakable obelisk shadows and sequential eruptions. */
public final class AkhmarCombat {
    public static final String PROCESSION="dune_procession", OBELISKS="sun_obelisks", COURT="buried_court";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO;
    private double heading;
    private List<Boolean> obeliskBroken=List.of(false,false,false);
    private int phase=1,laneShift,processionPulse=-1,processionDelay,eruptionIndex=-1,eruptionDelay;
    public AkhmarCombat(RegionalBossEntity boss){this.boss=boss;var d=BossDesigns.require("akhmar_sand_emperor");clock=new RegionalCombatClock(Map.of(PROCESSION,time(d,0),OBELISKS,time(d,1),COURT,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,50)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}
    public int eruptionProgress(){return eruptionIndex;}
    public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();if(phase==2){laneShift=(laneShift+1)%3;heading+=0.19;boss.playSound(SoundEvents.SAND_PLACE,1.1f,0.65f);}
        if(id.equals(PROCESSION)){processionPulse=0;processionDelay=0;}if(id.equals(OBELISKS))obeliskBroken=List.of(false,false,false);if(id.equals(COURT)){eruptionIndex=0;eruptionDelay=0;}return true;}
    public void tick(ServerLevel level){String active=clock.active();if(!active.isEmpty()&&!clock.resolved()){if(active.equals(OBELISKS))checkObeliskBreaks(level);if(clock.remaining()%5==0)telegraph(level,active);}
        if(!active.isEmpty()&&active.equals(PROCESSION)&&clock.resolved()&&processionPulse>=0&&processionPulse<3){if(processionDelay>0)processionDelay--;else{resolveProcession(level,processionPulse);processionPulse++;processionDelay=8;}}
        if(!active.isEmpty()&&active.equals(COURT)&&clock.resolved()&&eruptionIndex>=0&&eruptionIndex<3){if(eruptionDelay>0)eruptionDelay--;else{resolveEruption(level,eruptionIndex);eruptionIndex++;eruptionDelay=12;}}
        if(active.isEmpty()){clock.tick();return;}String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case PROCESSION->{processionPulse=0;processionDelay=1;boss.playSound(SoundEvents.SAND_PLACE,1.2f,0.6f);}
            case OBELISKS->{resolveSunPulse(level);boss.playSound(SoundEvents.BEACON_ACTIVATE,1.3f,0.7f);}
            case COURT->{eruptionIndex=0;eruptionDelay=1;boss.playSound(SoundEvents.SAND_BREAK,1.25f,0.65f);}
            case "basic"->strike(level,List.of(mark),0.7f);default->{}}
    }
    private List<Vec3> processionLane(int lane){var path=new ArrayList<Vec3>();Vec3 forward=new Vec3(Math.cos(heading),0,Math.sin(heading)),side=new Vec3(-forward.z,0,forward.x);double offset=(lane-1)*4.2;
        for(int i=0;i<=32;i++){double t=i/32.0,d=12*t,lateral=Math.sin(t*Math.PI*3+lane*0.8)*1.05;Vec3 p=origin.add(forward.scale(d)).add(side.scale(offset+lateral));if(boss.insideArena(p))path.add(p);}return path;}
    private List<Vec3> obelisks(){var out=new ArrayList<Vec3>();for(int i=0;i<3;i++){double a=heading+i*Math.PI*2/3;out.add(origin.add(Math.cos(a)*6,0.2,Math.sin(a)*6));}return out;}
    public Vec3 shadowPosition(int index){if(index<0||index>=3)throw new IndexOutOfBoundsException("Akhmar obelisk index must be in [0,2]");Vec3 p=obelisks().get(index);Vec3 sun=new Vec3(Math.cos(heading+Math.PI/2),0,Math.sin(heading+Math.PI/2));return p.add(sun.scale(2.8));}
    private void checkObeliskBreaks(ServerLevel level){var next=new ArrayList<>(obeliskBroken);for(int i=0;i<3;i++)if(!next.get(i)){Vec3 p=obelisks().get(i);var projectiles=level.getEntitiesOfClass(Projectile.class,new net.minecraft.world.phys.AABB(p.add(-1.2,-1.2,-1.2),p.add(1.2,1.2,1.2)),Projectile::isAlive);if(!projectiles.isEmpty()){next.set(i,true);projectiles.forEach(Projectile::discard);level.sendParticles(ParticleTypes.POOF,p.x,p.y,p.z,12,0.35,0.3,0.35,0.02);}}obeliskBroken=List.copyOf(next);}
    private void telegraph(ServerLevel level,String id){if(id.equals(PROCESSION)){for(int i=0;i<3;i++){var path=processionLane(i);for(int n=0;n<path.size();n+=4){Vec3 p=path.get(n);level.sendParticles(i==(phase==2?laneShift:1)?ParticleTypes.HAPPY_VILLAGER:ParticleTypes.CAMPFIRE_COSY_SMOKE,p.x,p.y+0.08,p.z,1,0.04,0.01,0.04,0);}}}
        else if(id.equals(OBELISKS)){var nodes=obelisks();for(int i=0;i<3;i++){Vec3 p=nodes.get(i),shadow=shadowPosition(i);level.sendParticles(obeliskBroken.get(i)?ParticleTypes.POOF:ParticleTypes.END_ROD,p.x,p.y+0.5,p.z,10,0.15,0.35,0.15,0.005);level.sendParticles(ParticleTypes.HAPPY_VILLAGER,shadow.x,shadow.y+0.06,shadow.z,10,0.3,0.03,0.3,0);}}
        else{for(int i=eruptionIndex;i<3;i++){Vec3 p=eruptionPoint(i);level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,p.x,p.y+0.05,p.z,12,0.4,0.05,0.4,0);}}}
    public Vec3 eruptionPosition(int index){if(index<0||index>=3)throw new IndexOutOfBoundsException("Akhmar eruption index must be in [0,2]");Vec3 forward=new Vec3(Math.cos(heading),0,Math.sin(heading)),side=new Vec3(-forward.z,0,forward.x);double d=4+index*3.4;double lateral=(index-1)*2.8;return origin.add(forward.scale(d)).add(side.scale(lateral));}
    private Vec3 eruptionPoint(int index){return eruptionPosition(index);}
    private void resolveProcession(ServerLevel level,int pulse){int safeLane=phase==2?laneShift:1;for(int lane=0;lane<3;lane++)if(lane!=safeLane){List<Vec3> path=processionLane(lane);int start=pulse*path.size()/3,end=(pulse+1)*path.size()/3;if(start<end)strike(level,path.subList(start,end),0.5f);}}
    private void resolveSunPulse(ServerLevel level){var nodes=obelisks();var shadows=new ArrayList<Vec3>();for(int i=0;i<3;i++)if(!obeliskBroken.get(i)){Vec3 p=nodes.get(i);level.sendParticles(ParticleTypes.END_ROD,p.x,p.y+1,p.z,10,0.16,0.3,0.16,0.02);shadows.add(shadowPosition(i));}
        for(Player player:players(level)){boolean safe=shadows.stream().anyMatch(p->Math.abs(p.y-player.getY())<2.6&&p.distanceToSqr(player.position())<=1.8*1.8);if(!safe)damage(level,player,0.66f);}}
    private void resolveEruption(ServerLevel level,int index){Vec3 p=eruptionPoint(index);level.sendParticles(ParticleTypes.EXPLOSION,p.x,p.y,p.z,4,0.25,0.08,0.25,0.01);for(Player player:players(level))if(Math.abs(player.getY()-p.y)<2.8&&player.position().distanceToSqr(p)<=2.2*2.2)damage(level,player,0.62f);}
    private void strike(ServerLevel level,List<Vec3> points,float scale){for(Player p:players(level))if(points.stream().anyMatch(v->Math.abs(p.getY()-v.y)<2.8&&p.position().distanceToSqr(v)<=1.7*1.7))damage(level,p,scale);}
    private List<Player> players(ServerLevel level){return level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(26),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    private void damage(ServerLevel level,Player p,float scale){p.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    public boolean updatePhase(float health,float max){boolean changed=clock.updatePhase(health,max);if(changed)phase=2;return changed;}
    public void save(ValueOutput o){clock.save(o);o.store("AkhmarOrigin",Vec3.CODEC,origin);o.store("AkhmarMark",Vec3.CODEC,mark);o.store("AkhmarObelisks",com.mojang.serialization.Codec.BOOL.listOf(),obeliskBroken);o.putDouble("AkhmarHeading",heading);o.putInt("AkhmarPhase",phase);o.putInt("AkhmarLaneShift",laneShift);o.putInt("AkhmarProcessionPulse",processionPulse);o.putInt("AkhmarProcessionDelay",processionDelay);o.putInt("AkhmarEruptionIndex",eruptionIndex);o.putInt("AkhmarEruptionDelay",eruptionDelay);}
    public void load(ValueInput i){clock.load(i);origin=i.read("AkhmarOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("AkhmarMark",Vec3.CODEC).orElse(Vec3.ZERO);List<Boolean> saved=i.read("AkhmarObelisks",com.mojang.serialization.Codec.BOOL.listOf()).orElse(List.of());var normalized=new ArrayList<Boolean>(3);for(int index=0;index<3;index++)normalized.add(index<saved.size()&&saved.get(index));obeliskBroken=List.copyOf(normalized);heading=i.getDoubleOr("AkhmarHeading",0);phase=clock.phase();laneShift=Math.floorMod(i.getIntOr("AkhmarLaneShift",0),3);processionPulse=Math.clamp(i.getIntOr("AkhmarProcessionPulse",-1),-1,3);processionDelay=Math.clamp(i.getIntOr("AkhmarProcessionDelay",0),0,8);eruptionIndex=Math.clamp(i.getIntOr("AkhmarEruptionIndex",-1),-1,3);eruptionDelay=Math.clamp(i.getIntOr("AkhmarEruptionDelay",0),0,12);}
}
