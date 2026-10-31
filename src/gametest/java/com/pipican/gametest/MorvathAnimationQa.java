package com.pipican.gametest;

import com.pipican.Pipican;
import com.pipican.boss.BossCombatState.Ability;
import com.pipican.client.boss.RegionalBossRenderer;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/** Real production renderer with isolated, explicitly labeled preview inputs. Never packaged. */
@EventBusSubscriber(modid=Pipican.MOD_ID,value=Dist.CLIENT)
public final class MorvathAnimationQa {
    private static final String[] NAMES={"IDLE","WALK","BASIC","CLEAVE","METEOR_RAIN","ERUPTION","SUMMONING","HURT","PHASE_CHANGE","DEATH"};
    private static int preview=-1,lastSlot=-1;
    private static float start,heldTime;
    private static boolean frozen;
    @SubscribeEvent public static void register(RegisterRenderStateModifiersEvent event){
        event.registerEntityModifier(RegionalBossRenderer.class,(entity,state)->{
            var mc=Minecraft.getInstance(); var server=mc.getSingleplayerServer();
            if(!Boolean.getBoolean("pipican.clientQa") || server==null || mc.player==null) return;
            String configured=System.getProperty("pipican.clientQaRoot","");
            if(configured.isBlank() || !server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize()
                .startsWith(Path.of(configured).toAbsolutePath().normalize().resolve("saves"))) return;
            int slot=mc.player.getInventory().getSelectedSlot();
            if(slot!=lastSlot){
                if(slot==1 || slot==2){
                    preview=slot==1 ? (preview<0 ? NAMES.length-1 : Math.floorMod(preview-1,NAMES.length)) : (preview+1)%NAMES.length;
                    // Start at a reproducible inspection frame; 5 resumes the real-time loop.
                    heldTime=switch(preview){
                        case 0 -> 10;
                        case 1 -> 4;
                        case 7 -> 4;
                        case 9 -> 18;
                        default -> Ability.valueOf(NAMES[preview]).windup*0.65f;
                    };
                    start=state.ageInTicks-heldTime; frozen=true;
                    Pipican.LOGGER.info("CLIENT QA PREVIEW name={} frame={}",NAMES[preview],heldTime);
                }
                if(slot==4 && preview>=0){
                    if(frozen) start=state.ageInTicks-heldTime;
                    else heldTime=state.ageInTicks-start;
                    frozen=!frozen;
                }
                if(slot==8) preview=-1;
                lastSlot=slot;
            }
            if(preview<0) return;
            float t=frozen ? heldTime : state.ageInTicks-start;
            String name=NAMES[preview];
            state.phase=preview==8 ? 2 : 1;
            state.bodyRot=0; state.yRot=0; state.xRot=0;
            state.walkAnimationSpeed=0; state.hurtTime=0; state.deathTime=0; state.hasRedOverlay=false;
            state.ability=Ability.NONE.ordinal(); state.remaining=0;
            if(preview==1){state.walkAnimationPos=t;state.walkAnimationSpeed=0.65f;}
            else if(preview==7){state.hurtTime=Math.max(0,10-t%35);state.hasRedOverlay=state.hurtTime>0;}
            else if(preview==9) state.deathTime=Math.min(19,t%50);
            else if(preview>=2){
                var ability=Ability.valueOf(name);
                state.ability=ability.ordinal();
                state.remaining=Math.max(0,ability.windup+ability.recovery-t%(ability.windup+ability.recovery+20));
            }
            mc.gui.setOverlayMessage(Component.literal("QA PREVIEW "+name+(frozen ? " [held]" : "")+" | 2 prev, 3 next, 4 angle, 5 loop, 9 live"),false);
        });
    }
}
