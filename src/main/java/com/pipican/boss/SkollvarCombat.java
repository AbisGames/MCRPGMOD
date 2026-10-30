package com.pipican.boss;

import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Three predator patterns based on a locked trail, alternating pack feints and a remembered footprint. */
public final class SkollvarCombat {
    public static final String CHARGE="scent_charge", MIRAGES="pack_mirages", PROWL="silent_prowl";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,chargeEnd=Vec3.ZERO,footprint=Vec3.ZERO;
    private float heading;
    private int pounce;
    private boolean feint;
    public SkollvarCombat(RegionalBossEntity boss){
        this.boss=boss;
        var design=BossDesigns.require("skollvar_winterfang");
        clock=new RegionalCombatClock(Map.of(
            CHARGE,new RegionalCombatClock.Timing(design.abilities().get(0).windup(),design.abilities().get(0).recovery(),design.abilities().get(0).cooldown()),
            MIRAGES,new RegionalCombatClock.Timing(design.abilities().get(1).windup(),design.abilities().get(1).recovery(),design.abilities().get(1).cooldown()),
            PROWL,new RegionalCombatClock.Timing(design.abilities().get(2).windup(),design.abilities().get(2).recovery(),design.abilities().get(2).cooldown()),
            "basic",new RegionalCombatClock.Timing(16,20,50)));
    }
    public RegionalCombatClock clock(){ return clock; }
    public boolean begin(String id,LivingEntity target){
        if(target==null || !target.isAlive() || !boss.insideArena(target.position()) || !clock.begin(id)) return false;
        origin=boss.position(); footprint=target.position();
        heading=(float)Math.toDegrees(Math.atan2(-(target.getX()-origin.x),target.getZ()-origin.z));
        chargeEnd=origin.add(-Math.sin(Math.toRadians(heading))*12,0,Math.cos(Math.toRadians(heading))*12);
        feint=clock.phase()==2 && id.equals(CHARGE) && pounce%3==2;
        return true;
    }
    public void tick(ServerLevel level){
        String active=clock.active();
        if(active.isEmpty()){ clock.tick(); return; }
        if(!clock.resolved() && clock.remaining()%4==0) telegraph(level,active);
        String impact=clock.tick();
        if(impact.isEmpty()) return;
        switch(impact){
            case CHARGE -> {
                if(feint){ boss.playSound(SoundEvents.RAVAGER_ROAR,1.5f,0.7f); }
                else { boss.setPos(chargeEnd.x,chargeEnd.y,chargeEnd.z); strike(level,linePoints(origin,chargeEnd),1.0f); }
                pounce++;
            }
            case MIRAGES -> { pounce++; strike(level,List.of(mirageMark()),0.9f); }
            case PROWL -> strike(level,List.of(footprint),clock.phase()==2 ? 1.2f : 1.0f);
            case "basic" -> strike(level,List.of(footprint),0.75f);
            default -> {}
        }
    }
    private void telegraph(ServerLevel level,String active){
        if(active.equals(CHARGE)){
            for(int i=1;i<=12;i++){
                Vec3 point=origin.add(-Math.sin(Math.toRadians(heading))*i,0,Math.cos(Math.toRadians(heading))*i);
                level.sendParticles(ParticleTypes.SNOWFLAKE,point.x,point.y+0.1,point.z,2,0.1,0.1,0.1,0);
            }
        }else if(active.equals(MIRAGES)){
            var mark=mirageMark(); level.sendParticles(ParticleTypes.CLOUD,mark.x,mark.y+0.2,mark.z,12,0.5,0.4,0.5,0.02);
            level.sendParticles(ParticleTypes.CRIT,mark.x,mark.y+1,mark.z,3,0.2,0.2,0.2,0.02);
        }else{
            level.sendParticles(ParticleTypes.SNOWFLAKE,footprint.x,footprint.y+0.1,footprint.z,3,0.3,0.1,0.3,0);
        }
    }
    private Vec3 mirageMark(){
        double side=(pounce%2==0 ? -1:1)*3.5;
        double a=Math.toRadians(heading);
        return footprint.add(Math.cos(a)*side,0,Math.sin(a)*side);
    }
    private List<Vec3> linePoints(Vec3 from,Vec3 to){
        return java.util.stream.IntStream.rangeClosed(0,24).mapToObj(i->from.lerp(to,i/24.0)).filter(boss::insideArena).toList();
    }
    private void strike(ServerLevel level,List<Vec3> marks,float multiplier){
        for(var player:level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(32),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()))){
            if(marks.stream().anyMatch(mark->Math.abs(mark.y-player.getY())<2.5 && mark.distanceToSqr(player.position())<=1.8*1.8))
                player.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*multiplier));
        }
    }
    public boolean updatePhase(float health,float max){ return clock.updatePhase(health,max); }
    public void save(ValueOutput out){
        clock.save(out); out.store("SkollOrigin",Vec3.CODEC,origin); out.store("SkollChargeEnd",Vec3.CODEC,chargeEnd);
        out.store("SkollFootprint",Vec3.CODEC,footprint); out.putFloat("SkollHeading",heading);
        out.putInt("SkollPounce",pounce); out.putBoolean("SkollFeint",feint);
    }
    public void load(ValueInput in){
        clock.load(in); origin=in.read("SkollOrigin",Vec3.CODEC).orElse(Vec3.ZERO); chargeEnd=in.read("SkollChargeEnd",Vec3.CODEC).orElse(Vec3.ZERO);
        footprint=in.read("SkollFootprint",Vec3.CODEC).orElse(Vec3.ZERO); heading=in.getFloatOr("SkollHeading",0);
        pounce=Math.max(0,in.getIntOr("SkollPounce",0)); feint=in.getBooleanOr("SkollFeint",false);
    }
}
