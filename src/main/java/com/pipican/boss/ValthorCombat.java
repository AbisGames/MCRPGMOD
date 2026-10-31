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

/** Interrupted conductor chains, expanding refuge rings and directional gale lanes. */
public final class ValthorCombat {
    public static final String CONDUCTORS="conductor_link",DONUT="thunder_donut",GALES="gale_channels";
    private final RegionalBossEntity boss;private final RegionalCombatClock clock;private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO;private double heading;
    private List<Boolean> broken=List.of(false,false,false);private int phase=1,safeLane=1;
    public ValthorCombat(RegionalBossEntity b){boss=b;var d=BossDesigns.require("valthor_stormbringer");clock=new RegionalCombatClock(Map.of(CONDUCTORS,time(d,0),DONUT,time(d,1),GALES,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,55)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int n){var a=d.abilities().get(n);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}public int brokenConductors(){return(int)broken.stream().filter(Boolean::booleanValue).count();}public int safeLane(){return safeLane;}public Vec3 conductor(int i){if(i<0||i>=3)throw new IndexOutOfBoundsException();return anchor(i);}public Vec3 galeLanePosition(int i){if(i<0||i>=3)throw new IndexOutOfBoundsException();return lane(i).get(20);}public Vec3 safeGalePosition(){return origin.add(side().scale((safeLane-1)*4)).add(forward().scale(3));}public Vec3 donutRefuge(){return origin;}
    public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();if(phase==2){safeLane=(safeLane+1)%3;heading+=.12;boss.playSound(SoundEvents.COPPER_BULB_TURN_ON,1.2f,.65f);}if(id.equals(CONDUCTORS))broken=List.of(false,false,false);return true;}
    private Vec3 forward(){return new Vec3(Math.cos(heading),0,Math.sin(heading));}private Vec3 side(){Vec3 f=forward();return new Vec3(-f.z,0,f.x);}private Vec3 anchor(int i){double a=heading+Math.PI*2*i/3;return origin.add(Math.cos(a)*5,0,Math.sin(a)*5);}
    private List<Vec3> line(Vec3 a,Vec3 b){var p=new ArrayList<Vec3>();for(int i=0;i<=20;i++){Vec3 q=a.lerp(b,i/20.0);if(boss.insideArena(q))p.add(q);}return p;}
    private List<Vec3> donut(){var p=new ArrayList<Vec3>();for(double radius=3.5;radius<=8;radius+=.75)for(int a=0;a<360;a+=5){double r=Math.toRadians(a);Vec3 q=origin.add(Math.cos(r)*radius,0,Math.sin(r)*radius);if(boss.insideArena(q))p.add(q);}return p;}
    private List<Vec3> lane(int i){var p=new ArrayList<Vec3>();for(int n=0;n<=30;n++){Vec3 q=origin.add(forward().scale(-7+n*.5)).add(side().scale((i-1)*4));if(boss.insideArena(q))p.add(q);}return p;}
    public void tick(ServerLevel l){String id=clock.active();if(id.equals(CONDUCTORS)&&!clock.resolved())checkBreaks(l);if(!id.isEmpty()&&!clock.resolved()&&clock.remaining()%5==0)telegraph(l,id);if(id.isEmpty()){clock.tick();return;}String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case CONDUCTORS->{for(int i=0;i<3;i++)if(!broken.get(i))strike(l,line(anchor(i),anchor((i+1)%3)),.33f);boss.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER,.9f,.8f);}case DONUT->{strike(l,donut(),.48f);boss.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT,1.2f,.65f);}case GALES->{for(int i=0;i<3;i++)if(i!=safeLane){var points=lane(i);strike(l,points,.3f);for(Player p:players(l))if(points.stream().anyMatch(q->p.position().distanceToSqr(q)<1.5*1.5))p.push(forward().x*.45,0,forward().z*.45);}boss.playSound(SoundEvents.ELYTRA_FLYING,1.1f,.7f);}case "basic"->strike(l,List.of(mark),.68f);default->{}}}
    private void checkBreaks(ServerLevel l){var next=new ArrayList<>(broken);for(int i=0;i<3;i++)if(!next.get(i)){Vec3 p=anchor(i);var shots=l.getEntitiesOfClass(Projectile.class,new net.minecraft.world.phys.AABB(p.add(-1,-1,-1),p.add(1,1.5,1)),Projectile::isAlive);if(!shots.isEmpty()){next.set(i,true);shots.forEach(Projectile::discard);l.sendParticles(ParticleTypes.POOF,p.x,p.y,p.z,14,.25,.2,.25,.02);}}broken=List.copyOf(next);}
    private void telegraph(ServerLevel l,String id){if(id.equals(CONDUCTORS)){for(int i=0;i<3;i++){Vec3 p=anchor(i);l.sendParticles(broken.get(i)?ParticleTypes.POOF:ParticleTypes.ELECTRIC_SPARK,p.x,p.y+.3,p.z,9,.2,.2,.2,0);if(!broken.get(i))for(Vec3 q:line(anchor(i),anchor((i+1)%3)))if((int)(q.x+q.z)%2==0)l.sendParticles(ParticleTypes.END_ROD,q.x,q.y+.08,q.z,1,.03,.03,.03,0);}}else if(id.equals(DONUT)){for(Vec3 p:donut())if((int)(p.x+p.z)%3==0)l.sendParticles(ParticleTypes.ELECTRIC_SPARK,p.x,p.y+.08,p.z,1,.04,.03,.04,0);}else for(int i=0;i<3;i++)for(Vec3 p:lane(i))if((int)(p.x+p.z)%3==0)l.sendParticles(i==safeLane?ParticleTypes.HAPPY_VILLAGER:ParticleTypes.CLOUD,p.x,p.y+.05,p.z,1,.05,.02,.05,0);}
    private void strike(ServerLevel l,List<Vec3> points,float scale){for(Player p:players(l))if(points.stream().anyMatch(q->Math.abs(p.getY()-q.y)<2.8&&p.position().distanceToSqr(q)<1.65*1.65))p.hurtServer(l,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    private List<Player> players(ServerLevel l){return l.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(28),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    public boolean updatePhase(float hp,float max){boolean changed=clock.updatePhase(hp,max);if(changed)phase=2;return changed;}
    public void save(ValueOutput o){clock.save(o);o.store("ValthorOrigin",Vec3.CODEC,origin);o.store("ValthorMark",Vec3.CODEC,mark);o.store("ValthorBroken",com.mojang.serialization.Codec.BOOL.listOf(),broken);o.putDouble("ValthorHeading",heading);o.putInt("ValthorPhase",phase);o.putInt("ValthorSafeLane",safeLane);}
    public void load(ValueInput i){clock.load(i);origin=i.read("ValthorOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("ValthorMark",Vec3.CODEC).orElse(Vec3.ZERO);broken=i.read("ValthorBroken",com.mojang.serialization.Codec.BOOL.listOf()).orElse(List.of(false,false,false)).stream().limit(3).toList();if(broken.size()<3){var v=new ArrayList<>(broken);while(v.size()<3)v.add(false);broken=List.copyOf(v);}heading=i.getDoubleOr("ValthorHeading",0);phase=clock.phase();safeLane=Math.floorMod(i.getIntOr("ValthorSafeLane",1),3);}
}
