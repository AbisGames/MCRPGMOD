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

/** Temporary, non-destructive canyon hazards with individually locked landings. */
public final class KragmorCombat {
    public static final String ROCKFALL="canyon_rockfall", TETHERS="cairn_tethers", LEAP="ravine_leap";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO,shockStart=Vec3.ZERO;
    private double heading;
    private List<Vec3> rocks=List.of();
    private int nextRock,rockDelay,shockDelay,phase=1,rockCycle;
    public KragmorCombat(RegionalBossEntity boss){
        this.boss=boss;var d=BossDesigns.require("kragmor_canyon_warlord");
        clock=new RegionalCombatClock(Map.of(ROCKFALL,time(d,0),TETHERS,time(d,1),LEAP,time(d,2),"basic",new RegionalCombatClock.Timing(20,24,52)));
    }
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}
    public boolean begin(String id,LivingEntity target){
        if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;
        origin=boss.position();mark=target.position();heading=Math.atan2(target.getZ()-origin.z,target.getX()-origin.x);phase=clock.phase();
        if(id.equals(ROCKFALL)){rocks=rockMarks();nextRock=0;rockDelay=0;}
        return true;
    }
    public void tick(ServerLevel level){
        if(rockDelay>0){if(rockDelay<=4)telegraph(level,List.of(rocks.get(nextRock)));if(--rockDelay==0){strike(level,List.of(rocks.get(nextRock)),1.0f);nextRock++;if(nextRock<rocks.size())rockDelay=8;else rocks=List.of();}}
        if(shockDelay>0){if(shockDelay<=8&&shockDelay%2==0)telegraph(level,shockLine());if(--shockDelay==0)strike(level,shockLine(),1.15f);}
        String active=clock.active();if(active.isEmpty()){clock.tick();return;}
        if(!clock.resolved()&&clock.remaining()%5==0)telegraph(level,marks(active));
        String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){
            case ROCKFALL->{rocks=rockMarks();nextRock=0;rockDelay=8;boss.playSound(SoundEvents.RAVAGER_ATTACK,1.5f,0.65f);}
            case TETHERS->{strike(level,tetherWeb(),1.0f);boss.playSound(SoundEvents.IRON_GOLEM_ATTACK,1.3f,0.7f);}
            case LEAP->{var landing=leapLanding();strikeCircle(level,landing,3.0,1.1f);boss.setPos(landing.x,landing.y,landing.z);shockStart=landing;shockDelay=18;boss.playSound(SoundEvents.RAVAGER_ROAR,1.4f,0.6f);}
            case "basic"->strike(level,List.of(mark),0.8f);
            default->{}
        }
    }
    private Vec3 point(Vec3 center,double angle,double distance){return center.add(Math.cos(angle)*distance,0,Math.sin(angle)*distance);}
    private List<Vec3> rockMarks(){
        double side=(phase==2?(rockCycle++%2==0?3:-3):0),px=-Math.sin(heading),pz=Math.cos(heading);var result=new ArrayList<Vec3>();
        for(int i=0;i<3;i++){var p=point(origin,heading,4+i*3.3).add(px*side,0,pz*side);if(boss.insideArena(p))result.add(p);}
        return List.copyOf(result);
    }
    private List<Vec3> tetherWeb(){
        var anchors=List.of(origin.add(0,0,-6),origin.add(6,0,0),origin.add(0,0,6),origin.add(-6,0,0));var result=new ArrayList<Vec3>();
        for(int edge=0;edge<4;edge++){var a=anchors.get(edge);var b=anchors.get((edge+1)%4);for(int i=0;i<=20;i++){var p=a.lerp(b,i/20.0);if(boss.insideArena(p))result.add(p);}}
        return result;
    }
    private Vec3 leapLanding(){double distance=Math.clamp(Math.sqrt(origin.distanceToSqr(mark)),6,10);return point(origin,heading,distance);}
    private List<Vec3> shockLine(){var end=point(shockStart,heading,12);int n=24;return java.util.stream.IntStream.rangeClosed(0,n).mapToObj(i->shockStart.lerp(end,i/(double)n)).filter(boss::insideArena).toList();}
    private List<Vec3> marks(String active){return switch(active){case ROCKFALL->rocks;case TETHERS->tetherWeb();case LEAP->List.of(leapLanding());default->List.of(mark);};}
    private void telegraph(ServerLevel level,List<Vec3> points){for(var p:points)level.sendParticles(points.size()==1?ParticleTypes.POOF:ParticleTypes.CRIT,p.x,p.y+0.15,p.z,points.size()==1?8:1,0.18,0.08,0.18,0.02);}
    private List<Player> players(ServerLevel level){return level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(26),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    private void strike(ServerLevel level,List<Vec3> marks,float scale){for(var p:players(level))if(marks.stream().anyMatch(m->Math.abs(m.y-p.getY())<3&&m.distanceToSqr(p.position())<=2.0*2.0))damage(level,p,scale);}
    private void strikeCircle(ServerLevel level,Vec3 center,double radius,float scale){for(var p:players(level))if(Math.abs(center.y-p.getY())<3&&center.distanceToSqr(p.position())<=radius*radius)damage(level,p,scale);}
    private void damage(ServerLevel level,Player player,float scale){player.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    public boolean updatePhase(float health,float max){boolean changed=clock.updatePhase(health,max);if(changed){rocks=List.of();nextRock=rockDelay=shockDelay=0;}return changed;}
    public void save(ValueOutput out){clock.save(out);out.store("KragOrigin",Vec3.CODEC,origin);out.store("KragMark",Vec3.CODEC,mark);out.store("KragShock",Vec3.CODEC,shockStart);out.store("KragRocks",Vec3.CODEC.listOf(),rocks);out.putDouble("KragHeading",heading);out.putInt("KragNextRock",nextRock);out.putInt("KragRockDelay",rockDelay);out.putInt("KragShockDelay",shockDelay);out.putInt("KragPhase",phase);out.putInt("KragRockCycle",rockCycle);}
    public void load(ValueInput in){clock.load(in);origin=in.read("KragOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=in.read("KragMark",Vec3.CODEC).orElse(Vec3.ZERO);shockStart=in.read("KragShock",Vec3.CODEC).orElse(Vec3.ZERO);rocks=in.read("KragRocks",Vec3.CODEC.listOf()).orElse(List.of()).stream().limit(3).toList();nextRock=Math.clamp(in.getIntOr("KragNextRock",0),0,rocks.size());rockDelay=nextRock<rocks.size()?Math.clamp(in.getIntOr("KragRockDelay",0),0,8):0;shockDelay=Math.clamp(in.getIntOr("KragShockDelay",0),0,18);heading=in.getDoubleOr("KragHeading",0);phase=in.getIntOr("KragPhase",1)==2?2:1;rockCycle=Math.max(0,in.getIntOr("KragRockCycle",0));}
}
