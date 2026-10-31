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

/** Telegraphs rune circuits, one-wall ricochets, and a rune-safe hammer stamp. */
public final class DurmagonCombat {
 public static final String CIRCUIT="rune_circuit",REBOUNDS="forge_rebounds",STAMP="gravity_stamp";
 private final RegionalBossEntity boss;private final RegionalCombatClock clock;private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO;private double heading;private List<Boolean> broken=List.of(false,false,false,false);private int phase=1,safePad=1;
 public DurmagonCombat(RegionalBossEntity b){boss=b;var d=BossDesigns.require("durmagon_runic_titan");clock=new RegionalCombatClock(Map.of(CIRCUIT,time(d,0),REBOUNDS,time(d,1),STAMP,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,55)));}
 private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
 public RegionalCombatClock clock(){return clock;}public int brokenRunes(){return(int)broken.stream().filter(Boolean::booleanValue).count();}public int safePad(){return safePad;}public Vec3 rune(int i){if(i<0||i>=4)throw new IndexOutOfBoundsException();return anchor(i);}public Vec3 reboundPoint(int ray,int segment){if(ray<0||ray>=3)throw new IndexOutOfBoundsException();return rebound(ray).get(Math.clamp(segment,0,rebound(ray).size()-1));}
 public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();if(phase==2){safePad=(safePad+1)%4;heading+=.13;boss.playSound(SoundEvents.ANVIL_LAND,1.05f,.75f);}if(id.equals(CIRCUIT))broken=List.of(false,false,false,false);return true;}
 private Vec3 anchor(int i){double a=heading+Math.PI/2*i;return origin.add(Math.cos(a)*6,0,Math.sin(a)*6);}
 private List<Vec3> segment(Vec3 a,Vec3 b){var p=new ArrayList<Vec3>();for(int i=0;i<=24;i++){Vec3 q=a.lerp(b,i/24.0);if(boss.insideArena(q))p.add(q);}return p;}
 private List<Vec3> circuit(int i){return segment(anchor(i),anchor((i+1)%4));}
 private List<Vec3> rebound(int i){var p=new ArrayList<Vec3>();double a=heading+(i-1)*.25;Vec3 d=new Vec3(Math.cos(a),0,Math.sin(a));for(int n=2;n<=12;n++){Vec3 q=origin.add(d.scale(n));if(boss.insideArena(q))p.add(q);}Vec3 wall=origin.add(d.scale(12));Vec3 reflected=new Vec3(-d.z,0,d.x);for(int n=1;n<=7;n++){Vec3 q=wall.add(reflected.scale(n));if(boss.insideArena(q))p.add(q);}return p;}
 private List<Vec3> stampPads(){var p=new ArrayList<Vec3>();for(int i=0;i<4;i++)if(i==safePad)p.add(anchor(i));return p;}
 public void tick(ServerLevel l){String id=clock.active();if(id.equals(CIRCUIT)&&!clock.resolved())checkRunes(l);if(!id.isEmpty()&&!clock.resolved()&&clock.remaining()%5==0)telegraph(l,id);if(id.isEmpty()){clock.tick();return;}String impact=clock.tick();if(impact.isEmpty())return;switch(impact){case CIRCUIT->{for(int i=0;i<4;i++)if(!broken.get(i))strike(l,circuit(i),.38f);boss.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER,1f,.8f);}case REBOUNDS->{for(int i=0;i<3;i++)strike(l,rebound(i),.42f);boss.playSound(SoundEvents.FIRECHARGE_USE,1.1f,.7f);}case STAMP->{for(int i=0;i<4;i++)if(i!=safePad)strike(l,List.of(anchor(i)),.65f);strike(l,segment(origin.add(-5,0,-5),origin.add(5,0,5)),.28f);boss.playSound(SoundEvents.ANVIL_LAND,1.2f,.65f);}case "basic"->strike(l,List.of(mark),.68f);default->{}}}
 private void checkRunes(ServerLevel l){var n=new ArrayList<>(broken);for(int i=0;i<4;i++)if(!n.get(i)){Vec3 p=anchor(i);var shots=l.getEntitiesOfClass(Projectile.class,new net.minecraft.world.phys.AABB(p.add(-1,-1,-1),p.add(1,1.5,1)),Projectile::isAlive);if(!shots.isEmpty()){n.set(i,true);shots.forEach(Projectile::discard);l.sendParticles(ParticleTypes.POOF,p.x,p.y,p.z,14,.25,.2,.25,.02);}}broken=List.copyOf(n);}
 private void telegraph(ServerLevel l,String id){if(id.equals(CIRCUIT)){for(int i=0;i<4;i++){Vec3 a=anchor(i);l.sendParticles(broken.get(i)?ParticleTypes.POOF:ParticleTypes.END_ROD,a.x,a.y+.3,a.z,8,.2,.3,.2,0);if(!broken.get(i))for(Vec3 p:circuit(i))if((int)(p.x+p.z)%3==0)l.sendParticles(ParticleTypes.ELECTRIC_SPARK,p.x,p.y+.1,p.z,1,.02,.04,.02,0);}}else if(id.equals(REBOUNDS)){for(int i=0;i<3;i++)for(Vec3 p:rebound(i))if((int)(p.x+p.z)%3==0)l.sendParticles(ParticleTypes.CRIT,p.x,p.y+.2,p.z,1,.03,.03,.03,0);}else{for(int i=0;i<4;i++){Vec3 p=anchor(i);l.sendParticles(i==safePad?ParticleTypes.HAPPY_VILLAGER:ParticleTypes.ASH,p.x,p.y+.1,p.z,10,.25,.05,.25,0);}for(Vec3 p:segment(origin.add(-5,0,-5),origin.add(5,0,5)))if((int)(p.x+p.z)%3==0)l.sendParticles(ParticleTypes.CRIT,p.x,p.y+.1,p.z,1,.03,.03,.03,0);}}
 private void strike(ServerLevel l,List<Vec3> points,float scale){for(Player p:l.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(28),q->q.isAlive()&&!q.isCreative()&&!q.isSpectator()&&boss.insideArena(q.position())))if(points.stream().anyMatch(q->Math.abs(q.y-p.getY())<3&&q.distanceToSqr(p.position())<1.8*1.8))p.hurtServer(l,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
 public boolean updatePhase(float hp,float max){boolean c=clock.updatePhase(hp,max);if(c)phase=2;return c;}
 public void save(ValueOutput o){clock.save(o);o.store("DurmagonOrigin",Vec3.CODEC,origin);o.store("DurmagonMark",Vec3.CODEC,mark);o.store("DurmagonBroken",com.mojang.serialization.Codec.BOOL.listOf(),broken);o.putDouble("DurmagonHeading",heading);o.putInt("DurmagonPhase",phase);o.putInt("DurmagonSafePad",safePad);}
 public void load(ValueInput i){clock.load(i);origin=i.read("DurmagonOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("DurmagonMark",Vec3.CODEC).orElse(Vec3.ZERO);broken=i.read("DurmagonBroken",com.mojang.serialization.Codec.BOOL.listOf()).orElse(List.of(false,false,false,false)).stream().limit(4).toList();if(broken.size()<4){var n=new ArrayList<>(broken);while(n.size()<4)n.add(false);broken=List.copyOf(n);}heading=i.getDoubleOr("DurmagonHeading",0);phase=clock.phase();safePad=Math.floorMod(i.getIntOr("DurmagonSafePad",1),4);}
}
