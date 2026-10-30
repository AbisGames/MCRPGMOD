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

/** Spatial seam crossings, bounded gravity wells and a rotating eclipse with visible safe lanes. */
public final class NoctharCombat {
    public static final String SEAMS="void_seams", WELLS="gravity_wells", ECLIPSE="eclipse_lanes";
    private final RegionalBossEntity boss;
    private final RegionalCombatClock clock;
    private Vec3 origin=Vec3.ZERO,mark=Vec3.ZERO,wellA=Vec3.ZERO,wellB=Vec3.ZERO;
    private double heading,eclipseRotation;
    private int topology;
    public NoctharCombat(RegionalBossEntity boss){this.boss=boss;var d=BossDesigns.require("nocthar_void_sovereign");clock=new RegionalCombatClock(Map.of(SEAMS,time(d,0),WELLS,time(d,1),ECLIPSE,time(d,2),"basic",new RegionalCombatClock.Timing(20,22,48)));}
    private static RegionalCombatClock.Timing time(BossDesigns.Design d,int i){var a=d.abilities().get(i);return new RegionalCombatClock.Timing(a.windup(),a.recovery(),a.cooldown());}
    public RegionalCombatClock clock(){return clock;}
    public boolean begin(String id,LivingEntity target){
        if(target==null||!target.isAlive()||!boss.insideArena(target.position())||!clock.begin(id))return false;
        origin=boss.position();mark=target.position();heading=Math.atan2(mark.z-origin.z,mark.x-origin.x);
        if(id.equals(WELLS)){double a=heading+Math.PI/2;wellA=mark.add(Math.cos(a)*3.5,0,Math.sin(a)*3.5);wellB=mark.add(-Math.cos(a)*3.5,0,-Math.sin(a)*3.5);}
        if(id.equals(ECLIPSE))eclipseRotation+=clock.phase()==2?Math.PI/4:Math.PI/8;
        topology=clock.phase()==2?(topology+1)%2:0;return true;
    }
    public void tick(ServerLevel level){String active=clock.active();if(active.isEmpty()){clock.tick();return;}
        if(!clock.resolved()&&clock.remaining()%5==0)telegraph(level,active);
        String impact=clock.tick();if(impact.isEmpty())return;
        switch(impact){case SEAMS->{for(var segment:seamLines())strike(level,segment,0.9f);boss.playSound(SoundEvents.ENDERMAN_TELEPORT,1.5f,0.6f);}
            case WELLS->{pullAndDamage(level,wellA);pullAndDamage(level,wellB);boss.playSound(SoundEvents.WARDEN_SONIC_BOOM,1.3f,0.65f);}
            case ECLIPSE->{strikeEclipse(level);boss.playSound(SoundEvents.BEACON_ACTIVATE,1.2f,0.55f);}
            case "basic"->strike(level,List.of(mark),0.8f);default->{}}
    }
    private List<Vec3> segment(Vec3 a,Vec3 b){int n=Math.max(1,(int)(Math.sqrt(a.distanceToSqr(b))*2));return java.util.stream.IntStream.rangeClosed(0,n).mapToObj(i->a.lerp(b,i/(double)n)).filter(boss::insideArena).toList();}
    private List<List<Vec3>> seamLines(){
        double d=topology==1?Math.PI/4:-Math.PI/4;Vec3 side=new Vec3(-Math.sin(heading+d),0,Math.cos(heading+d));
        Vec3 left=origin.add(side.scale(5)),right=origin.add(side.scale(-5));
        Vec3 farA=mark.add(side.scale(topology==1?4:-4)),farB=mark.add(side.scale(topology==1?-4:4));
        return List.of(segment(left,farA),segment(right,farB));
    }
    private List<Vec3> wellRings(){var points=new ArrayList<Vec3>();for(var c:List.of(wellA,wellB))for(int i=0;i<32;i++){double a=Math.PI*2*i/32;points.add(c.add(Math.cos(a)*4,0,Math.sin(a)*4));}return points;}
    private List<Vec3> eclipseLines(){var marks=new ArrayList<Vec3>();for(int ray=0;ray<4;ray++){double a=eclipseRotation+ray*Math.PI/2;for(int i=3;i<=24;i++){var p=origin.add(Math.cos(a)*i/2.0,0,Math.sin(a)*i/2.0);if(boss.insideArena(p))marks.add(p);}}return marks;}
    private List<Vec3> marks(String active){return switch(active){case SEAMS->seamLines().stream().flatMap(List::stream).toList();case WELLS->wellRings();case ECLIPSE->eclipseLines();default->List.of(mark);};}
    private void telegraph(ServerLevel level,String active){
        if(active.equals(ECLIPSE)){
            for(int i=0;i<4;i++){double a=eclipseRotation+i*Math.PI/2;for(int d=2;d<=12;d++){var p=origin.add(Math.cos(a)*d,0,Math.sin(a)*d);level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,p.x,p.y+0.15,p.z,1,0.04,0.04,0.04,0);}}
            // End rods trace the unshaded corridor centered halfway between adjacent beams.
            double safe=eclipseRotation+Math.PI/4;for(int d=2;d<=12;d++){var p=origin.add(Math.cos(safe)*d,0,Math.sin(safe)*d);level.sendParticles(ParticleTypes.END_ROD,p.x,p.y+0.2,p.z,1,0.03,0.03,0.03,0);}
        }else{var points=marks(active);for(int i=0;i<points.size();i+=2){var p=points.get(i);level.sendParticles(active.equals(WELLS)?ParticleTypes.REVERSE_PORTAL:ParticleTypes.PORTAL,p.x,p.y+0.2,p.z,1,0.05,0.05,0.05,0);}}
    }
    private List<Player> players(ServerLevel level){return level.getEntitiesOfClass(Player.class,boss.getBoundingBox().inflate(28),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&boss.insideArena(p.position()));}
    private void strike(ServerLevel level,List<Vec3> points,float scale){for(var p:players(level))if(points.stream().anyMatch(v->Math.abs(v.y-p.getY())<3.5&&v.distanceToSqr(p.position())<=1.8*1.8))damage(level,p,scale);}
    private void pullAndDamage(ServerLevel level,Vec3 focus){for(var p:players(level)){double dx=focus.x-p.getX(),dz=focus.z-p.getZ(),dist=Math.sqrt(dx*dx+dz*dz);if(dist<=5.0&&Math.abs(focus.y-p.getY())<3.5){if(dist>0.1)p.push(dx/dist*0.32,0.08,dz/dist*0.32);if(dist<=1.8)damage(level,p,0.8f);}}}
    private void strikeEclipse(ServerLevel level){for(var p:players(level)){double dx=p.getX()-origin.x,dz=p.getZ()-origin.z,r=Math.sqrt(dx*dx+dz*dz),a=Math.atan2(dz,dx);if(r<2||r>12||Math.abs(p.getY()-origin.y)>4)continue;
        double nearest=Double.POSITIVE_INFINITY;for(int i=0;i<4;i++){double beam=eclipseRotation+i*Math.PI/2;double delta=Math.abs(Math.atan2(Math.sin(a-beam),Math.cos(a-beam)));nearest=Math.min(nearest,delta);}if(nearest<0.13)damage(level,p,0.9f);}}
    private void damage(ServerLevel level,Player p,float scale){p.hurtServer(level,boss.damageSources().mobAttack(boss),(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*scale));}
    public boolean updatePhase(float health,float max){return clock.updatePhase(health,max);}
    public void save(ValueOutput o){clock.save(o);o.store("NoctharOrigin",Vec3.CODEC,origin);o.store("NoctharMark",Vec3.CODEC,mark);o.store("NoctharWellA",Vec3.CODEC,wellA);o.store("NoctharWellB",Vec3.CODEC,wellB);o.putDouble("NoctharHeading",heading);o.putDouble("NoctharEclipse",eclipseRotation);o.putInt("NoctharTopology",topology);}
    public void load(ValueInput i){clock.load(i);origin=i.read("NoctharOrigin",Vec3.CODEC).orElse(Vec3.ZERO);mark=i.read("NoctharMark",Vec3.CODEC).orElse(Vec3.ZERO);wellA=i.read("NoctharWellA",Vec3.CODEC).orElse(Vec3.ZERO);wellB=i.read("NoctharWellB",Vec3.CODEC).orElse(Vec3.ZERO);heading=i.getDoubleOr("NoctharHeading",0);eclipseRotation=i.getDoubleOr("NoctharEclipse",0);topology=Math.floorMod(i.getIntOr("NoctharTopology",0),2);}
}
