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

/** Fixed current lanes, orbiting undertow gates and delayed leviathan breaches. */
public final class NhalkorrCombat {
    public static final String CURRENT="anchor_current",UNDERTOW="undertow_loop",BREACHES="ink_breaches";
    private final RegionalBossEntity boss;private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO;private double heading;
    private List<Boolean> anchors=List.of(false,false,false);private List<Vec3> breaches=List.of(Vec3.ZERO,Vec3.ZERO,Vec3.ZERO);
    private int phase=1,safeCurrent=1,breachDelay=-1,breachIndex,loopShift;
    public NhalkorrCombat(RegionalBossEntity boss){this.boss=boss;var d=BossDesigns.require("nhalkorr_leviathan");clock=new RegionalCombatClock(Map.of(CURRENT,time(d,0),UNDERTOW,time(d,1),BREACHES,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,55)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int n){var a=d.abilities().get(n);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}public int brokenAnchors(){return (int)anchors.stream().filter(Boolean::booleanValue).count();}public int pendingBreachTicks(){return breachDelay;}public int breachIndex(){return breachIndex;}
    public Vec3 anchorPosition(int i){if(i<0||i>=3)throw new IndexOutOfBoundsException();return anchor(i);}public Vec3 safeCurrentPosition(){return origin.add(side().scale((safeCurrent-1)*4.5+7)).add(forward().scale(4));}public Vec3 breachPosition(int i){if(i<0||i>=3)throw new IndexOutOfBoundsException();return breaches.get(i);}public Vec3 vortexPosition(int i){double progress=idProgress(UNDERTOW),radius=6-1.5*progress,a=loopShift*.35+i*Math.PI*2/3+progress*.55;return origin.add(Math.cos(a)*radius,0,Math.sin(a)*radius);}private double idProgress(String id){return clock.active().equals(id)?Math.clamp((time(id)-clock.remaining())/(double)time(id),0,1):0;}private int time(String id){return BossDesigns.require("nhalkorr_leviathan").abilities().stream().filter(a->a.id().equals(id)).mapToInt(BossDesigns.Ability::windup).findFirst().orElse(1);}
    public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);phase=clock.phase();if(phase==2){safeCurrent=(safeCurrent+1)%3;loopShift=(loopShift+1)%12;heading+=0.12;boss.playSound(SoundEvents.CONDUIT_ACTIVATE,1.1f,0.72f);}if(id.equals(CURRENT))anchors=List.of(false,false,false);if(id.equals(BREACHES)){breaches=List.of(mark,mark.add(side().scale(4.5)),mark.add(side().scale(-4.5)));breachIndex=0;breachDelay=-1;}return true;}
    public void tick(ServerLevel l){if(clock.active().equals(CURRENT)&&!clock.resolved())checkAnchors(l);String id=clock.active();if(!id.isEmpty()&&!clock.resolved()&&clock.remaining()%5==0)telegraph(l,id);
        if(id.equals(BREACHES)&&clock.resolved()&&breachIndex<3){if(breachDelay<0)breachDelay=2;else if(--breachDelay==0){resolveBreach(l,breachIndex++);breachDelay=breachIndex<3?10:-1;}}
        if(id.isEmpty()){clock.tick();return;}String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case CURRENT->{if(!anchors.get(safeCurrent))strike(l,currentLine(),0.48f);boss.playSound(SoundEvents.PLAYER_SPLASH,1.25f,0.6f);}case UNDERTOW->{for(int i=0;i<3;i++)if(i!=safeCurrent)strike(l,circle(vortexPosition(i),2.0),0.32f);boss.playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,1.3f,0.65f);}case BREACHES->{breachIndex=0;breachDelay=2;boss.playSound(SoundEvents.DOLPHIN_PLAY,1.15f,0.55f);}case "basic"->strike(l,List.of(mark),0.68f);default->{}}}
    private Vec3 forward(){return new Vec3(Math.cos(heading),0,Math.sin(heading));}private Vec3 side(){Vec3 f=forward();return new Vec3(-f.z,0,f.x);}private Vec3 anchor(int i){return origin.add(forward().scale(-4)).add(side().scale((i-1)*4));}
    private List<Vec3> currentLine(){var out=new ArrayList<Vec3>();Vec3 start=origin.add(forward().scale(-8)).add(side().scale((safeCurrent-1)*4.5));for(int n=0;n<=32;n++){Vec3 p=start.add(forward().scale(n*.5));if(boss.insideArena(p))out.add(p);}return out;}
    private List<Vec3> circle(Vec3 center,double radius){var out=new ArrayList<Vec3>();for(int a=0;a<360;a+=8){double r=Math.toRadians(a);Vec3 p=center.add(Math.cos(r)*radius,0,Math.sin(r)*radius);if(boss.insideArena(p))out.add(p);}return out;}
    private void checkAnchors(ServerLevel l){var next=new ArrayList<>(anchors);for(int i=0;i<3;i++)if(!next.get(i)){Vec3 p=anchor(i);var shots=l.getEntitiesOfClass(Projectile.class,new net.minecraft.world.phys.AABB(p.add(-1,-1,-1),p.add(1,1.5,1)),Projectile::isAlive);if(!shots.isEmpty()){next.set(i,true);shots.forEach(Projectile::discard);l.sendParticles(ParticleTypes.POOF,p.x,p.y,p.z,14,.25,.2,.25,.02);}}anchors=List.copyOf(next);}
    private void telegraph(ServerLevel l,String id){if(id.equals(CURRENT)){for(int i=0;i<3;i++){Vec3 p=anchor(i);l.sendParticles(anchors.get(i)?ParticleTypes.POOF:ParticleTypes.NAUTILUS,p.x,p.y+.2,p.z,9,.2,.15,.2,0);}for(Vec3 p:currentLine())if((int)(p.subtract(origin).length()*2)%4==0)l.sendParticles(ParticleTypes.BUBBLE,p.x,p.y+.08,p.z,2,.08,.03,.08,0);}else if(id.equals(UNDERTOW)){for(int i=0;i<3;i++)for(Vec3 p:circle(vortexPosition(i),2.0))if((int)(p.x+p.z)%3==0)l.sendParticles(i==safeCurrent?ParticleTypes.HAPPY_VILLAGER:ParticleTypes.BUBBLE_COLUMN_UP,p.x,p.y+.08,p.z,1,.03,.02,.03,0);}else for(int i=breachIndex;i<3;i++){Vec3 p=breaches.get(i);l.sendParticles(ParticleTypes.SQUID_INK,p.x,p.y+.1,p.z,8,.45,.12,.45,0);}}
    private void resolveBreach(ServerLevel l,int i){Vec3 p=breaches.get(i);var points=circle(p,2.2);l.sendParticles(ParticleTypes.SPLASH,p.x,p.y,p.z,24,.6,.2,.6,.06);strike(l,points,0.4f);}
    private void strike(ServerLevel l,List<Vec3> pts,float scale){for(Player p:players(l))if(pts.stream().anyMatch(q->Math.abs(p.getY()-q.y)<2.7&&p.position().distanceToSqr(q)<1.55*1.55))p.hurtServer(l,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    private List<Player> players(ServerLevel l){return l.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(28),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    public boolean updatePhase(float hp,float max){boolean changed=clock.updatePhase(hp,max);if(changed)phase=2;return changed;}
    public void save(ValueOutput o){clock.save(o);o.store("NhalkorrOrigin",Vec3.CODEC,origin);o.store("NhalkorrMark",Vec3.CODEC,mark);o.store("NhalkorrAnchors",com.mojang.serialization.Codec.BOOL.listOf(),anchors);o.store("NhalkorrBreaches",Vec3.CODEC.listOf(),breaches);o.putDouble("NhalkorrHeading",heading);o.putInt("NhalkorrPhase",phase);o.putInt("NhalkorrSafeCurrent",safeCurrent);o.putInt("NhalkorrBreachDelay",breachDelay);o.putInt("NhalkorrBreachIndex",breachIndex);o.putInt("NhalkorrLoopShift",loopShift);}
    public void load(ValueInput i){clock.load(i);origin=i.read("NhalkorrOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("NhalkorrMark",Vec3.CODEC).orElse(Vec3.ZERO);anchors=normalize(i.read("NhalkorrAnchors",com.mojang.serialization.Codec.BOOL.listOf()).orElse(List.of()),false);breaches=normalize(i.read("NhalkorrBreaches",Vec3.CODEC.listOf()).orElse(List.of()),Vec3.ZERO);heading=i.getDoubleOr("NhalkorrHeading",0);phase=clock.phase();safeCurrent=Math.floorMod(i.getIntOr("NhalkorrSafeCurrent",1),3);breachDelay=Math.clamp(i.getIntOr("NhalkorrBreachDelay",-1),-1,12);breachIndex=Math.clamp(i.getIntOr("NhalkorrBreachIndex",0),0,3);loopShift=Math.floorMod(i.getIntOr("NhalkorrLoopShift",0),12);}
    private static <T> List<T> normalize(List<T> values,T fallback){var out=new ArrayList<T>(3);for(int i=0;i<3;i++)out.add(i<values.size()?values.get(i):fallback);return List.copyOf(out);}
}
