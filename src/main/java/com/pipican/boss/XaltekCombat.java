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

/** Seed-pod snares, interruptible tongue lashes, and locked canopy pounces. */
public final class XaltekCombat {
    public static final String SNARES="thorn_snares", TONGUE="tongue_line", POUNCE="canopy_pounce";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO,landing=Vec3.ZERO;
    private double heading;
    private List<Vec3> pods=List.of(Vec3.ZERO,Vec3.ZERO,Vec3.ZERO);
    private List<Boolean> broken=List.of(false,false,false);
    private int phase=1,laneShift,exposureTicks;
    private boolean tongueCut;
    public XaltekCombat(RegionalBossEntity boss){this.boss=boss;var d=BossDesigns.require("xaltek_emerald_devourer");clock=new RegionalCombatClock(Map.of(SNARES,time(d,0),TONGUE,time(d,1),POUNCE,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,50)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}
    public int brokenPods(){return (int)broken.stream().filter(Boolean::booleanValue).count();}
    public int exposureTicks(){return exposureTicks;}
    public Vec3 landing(){return landing;}
    public Vec3 podPosition(int index){if(index<0||index>=3)throw new IndexOutOfBoundsException("Xaltek pod index must be in [0,2]");return pods.get(index);}
    public float damageMultiplier(){return phase==2&&exposureTicks>0?1.35f:1f;}
    public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;
        origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();if(phase==2){laneShift=(laneShift+1)%3;heading+=0.11;boss.playSound(SoundEvents.ROOTED_DIRT_BREAK,1.1f,0.65f);}
        Vec3 f=forward(),s=side();pods=List.of(mark.add(f.scale(2.4)),mark.add(f.scale(-1.2)).add(s.scale(2.1)),mark.add(f.scale(-1.2)).subtract(s.scale(2.1)));broken=List.of(false,false,false);tongueCut=false;
        landing=mark.add(s.scale(phase==2?(laneShift-1)*2.2:0));if(!boss.insideArena(landing))landing=origin.add(f.scale(5)).add(s.scale((laneShift-1)*1.5));return true;}
    public void tick(ServerLevel level){if(exposureTicks>0)exposureTicks--;String active=clock.active();if(active.equals(SNARES)&&!clock.resolved())checkPods(level);if(active.equals(TONGUE)&&!clock.resolved())checkTongue(level);
        if(!active.isEmpty()&&!clock.resolved()&&clock.remaining()%5==0)telegraph(level,active);
        if(active.isEmpty()){clock.tick();return;}String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case SNARES->{List<Vec3> live=new ArrayList<>();for(int i=0;i<3;i++)if(!broken.get(i))live.addAll(line(pods.get(i),pods.get((i+1)%3)));strike(level,live,0.48f);if(phase==2&&brokenPods()==3)exposureTicks=70;boss.playSound(SoundEvents.BEEHIVE_WORK,1.1f,0.6f);}
            case TONGUE->{if(!tongueCut){strike(level,line(origin.add(0,1.4,0),mark.add(0,0.5,0)),0.62f);var t=players(level).stream().filter(p->p.position().distanceToSqr(mark)<3.2*3.2).findFirst().orElse(null);if(t!=null){Vec3 pull=origin.subtract(t.position()).normalize().scale(0.42);t.push(pull.x,0.08,pull.z);}}boss.playSound(SoundEvents.SLIME_BLOCK_PLACE,1.2f,0.7f);}
            case POUNCE->{boss.teleportTo(landing.x,landing.y,landing.z);strike(level,List.of(landing),0.72f);boss.playSound(SoundEvents.RAVAGER_ATTACK,1.1f,0.7f);}
            case "basic"->strike(level,List.of(mark),0.68f);default->{}}
    }
    private Vec3 forward(){return new Vec3(Math.cos(heading),0,Math.sin(heading));}
    private Vec3 side(){Vec3 f=forward();return new Vec3(-f.z,0,f.x);}
    private List<Vec3> line(Vec3 a,Vec3 b){int n=Math.max(1,(int)(Math.sqrt(a.distanceToSqr(b))*3));var points=new ArrayList<Vec3>();for(int i=0;i<=n;i++){Vec3 p=a.lerp(b,i/(double)n);if(boss.insideArena(p))points.add(p);}return points;}
    private void checkPods(ServerLevel level){var state=new ArrayList<>(broken);for(int i=0;i<3;i++)if(!state.get(i)){Vec3 p=pods.get(i);var shots=level.getEntitiesOfClass(Projectile.class,new net.minecraft.world.phys.AABB(p.add(-1,-1,-1),p.add(1,1.5,1)),Projectile::isAlive);if(!shots.isEmpty()){state.set(i,true);shots.forEach(Projectile::discard);level.sendParticles(ParticleTypes.COMPOSTER,p.x,p.y+0.2,p.z,16,0.35,0.2,0.35,0.02);}}broken=List.copyOf(state);}
    private void checkTongue(ServerLevel level){Vec3 tip=mark.add(0,0.5,0);var shots=level.getEntitiesOfClass(Projectile.class,new net.minecraft.world.phys.AABB(tip.add(-1,-1,-1),tip.add(1,1,1)),Projectile::isAlive);if(!shots.isEmpty()){tongueCut=true;shots.forEach(Projectile::discard);level.sendParticles(ParticleTypes.CRIT,tip.x,tip.y,tip.z,14,0.3,0.3,0.3,0.03);}}
    private void telegraph(ServerLevel level,String id){if(id.equals(SNARES)){for(int i=0;i<3;i++){Vec3 p=pods.get(i);level.sendParticles(broken.get(i)?ParticleTypes.POOF:ParticleTypes.HAPPY_VILLAGER,p.x,p.y+0.2,p.z,8,0.25,0.15,0.25,0);if(!broken.get(i)){for(Vec3 q:line(p,pods.get((i+1)%3))){if(q.distanceToSqr(p)%2<0.25)level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,q.x,q.y+0.1,q.z,1,0.05,0.02,0.05,0);}}}}
        else if(id.equals(TONGUE)){for(Vec3 p:line(origin.add(0,1,0),mark.add(0,0.5,0))){if(p.distanceToSqr(origin)%2<0.3)level.sendParticles(ParticleTypes.CRIT,p.x,p.y,p.z,1,0.05,0.03,0.05,0);}level.sendParticles(ParticleTypes.ANGRY_VILLAGER,mark.x,mark.y+0.7,mark.z,3,0.15,0.2,0.15,0);}
        else level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,landing.x,landing.y+0.1,landing.z,16,0.45,0.08,0.45,0.005);}
    private void strike(ServerLevel level,List<Vec3> points,float scale){for(Player p:players(level))if(points.stream().anyMatch(q->Math.abs(p.getY()-q.y)<2.8&&p.position().distanceToSqr(q)<=1.7*1.7))p.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    private List<Player> players(ServerLevel level){return level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(28),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    public boolean updatePhase(float health,float max){boolean changed=clock.updatePhase(health,max);if(changed)phase=2;return changed;}
    public void save(ValueOutput o){clock.save(o);o.store("XaltekOrigin",Vec3.CODEC,origin);o.store("XaltekMark",Vec3.CODEC,mark);o.store("XaltekLanding",Vec3.CODEC,landing);o.store("XaltekPods",Vec3.CODEC.listOf(),pods);o.store("XaltekBroken",com.mojang.serialization.Codec.BOOL.listOf(),broken);o.putDouble("XaltekHeading",heading);o.putInt("XaltekPhase",phase);o.putInt("XaltekShift",laneShift);o.putInt("XaltekExposure",exposureTicks);o.putBoolean("XaltekTongueCut",tongueCut);}
    public void load(ValueInput i){clock.load(i);origin=i.read("XaltekOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("XaltekMark",Vec3.CODEC).orElse(Vec3.ZERO);landing=i.read("XaltekLanding",Vec3.CODEC).orElse(Vec3.ZERO);pods=normalize(i.read("XaltekPods",Vec3.CODEC.listOf()).orElse(List.of()),Vec3.ZERO);broken=normalize(i.read("XaltekBroken",com.mojang.serialization.Codec.BOOL.listOf()).orElse(List.of()),false);heading=i.getDoubleOr("XaltekHeading",0);phase=clock.phase();laneShift=Math.floorMod(i.getIntOr("XaltekShift",0),3);exposureTicks=Math.clamp(i.getIntOr("XaltekExposure",0),0,70);tongueCut=i.getBooleanOr("XaltekTongueCut",false);}
    private static <T> List<T> normalize(List<T> v,T fallback){var r=new ArrayList<T>(3);for(int n=0;n<3;n++)r.add(n<v.size()?v.get(n):fallback);return List.copyOf(r);}
}
