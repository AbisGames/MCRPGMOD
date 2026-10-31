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

/** Sweeping river gates, sequential silt javelins and interruptible tributary anchors. */
public final class NetharisCombat {
    public static final String GATES="river_gates", JAVELINS="silt_javelins", TRIBUTARY="tributary_call";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO;
    private double heading;
    private List<Boolean> anchorBroken=List.of(false,false,false);
    private int phase=1,laneShift,gateWave=-1,gateDelay,javelinIndex=-1,javelinDelay,exposureTicks;
    public NetharisCombat(RegionalBossEntity boss){this.boss=boss;var d=BossDesigns.require("netharis_river_guardian");clock=new RegionalCombatClock(Map.of(GATES,time(d,0),JAVELINS,time(d,1),TRIBUTARY,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,50)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}
    public int exposureTicks(){return exposureTicks;}
    public int javelinProgress(){return javelinIndex;}
    public int brokenAnchors(){return (int)anchorBroken.stream().filter(Boolean::booleanValue).count();}
    public float damageMultiplier(net.minecraft.world.damagesource.DamageSource source){return phase==2&&exposureTicks>0&&brokenAnchors()==3?1.4f:1f;}
    public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();if(phase==2){laneShift=(laneShift+1)%3;heading+=0.12;boss.playSound(SoundEvents.CONDUIT_ACTIVATE,1.1f,0.75f);}if(id.equals(GATES)){gateWave=0;gateDelay=0;}if(id.equals(JAVELINS)){javelinIndex=0;javelinDelay=0;}if(id.equals(TRIBUTARY))anchorBroken=List.of(false,false,false);return true;}
    public void tick(ServerLevel level){if(exposureTicks>0)exposureTicks--;String active=clock.active();if(active.equals(TRIBUTARY)&&!clock.resolved())checkAnchorHits(level);if(!active.isEmpty()&&!clock.resolved()&&clock.remaining()%5==0)telegraph(level,active);
        if(active.equals(GATES)&&clock.resolved()&&gateWave>=0&&gateWave<3){if(gateDelay>0)gateDelay--;else{resolveGate(level,gateWave);gateWave++;gateDelay=8;}}
        if(active.equals(JAVELINS)&&clock.resolved()&&javelinIndex>=0&&javelinIndex<4){if(javelinDelay>0)javelinDelay--;else{resolveJavelin(level,javelinIndex);javelinIndex++;javelinDelay=8;}}
        if(active.isEmpty()){clock.tick();return;}String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case GATES->{gateWave=0;gateDelay=1;boss.playSound(SoundEvents.PLAYER_SPLASH,1.3f,0.65f);}
            case JAVELINS->{javelinIndex=0;javelinDelay=1;boss.playSound(SoundEvents.TRIDENT_RETURN,1.1f,0.7f);}
            case TRIBUTARY->{resolveSurge(level);boss.playSound(SoundEvents.CONDUIT_AMBIENT,1.4f,0.6f);}
            case "basic"->strike(level,List.of(mark),0.68f);default->{}}
    }
    private Vec3 forward(){return new Vec3(Math.cos(heading),0,Math.sin(heading));}
    private Vec3 side(){Vec3 f=forward();return new Vec3(-f.z,0,f.x);}
    private double laneOffset(int index){return (index-1)*4.4;}
    private int safeLane(int wave){return phase==2?Math.floorMod(laneShift+wave,3):1;}
    public Vec3 safeGatePosition(int wave){return origin.add(forward().scale(4+wave*4.4)).add(side().scale(laneOffset(safeLane(wave))));}
    private List<Vec3> gatePoints(int wave){var points=new ArrayList<Vec3>();double d=4+wave*4.4,opening=laneOffset(safeLane(wave));for(int i=0;i<=32;i++){double lateral=-8+i*0.5;if(Math.abs(lateral-opening)<2.55)continue;Vec3 p=origin.add(forward().scale(d)).add(side().scale(lateral));if(boss.insideArena(p))points.add(p);}return points;}
    private List<Vec3> javelinPoints(){var out=new ArrayList<Vec3>();double[][] offsets={{-5,3},{-1.7,7},{2.1,7},{5.4,3}};for(double[] v:offsets){Vec3 p=origin.add(forward().scale(v[1])).add(side().scale(v[0]));if(boss.insideArena(p))out.add(p);}return out;}
    public Vec3 javelinPosition(int index){if(index<0||index>=4)throw new IndexOutOfBoundsException("Netharis javelin index must be in [0,3]");return javelinPoints().get(index);}
    private List<Vec3> anchors(){var out=new ArrayList<Vec3>();for(int i=0;i<3;i++){double angle=heading+Math.PI*2*i/3+laneShift*0.17;Vec3 p=origin.add(Math.cos(angle)*6,0.1,Math.sin(angle)*6);if(boss.insideArena(p))out.add(p);}return out;}
    public Vec3 anchorPosition(int index){if(index<0||index>=3)throw new IndexOutOfBoundsException("Netharis anchor index must be in [0,2]");return anchors().get(index);}
    private int crossingLane(){return phase==2?Math.floorMod(laneShift+1,3):1;}
    public Vec3 safeCrossingPosition(){return origin.add(forward().scale(8)).add(side().scale(laneOffset(crossingLane())));}
    private List<Vec3> surgePoints(){var out=new ArrayList<Vec3>();Vec3 f=forward(),s=side();double opening=laneOffset(crossingLane());for(int i=0;i<=32;i++){double lateral=-8+i*0.5;if(Math.abs(lateral-opening)<2.8)continue;Vec3 p=origin.add(f.scale(8)).add(s.scale(lateral));if(boss.insideArena(p))out.add(p);}return out;}
    private void checkAnchorHits(ServerLevel level){var next=new ArrayList<>(anchorBroken);var nodes=anchors();for(int i=0;i<3;i++)if(!next.get(i)&&i<nodes.size()){Vec3 p=nodes.get(i);var box=new net.minecraft.world.phys.AABB(p.add(-1.4,-1.2,-1.4),p.add(1.4,1.8,1.4));var shots=level.getEntitiesOfClass(Projectile.class,box,Projectile::isAlive);if(!shots.isEmpty()){next.set(i,true);shots.forEach(Projectile::discard);level.sendParticles(ParticleTypes.POOF,p.x,p.y,p.z,18,0.35,0.3,0.35,0.02);}}anchorBroken=List.copyOf(next);}
    private void telegraph(ServerLevel level,String id){if(id.equals(GATES)){for(int wave=0;wave<3;wave++){List<Vec3> points=gatePoints(wave);for(int i=0;i<points.size();i+=3){Vec3 p=points.get(i);level.sendParticles(ParticleTypes.SPLASH,p.x,p.y+0.08,p.z,2,0.08,0.02,0.08,0.003);}Vec3 safe=safeGatePosition(wave);level.sendParticles(ParticleTypes.HAPPY_VILLAGER,safe.x,safe.y+0.12,safe.z,12,0.65,0.04,0.65,0);}}
        else if(id.equals(JAVELINS)){List<Vec3> spots=javelinPoints();for(int i=javelinIndex;i<spots.size();i++){Vec3 p=spots.get(i);level.sendParticles(i==javelinIndex?ParticleTypes.BUBBLE:ParticleTypes.SPLASH,p.x,p.y+0.1,p.z,i==javelinIndex?12:5,0.24,0.04,0.24,0);}}
        else{var nodes=anchors();for(int i=0;i<nodes.size();i++){Vec3 p=nodes.get(i);level.sendParticles(anchorBroken.get(i)?ParticleTypes.POOF:ParticleTypes.END_ROD,p.x,p.y+0.3,p.z,anchorBroken.get(i)?3:12,0.18,0.25,0.18,0.005);}var points=surgePoints();for(int i=0;i<points.size();i+=3){Vec3 p=points.get(i);level.sendParticles(ParticleTypes.SPLASH,p.x,p.y+0.1,p.z,1,0.06,0.02,0.06,0.003);}Vec3 safe=safeCrossingPosition();level.sendParticles(ParticleTypes.HAPPY_VILLAGER,safe.x,safe.y+0.14,safe.z,16,0.5,0.04,0.5,0);}}
    private void resolveGate(ServerLevel level,int wave){var points=gatePoints(wave);for(int i=0;i<points.size();i+=2){Vec3 p=points.get(i);level.sendParticles(ParticleTypes.SPLASH,p.x,p.y,p.z,2,0.25,0.1,0.25,0.02);}strike(level,points,0.53f);}
    private void resolveJavelin(ServerLevel level,int index){Vec3 p=javelinPosition(index);level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,p.x,p.y,p.z,12,0.24,0.3,0.24,0.03);strike(level,List.of(p),0.64f);}
    private void resolveSurge(ServerLevel level){int active=(int)anchorBroken.stream().filter(v->!v).count();var points=surgePoints();for(int i=0;i<points.size();i+=2){Vec3 p=points.get(i);level.sendParticles(ParticleTypes.SPLASH,p.x,p.y,p.z,2,0.28,0.12,0.28,0.02);}if(active>0)strike(level,points,0.28f+active*0.14f);else boss.heal(0.0f);if(phase==2&&active==0)exposureTicks=50;}
    private void strike(ServerLevel level,List<Vec3> points,float scale){for(Player player:players(level))if(points.stream().anyMatch(p->Math.abs(player.getY()-p.y)<2.8&&p.distanceToSqr(player.position())<=1.8*1.8))player.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    private List<Player> players(ServerLevel level){return level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(28),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    public boolean updatePhase(float health,float max){boolean changed=clock.updatePhase(health,max);if(changed)phase=2;return changed;}
    public void save(ValueOutput o){clock.save(o);o.store("NetharisOrigin",Vec3.CODEC,origin);o.store("NetharisMark",Vec3.CODEC,mark);o.store("NetharisAnchors",com.mojang.serialization.Codec.BOOL.listOf(),anchorBroken);o.putDouble("NetharisHeading",heading);o.putInt("NetharisPhase",phase);o.putInt("NetharisLaneShift",laneShift);o.putInt("NetharisGateWave",gateWave);o.putInt("NetharisGateDelay",gateDelay);o.putInt("NetharisJavelinIndex",javelinIndex);o.putInt("NetharisJavelinDelay",javelinDelay);o.putInt("NetharisExposure",exposureTicks);}
    public void load(ValueInput i){clock.load(i);origin=i.read("NetharisOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("NetharisMark",Vec3.CODEC).orElse(Vec3.ZERO);var saved=i.read("NetharisAnchors",com.mojang.serialization.Codec.BOOL.listOf()).orElse(List.of());var normalized=new ArrayList<Boolean>(3);for(int n=0;n<3;n++)normalized.add(n<saved.size()&&saved.get(n));anchorBroken=List.copyOf(normalized);heading=i.getDoubleOr("NetharisHeading",0);phase=clock.phase();laneShift=Math.floorMod(i.getIntOr("NetharisLaneShift",0),3);gateWave=Math.clamp(i.getIntOr("NetharisGateWave",-1),-1,3);gateDelay=Math.clamp(i.getIntOr("NetharisGateDelay",0),0,8);javelinIndex=Math.clamp(i.getIntOr("NetharisJavelinIndex",-1),-1,4);javelinDelay=Math.clamp(i.getIntOr("NetharisJavelinDelay",0),0,8);exposureTicks=Math.clamp(i.getIntOr("NetharisExposure",0),0,50);}
}
