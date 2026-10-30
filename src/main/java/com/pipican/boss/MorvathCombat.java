package com.pipican.boss;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Owned by one boss; telegraphs, fixed target snapshots and hazards never edit terrain. */
public final class MorvathCombat {
    public static final double CLEAVE_RANGE=6, CLEAVE_HALF_ANGLE=60;
    private final RegionalBossEntity boss;
    private List<Vec3> marks=List.of(), zones=List.of();
    private int zoneTicks, sequence;
    private float yaw;
    public MorvathCombat(RegionalBossEntity boss){ this.boss=boss; }
    public boolean begin(BossCombatState clock,BossCombatState.Ability ability,LivingEntity target){
        if(!clock.begin(ability)) return false;
        double dx=target.getX()-boss.getX(), dz=target.getZ()-boss.getZ();
        yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));
        var selected=new ArrayList<Vec3>();
        if(ability==BossCombatState.Ability.METEOR_RAIN){
            for(var player:players()){
                selected.add(player.position());
                if(clock.phase()==2) selected.add(player.position().add(3,0,0));
                if(selected.size()>=6) break;
            }
        }else if(ability==BossCombatState.Ability.ERUPTION){
            selected.add(target.position());
            selected.add(target.position().add(4,0,0)); selected.add(target.position().add(-4,0,0));
        }
        marks=selected.stream().filter(boss::insideArena).limit(6).toList();
        return true;
    }
    public void tick(ServerLevel level,BossCombatState clock){
        tickZones(level,clock.phase());
        var target=boss.getTarget();
        if(target!=null && (!target.isAlive() || !boss.insideArena(target.position()))) { boss.setTarget(null); target=null; }
        if(clock.ready() && target!=null && boss.hasLineOfSight(target)){
            var pattern=List.of(BossCombatState.Ability.CLEAVE,BossCombatState.Ability.METEOR_RAIN,BossCombatState.Ability.ERUPTION);
            var next=pattern.get(Math.floorMod(sequence++,3));
            if(next==BossCombatState.Ability.CLEAVE && boss.distanceToSqr(target)>BossBalance.CLEAVE_RANGE.get()*BossBalance.CLEAVE_RANGE.get()) next=BossCombatState.Ability.METEOR_RAIN;
            begin(clock,next,target);
        }
        if(clock.ability()==BossCombatState.Ability.NONE){
            if(target!=null) boss.getNavigation().moveTo(target,clock.phase()==2 ? 1.25 : 1.0);
        }else{
            boss.getNavigation().stop();
            if(clock.ability()==BossCombatState.Ability.CLEAVE){ boss.setYRot(yaw); boss.yBodyRot=yaw; }
            if(!clock.resolved()) telegraph(level,clock);
        }
        var impact=clock.tick();
        switch(impact){
            case CLEAVE -> {
                for(var player:players()) if(inCleave(boss.position(),yaw,player.position(),BossBalance.CLEAVE_RANGE.get(),BossBalance.CLEAVE_HALF_ANGLE.get())
                    && boss.hasLineOfSight(player)) hit(level,player,clock.phase()==2 ? 18 : 14,1.1);
                boss.playSound(SoundEvents.PLAYER_ATTACK_SWEEP,2,0.65f);
            }
            case METEOR_RAIN -> {
                var victims=new HashSet<Player>();
                for(var mark:marks){
                    level.sendParticles(ParticleTypes.FLAME,mark.x,mark.y+0.5,mark.z,25,1,0.5,1,0.05);
                    level.sendParticles(ParticleTypes.EXPLOSION,mark.x,mark.y+0.5,mark.z,1,0,0,0,0);
                    for(var player:players()) if(near(mark,player.position(),2.5) && boss.hasLineOfSight(player)) victims.add(player);
                }
                for(var player:victims) hit(level,player,clock.phase()==2 ? 16 : 12,0.6);
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(),1.6f,0.8f);
            }
            case ERUPTION -> { zones=List.copyOf(marks); zoneTicks=clock.phase()==2 ? 120 : 80; }
            case PHASE_CHANGE -> { marks=List.of(); boss.playSound(SoundEvents.WITHER_SPAWN,1,0.7f); }
            default -> {}
        }
    }
    private List<Player> players(){
        return boss.level().getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(40),
            player->player.isAlive() && !player.isCreative() && !player.isSpectator() && boss.insideArena(player.position()));
    }
    private void telegraph(ServerLevel level,BossCombatState clock){
        if(clock.ability()==BossCombatState.Ability.PHASE_CHANGE){
            if(clock.remaining()%4==0) level.sendParticles(ParticleTypes.FLAME,boss.getX(),boss.getY()+2,boss.getZ(),12,1,1.5,1,0.02);
            return;
        }
        if(clock.ability()==BossCombatState.Ability.CLEAVE){
            if(clock.remaining()%3!=0) return;
            for(double angle=-BossBalance.CLEAVE_HALF_ANGLE.get();angle<=BossBalance.CLEAVE_HALF_ANGLE.get();angle+=10){
                double theta=Math.toRadians(yaw+angle);
                for(int radius=2;radius<=BossBalance.CLEAVE_RANGE.get();radius+=2) level.sendParticles(ParticleTypes.FLAME,boss.getX()-Math.sin(theta)*radius,boss.getY()+0.15,boss.getZ()+Math.cos(theta)*radius,1,0,0,0,0);
            }
        }else for(var mark:marks){
            if(clock.remaining()%4==0) ring(level,mark,2.5,false);
            int until=clock.remaining()-clock.ability().recovery;
            if(clock.ability()==BossCombatState.Ability.METEOR_RAIN && until<=15){
                double height=until*0.8;
                level.sendParticles(ParticleTypes.FLAME,mark.x,mark.y+height,mark.z,8,0.3,0.6,0.3,0.02);
                level.sendParticles(ParticleTypes.SMOKE,mark.x,mark.y+height+0.7,mark.z,3,0.3,0.2,0.3,0.01);
            }
        }
    }
    private void tickZones(ServerLevel level,int phase){
        if(zoneTicks<=0) return;
        zoneTicks--; // Persist the advanced clock before any damage; never replay a pulse on reload.
        if(zoneTicks%4==0) for(var zone:zones) ring(level,zone,2.5,true);
        if(zoneTicks%20==0){
            for(var player:players()) if(zones.stream().anyMatch(zone->near(zone,player.position(),2.5)) && boss.hasLineOfSight(player))
                hit(level,player,phase==2 ? 7 : 4,0);
        }
        if(zoneTicks==0) zones=List.of();
    }
    private void hit(ServerLevel level,Player player,float damage,double knockback){
        if(player.hurtServer(level,boss.damageSources().mobAttack(boss),damage)){
            player.igniteForSeconds(2);
            if(knockback>0) player.knockback(knockback,boss.getX()-player.getX(),boss.getZ()-player.getZ());
        }
    }
    public static boolean inCleave(Vec3 origin,float yaw,Vec3 target,double range,double halfAngle){
        double dx=target.x-origin.x,dz=target.z-origin.z,distance=Math.sqrt(dx*dx+dz*dz);
        return Math.abs(target.y-origin.y)<3 && distance<=range && (distance<0.001 || (-Math.sin(Math.toRadians(yaw))*dx+Math.cos(Math.toRadians(yaw))*dz)/distance>=Math.cos(Math.toRadians(halfAngle)));
    }
    private static boolean near(Vec3 mark,Vec3 target,double radius){ return Math.abs(mark.y-target.y)<2.5 && new Vec3(mark.x,0,mark.z).distanceToSqr(new Vec3(target.x,0,target.z))<=radius*radius; }
    private static void ring(ServerLevel level,Vec3 mark,double radius,boolean active){
        for(int angle=0;angle<360;angle+=30){ double theta=Math.toRadians(angle);
            level.sendParticles(active ? ParticleTypes.LAVA : ParticleTypes.SOUL_FIRE_FLAME,mark.x+Math.cos(theta)*radius,mark.y+0.1,mark.z+Math.sin(theta)*radius,1,0,0,0,0);
        }
    }
    public void clear(){ marks=List.of(); zones=List.of(); zoneTicks=0; }
    public void save(ValueOutput output){
        output.store("BossMarks",Vec3.CODEC.listOf(),marks); output.store("BossZones",Vec3.CODEC.listOf(),zones);
        output.putInt("BossZoneTicks",zoneTicks); output.putInt("BossSequence",sequence); output.putFloat("BossAttackYaw",yaw);
    }
    public void load(ValueInput input){
        marks=input.read("BossMarks",Vec3.CODEC.listOf()).orElse(List.of()).stream().limit(6).toList();
        zones=input.read("BossZones",Vec3.CODEC.listOf()).orElse(List.of()).stream().limit(6).toList();
        zoneTicks=Math.clamp(input.getIntOr("BossZoneTicks",0),0,120); sequence=Math.max(0,input.getIntOr("BossSequence",0))%3;
        yaw=input.getFloatOr("BossAttackYaw",0);
    }
}
