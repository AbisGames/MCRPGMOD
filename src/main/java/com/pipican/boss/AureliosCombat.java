package com.pipican.boss;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Directional mirror shield, alternating pressure rows and interceptable tribute orbs. */
public final class AureliosCombat {
    public static final String MIRRORS="mirror_guards", GRID="pressure_grid", TITHE="treasury_tithe";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO;
    private double heading;
    private List<Boolean> mirrorBroken=List.of(false,false,false),orbIntercepted=List.of(false,false,false);
    private int phase=1,mirrorTicks,shieldTicks,gridPass,gridTicks,titheTicks,titheCycle;
    public AureliosCombat(RegionalBossEntity boss){this.boss=boss;var d=BossDesigns.require("aurelios_sunken_emperor");clock=new RegionalCombatClock(Map.of(MIRRORS,time(d,0),GRID,time(d,1),TITHE,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,50)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}
    public int shieldTicks(){return shieldTicks;}
    public long interceptedOrbs(){return orbIntercepted.stream().filter(Boolean::booleanValue).count();}
    public Vec3 tributeOrbPosition(int index){return orbPosition(index);}
    public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();
        if(id.equals(MIRRORS)){mirrorBroken=List.of(false,false,false);mirrorTicks=0;}if(id.equals(GRID)){gridPass=0;gridTicks=0;}if(id.equals(TITHE)){orbIntercepted=List.of(false,false,false);titheTicks=0;}return true;}
    public void tick(ServerLevel level){tickShield();if(shieldTicks>0&&shieldTicks%5==0)for(int i=0;i<3;i++)if(!mirrorBroken.get(i)){Vec3 p=mirrorPoints().get(i);level.sendParticles(ParticleTypes.BUBBLE,p.x,p.y,p.z,3,0.14,0.18,0.14,0.005);}tickTithe(level);if(gridTicks>0){telegraphGrid(level);if(--gridTicks==0)resolveGrid(level);}
        String active=clock.active();if(!active.isEmpty()&&!clock.resolved()&&clock.remaining()%5==0)telegraph(level,active);
        if(active.isEmpty()){clock.tick();return;}String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case MIRRORS->{mirrorTicks=phase==2?180:260;shieldTicks=mirrorTicks;resolveMirrors(level);boss.playSound(SoundEvents.CONDUIT_ACTIVATE,1.4f,0.65f);}
            case GRID->{gridPass=0;gridTicks=1;}
            case TITHE->{titheTicks=32;titheCycle++;boss.playSound(SoundEvents.AMETHYST_BLOCK_CHIME,1.2f,0.7f);}
            case "basic"->strike(level,List.of(mark),0.7f);default->{}}
    }
    private void tickShield(){if(shieldTicks>0){shieldTicks--;if(mirrorTicks>0)mirrorTicks--;}}
    /** Three announced mirror sectors reduce only incoming damage from their guarded direction. */
    public float damageMultiplier(net.minecraft.world.damagesource.DamageSource source){if(shieldTicks<=0)return 1;var attacker=source.getEntity();if(attacker==null)return 1;Vec3 p=attacker.position();var nodes=mirrorPoints();
        for(int i=0;i<nodes.size();i++){if(mirrorBroken.get(i))continue;if(p.distanceToSqr(nodes.get(i))<=2.2*2.2){var broken=new ArrayList<>(mirrorBroken);broken.set(i,true);mirrorBroken=List.copyOf(broken);return 1;}}
        double angle=Math.atan2(p.z-origin.z,p.x-origin.x);double delta=Double.POSITIVE_INFINITY;for(int i=0;i<3;i++)if(!mirrorBroken.get(i)){double a=heading+i*Math.PI*2/3+titheCycle*0.12;delta=Math.min(delta,Math.abs(Math.atan2(Math.sin(angle-a),Math.cos(angle-a))));}
        return delta<0.48?0.22f:1f;}
    private void resolveMirrors(ServerLevel level){var nodes=mirrorPoints();for(int i=0;i<nodes.size();i++)if(!mirrorBroken.get(i)){Vec3 p=nodes.get(i);level.sendParticles(ParticleTypes.BUBBLE,p.x,p.y+0.5,p.z,18,0.25,0.3,0.25,0.02);Vec3 end=origin.add(Math.cos(heading+i*Math.PI*2/3)*12,0,Math.sin(heading+i*Math.PI*2/3)*12);strike(level,line(p,end),0.44f);}}
    private List<Vec3> mirrorPoints(){var out=new ArrayList<Vec3>(3);for(int i=0;i<3;i++){double a=heading+i*Math.PI*2/3+titheCycle*0.12;out.add(origin.add(Math.cos(a)*3.2,0.8,Math.sin(a)*3.2));}return out;}
    private void telegraph(ServerLevel level,String id){if(id.equals(MIRRORS)){var nodes=mirrorPoints();for(int i=0;i<3;i++){Vec3 p=nodes.get(i);level.sendParticles(mirrorBroken.get(i)?ParticleTypes.POOF:ParticleTypes.BUBBLE,p.x,p.y,p.z,12,0.2,0.28,0.2,0.01);Vec3 end=origin.add(Math.cos(heading+i*Math.PI*2/3)*12,0,Math.sin(heading+i*Math.PI*2/3)*12);telegraphLine(level,p,end);}}
        else if(id.equals(GRID))telegraphGrid(level);else{for(int i=0;i<3;i++)if(!orbIntercepted.get(i)){Vec3 p=orbPosition(i);level.sendParticles(ParticleTypes.GLOW,p.x,p.y+0.4,p.z,8,0.2,0.2,0.2,0.01);telegraphLine(level,p,origin);}}}
    private void telegraphLine(ServerLevel level,Vec3 from,Vec3 to){int n=18;for(int i=0;i<=n;i+=2){Vec3 p=from.lerp(to,i/(double)n);if(boss.insideArena(p))level.sendParticles(ParticleTypes.BUBBLE,p.x,p.y+0.12,p.z,2,0.05,0.05,0.05,0);}}
    private void telegraphGrid(ServerLevel level){int row=(gridPass%2==0?0:2)+(phase==2?1:0);Vec3 forward=new Vec3(Math.cos(heading),0,Math.sin(heading)),side=new Vec3(-forward.z,0,forward.x);for(int c=-2;c<=2;c++){Vec3 p=origin.add(forward.scale((row-1.5)*4.2)).add(side.scale(c*3.2));if(boss.insideArena(p))level.sendParticles(ParticleTypes.BUBBLE,p.x,p.y+0.08,p.z,8,0.3,0.04,0.3,0.005);} }
    private void resolveGrid(ServerLevel level){int row=(gridPass%2==0?0:2)+(phase==2?1:0);Vec3 forward=new Vec3(Math.cos(heading),0,Math.sin(heading)),side=new Vec3(-forward.z,0,forward.x);var points=new ArrayList<Vec3>();for(int c=-2;c<=2;c++){Vec3 p=origin.add(forward.scale((row-1.5)*4.2)).add(side.scale(c*3.2));if(boss.insideArena(p))points.add(p);}strike(level,points,0.66f);gridPass++;if(gridPass<2)gridTicks=8;}
    private Vec3 orbPosition(int index){double a=heading+index*Math.PI*2/3+titheCycle*0.17;Vec3 start=origin.add(Math.cos(a)*9,0.45,Math.sin(a)*9);double progress=1.0-Math.clamp(titheTicks/32.0,0,1);return start.lerp(origin.add(0,0.45,0),progress);}
    private void tickTithe(ServerLevel level){if(titheTicks<=0)return;var next=new ArrayList<>(orbIntercepted);for(int i=0;i<3;i++)if(!next.get(i)){Vec3 p=orbPosition(i);level.sendParticles(ParticleTypes.GLOW,p.x,p.y,p.z,5,0.14,0.14,0.14,0.005);for(Player player:players(level))if(player.position().distanceToSqr(p)<=1.15*1.15){next.set(i,true);shieldTicks=Math.max(0,shieldTicks-(phase==2?35:24));level.sendParticles(ParticleTypes.SPLASH,p.x,p.y,p.z,12,0.25,0.2,0.25,0.03);break;}}
        orbIntercepted=List.copyOf(next);if(--titheTicks==0){for(int i=0;i<3;i++)if(!orbIntercepted.get(i)){Vec3 p=origin;level.sendParticles(ParticleTypes.SPLASH,p.x,p.y+0.2,p.z,20,1.1,0.2,1.1,0.04);for(Player player:players(level))if(Math.abs(player.getY()-p.y)<2.5&&player.position().distanceToSqr(p)<=3.2*3.2)damage(level,player,0.52f);}}}
    private List<Vec3> line(Vec3 from,Vec3 to){int n=28;var out=new ArrayList<Vec3>();for(int i=0;i<=n;i++){Vec3 p=from.lerp(to,i/(double)n);if(boss.insideArena(p))out.add(p);}return out;}
    private void strike(ServerLevel level,List<Vec3> points,float scale){for(Player player:players(level))if(points.stream().anyMatch(p->Math.abs(p.y-player.getY())<2.8&&p.distanceToSqr(player.position())<=1.7*1.7))damage(level,player,scale);}
    private List<Player> players(ServerLevel level){return level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(26),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    private void damage(ServerLevel level,Player p,float scale){p.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    public boolean updatePhase(float health,float max){boolean changed=clock.updatePhase(health,max);if(changed)phase=2;return changed;}
    public void save(ValueOutput o){clock.save(o);o.store("AureliosOrigin",Vec3.CODEC,origin);o.store("AureliosMark",Vec3.CODEC,mark);o.store("AureliosMirrors",com.mojang.serialization.Codec.BOOL.listOf(),mirrorBroken);o.store("AureliosOrbs",com.mojang.serialization.Codec.BOOL.listOf(),orbIntercepted);o.putDouble("AureliosHeading",heading);o.putInt("AureliosPhase",phase);o.putInt("AureliosMirrorTicks",mirrorTicks);o.putInt("AureliosShieldTicks",shieldTicks);o.putInt("AureliosGridPass",gridPass);o.putInt("AureliosGridTicks",gridTicks);o.putInt("AureliosTitheTicks",titheTicks);o.putInt("AureliosTitheCycle",titheCycle);}
    public void load(ValueInput i){clock.load(i);origin=i.read("AureliosOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("AureliosMark",Vec3.CODEC).orElse(Vec3.ZERO);mirrorBroken=i.read("AureliosMirrors",com.mojang.serialization.Codec.BOOL.listOf()).orElse(List.of(false,false,false)).stream().limit(3).toList();orbIntercepted=i.read("AureliosOrbs",com.mojang.serialization.Codec.BOOL.listOf()).orElse(List.of(false,false,false)).stream().limit(3).toList();heading=i.getDoubleOr("AureliosHeading",0);phase=i.getIntOr("AureliosPhase",clock.phase())==2?2:1;mirrorTicks=Math.clamp(i.getIntOr("AureliosMirrorTicks",0),0,260);shieldTicks=Math.clamp(i.getIntOr("AureliosShieldTicks",0),0,260);gridPass=Math.clamp(i.getIntOr("AureliosGridPass",0),0,2);gridTicks=Math.clamp(i.getIntOr("AureliosGridTicks",0),0,8);titheTicks=Math.clamp(i.getIntOr("AureliosTitheTicks",0),0,32);titheCycle=Math.max(0,i.getIntOr("AureliosTitheCycle",0));}
}
