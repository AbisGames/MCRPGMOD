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

/** Announced trident fans, an expanding pressure field, and a shootable-source mine chain. */
public final class ThalassorCombat {
 public static final String FAN="trident_fan",PRESSURE="pressure_bubbles",MINES="abyssal_mines";
 private final RegionalBossEntity boss;private final RegionalCombatClock clock;private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO;private double heading;private List<Boolean> broken=List.of(false,false,false,false);private int phase=1,safeGap=3;
 public ThalassorCombat(RegionalBossEntity b){boss=b;var d=BossDesigns.require("thalassor_deep_tyrant");clock=new RegionalCombatClock(Map.of(FAN,time(d,0),PRESSURE,time(d,1),MINES,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,55)));}
 private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
 public RegionalCombatClock clock(){return clock;}public int brokenMines(){return(int)broken.stream().filter(Boolean::booleanValue).count();}public int safeGap(){return safeGap;}public Vec3 mine(int i){if(i<0||i>=4)throw new IndexOutOfBoundsException();return anchor(i);}public Vec3 chainPoint(int i){if(i<0||i>=4)throw new IndexOutOfBoundsException();return chain(i).get(12);}public Vec3 fanPoint(int ray,int n){if(ray<0||ray>=7)throw new IndexOutOfBoundsException();return fan(ray).get(Math.clamp(n,0,fan(ray).size()-1));}
 public boolean begin(String id,LivingEntity t){if(t==null||!t.isAlive()||!boss.insideArena(t.position())||!clock.begin(id))return false;origin=boss.position();mark=t.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();if(phase==2){safeGap=(safeGap+2)%7;heading+=.12;boss.playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT,1.15f,.7f);}if(id.equals(MINES))broken=List.of(false,false,false,false);return true;}
 private Vec3 forward(){return new Vec3(Math.cos(heading),0,Math.sin(heading));}private Vec3 side(){Vec3 f=forward();return new Vec3(-f.z,0,f.x);}private Vec3 anchor(int i){double a=heading+i*Math.PI/2;return origin.add(Math.cos(a)*6,0,Math.sin(a)*6);}
 private List<Vec3> segment(Vec3 a,Vec3 b){var p=new ArrayList<Vec3>();for(int i=0;i<=24;i++){Vec3 q=a.lerp(b,i/24.0);if(boss.insideArena(q))p.add(q);}return p;}
 private List<Vec3> fan(int i){var p=new ArrayList<Vec3>();double a=heading+(i-3)*.2;Vec3 d=new Vec3(Math.cos(a),0,Math.sin(a));for(int n=2;n<=15;n++){Vec3 q=origin.add(d.scale(n));if(boss.insideArena(q))p.add(q);}return p;}
 private List<Vec3> circle(double radius){var p=new ArrayList<Vec3>();for(int a=0;a<360;a+=8){double r=Math.toRadians(a);Vec3 q=origin.add(Math.cos(r)*radius,0,Math.sin(r)*radius);if(boss.insideArena(q))p.add(q);}return p;}
 private List<Vec3> disk(double radius){var p=new ArrayList<Vec3>();for(double r=1;r<=radius;r+=1.35)p.addAll(circle(r));return p;}
 private List<Vec3> chain(int i){return segment(anchor(i),anchor((i+1)%4));}
 public void tick(ServerLevel l){String id=clock.active();if(id.equals(MINES)&&!clock.resolved())checkMines(l);if(!id.isEmpty()&&!clock.resolved()&&clock.remaining()%5==0)telegraph(l,id);if(id.isEmpty()){clock.tick();return;}String impact=clock.tick();if(impact.isEmpty())return;
  switch(impact){case FAN->{for(int i=0;i<7;i++)if(i!=safeGap)strike(l,fan(i),.24f);boss.playSound(SoundEvents.TRIDENT_THROW.value(),1.1f,.8f);}case PRESSURE->{strike(l,disk(11),.46f);boss.playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,1.2f,.6f);}case MINES->{if(!broken.get(0)){for(int i=1;i<4;i++)if(!broken.get(i))strike(l,chain(i),.32f);strike(l,segment(anchor(0),origin),.32f);}boss.playSound(SoundEvents.GENERIC_EXPLODE.value(),1.15f,.7f);}case "basic"->strike(l,List.of(mark),.7f);default->{}}}
 private void checkMines(ServerLevel l){var next=new ArrayList<>(broken);for(int i=0;i<4;i++)if(!next.get(i)){Vec3 p=anchor(i);var shots=l.getEntitiesOfClass(Projectile.class,new net.minecraft.world.phys.AABB(p.add(-1,-1,-1),p.add(1,1.5,1)),Projectile::isAlive);if(!shots.isEmpty()){next.set(i,true);shots.forEach(Projectile::discard);l.sendParticles(ParticleTypes.POOF,p.x,p.y,p.z,16,.3,.25,.3,.02);}}broken=List.copyOf(next);}
 private void telegraph(ServerLevel l,String id){if(id.equals(FAN)){for(int i=0;i<7;i++)for(Vec3 p:fan(i))if((int)(p.x+p.z)%4==0)l.sendParticles(i==safeGap?ParticleTypes.HAPPY_VILLAGER:ParticleTypes.SPLASH,p.x,p.y+.25,p.z,1,.04,.04,.04,0);}else if(id.equals(PRESSURE)){var timing=BossDesigns.require("thalassor_deep_tyrant").abilities().get(1);double progress=1-(clock.remaining()-timing.recovery())/(double)timing.windup(),radius=2+9*Math.clamp(progress,0,1);for(Vec3 p:circle(radius))if((int)(p.x+p.z)%4==0)l.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,p.x,p.y+.1,p.z,1,.03,.03,.03,0);}else for(int i=0;i<4;i++){Vec3 p=anchor(i);l.sendParticles(broken.get(i)?ParticleTypes.POOF:ParticleTypes.BUBBLE,p.x,p.y+.2,p.z,9,.2,.2,.2,0);if(!broken.get(i))for(Vec3 q:chain(i))if((int)(q.x+q.z)%3==0)l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,q.x,q.y+.1,q.z,1,.02,.03,.02,0);}}
 private void strike(ServerLevel l,List<Vec3> points,float scale){for(Player p:players(l))if(points.stream().anyMatch(q->Math.abs(q.y-p.getY())<3.1&&q.distanceToSqr(p.position())<1.75*1.75))p.hurtServer(l,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
 private List<Player> players(ServerLevel l){return l.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(28),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
 public boolean updatePhase(float hp,float max){boolean c=clock.updatePhase(hp,max);if(c)phase=2;return c;}
 public void save(ValueOutput o){clock.save(o);o.store("ThalassorOrigin",Vec3.CODEC,origin);o.store("ThalassorMark",Vec3.CODEC,mark);o.store("ThalassorBroken",com.mojang.serialization.Codec.BOOL.listOf(),broken);o.putDouble("ThalassorHeading",heading);o.putInt("ThalassorPhase",phase);o.putInt("ThalassorSafeGap",safeGap);}
 public void load(ValueInput i){clock.load(i);origin=i.read("ThalassorOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("ThalassorMark",Vec3.CODEC).orElse(Vec3.ZERO);broken=i.read("ThalassorBroken",com.mojang.serialization.Codec.BOOL.listOf()).orElse(List.of(false,false,false,false)).stream().limit(4).toList();if(broken.size()<4){var v=new ArrayList<>(broken);while(v.size()<4)v.add(false);broken=List.copyOf(v);}heading=i.getDoubleOr("ThalassorHeading",0);phase=clock.phase();safeGap=Math.floorMod(i.getIntOr("ThalassorSafeGap",3),7);}
}
