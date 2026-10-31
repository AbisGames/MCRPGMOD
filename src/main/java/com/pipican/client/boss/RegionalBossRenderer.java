package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.RegionalBossEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class RegionalBossRenderer extends MobRenderer<RegionalBossEntity,RegionalBossRenderer.State,RegionalBossRenderer.Model> {
    public RegionalBossRenderer(EntityRendererProvider.Context context){ super(context,new Model(BossGeometry.load("morvath_ashen_king")),1.1f);
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<State,Model>(this){
            @Override public void submit(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,int light,State state,float yaw,float pitch){
                var texture=Pipican.id("textures/entity/boss/morvath_"+(state.phase==2 ? "enraged" : "armored")+"_glow.png");
                collector.order(1).submitModel(getParentModel(),state,pose,net.minecraft.client.renderer.rendertype.RenderTypes.eyes(texture),light,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,state.outlineColor,null);
            }
        }); }
    public static final class State extends LivingEntityRenderState { public int phase=1,ability; public float remaining,hurtTime; }
    @Override public State createRenderState(){ return new State(); }
    @Override public Identifier getTextureLocation(State state){ return Pipican.id("textures/entity/boss/morvath_"+(state.phase==2 ? "enraged" : "armored")+".png"); }
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(RegionalBossEntity entity){
        // Raised blade/crests and the sideways death pose extend beyond the combat hitbox.
        return super.getBoundingBoxForCulling(entity).inflate(entity.deathTime>0 ? 4 : 2);
    }
    @Override protected void setupRotations(State state,com.mojang.blaze3d.vertex.PoseStack pose,float bodyRot,float scale){
        if(state.deathTime>0){
            // Vanilla rolls around the feet, burying a wide boss and his sword in the floor.
            // Measure the animated boxes in that same transform and keep the lowest surface above it.
            var bounds=new com.mojang.blaze3d.vertex.PoseStack();
            super.setupRotations(state,bounds,bodyRot,scale);
            bounds.scale(-1,-1,1); bounds.translate(0,-1.501f,0);
            model.setupAnim(state);
            float[] lowest={0}; var vertex=new org.joml.Vector3f();
            model.root().visit(bounds,(partPose,path,index,cube)->{
                for(int corner=0;corner<8;corner++){
                    partPose.pose().transformPosition((corner&1)==0 ? cube.minX/16 : cube.maxX/16,
                        (corner&2)==0 ? cube.minY/16 : cube.maxY/16,
                        (corner&4)==0 ? cube.minZ/16 : cube.maxZ/16,vertex);
                    lowest[0]=Math.min(lowest[0],vertex.y);
                }
            });
            pose.translate(0,-lowest[0]+0.01f,0);
        }
        super.setupRotations(state,pose,bodyRot,scale);
    }
    @Override public void extractRenderState(RegionalBossEntity entity,State state,float partial){ super.extractRenderState(entity,state,partial); state.phase=entity.phase(); state.ability=entity.ability().ordinal(); state.remaining=Math.max(0,entity.abilityTicksRemaining()-(entity.isNoAi() ? 0 : partial)); state.hurtTime=entity.hurtTime-partial; }
    static final class Model extends EntityModel<State> {
        private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,eyes;
        Model(ModelPart modelRoot){
            super(modelRoot); var root=modelRoot.getChild("root");
            body=root.getChild("body");
            head=body.getChild("head"); eyes=head.getChild("animated_eyes"); leftArm=body.getChild("left_arm"); rightArm=body.getChild("right_arm");
            leftLeg=root.getChild("left_leg"); rightLeg=root.getChild("right_leg");
        }
        @Override public void setupAnim(State state){
            super.setupAnim(state);
            eyes.visible=((int)state.ageInTicks%100)<95;
            head.yRot=Mth.clamp(state.yRot,-45,45)*Mth.DEG_TO_RAD; head.xRot=Mth.clamp(state.xRot,-30,35)*Mth.DEG_TO_RAD;
            leftLeg.xRot=Mth.cos(state.walkAnimationPos*0.6662f)*1.4f*state.walkAnimationSpeed;
            rightLeg.xRot=-leftLeg.xRot;
            // Limit the armed hand's gait so the heavy blade never sweeps through the floor.
            leftArm.xRot=-leftLeg.xRot*0.65f; rightArm.xRot=-0.08f-rightLeg.xRot*0.16f;
            leftArm.zRot=0.035f*Mth.sin(state.ageInTicks*0.09f); rightArm.zRot=-leftArm.zRot;
            body.y+=0.12f*Mth.sin(state.ageInTicks*0.07f);
            var ability=com.pipican.boss.BossCombatState.Ability.values()[state.ability];
            float elapsed=ability.windup+ability.recovery-state.remaining;
            float windup=ease(elapsed/Math.max(1,ability.windup));
            float recovery=ease((elapsed-ability.windup)/Math.max(1,ability.recovery));
            float held=windup*(1-recovery);
            switch(ability){
                case BASIC, CLEAVE -> {
                    boolean cleave=ability==com.pipican.boss.BossCombatState.Ability.CLEAVE;
                    // Commit the visible downswing at the authoritative end of windup.
                    float charge=ease(elapsed/(ability.windup*0.72f));
                    float strike=ease((elapsed-ability.windup*0.72f)/(ability.windup*0.28f));
                    rightArm.xRot=Mth.lerp(recovery,Mth.lerp(strike,-1.65f*charge,0.12f),-0.08f);
                    rightArm.yRot=cleave ? Mth.lerp(strike,-0.85f*charge,0.65f)*(1-recovery) : 0;
                    rightArm.zRot=cleave ? -0.3f*held : -0.1f*held;
                    body.yRot=(cleave ? -0.18f : -0.07f)*held*(1-2*strike);
                    leftArm.xRot=-0.35f*held;
                }
                case METEOR_RAIN -> { leftArm.xRot=-2.65f*held; leftArm.zRot=0.22f*held; rightArm.xRot=-0.95f*held; rightArm.zRot=-0.18f*held; head.xRot=-0.3f*held; }
                case ERUPTION -> {
                    float strike=ease((elapsed-ability.windup*0.75f)/(ability.windup*0.25f));
                    rightArm.xRot=Mth.lerp(recovery,Mth.lerp(strike,-1.55f*windup,-0.2f),-0.08f);
                    leftArm.xRot=-1.3f*held; head.xRot=0.35f*held; body.xRot=0.1f*held;
                }
                case PHASE_CHANGE -> { leftArm.zRot=0.8f*held; rightArm.zRot=-0.8f*held; leftArm.xRot=-0.25f*held; rightArm.xRot=-0.4f*held; head.xRot=-0.4f*held; body.y-=0.8f*held; }
                case SUMMONING -> { leftArm.xRot=-1.2f*(1-windup); rightArm.xRot=-1.2f*(1-windup); head.xRot=0.7f*(1-windup); }
                default -> {}
            }
            if(state.hurtTime>0){ head.zRot=0.12f*Mth.sin(state.hurtTime*1.5f); body.zRot=0.025f*Mth.sin(state.hurtTime*1.5f); }
            if(state.deathTime>0){ leftArm.xRot=-0.5f; rightArm.xRot=-0.5f; leftArm.zRot=0.25f; rightArm.zRot=-0.25f; head.xRot=0.6f; }
        }
        private static float ease(float value){ float t=Mth.clamp(value,0,1); return t*t*(3-2*t); }
    }
}
