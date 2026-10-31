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

/** Breakable sun plates, a shifting stampede gap, and staggered ring shocks with a core refuge. */
public final class AzharCombat {
    public static final String REFRACTION="sun_refraction",STAMPEDE="savanna_stampede",KNEEL="gilded_kneel";
    private final RegionalBossEntity boss;private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO;private double heading;
    private List<Boolean> plates=List.of(false,false,false);private int phase=1,safeLane=1,echoDelay,lobePass;
    public AzharCombat(RegionalBossEntity b){boss=b;var d=BossDesigns.require("azhar_golden_behemoth");clock=new RegionalCombatClock(Map.of(REFRACTION,time(d,0),STAMPEDE,time(d,1),KNEEL,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,55)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int n){var a=d.abilities().get(n);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}public int brokenPlates(){return (int)plates.stream().filter(Boolean::booleanValue).count();}public int pendingLobeTicks(){return echoDelay;}public int lobePass(){return lobePass;}public Vec3 platePosition(int i){if(i<0||i>=3)throw new IndexOutOfBoundsException();return plate(i);}public Vec3 safeStampedePosition(){return origin.add(forward().scale(5)).add(side().scale((safeLane-1)*4.5));}public Vec3 kneelRefuge(){return origin;}public Vec3 lobeTarget(double radius){return origin.add(forward().scale(radius));}
    public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();if(phase==2){safeLane=(safeLane+1)%3;heading+=0.13;boss.playSound(SoundEvents.BLAZE_AMBIENT,1.1f,0.65f);}plates=List.of(false,false,false);echoDelay=0;lobePass=0;return true;}
    public void tick(ServerLevel l){checkPlates(l);if(echoDelay>0){telegraphLobe(l,3+lobePass*2);if(--echoDelay==0){resolveLobe(l,3+lobePass*2);lobePass++;if(phase==2&&lobePass<3)echoDelay=12;}}
        String id=clock.active();if(!id.isEmpty()&&!clock.resolved()&&clock.remaining()%5==0)telegraph(l,id);if(id.isEmpty()){clock.tick();return;}String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case REFRACTION->{resolveRays(l);boss.playSound(SoundEvents.BEACON_ACTIVATE,1.3f,0.7f);}case STAMPEDE->{for(int i=0;i<3;i++)if(i!=safeLane)strike(l,lane(i),0.52f);boss.playSound(SoundEvents.HORSE_GALLOP,1.25f,0.72f);}case KNEEL->{echoDelay=10;lobePass=0;boss.playSound(SoundEvents.RAVAGER_ROAR,1.15f,0.55f);}case "basic"->strike(l,List.of(mark),0.68f);default->{}}}
    private Vec3 forward(){return new Vec3(Math.cos(heading),0,Math.sin(heading));}private Vec3 side(){Vec3 f=forward();return new Vec3(-f.z,0,f.x);}private Vec3 plate(int i){return origin.add(forward().scale(-5)).add(side().scale((i-1)*3.6));}
    private List<Vec3> lane(int i){var pts=new ArrayList<Vec3>();Vec3 f=forward(),s=side();for(int n=0;n<=32;n++){Vec3 p=origin.add(f.scale(-8+n*.5)).add(s.scale((i-1)*4.5));if(boss.insideArena(p))pts.add(p);}return pts;}
    private List<Vec3> ray(int i){Vec3 start=plate(i).add(forward().scale(0.5)),dir=forward().add(side().scale((i-1)*0.38)).normalize();var pts=new ArrayList<Vec3>();for(int n=0;n<=32;n++){Vec3 p=start.add(dir.scale(n*.5));if(boss.insideArena(p))pts.add(p);}return pts;}
    private void checkPlates(ServerLevel l){var next=new ArrayList<>(plates);for(int i=0;i<3;i++)if(!next.get(i)){Vec3 p=plate(i);var shots=l.getEntitiesOfClass(Projectile.class,new net.minecraft.world.phys.AABB(p.add(-1,-1,-1),p.add(1,1.5,1)),Projectile::isAlive);if(!shots.isEmpty()){next.set(i,true);shots.forEach(Projectile::discard);l.sendParticles(ParticleTypes.POOF,p.x,p.y,p.z,15,0.3,0.2,0.3,0.02);}}plates=List.copyOf(next);}
    private void telegraph(ServerLevel l,String id){if(id.equals(REFRACTION)){for(int i=0;i<3;i++){Vec3 p=plate(i);l.sendParticles(plates.get(i)?ParticleTypes.POOF:ParticleTypes.FLAME,p.x,p.y+0.35,p.z,10,0.15,0.1,0.15,0);for(Vec3 q:ray(i))if(q.distanceToSqr(p)%2<0.25)l.sendParticles(ParticleTypes.END_ROD,q.x,q.y+0.05,q.z,1,0.04,0.04,0.04,0);}}else if(id.equals(STAMPEDE)){for(int i=0;i<3;i++)if(i!=safeLane)for(Vec3 p:lane(i))if(p.distanceToSqr(origin)%2<0.2)l.sendParticles(ParticleTypes.CLOUD,p.x,p.y+0.05,p.z,2,0.07,0.02,0.07,0);}else telegraphLobe(l,3);}
    private void resolveRays(ServerLevel l){for(int i=0;i<3;i++)if(!plates.get(i)){var pts=ray(i);for(Vec3 p:pts)l.sendParticles(ParticleTypes.FLAME,p.x,p.y,p.z,2,0.08,0.04,0.08,0.02);strike(l,pts,0.48f);}}
    private void telegraphLobe(ServerLevel l,double radius){for(int a=0;a<360;a+=12){double r=Math.toRadians(a);Vec3 p=origin.add(forward().scale(Math.cos(r)*radius)).add(side().scale(Math.sin(r)*radius));if(boss.insideArena(p))l.sendParticles(ParticleTypes.FLAME,p.x,p.y+0.06,p.z,2,0.08,0.04,0.08,0);}}
    private void resolveLobe(ServerLevel l,double radius){var pts=new ArrayList<Vec3>();for(int a=0;a<360;a+=4){double r=Math.toRadians(a);Vec3 p=origin.add(forward().scale(Math.cos(r)*radius)).add(side().scale(Math.sin(r)*radius));if(boss.insideArena(p))pts.add(p);}strike(l,pts,0.42f);}
    private void strike(ServerLevel l,List<Vec3> pts,float scale){for(Player p:players(l))if(pts.stream().anyMatch(q->Math.abs(p.getY()-q.y)<2.8&&p.position().distanceToSqr(q)<1.7*1.7))p.hurtServer(l,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    private List<Player> players(ServerLevel l){return l.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(28),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    public boolean updatePhase(float hp,float max){boolean changed=clock.updatePhase(hp,max);if(changed)phase=2;return changed;}
    public void save(ValueOutput o){clock.save(o);o.store("AzharOrigin",Vec3.CODEC,origin);o.store("AzharMark",Vec3.CODEC,mark);o.store("AzharPlates",com.mojang.serialization.Codec.BOOL.listOf(),plates);o.putDouble("AzharHeading",heading);o.putInt("AzharPhase",phase);o.putInt("AzharSafeLane",safeLane);o.putInt("AzharEchoDelay",echoDelay);o.putInt("AzharLobePass",lobePass);}
    public void load(ValueInput i){clock.load(i);origin=i.read("AzharOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("AzharMark",Vec3.CODEC).orElse(Vec3.ZERO);plates=normalize(i.read("AzharPlates",com.mojang.serialization.Codec.BOOL.listOf()).orElse(List.of()),false);heading=i.getDoubleOr("AzharHeading",0);phase=clock.phase();safeLane=Math.floorMod(i.getIntOr("AzharSafeLane",1),3);echoDelay=Math.clamp(i.getIntOr("AzharEchoDelay",0),0,12);lobePass=Math.clamp(i.getIntOr("AzharLobePass",0),0,2);}
    private static <T> List<T> normalize(List<T> v,T d){var r=new ArrayList<T>(3);for(int n=0;n<3;n++)r.add(n<v.size()?v.get(n):d);return List.copyOf(r);}
}
