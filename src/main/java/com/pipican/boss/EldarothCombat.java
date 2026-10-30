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

/** Eldaroth's root lanes, fixed seed-pod landings and rotating branch sweep. */
public final class EldarothCombat {
    public static final String ROOTS="root_lattice", PODS="seed_pods", BRANCH="branch_clock";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO, targetMark=Vec3.ZERO;
    private int podCycle;
    private int secondBloomTicks;
    private float sweepAngle;
    public EldarothCombat(RegionalBossEntity boss){
        this.boss=boss; var d=BossDesigns.require("eldaroth_treant");
        clock=new RegionalCombatClock(Map.of(
            ROOTS,timing(d,0),PODS,timing(d,1),BRANCH,timing(d,2),"basic",new RegionalCombatClock.Timing(24,24,60)));
    }
    private static RegionalCombatClock.Timing timing(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}
    public boolean begin(String id,LivingEntity target){
        if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;
        origin=boss.position();targetMark=target.position();sweepAngle=(float)Math.atan2(target.getZ()-origin.z,target.getX()-origin.x);return true;
    }
    public void tick(ServerLevel level){
        if(secondBloomTicks>0){
            if(secondBloomTicks<=8&&secondBloomTicks%2==0){for(int i=0;i<5;i++){var p=podMark(i,5);level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,p.x,p.y+0.8,p.z,3,0.2,0.2,0.2,0.01);}}
            if(--secondBloomTicks==0){for(int i=0;i<5;i++)strike(level,List.of(podMark(i,5)),0.85f);podCycle++;}
        }
        String active=clock.active(); if(active.isEmpty()){clock.tick();return;}
        if(!clock.resolved()&&clock.remaining()%5==0) telegraph(level,active);
        String impact=clock.tick(); if(impact.isEmpty())return;
        switch(impact){
            case ROOTS -> {strike(level,rootLattice(),1.05f);boss.playSound(SoundEvents.ROOTED_DIRT_BREAK,1.4f,0.7f);}
            case PODS -> {int count=clock.phase()==2?5:3; for(int i=0;i<count;i++) strike(level,List.of(podMark(i,count)),0.85f);podCycle++;if(clock.phase()==2)secondBloomTicks=16;}
            case BRANCH -> {strike(level,List.of(),1.2f);boss.playSound(SoundEvents.WOOD_BREAK,1.7f,0.6f);}
            case "basic" -> strike(level,List.of(targetMark),0.8f);
            default -> {}
        }
    }
    private List<Vec3> rootLattice(){
        var marks=new ArrayList<Vec3>(); double shift=clock.phase()==2?2:0;
        // Two crossing lanes form four cells; a narrow central refuge remains open.
        for(int i=-8;i<=8;i++){
            double v=i;
            if(Math.abs(v)<1.2)continue;
            marks.add(origin.add(-5+shift,0,v));marks.add(origin.add(5-shift,0,v));
            marks.add(origin.add(v,0,-5+shift));marks.add(origin.add(v,0,5-shift));
        }
        return marks.stream().filter(boss::insideArena).toList();
    }
    private Vec3 podMark(int index,int count){
        double angle=(Math.PI*2*index/count)+podCycle*0.43;
        double radius=clock.phase()==2?5.6:6.5;
        return origin.add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
    }
    private void telegraph(ServerLevel level,String active){
        if(active.equals(ROOTS)){
            for(var p:rootLattice())level.sendParticles(ParticleTypes.HAPPY_VILLAGER,p.x,p.y+0.05,p.z,1,0.08,0.03,0.08,0);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,origin.x,origin.y+0.1,origin.z,3,0.5,0.1,0.5,0);
        }else if(active.equals(PODS)){
            int count=clock.phase()==2?5:3;for(int i=0;i<count;i++){var p=podMark(i,count);level.sendParticles(ParticleTypes.HAPPY_VILLAGER,p.x,p.y+0.1,p.z,5,0.35,0.2,0.35,0.01);level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,p.x,p.y+1,p.z,2,0.1,0.1,0.1,0);}
        }else if(active.equals(BRANCH)){
            for(int i=0;i<=20;i++){double a=sweepAngle+Math.PI*2*i/20;var p=origin.add(Math.cos(a)*6,2.4,Math.sin(a)*6);level.sendParticles(ParticleTypes.CRIT,p.x,p.y,p.z,1,0,0,0,0);}
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,origin.x,origin.y+0.1,origin.z,8,0.6,0.1,0.6,0);
        }else level.sendParticles(ParticleTypes.HAPPY_VILLAGER,targetMark.x,targetMark.y+0.15,targetMark.z,3,0.2,0.1,0.2,0);
    }
    private void strike(ServerLevel level,List<Vec3> marks,float multiplier){
        for(var player:level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(20),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()))){
            boolean hit;
            if(marks.isEmpty()){
                double dx=player.getX()-origin.x,dz=player.getZ()-origin.z,r=Math.sqrt(dx*dx+dz*dz);
                double angle=Math.atan2(dz,dx),delta=Math.abs(Math.atan2(Math.sin(angle-sweepAngle),Math.cos(angle-sweepAngle)));
                hit=r>=2.8&&r<=9&&delta<0.23&&Math.abs(player.getY()-origin.y)<3.2;
            }else hit=marks.stream().anyMatch(mark->Math.abs(mark.y-player.getY())<2.5&&mark.distanceToSqr(player.position())<=2.0*2.0);
            if(hit)player.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*multiplier));
        }
    }
    public boolean updatePhase(float health,float max){return clock.updatePhase(health,max);}
    public void save(ValueOutput out){clock.save(out);out.store("EldarothOrigin",Vec3.CODEC,origin);out.store("EldarothTarget",Vec3.CODEC,targetMark);out.putInt("EldarothPods",podCycle);out.putInt("EldarothSecondBloom",secondBloomTicks);out.putFloat("EldarothSweep",sweepAngle);}
    public void load(ValueInput in){clock.load(in);origin=in.read("EldarothOrigin",Vec3.CODEC).orElse(Vec3.ZERO);targetMark=in.read("EldarothTarget",Vec3.CODEC).orElse(Vec3.ZERO);podCycle=Math.max(0,in.getIntOr("EldarothPods",0));secondBloomTicks=Math.clamp(in.getIntOr("EldarothSecondBloom",0),0,16);sweepAngle=in.getFloatOr("EldarothSweep",0);}
}
