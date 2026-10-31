package com.pipican.boss;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Frostbound's attacks use fixed, warned positions and never replace world blocks. */
public final class FrostboundCombat {
    public static final String BASIC="basic", FISSURE="frost_fissure", WHEEL="winter_wheel", LANCES="crystal_lances";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO;
    private float heading;
    private boolean secondRow;

    public FrostboundCombat(RegionalBossEntity boss){
        this.boss=boss;
        var design=BossDesigns.require("frostbound_colossus");
        var timing=new java.util.HashMap<String,RegionalCombatClock.Timing>();
        timing.put(BASIC,new RegionalCombatClock.Timing(18,18,55));
        for(var ability:design.abilities()) timing.put(ability.id(),new RegionalCombatClock.Timing(ability.windup(),ability.recovery(),ability.cooldown()));
        clock=new RegionalCombatClock(timing);
    }
    public RegionalCombatClock clock(){ return clock; }
    public boolean begin(String id,LivingEntity target){
        if(target==null || !target.isAlive() || !boss.insideArena(target.position()) || !clock.begin(id)) return false;
        origin=boss.position();
        heading=(float)Math.toDegrees(Math.atan2(-(target.getX()-origin.x),target.getZ()-origin.z));
        secondRow=false;
        return true;
    }
    /** Caller owns target selection and invokes this once per server tick. */
    public void tick(ServerLevel level){
        String active=clock.active();
        if(active.isEmpty()){ clock.tick(); return; }
        if(!clock.resolved() && clock.remaining()%4==0) draw(level,warningPoints(active));
        if(active.equals(LANCES) && clock.resolved() && !secondRow && clock.remaining()%4==0) draw(level,lancePoints(1));
        String impact=clock.tick();
        switch(impact){
            case BASIC -> strike(level,basicPoints(),0.8);
            case FISSURE -> strike(level,fissurePoints(),1.0);
            case WHEEL -> strike(level,wheelPoints(wedge()),1.0);
            case LANCES -> strike(level,lancePoints(0),1.0);
            default -> {}
        }
        if(active.equals(LANCES) && clock.resolved() && !secondRow && clock.remaining()<=30){
            secondRow=true;
            strike(level,lancePoints(1),1.0);
        }
    }
    public boolean updatePhase(float health,float maximum){ return clock.updatePhase(health,maximum); }
    private double wedge(){
        double progress=Math.clamp((90-clock.remaining())/50.0,0,1);
        return heading+90+(clock.phase()==2 ? -1 : 1)*45*progress;
    }
    private List<Vec3> warningPoints(String id){
        return switch(id){
            case BASIC -> basicPoints();
            case FISSURE -> fissurePoints();
            case WHEEL -> wheelPoints(wedge());
            case LANCES -> lancePoints(0);
            default -> List.of();
        };
    }
    private Vec3 local(double x,double z){
        double radians=Math.toRadians(heading);
        return origin.add(x*Math.cos(radians)-z*Math.sin(radians),0,x*Math.sin(radians)+z*Math.cos(radians));
    }
    private List<Vec3> basicPoints(){
        var points=new ArrayList<Vec3>();
        for(int x=-2;x<=2;x++) for(int z=1;z<=3;z++) points.add(local(x,z));
        return points;
    }
    private List<Vec3> fissurePoints(){
        var points=new ArrayList<Vec3>();
        for(int arm:new int[]{-1,1}) for(int distance=1;distance<=9;distance++){
            points.add(local(arm*(1+distance*0.25),distance));
            if(distance>=5) points.add(local(arm*(1+distance*0.7),distance));
        }
        return points;
    }
    private List<Vec3> wheelPoints(double safeAngle){
        var points=new ArrayList<Vec3>();
        for(int radius:new int[]{3,6,9}) for(int angle=0;angle<360;angle+=10){
            double delta=Math.abs(Math.IEEEremainder(angle-safeAngle,360));
            if(delta<=35) continue;
            double a=Math.toRadians(angle); points.add(origin.add(Math.cos(a)*radius,0,Math.sin(a)*radius));
        }
        return points;
    }
    private List<Vec3> lancePoints(int row){
        var points=new ArrayList<Vec3>();
        for(int x:row==0 ? new int[]{-6,0,6} : new int[]{-3,3})
            for(int z=-6;z<=6;z+=2) points.add(local(x,z));
        return points;
    }
    private void draw(ServerLevel level,List<Vec3> points){
        for(var point:points) if(boss.insideArena(point))
            level.sendParticles(ParticleTypes.SNOWFLAKE,point.x,point.y+0.2,point.z,1,0,0,0,0);
    }
    private void strike(ServerLevel level,List<Vec3> points,double multiplier){
        draw(level,points);
        for(var player:level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(24),p->p.isAlive() && !p.isCreative() && !p.isSpectator() && boss.insideArena(p.position()))){
            if(!boss.hasLineOfSight(player)) continue;
            for(var point:points){
                double dx=player.getX()-point.x,dz=player.getZ()-point.z;
                if(Math.abs(player.getY()-point.y)<2.5 && dx*dx+dz*dz<=1.0){
                    player.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*multiplier));
                    break;
                }
            }
        }
    }
    public void save(ValueOutput output){
        clock.save(output); output.store("FrostOrigin",Vec3.CODEC,origin);
        output.putFloat("FrostHeading",heading); output.putBoolean("FrostSecondRow",secondRow);
    }
    public void load(ValueInput input){
        clock.load(input); origin=input.read("FrostOrigin",Vec3.CODEC).orElse(Vec3.ZERO);
        heading=input.getFloatOr("FrostHeading",0); secondRow=input.getBooleanOr("FrostSecondRow",false);
    }
}
