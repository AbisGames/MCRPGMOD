package com.pipican.boss;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Locked jade breath route, spiral dive with refuges and ordered five-orb pulses. */
public final class LongweiCombat {
    public static final String RIBBON="jade_ribbon", SPIRAL="spiral_descent", ORBITS="five_orbits";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO;
    private List<Vec3> ribbon=List.of();
    private double heading,safeAngle;
    private int phase=1,orbitCycle,pulseIndex=-1,pulseDelay,followupTicks;
    public LongweiCombat(RegionalBossEntity boss){this.boss=boss;var d=BossDesigns.require("longwei_jade_dragon");clock=new RegionalCombatClock(Map.of(RIBBON,time(d,0),SPIRAL,time(d,1),ORBITS,time(d,2),"basic",new RegionalCombatClock.Timing(20,22,52)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}
    public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();safeAngle=heading+Math.PI*0.8+orbitCycle*0.18;
        if(id.equals(RIBBON))ribbon=buildRibbon();if(id.equals(ORBITS)){pulseIndex=0;pulseDelay=0;}return true;}
    public void tick(ServerLevel level){
        if(followupTicks>0){telegraph(level,spiralPath());if(--followupTicks==0){resolveSpiral(level);boss.playSound(SoundEvents.ENDER_DRAGON_FLAP,1.2f,0.85f);}}
        if(!clock.active().isEmpty()&&clock.active().equals(ORBITS)&&clock.resolved()&&pulseIndex>=0&&pulseIndex<5){if(pulseDelay>0)pulseDelay--;else{pulse(level,orbPoints().get(pulseIndex));pulseIndex++;if(pulseIndex==5)orbitCycle++;pulseDelay=8;}}
        String active=clock.active();if(active.isEmpty()){clock.tick();return;}if(!clock.resolved()&&clock.remaining()%5==0)telegraph(level,active);String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case RIBBON->{strikePath(level,ribbon,0.8f);if(phase==2)followupTicks=16;boss.playSound(SoundEvents.ENDER_DRAGON_SHOOT,1.2f,0.75f);}
            case SPIRAL->{resolveSpiral(level);orbitCycle++;boss.playSound(SoundEvents.ENDER_DRAGON_FLAP,1.5f,0.65f);}
            case ORBITS->{pulseIndex=0;pulseDelay=1;boss.playSound(SoundEvents.AMETHYST_BLOCK_CHIME,1.4f,0.7f);}
            case "basic"->strikePath(level,List.of(mark),0.72f);default->{}}
    }
    private List<Vec3> buildRibbon(){int count=48;double length=phase==2?17:22;var points=new ArrayList<Vec3>(count+1);double side=heading+Math.PI/2;
        for(int i=0;i<=count;i++){double t=i/(double)count,d=length*t,lateral=Math.sin(t*Math.PI*4)*1.25+Math.sin(t*Math.PI*2)*0.55;Vec3 p=origin.add(Math.cos(heading)*d+Math.cos(side)*lateral,0.1,Math.sin(heading)*d+Math.sin(side)*lateral);if(boss.insideArena(p))points.add(p);}return List.copyOf(points);}
    private List<Vec3> spiralPath(){var points=new ArrayList<Vec3>(97);int count=96;for(int i=0;i<=count;i++){double t=i/(double)count*Math.PI*3,r=2.5+t/(Math.PI*3)*6.4,a=heading+t;
            double gap=Math.abs(Math.atan2(Math.sin(a-safeAngle),Math.cos(a-safeAngle)));if(r>3&&gap<0.34)continue;Vec3 p=origin.add(Math.cos(a)*r,0.15,Math.sin(a)*r);if(boss.insideArena(p))points.add(p);}return points;}
    private List<Vec3> orbPoints(){var points=new ArrayList<Vec3>(5);for(int i=0;i<5;i++){double a=heading+i*Math.PI*2/5+orbitCycle*0.13;points.add(origin.add(Math.cos(a)*6.5,0,Math.sin(a)*6.5));}return points;}
    public Vec3 orbPosition(int index){if(index<0||index>=5)throw new IndexOutOfBoundsException("Longwei orb index must be in [0,4]");return orbPoints().get(index);}
    private List<Player> players(ServerLevel level){return level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(28),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    private void telegraph(ServerLevel level,String id){if(id.equals(RIBBON))telegraph(level,ribbon);else if(id.equals(SPIRAL)){telegraph(level,spiralPath());level.sendParticles(ParticleTypes.END_ROD,origin.x,origin.y+0.25,origin.z,16,0.35,0.06,0.35,0);}
        else if(id.equals(ORBITS)){var nodes=orbPoints();for(int i=0;i<nodes.size();i++){var p=nodes.get(i);var particle=i==Math.max(0,pulseIndex)?ParticleTypes.END_ROD:ParticleTypes.HAPPY_VILLAGER;level.sendParticles(particle,p.x,p.y+0.25,p.z,7,0.2,0.15,0.2,0.01);}}
        else{level.sendParticles(ParticleTypes.HAPPY_VILLAGER,mark.x,mark.y+0.1,mark.z,8,0.3,0.1,0.3,0.01);}}
    private void telegraph(ServerLevel level,List<Vec3> points){for(int i=0;i<points.size();i+=2){Vec3 p=points.get(i);level.sendParticles(ParticleTypes.HAPPY_VILLAGER,p.x,p.y+0.12,p.z,1,0,0,0,0);}}
    private void strikePath(ServerLevel level,List<Vec3> path,float scale){for(Player p:players(level))if(path.stream().anyMatch(v->Math.abs(v.y-p.getY())<3&&v.distanceToSqr(p.position())<=1.75*1.75))damage(level,p,scale);}
    private void resolveSpiral(ServerLevel level){strikePath(level,spiralPath(),0.82f);}
    private void pulse(ServerLevel level,Vec3 point){level.sendParticles(ParticleTypes.END_ROD,point.x,point.y+0.2,point.z,12,0.28,0.12,0.28,0.02);for(Player p:players(level))if(Math.abs(p.getY()-point.y)<3&&p.position().distanceToSqr(point)<=2.35*2.35)damage(level,p,0.75f);}
    private void damage(ServerLevel level,Player p,float scale){p.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    public boolean updatePhase(float health,float max){boolean changed=clock.updatePhase(health,max);if(changed)phase=2;return changed;}
    public void save(ValueOutput o){clock.save(o);o.store("LongweiOrigin",Vec3.CODEC,origin);o.store("LongweiMark",Vec3.CODEC,mark);o.store("LongweiRibbon",Vec3.CODEC.listOf(),ribbon);o.putDouble("LongweiHeading",heading);o.putDouble("LongweiSafeAngle",safeAngle);o.putInt("LongweiPhase",phase);o.putInt("LongweiOrbitCycle",orbitCycle);o.putInt("LongweiPulseIndex",pulseIndex);o.putInt("LongweiPulseDelay",pulseDelay);o.putInt("LongweiFollowupTicks",followupTicks);}
    public void load(ValueInput i){clock.load(i);origin=i.read("LongweiOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("LongweiMark",Vec3.CODEC).orElse(Vec3.ZERO);ribbon=i.read("LongweiRibbon",Vec3.CODEC.listOf()).orElse(List.of()).stream().limit(64).toList();heading=i.getDoubleOr("LongweiHeading",0);safeAngle=i.getDoubleOr("LongweiSafeAngle",Math.PI);phase=i.getIntOr("LongweiPhase",clock.phase())==2?2:1;orbitCycle=Math.max(0,i.getIntOr("LongweiOrbitCycle",0));pulseIndex=Math.clamp(i.getIntOr("LongweiPulseIndex",-1),-1,5);pulseDelay=Math.clamp(i.getIntOr("LongweiPulseDelay",0),0,8);followupTicks=Math.clamp(i.getIntOr("LongweiFollowupTicks",0),0,16);}
}
