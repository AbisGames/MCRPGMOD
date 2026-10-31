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

/** Lotus petal arcs, a previewed river coil and delayed reflected blooms. */
public final class LianhuaCombat {
    public static final String PETALS="lotus_petals", COILS="river_coils", BLOOMS="bloom_reflections";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO;
    private double heading,safeAngle;
    private int phase=1,petalPulse=-1,petalDelay,echoTicks,tailExposure;
    public LianhuaCombat(RegionalBossEntity boss){this.boss=boss;var d=BossDesigns.require("lianhua_lotus_serpent");clock=new RegionalCombatClock(Map.of(PETALS,time(d,0),COILS,time(d,1),BLOOMS,time(d,2),"basic",new RegionalCombatClock.Timing(18,20,50)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}
    public int tailExposure(){return tailExposure;}
    public float damageMultiplier(net.minecraft.world.damagesource.DamageSource source){if(phase!=2||tailExposure<=0||source.getEntity()==null)return 1;Vec3 p=source.getEntity().position();double angle=Math.atan2(p.z-origin.z,p.x-origin.x);double back=heading+Math.PI;double delta=Math.abs(Math.atan2(Math.sin(angle-back),Math.cos(angle-back)));return delta<0.7?1.45f:1f;}
    public boolean begin(String id,LivingEntity target){if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);safeAngle=heading+Math.PI*0.75+phase*0.2;phase=clock.phase();if(id.equals(PETALS)){petalPulse=0;petalDelay=0;}if(id.equals(BLOOMS))echoTicks=0;return true;}
    public void tick(ServerLevel level){if(tailExposure>0)tailExposure--;if(echoTicks>0){telegraphBloomEcho(level);if(--echoTicks==0)resolveBloomEcho(level);}
        String active=clock.active();if(!active.isEmpty()&&!clock.resolved()&&clock.remaining()%5==0)telegraph(level,active);
        if(!active.isEmpty()&&active.equals(PETALS)&&clock.resolved()&&petalPulse>=0&&petalPulse<3){if(petalDelay>0)petalDelay--;else{resolvePetals(level);petalPulse++;if(phase==2)safeAngle+=Math.PI/7;petalDelay=12;}}
        if(active.isEmpty()){clock.tick();return;}String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case PETALS->{petalPulse=0;petalDelay=1;boss.playSound(SoundEvents.BIG_DRIPLEAF_TILT_DOWN,1.2f,0.65f);}
            case COILS->{strike(level,coilPath(),0.78f);boss.playSound(SoundEvents.DOLPHIN_SPLASH,1.35f,0.75f);}
            case BLOOMS->{resolveBlooms(level);echoTicks=18;boss.playSound(SoundEvents.AMETHYST_BLOCK_CHIME,1.3f,0.8f);}
            case "basic"->strike(level,List.of(mark),0.68f);default->{}}
        if(phase==2)tailExposure=40;
    }
    private List<Vec3> petalPoints(){var points=new ArrayList<Vec3>();for(int petal=0;petal<8;petal++){double center=heading+petal*Math.PI/4;for(int r=0;r<8;r++){double radius=3.4+r*0.95;for(int a=-2;a<=2;a++){double angle=center+a*0.055;double gap=Math.abs(Math.atan2(Math.sin(angle-safeAngle),Math.cos(angle-safeAngle)));if(radius>2.8&&gap<0.36)continue;Vec3 p=origin.add(Math.cos(angle)*radius,0.08,Math.sin(angle)*radius);if(boss.insideArena(p))points.add(p);}}}return points;}
    private List<Vec3> coilPath(){var points=new ArrayList<Vec3>();for(int i=0;i<=96;i++){double t=i/96.0*Math.PI*4,r=2.0+t/(Math.PI*4)*8.6,a=heading+t;double gap=Math.abs(Math.atan2(Math.sin(a-safeAngle),Math.cos(a-safeAngle)));if(r>4&&gap<0.32)continue;Vec3 p=origin.add(Math.cos(a)*r,0.12,Math.sin(a)*r);if(boss.insideArena(p))points.add(p);}return points;}
    private List<Vec3> blooms(){var points=new ArrayList<Vec3>();for(int i=0;i<4;i++){double a=heading+i*Math.PI/2+0.18;Vec3 p=origin.add(Math.cos(a)*7,0.12,Math.sin(a)*7);if(boss.insideArena(p))points.add(p);}return points;}
    private void telegraph(ServerLevel level,String id){if(id.equals(PETALS)){for(int i=0;i<petalPoints().size();i+=3){Vec3 p=petalPoints().get(i);level.sendParticles(ParticleTypes.CHERRY_LEAVES,p.x,p.y,p.z,2,0.08,0.02,0.08,0.003);}level.sendParticles(ParticleTypes.HAPPY_VILLAGER,origin.x,origin.y+0.2,origin.z,12,0.3,0.04,0.3,0);}
        else if(id.equals(COILS))telegraph(level,coilPath(),ParticleTypes.SPLASH);else for(Vec3 p:blooms())level.sendParticles(ParticleTypes.CHERRY_LEAVES,p.x,p.y+0.15,p.z,10,0.45,0.08,0.45,0.005);}
    private void telegraphBloomEcho(ServerLevel level){for(Vec3 p:reflectedBlooms())level.sendParticles(ParticleTypes.END_ROD,p.x,p.y+0.15,p.z,8,0.3,0.08,0.3,0.005);}
    private List<Vec3> reflectedBlooms(){var out=new ArrayList<Vec3>();for(Vec3 p:blooms()){Vec3 reflected=origin.add(-(p.x-origin.x),0.12,-(p.z-origin.z));if(boss.insideArena(reflected))out.add(reflected);}return out;}
    private void resolvePetals(ServerLevel level){var points=petalPoints();for(int i=0;i<points.size();i+=2){Vec3 p=points.get(i);level.sendParticles(ParticleTypes.SPLASH,p.x,p.y,p.z,1,0.18,0.04,0.18,0.01);}strike(level,points,0.58f);}
    private void resolveBlooms(ServerLevel level){for(Vec3 p:blooms()){level.sendParticles(ParticleTypes.CHERRY_LEAVES,p.x,p.y,p.z,18,0.3,0.1,0.3,0.01);strike(level,List.of(p),0.45f);}}
    private void resolveBloomEcho(ServerLevel level){for(Vec3 p:reflectedBlooms()){level.sendParticles(ParticleTypes.SPLASH,p.x,p.y,p.z,16,0.45,0.12,0.45,0.02);strike(level,List.of(p),0.48f);}}
    private void telegraph(ServerLevel level,List<Vec3> points,net.minecraft.core.particles.ParticleOptions effect){for(int i=0;i<points.size();i+=3){Vec3 p=points.get(i);level.sendParticles(effect,p.x,p.y,p.z,1,0.02,0.01,0.02,0);}}
    private void telegraph(ServerLevel level,List<Vec3> points){telegraph(level,points,ParticleTypes.CHERRY_LEAVES);}
    private void strike(ServerLevel level,List<Vec3> points,float scale){for(Player player:players(level))if(points.stream().anyMatch(p->Math.abs(player.getY()-p.y)<2.8&&p.distanceToSqr(player.position())<=1.65*1.65))player.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    private List<Player> players(ServerLevel level){return level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(26),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    public boolean updatePhase(float health,float max){boolean changed=clock.updatePhase(health,max);if(changed)phase=2;return changed;}
    public void save(ValueOutput o){clock.save(o);o.store("LianhuaOrigin",Vec3.CODEC,origin);o.store("LianhuaMark",Vec3.CODEC,mark);o.putDouble("LianhuaHeading",heading);o.putDouble("LianhuaSafeAngle",safeAngle);o.putInt("LianhuaPhase",phase);o.putInt("LianhuaPetalPulse",petalPulse);o.putInt("LianhuaPetalDelay",petalDelay);o.putInt("LianhuaEchoTicks",echoTicks);o.putInt("LianhuaTailExposure",tailExposure);}
    public void load(ValueInput i){clock.load(i);origin=i.read("LianhuaOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("LianhuaMark",Vec3.CODEC).orElse(Vec3.ZERO);heading=i.getDoubleOr("LianhuaHeading",0);safeAngle=i.getDoubleOr("LianhuaSafeAngle",Math.PI);phase=i.getIntOr("LianhuaPhase",clock.phase())==2?2:1;petalPulse=Math.clamp(i.getIntOr("LianhuaPetalPulse",-1),-1,3);petalDelay=Math.clamp(i.getIntOr("LianhuaPetalDelay",0),0,12);echoTicks=Math.clamp(i.getIntOr("LianhuaEchoTicks",0),0,18);tailExposure=Math.clamp(i.getIntOr("LianhuaTailExposure",0),0,40);}
}
