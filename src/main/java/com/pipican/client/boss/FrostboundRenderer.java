package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.FrostboundEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native posed renderer for Frostbound's existing ice-golem geometry prototype. */
public final class FrostboundRenderer extends MobRenderer<FrostboundEntity,FrostboundRenderer.State,FrostboundRenderer.Model> {
    public FrostboundRenderer(EntityRendererProvider.Context context){ super(context,new Model(BossGeometry.load("frostbound_colossus")),1.6f); }
    public static final class State extends LivingEntityRenderState { int phase,action; float remaining,hurt; }
    @Override public State createRenderState(){ return new State(); }
    @Override public Identifier getTextureLocation(State state){
        return Pipican.id("textures/entity/boss/frostbound_"+(state.phase==2 ? "awakened" : "dormant")+".png");
    }
    @Override public void extractRenderState(FrostboundEntity entity,State state,float partial){
        super.extractRenderState(entity,state,partial); state.phase=entity.phase(); state.action=entity.frostAction();
        state.remaining=Math.max(0,entity.abilityTicksRemaining()-partial); state.hurt=entity.hurtTime-partial;
    }
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(FrostboundEntity entity){ return super.getBoundingBoxForCulling(entity).inflate(2.5); }
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,core,crestLeft,crestRight;
        Model(ModelPart root){
            super(root); var base=root.getChild("root"); body=base.getChild("body"); head=body.getChild("head");
            leftArm=body.getChild("left_arm"); rightArm=body.getChild("right_arm");
            leftLeg=base.getChild("left_leg"); rightLeg=base.getChild("right_leg");
            core=body.getChild("chest_core"); crestLeft=head.getChild("left_crest"); crestRight=head.getChild("right_crest");
        }
        @Override public void setupAnim(State s){
            super.setupAnim(s);
            float walk=Mth.clamp(s.walkAnimationSpeed,0,1);
            leftLeg.xRot=Mth.cos(s.walkAnimationPos*0.55f)*0.7f*walk; rightLeg.xRot=-leftLeg.xRot;
            leftArm.xRot=-leftLeg.xRot*0.35f; rightArm.xRot=-rightLeg.xRot*0.35f;
            body.y+=Mth.sin(s.ageInTicks*0.045f)*0.08f;
            head.yRot=Mth.clamp(s.yRot,-35,35)*Mth.DEG_TO_RAD;
            float charge=ease(1-s.remaining/Math.max(1,s.action==1?48:s.action==2?60:s.action==3?80:36));
            switch(s.action){
                case 1 -> { leftArm.xRot=-1.0f*charge; rightArm.xRot=-1.0f*charge; body.xRot=0.08f*charge; }
                case 2 -> { leftArm.xRot=Mth.lerp(charge,-0.15f,-1.1f); rightArm.xRot=Mth.lerp(charge,-0.15f,-1.1f); head.xRot=-0.15f*charge; }
                case 3 -> { leftArm.xRot=-1.35f*charge; rightArm.xRot=-1.35f*charge; body.yRot=0.1f*Mth.sin(s.ageInTicks*0.2f)*charge; }
                case 4 -> { leftArm.xRot=-0.9f*charge; rightArm.xRot=0.5f*charge; }
                case 5 -> { leftArm.zRot=0.45f*charge; rightArm.zRot=-0.45f*charge; body.y-=0.18f*charge; }
                default -> {}
            }
            if(s.phase==2){ core.visible=true; core.xRot=0.08f*Mth.sin(s.ageInTicks*0.12f); }
            else core.visible=((int)s.ageInTicks%80)<70;
            if(s.hurt>0){ body.zRot=0.04f*Mth.sin(s.hurt*1.4f); head.zRot=body.zRot*1.5f; }
            if(s.deathTime>0){ body.xRot=1.25f; leftArm.xRot=-0.3f; rightArm.xRot=-0.3f; }
        }
        private static float ease(float value){ float t=Mth.clamp(value,0,1); return t*t*(3-2*t); }
    }
}
