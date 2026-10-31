package com.pipican.boss;

import java.util.Map;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Per-encounter clock for boss-specific attacks; legacy Morvath saves keep their original clock. */
public final class RegionalCombatClock {
    public record Timing(int windup,int recovery,int cooldown) {
        public Timing { if(windup<1 || recovery<1 || cooldown<1) throw new IllegalArgumentException("Invalid attack timing"); }
    }
    private final Map<String,Timing> attacks;
    private String active="";
    private int remaining,cooldown,phase=1;
    private boolean resolved;

    public RegionalCombatClock(Map<String,Timing> attacks){
        this.attacks=Map.copyOf(attacks);
        if(this.attacks.isEmpty() || this.attacks.keySet().stream().anyMatch(String::isBlank)) throw new IllegalArgumentException("Missing attack IDs");
    }
    public String active(){ return active; }
    public int remaining(){ return remaining; }
    public int cooldown(){ return cooldown; }
    public int phase(){ return phase; }
    public boolean resolved(){ return resolved; }
    public boolean begin(String id){
        var timing=attacks.get(id);
        if(timing==null || !active.isEmpty() || cooldown>0) return false;
        active=id; remaining=timing.windup()+timing.recovery(); resolved=false; return true;
    }
    /** Returns an attack ID once at impact, or an empty string on other ticks. */
    public String tick(){
        if(active.isEmpty()){ if(cooldown>0) cooldown--; return ""; }
        var timing=attacks.get(active); remaining--;
        String impact="";
        if(!resolved && remaining<=timing.recovery()){ resolved=true; impact=active; }
        if(remaining==0){ active=""; cooldown=timing.cooldown(); }
        return impact;
    }
    /** Returns true only on the first half-health transition and cancels any pending impact. */
    public boolean updatePhase(float health,float maximum){
        if(phase!=1 || maximum<=0 || health>maximum*0.5f) return false;
        phase=2; active=""; remaining=0; resolved=false; cooldown=0; return true;
    }
    public void save(ValueOutput output){
        output.putString("RegionalAttack",active); output.putInt("RegionalRemaining",remaining);
        output.putInt("RegionalCooldown",cooldown); output.putInt("RegionalPhase",phase); output.putBoolean("RegionalResolved",resolved);
    }
    public void load(ValueInput input){
        phase=input.getIntOr("RegionalPhase",1)==2 ? 2 : 1;
        cooldown=Math.clamp(input.getIntOr("RegionalCooldown",0),0,12000);
        String id=input.getStringOr("RegionalAttack",""); var timing=attacks.get(id);
        if(timing==null){ active=""; remaining=0; resolved=false; return; }
        remaining=Math.clamp(input.getIntOr("RegionalRemaining",0),0,timing.windup()+timing.recovery());
        active=remaining==0 ? "" : id;
        resolved=!active.isEmpty() && (input.getBooleanOr("RegionalResolved",false) || remaining<=timing.recovery());
    }
}
