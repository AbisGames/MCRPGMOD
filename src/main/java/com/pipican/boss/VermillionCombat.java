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

/** Vessel tethers, staggered blade orbits and interruptible sanguine channels. */
public final class VermillionCombat {
    public static final String TETHERS="blood_tethers", ORBITS="crimson_orbits", COURT="sanguine_court";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO;
    private double heading,orbitAngle;
    private int orbitCycle,phase=1,exposedTicks;
    public VermillionCombat(RegionalBossEntity boss){this.boss=boss;var d=BossDesigns.require("vermillion_blood_monarch");clock=new RegionalCombatClock(Map.of(TETHERS,time(d,0),ORBITS,time(d,1),COURT,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,50)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}
    public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();orbitAngle=heading;return true;}
    public void tick(ServerLevel level){if(exposedTicks>0)exposedTicks--;String active=clock.active();if(active.isEmpty()){clock.tick();return;}if(!clock.resolved()&&clock.remaining()%5==0)telegraph(level,active);String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case TETHERS->{resolveTethers(level);boss.playSound(SoundEvents.CHAIN_PLACE,1.5f,0.65f);}case ORBITS->{resolveOrbits(level);orbitCycle++;boss.playSound(SoundEvents.IRON_GOLEM_ATTACK,1.3f,0.65f);}
            case COURT->{resolveCourt(level);boss.playSound(SoundEvents.EVOKER_PREPARE_SUMMON,1.5f,0.65f);}case "basic"->strike(level,List.of(mark),0.7f);default->{}}}
    private List<Vec3> vessels(){var result=new ArrayList<Vec3>(3);for(int i=-1;i<=1;i++){double angle=heading+i*0.72;result.add(origin.add(Math.cos(angle)*7,0,Math.sin(angle)*7));}return result;}
    private List<Vec3> courtiers(){var result=new ArrayList<Vec3>(3);for(int i=-1;i<=1;i++){double angle=heading+i*0.78;result.add(origin.add(Math.cos(angle)*5.5,0,Math.sin(angle)*5.5));}return result;}
    private List<Vec3> line(Vec3 a,Vec3 b){int n=Math.max(1,(int)(Math.sqrt(a.distanceToSqr(b))*2));return java.util.stream.IntStream.rangeClosed(0,n).mapToObj(i->a.lerp(b,i/(double)n)).filter(boss::insideArena).toList();}
    private List<Player> players(ServerLevel level){return level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(26),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    private void telegraph(ServerLevel level,String id){
        if(id.equals(TETHERS)){for(Vec3 v:vessels()){beam(level,origin.add(0,1.2,0),v.add(0,0.15,0),ParticleTypes.CRIMSON_SPORE);level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,v.x,v.y+0.2,v.z,8,0.22,0.12,0.22,0.01);}}
        else if(id.equals(ORBITS)){for(int lane=0;lane<3;lane++){double angle=orbitAngle+lane*Math.PI*2/3;Vec3 p=origin.add(Math.cos(angle)*6.5,0.35+lane*0.7,Math.sin(angle)*6.5);level.sendParticles(ParticleTypes.CRIMSON_SPORE,p.x,p.y,p.z,8,0.2,0.12,0.2,0.01);}level.sendParticles(ParticleTypes.END_ROD,origin.x,origin.y+0.1,origin.z,10,0.2,0.05,0.2,0);}
        else if(id.equals(COURT)){int i=0;for(Vec3 c:courtiers()){double a=heading+(i++-1)*0.78;Vec3 end=c.add(Math.cos(a)*5,0,Math.sin(a)*5);beam(level,c,end,ParticleTypes.SOUL_FIRE_FLAME);level.sendParticles(ParticleTypes.SOUL,c.x,c.y+1,c.z,10,0.2,0.4,0.2,0.01);}}
        else{level.sendParticles(ParticleTypes.CRIMSON_SPORE,mark.x,mark.y+0.1,mark.z,8,0.4,0.1,0.4,0.01);}}
    private static void beam(ServerLevel level,Vec3 a,Vec3 b,net.minecraft.core.particles.ParticleOptions particle){int n=24;for(int i=0;i<=n;i+=2){Vec3 p=a.lerp(b,i/(double)n);level.sendParticles(particle,p.x,p.y,p.z,1,0,0,0,0);}}
    private void resolveTethers(ServerLevel level){var points=vessels();var disabled=new boolean[points.size()];for(Player p:players(level))for(int i=0;i<points.size();i++)if(horizontal(p.position(),points.get(i))<=1.25)disabled[i]=true;
        int live=0;for(int i=0;i<points.size();i++){if(disabled[i])continue;live++;strike(level,line(origin.add(0,1,0),points.get(i)),0.8f);}if(live==0&&phase==2)exposedTicks=80;}
    private void resolveOrbits(ServerLevel level){double shift=orbitCycle*0.53+(phase==2?0.32:0);for(Player p:players(level)){double dx=p.getX()-origin.x,dz=p.getZ()-origin.z,r=Math.sqrt(dx*dx+dz*dz);if(r<4.5||r>8.2)continue;double a=Math.atan2(dz,dx);for(int lane=0;lane<3;lane++){double blade=orbitAngle+shift+lane*Math.PI*2/3;double delta=Math.abs(Math.atan2(Math.sin(a-blade),Math.cos(a-blade)));double height=0.35+lane*0.7;if(delta<0.24&&Math.abs(p.getY()-origin.y-height)<1.65){damage(level,p,0.78f);break;}}}}
    private void resolveCourt(ServerLevel level){var points=courtiers();var disabled=new boolean[points.size()];for(Player p:players(level))for(int i=0;i<points.size();i++)if(horizontal(p.position(),points.get(i))<=1.35)disabled[i]=true;
        float healing=0;for(int i=0;i<points.size();i++){if(disabled[i])continue;double a=heading+(i-1)*0.78;Vec3 end=points.get(i).add(Math.cos(a)*5,0,Math.sin(a)*5);var path=line(points.get(i),end);var hit=players(level).stream().anyMatch(p->path.stream().anyMatch(v->v.distanceToSqr(p.position())<=1.5*1.5&&Math.abs(v.y-p.getY())<3));if(hit){strike(level,path,0.68f);healing+=3;}}
        if(healing>0)boss.heal(Math.min(9,healing));}
    private void strike(ServerLevel level,List<Vec3> path,float multiplier){for(Player p:players(level))if(path.stream().anyMatch(v->Math.abs(v.y-p.getY())<3&&v.distanceToSqr(p.position())<=1.8*1.8))damage(level,p,multiplier);}
    private void damage(ServerLevel level,Player p,float multiplier){p.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*multiplier));}
    public boolean exposed(){return exposedTicks>0;}
    private static double horizontal(Vec3 a,Vec3 b){double x=a.x-b.x,z=a.z-b.z;return Math.sqrt(x*x+z*z);}
    public boolean updatePhase(float health,float max){boolean changed=clock.updatePhase(health,max);if(changed)phase=2;return changed;}
    public void save(ValueOutput o){clock.save(o);o.store("VermillionOrigin",Vec3.CODEC,origin);o.store("VermillionMark",Vec3.CODEC,mark);o.putDouble("VermillionHeading",heading);o.putDouble("VermillionOrbitAngle",orbitAngle);o.putInt("VermillionOrbitCycle",orbitCycle);o.putInt("VermillionPhase",phase);o.putInt("VermillionExposedTicks",exposedTicks);}
    public void load(ValueInput i){clock.load(i);origin=i.read("VermillionOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("VermillionMark",Vec3.CODEC).orElse(Vec3.ZERO);heading=i.getDoubleOr("VermillionHeading",0);orbitAngle=i.getDoubleOr("VermillionOrbitAngle",0);orbitCycle=Math.max(0,i.getIntOr("VermillionOrbitCycle",0));phase=i.getIntOr("VermillionPhase",clock.phase())==2?2:1;exposedTicks=Math.clamp(i.getIntOr("VermillionExposedTicks",0),0,80);}
}
