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

/** Three royal patterns: announced thrust/riposte, cavalry lanes and a broken-regalia safe sector. */
public final class FallenKingCombat {
    public static final String DUEL="royal_duel", CAVALRY="phantom_cavalry", REGALIA="broken_regalia";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO;
    private double heading;
    private int duelCount,pendingRiposte,phase=1;
    private boolean duelFeint;
    public FallenKingCombat(RegionalBossEntity boss){
        this.boss=boss;var d=BossDesigns.require("fallen_king");
        clock=new RegionalCombatClock(Map.of(DUEL,time(d,0),CAVALRY,time(d,1),REGALIA,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,48)));
    }
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}
    public boolean begin(String id,LivingEntity target){
        if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;
        origin=boss.position();mark=target.position();heading=Math.atan2(target.getZ()-origin.z,target.getX()-origin.x);
        duelFeint=id.equals(DUEL)&&clock.phase()==2&&duelCount%2==0;phase=clock.phase();return true;
    }
    public void tick(ServerLevel level){
        if(pendingRiposte>0){if(pendingRiposte<=6&&pendingRiposte%2==0)telegraphLine(level,riposteLine());if(--pendingRiposte==0){strike(level,riposteLine(),1.1f);boss.playSound(SoundEvents.IRON_GOLEM_ATTACK,1.2f,0.65f);}}
        String active=clock.active();if(active.isEmpty()){clock.tick();return;}
        if(!clock.resolved()&&clock.remaining()%5==0)telegraph(level,active);
        String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){
            case DUEL->{duelCount++;if(duelFeint){pendingRiposte=phase==2?18:12;boss.playSound(SoundEvents.WITHER_SKELETON_AMBIENT,1.3f,0.65f);}
                else strike(level,line(origin,point(heading,Math.min(12,Math.sqrt(origin.distanceToSqr(mark)))),1.2f),1.0f);}
            case CAVALRY->{for(var line:cavalryLanes())strike(level,line,1.05f);boss.playSound(SoundEvents.HORSE_GALLOP,1.6f,0.7f);}
            case REGALIA->{strikeRegalia(level);boss.playSound(SoundEvents.IRON_GOLEM_ATTACK,1.5f,0.6f);}
            case "basic"->strike(level,List.of(mark),0.75f);
            default->{}
        }
    }
    private Vec3 point(double angle,double distance){return origin.add(Math.cos(angle)*distance,0,Math.sin(angle)*distance);}
    private List<Vec3> line(Vec3 from,Vec3 to,double ignored){
        int steps=Math.max(1,(int)(Math.sqrt(from.distanceToSqr(to))*2));return java.util.stream.IntStream.rangeClosed(0,steps).mapToObj(i->from.lerp(to,i/(double)steps)).filter(boss::insideArena).toList();
    }
    private List<Vec3> riposteLine(){return line(origin,point(heading,Math.min(12,Math.sqrt(origin.distanceToSqr(mark)))),0);}
    private List<List<Vec3>> cavalryLanes(){
        int count=clock.phase()==2?4:3;var result=new ArrayList<List<Vec3>>();double px=-Math.sin(heading),pz=Math.cos(heading);
        for(int lane=0;lane<count;lane++){
            double offset=(lane-(count-1)/2.0)*3.4;var start=origin.add(px*offset,0,pz*offset);var end=start.add(Math.cos(heading)*15,0,Math.sin(heading)*15);
            result.add(line(start,end,0));
        }
        return result;
    }
    private void telegraph(ServerLevel level,String id){
        if(id.equals(DUEL)){var end=point(heading,Math.min(12,Math.sqrt(origin.distanceToSqr(mark))));telegraphLine(level,line(origin,end,0));
            if(duelFeint)level.sendParticles(ParticleTypes.ENCHANTED_HIT,origin.x,origin.y+1.2,origin.z,8,0.5,0.5,0.5,0.02);
        }else if(id.equals(CAVALRY)){for(var lane:cavalryLanes())telegraphLine(level,lane);}
        else if(id.equals(REGALIA)){
            for(int i=0;i<4;i++){double a=heading+i*Math.PI/2;double r=(i==0?1:0.12);var p=point(a,7);level.sendParticles(i==0?ParticleTypes.SOUL_FIRE_FLAME:ParticleTypes.SOUL,p.x,p.y+1,p.z,6,0.35,0.2,0.35,r);}
            level.sendParticles(ParticleTypes.ENCHANT,origin.x,origin.y+1,origin.z,18,0.5,0.5,0.5,0.03);
        }else telegraphLine(level,List.of(mark));
    }
    private void telegraphLine(ServerLevel level,List<Vec3> points){for(int i=0;i<points.size();i+=2){var p=points.get(i);level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,p.x,p.y+0.1,p.z,1,0.08,0.04,0.08,0);}}
    private void strikeRegalia(ServerLevel level){
        // Three lit cardinal crests sweep the perimeter; the unlit crest direction remains a safe approach.
        for(var player:players(level)){
            double dx=player.getX()-origin.x,dz=player.getZ()-origin.z,r=Math.sqrt(dx*dx+dz*dz),a=Math.atan2(dz,dx);
            double relative=Math.atan2(Math.sin(a-heading),Math.cos(a-heading));
            double sector=Math.floorMod((int)Math.round(relative/(Math.PI/2)),4);
            if(r>=3&&r<=10&&sector!=0&&Math.abs(player.getY()-origin.y)<3.5)damage(level,player,1.15f);
        }
    }
    private List<Player> players(ServerLevel level){return level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(24),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    private void strike(ServerLevel level,List<Vec3> marks,float scale){for(var player:players(level))if(marks.stream().anyMatch(p->Math.abs(p.y-player.getY())<2.8&&p.distanceToSqr(player.position())<=1.9*1.9))damage(level,player,scale);}
    private void damage(ServerLevel level,Player player,float scale){player.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    public boolean updatePhase(float health,float max){return clock.updatePhase(health,max);}
    public void save(ValueOutput out){clock.save(out);out.store("FallenOrigin",Vec3.CODEC,origin);out.store("FallenMark",Vec3.CODEC,mark);out.putDouble("FallenHeading",heading);out.putInt("FallenDuelCount",duelCount);out.putInt("FallenRiposte",pendingRiposte);out.putInt("FallenPhase",phase);out.putBoolean("FallenFeint",duelFeint);}
    public void load(ValueInput in){clock.load(in);origin=in.read("FallenOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=in.read("FallenMark",Vec3.CODEC).orElse(Vec3.ZERO);heading=in.getDoubleOr("FallenHeading",0);duelCount=Math.max(0,in.getIntOr("FallenDuelCount",0));pendingRiposte=Math.clamp(in.getIntOr("FallenRiposte",0),0,18);phase=in.getIntOr("FallenPhase",1)==2?2:1;duelFeint=in.getBooleanOr("FallenFeint",false);}
}
