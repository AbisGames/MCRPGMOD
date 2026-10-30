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

/** Air-lane lift, interruptible feather geometry and locked aerial dive/wake attacks. */
public final class AetherionCombat {
    public static final String UPDRAFT="updraft_paths", FEATHERS="feather_constellation", DIVE="sky_dive";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO,landing=Vec3.ZERO;
    private double heading;
    private int updraftCycle,wakeDelay,phase=1;
    public AetherionCombat(RegionalBossEntity boss){this.boss=boss;var d=BossDesigns.require("aetherion_sky_guardian");clock=new RegionalCombatClock(Map.of(UPDRAFT,time(d,0),FEATHERS,time(d,1),DIVE,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,50)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}
    public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;
        origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();
        if(id.equals(DIVE)){double distance=Math.clamp(Math.sqrt(origin.distanceToSqr(mark)),6,10);landing=origin.add(Math.cos(heading)*distance,0,Math.sin(heading)*distance);}
        return true;}
    public void tick(ServerLevel level){
        if(wakeDelay>0){if(wakeDelay<=8&&wakeDelay%2==0)telegraph(level,wakeLine());if(--wakeDelay==0)strike(level,wakeLine(),0.95f);}
        String active=clock.active();if(active.isEmpty()){clock.tick();return;}
        if(!clock.resolved()&&clock.remaining()%5==0)telegraph(level,active);
        String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case UPDRAFT->{resolveUpdrafts(level);boss.playSound(SoundEvents.PHANTOM_FLAP,1.4f,0.65f);}
            case FEATHERS->{resolveFeathers(level);boss.playSound(SoundEvents.ARROW_SHOOT,1.4f,0.7f);}
            case DIVE->{strikeCircle(level,landing,3,0.95f);boss.setPos(landing.x,landing.y+2,landing.z);wakeDelay=18;boss.playSound(SoundEvents.PHANTOM_SWOOP,1.8f,0.55f);}
            case "basic"->strike(level,List.of(mark),0.75f);default->{}}
    }
    private Vec3 point(double distance,double side){double perpendicular=heading+Math.PI/2;return origin.add(Math.cos(heading)*distance+Math.cos(perpendicular)*side,0,Math.sin(heading)*distance+Math.sin(perpendicular)*side);}
    private List<Vec3> updraftCenters(){double shift=phase==2?(updraftCycle%2==0?3.2:-3.2):0;return List.of(point(7,-4+shift),point(7,shift),point(7,4+shift));}
    private List<Vec3> featherNodes(){return List.of(origin.add(5.5,0,0),origin.add(0,0,5.5),origin.add(-5.5,0,0),origin.add(0,0,-5.5));}
    private List<List<Vec3>> featherLinks(){var nodes=featherNodes();var links=new ArrayList<List<Vec3>>();for(int i=0;i<4;i++)links.add(line(nodes.get(i),nodes.get((i+1)%4)));return links;}
    private List<Vec3> line(Vec3 from,Vec3 to){int n=Math.max(1,(int)(Math.sqrt(from.distanceToSqr(to))*2));return java.util.stream.IntStream.rangeClosed(0,n).mapToObj(i->from.lerp(to,i/(double)n)).filter(boss::insideArena).toList();}
    private List<Vec3> wakeLine(){return line(landing,landing.add(-Math.cos(heading)*12,0,-Math.sin(heading)*12));}
    private void telegraph(ServerLevel level,String active){
        if(active.equals(UPDRAFT)){for(var c:updraftCenters())for(int y=0;y<=10;y+=2)level.sendParticles(ParticleTypes.CLOUD,c.x,c.y+y,c.z,4,0.22,0.12,0.22,0.01);
            double sweep=heading+Math.PI/2;for(int i=-10;i<=10;i++){var p=origin.add(Math.cos(sweep)*i,0.8,Math.sin(sweep)*i);level.sendParticles(ParticleTypes.END_ROD,p.x,p.y,p.z,1,0,0,0,0);}}
        else if(active.equals(FEATHERS)){for(var c:featherNodes())level.sendParticles(ParticleTypes.END_ROD,c.x,c.y+0.2,c.z,5,0.15,0.15,0.15,0.01);for(var link:featherLinks())telegraph(level,link);}
        else if(active.equals(DIVE)){for(int i=0;i<32;i++){double a=Math.PI*2*i/32;var p=landing.add(Math.cos(a)*3,0,Math.sin(a)*3);level.sendParticles(ParticleTypes.CLOUD,p.x,p.y+0.08,p.z,1,0,0,0,0);}level.sendParticles(ParticleTypes.END_ROD,landing.x,landing.y+0.2,landing.z,6,0.4,0.1,0.4,0.01);}
        else telegraph(level,List.of(mark));
    }
    private void telegraph(ServerLevel level,List<Vec3> points){for(int i=0;i<points.size();i+=2){var p=points.get(i);level.sendParticles(ParticleTypes.END_ROD,p.x,p.y+0.12,p.z,1,0.03,0.03,0.03,0);}}
    private List<Player> players(ServerLevel level){return level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(28),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    private void resolveUpdrafts(ServerLevel level){
        var centers=updraftCenters();var lifted=new HashSet<UUID>();
        for(var p:players(level)){for(var c:centers)if(horizontal(p.position(),c)<=1.7&&Math.abs(p.getY()-c.y)<12){p.push(0,0.72,0);lifted.add(p.getUUID());break;}}
        double sweep=heading+Math.PI/2;var a=origin.add(Math.cos(sweep)*-11,0.8,Math.sin(sweep)*-11);var b=origin.add(Math.cos(sweep)*11,0.8,Math.sin(sweep)*11);
        for(var p:players(level))if(!lifted.contains(p.getUUID())&&distanceToSegment(p.position(),a,b)<=1.3&&Math.abs(p.getY()-origin.y)<2.2)damage(level,p,0.85f);
        updraftCycle++;
    }
    private void resolveFeathers(ServerLevel level){var nodes=featherNodes();var disabled=new boolean[4];for(var p:players(level))for(int i=0;i<4;i++)if(horizontal(p.position(),nodes.get(i))<=1.35)disabled[i]=true;
        var links=featherLinks();for(int i=0;i<4;i++)if(!disabled[i]&&!disabled[(i+1)%4])strike(level,links.get(i),0.85f);}
    private static double horizontal(Vec3 a,Vec3 b){double x=a.x-b.x,z=a.z-b.z;return Math.sqrt(x*x+z*z);}
    private static double distanceToSegment(Vec3 p,Vec3 a,Vec3 b){var ab=b.subtract(a);double t=Math.clamp(p.subtract(a).dot(ab)/Math.max(1e-9,ab.lengthSqr()),0,1);return p.distanceTo(a.add(ab.scale(t)));}
    private void strike(ServerLevel level,List<Vec3> points,float multiplier){for(var p:players(level))if(points.stream().anyMatch(v->Math.abs(v.y-p.getY())<3.2&&v.distanceToSqr(p.position())<=1.8*1.8))damage(level,p,multiplier);}
    private void strikeCircle(ServerLevel level,Vec3 center,double radius,float multiplier){for(var p:players(level))if(Math.abs(center.y-p.getY())<3.2&&center.distanceToSqr(p.position())<=radius*radius)damage(level,p,multiplier);}
    private void damage(ServerLevel level,Player p,float multiplier){p.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*multiplier));}
    public boolean updatePhase(float health,float max){boolean changed=clock.updatePhase(health,max);if(changed)wakeDelay=0;return changed;}
    public void save(ValueOutput o){clock.save(o);o.store("AetherionOrigin",Vec3.CODEC,origin);o.store("AetherionMark",Vec3.CODEC,mark);o.store("AetherionLanding",Vec3.CODEC,landing);o.putDouble("AetherionHeading",heading);o.putInt("AetherionUpdraftCycle",updraftCycle);o.putInt("AetherionWakeDelay",wakeDelay);o.putInt("AetherionPhase",phase);}
    public void load(ValueInput i){clock.load(i);origin=i.read("AetherionOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("AetherionMark",Vec3.CODEC).orElse(Vec3.ZERO);landing=i.read("AetherionLanding",Vec3.CODEC).orElse(Vec3.ZERO);heading=i.getDoubleOr("AetherionHeading",0);updraftCycle=Math.max(0,i.getIntOr("AetherionUpdraftCycle",0));wakeDelay=Math.clamp(i.getIntOr("AetherionWakeDelay",0),0,18);phase=i.getIntOr("AetherionPhase",1)==2?2:1;}
}
