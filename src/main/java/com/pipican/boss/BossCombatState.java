package com.pipican.boss;

/** Server-owned combat clock. Time is remaining ticks so unloading cannot skip a telegraph. */
public final class BossCombatState {
    public enum Ability {
        NONE(0, 0), CLEAVE(30, 18), METEOR_RAIN(50, 40), ERUPTION(45, 30), PHASE_CHANGE(60, 20), SUMMONING(60,20), BASIC(18,18);
        public final int windup, recovery;
        Ability(int windup, int recovery) { this.windup=windup; this.recovery=recovery; }
    }
    private int phase=1, remaining, cooldown=40;
    private Ability ability=Ability.NONE;
    private boolean resolved;
    public int phase(){ return phase; }
    public int remaining(){ return remaining; }
    public int cooldown(){ return cooldown; }
    public Ability ability(){ return ability; }
    public boolean resolved(){ return resolved; }
    public boolean ready(){ return ability==Ability.NONE && cooldown==0; }
    public boolean begin(Ability next){
        if(!ready() || next==Ability.NONE || next==Ability.PHASE_CHANGE) return false;
        start(next); return true;
    }
    public void summon(){ start(Ability.SUMMONING); }
    private void start(Ability next){ ability=next; remaining=next.windup+next.recovery; resolved=false; }
    /** Half health transition is latched, including across healing and save/reload. */
    public void updatePhase(float health, float maximum){
        if(phase==1 && maximum>0 && health<=maximum*0.5f){ phase=2; start(Ability.PHASE_CHANGE); }
    }
    /** Returns an impact once, after its complete windup; never repeats a restored impact. */
    public Ability tick(){
        if(ability==Ability.NONE){ if(cooldown>0) cooldown--; return Ability.NONE; }
        if(remaining>0) remaining--;
        Ability impact=Ability.NONE;
        if(!resolved && remaining<=ability.recovery){ resolved=true; impact=ability; }
        if(remaining==0){ ability=Ability.NONE; cooldown=phase==2 ? 35 : 55; }
        return impact;
    }
    public void restore(int phase, int ordinal, int remaining, int cooldown, boolean resolved){
        this.phase=phase==2 ? 2 : 1;
        this.ability=ordinal>=0 && ordinal<Ability.values().length ? Ability.values()[ordinal] : Ability.NONE;
        this.remaining=Math.clamp(remaining,0,this.ability.windup+this.ability.recovery);
        this.cooldown=Math.clamp(cooldown,0,1200);
        this.resolved=resolved;
        if(this.remaining==0) this.ability=Ability.NONE;
    }
}
