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

/** Locked triangle charges, fan volleys with an announced gap, and breakable rally banners. */
public final class TengriCombat {
    public static final String TRIANGLE="steppe_triangle",VOLLEY="falcon_volley",BANNER="khan_banner";
    private final RegionalBossEntity boss;private final RegionalCombatClock clock;private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO;private double heading;
    private List<Boolean> broken=List.of(false,false,false);private int phase=1,safeGap=2,chargeIndex,chargeDelay;
    public TengriCombat(RegionalBossEntity boss){this.boss=boss;var d=BossDesigns.require("tengri_champion");clock=new RegionalCombatClock(Map.of(TRIANGLE,time(d,0),VOLLEY,time(d,1),BANNER,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,55)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}public int brokenBanners(){return(int)broken.stream().filter(Boolean::booleanValue).count();}public int safeGap(){return safeGap;}public int chargeIndex(){return chargeIndex;}public Vec3 banner(int i){if(i<0||i>=3)throw new IndexOutOfBoundsException();return lane(i).getLast();}public Vec3 triangleEdgePoint(int i){if(i<0||i>=3)throw new IndexOutOfBoundsException();return line(vertex(i),vertex((i+1)%3)).get(12);}public Vec3 fanPoint(int i,int n){if(i<0||i>=5)throw new IndexOutOfBoundsException();return fan(i).get(Math.clamp(n,0,fan(i).size()-1));}
    public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();if(phase==2){safeGap=(safeGap+1)%5;heading+=.14;boss.playSound(SoundEvents.RAVAGER_ROAR,1.15f,.8f);}if(id.equals(BANNER)){broken=List.of(false,false,false);chargeIndex=0;}return true;}
    private Vec3 forward(){return new Vec3(Math.cos(heading),0,Math.sin(heading));}private Vec3 side(){Vec3 f=forward();return new Vec3(-f.z,0,f.x);}
    private Vec3 vertex(int i){double a=heading+Math.PI*2*i/3;return origin.add(Math.cos(a)*7,0,Math.sin(a)*7);}
    private List<Vec3> line(Vec3 a,Vec3 b){var p=new ArrayList<Vec3>();for(int i=0;i<=24;i++){Vec3 q=a.lerp(b,i/24.0);if(boss.insideArena(q))p.add(q);}return p;}
    private List<Vec3> lane(int i){var p=new ArrayList<Vec3>();Vec3 s=side(),f=forward();double lateral=(i-1)*5;for(int n=0;n<=24;n++){Vec3 q=origin.add(s.scale(lateral)).add(f.scale(-6+n*.5));if(boss.insideArena(q))p.add(q);}return p;}
    private List<Vec3> fan(int i){var p=new ArrayList<Vec3>();double angle=heading+(i-2)*.24;Vec3 dir=new Vec3(Math.cos(angle),0,Math.sin(angle));for(int n=2;n<=14;n++){Vec3 q=origin.add(dir.scale(n));if(boss.insideArena(q))p.add(q);}return p;}
    public void tick(ServerLevel level){String id=clock.active();if(id.equals(BANNER)&&!clock.resolved())checkBanners(level);if(!id.isEmpty()&&!clock.resolved()&&clock.remaining()%5==0)telegraph(level,id);if(id.equals(TRIANGLE)&&clock.resolved()&&chargeIndex>0&&chargeIndex<3&&--chargeDelay<=0){strike(level,line(vertex(chargeIndex),vertex((chargeIndex+1)%3)),.43f);chargeIndex++;chargeDelay=6;boss.playSound(SoundEvents.HORSE_GALLOP,1.2f,.8f);}if(id.isEmpty()){clock.tick();return;}String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case TRIANGLE->{chargeIndex=1;chargeDelay=6;strike(level,line(vertex(0),vertex(1)),.43f);boss.playSound(SoundEvents.HORSE_GALLOP,1.2f,.8f);}case VOLLEY->{for(int i=0;i<5;i++)if(i!=safeGap)strike(level,fan(i),.28f);boss.playSound(SoundEvents.ARROW_SHOOT,1.1f,.75f);}case BANNER->{var points=new ArrayList<Vec3>();for(int i=0;i<3;i++)if(!broken.get(i))points.addAll(lane(i));strike(level,points,.56f);boss.playSound(SoundEvents.RAID_HORN.value(),1.05f,.9f);}case "basic"->strike(level,List.of(mark),.68f);default->{}}}
    private void checkBanners(ServerLevel l){var next=new ArrayList<>(broken);for(int i=0;i<3;i++)if(!next.get(i)){Vec3 p=banner(i);var shots=l.getEntitiesOfClass(Projectile.class,new net.minecraft.world.phys.AABB(p.add(-1,-1,-1),p.add(1,1.5,1)),Projectile::isAlive);if(!shots.isEmpty()){next.set(i,true);shots.forEach(Projectile::discard);l.sendParticles(ParticleTypes.POOF,p.x,p.y,p.z,14,.25,.2,.25,.02);}}broken=List.copyOf(next);}
    private void telegraph(ServerLevel l,String id){if(id.equals(TRIANGLE)){for(int i=0;i<3;i++)for(Vec3 p:line(vertex(i),vertex((i+1)%3)))if((int)(p.x+p.z)%3==0)l.sendParticles(ParticleTypes.CRIT,p.x,p.y+.1,p.z,1,.03,.03,.03,0);}else if(id.equals(VOLLEY)){for(int i=0;i<5;i++)for(Vec3 p:fan(i))if((int)(p.x+p.z)%3==0)l.sendParticles(i==safeGap?ParticleTypes.HAPPY_VILLAGER:ParticleTypes.CRIT,p.x,p.y+.2,p.z,1,.03,.03,.03,0);}else for(int i=0;i<3;i++){Vec3 b=banner(i);l.sendParticles(broken.get(i)?ParticleTypes.POOF:ParticleTypes.SOUL_FIRE_FLAME,b.x,b.y+.4,b.z,8,.2,.4,.2,0);for(Vec3 p:lane(i))if((int)(p.x+p.z)%3==0)l.sendParticles(i==safeGap?ParticleTypes.HAPPY_VILLAGER:ParticleTypes.CRIT,p.x,p.y+.1,p.z,1,.03,.03,.03,0);}}
    private void strike(ServerLevel l,List<Vec3> points,float scale){for(Player p:players(l))if(points.stream().anyMatch(q->Math.abs(p.getY()-q.y)<2.8&&p.position().distanceToSqr(q)<1.7*1.7))p.hurtServer(l,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    private List<Player> players(ServerLevel l){return l.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(28),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    public boolean updatePhase(float hp,float max){boolean changed=clock.updatePhase(hp,max);if(changed)phase=2;return changed;}
    public void save(ValueOutput o){clock.save(o);o.store("TengriOrigin",Vec3.CODEC,origin);o.store("TengriMark",Vec3.CODEC,mark);o.store("TengriBroken",com.mojang.serialization.Codec.BOOL.listOf(),broken);o.putDouble("TengriHeading",heading);o.putInt("TengriPhase",phase);o.putInt("TengriSafeGap",safeGap);o.putInt("TengriChargeIndex",chargeIndex);o.putInt("TengriChargeDelay",chargeDelay);}
    public void load(ValueInput i){clock.load(i);origin=i.read("TengriOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("TengriMark",Vec3.CODEC).orElse(Vec3.ZERO);broken=i.read("TengriBroken",com.mojang.serialization.Codec.BOOL.listOf()).orElse(List.of(false,false,false)).stream().limit(3).toList();if(broken.size()<3){var v=new ArrayList<>(broken);while(v.size()<3)v.add(false);broken=List.copyOf(v);}heading=i.getDoubleOr("TengriHeading",0);phase=clock.phase();safeGap=Math.floorMod(i.getIntOr("TengriSafeGap",2),5);chargeIndex=Math.clamp(i.getIntOr("TengriChargeIndex",0),0,3);chargeDelay=Math.clamp(i.getIntOr("TengriChargeDelay",0),0,6);}
}
