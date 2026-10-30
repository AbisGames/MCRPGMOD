package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.FallenKingEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native sword, cavalry and regalia poses for the preserved king geometry. */
public final class FallenKingRenderer extends MobRenderer<FallenKingEntity,FallenKingRenderer.State,FallenKingRenderer.Model>{
    public FallenKingRenderer(EntityRendererProvider.Context context){super(context,new Model(BossGeometry.load("fallen_king")),0.95f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/fallen_king_"+(s.phase==2?"unbound":"regalia")+".png");}
    @Override public void extractRenderState(FallenKingEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.fallenAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(FallenKingEntity e){return super.getBoundingBoxForCulling(e).inflate(1.8);}
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,leftCrest,rightCrest;
        Model(ModelPart root){super(root);var base=root.getChild("root");body=base.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=base.getChild("left_leg");rightLeg=base.getChild("right_leg");leftCrest=head.getChild("left_crest");rightCrest=head.getChild("right_crest");}
        @Override public void setupAnim(State s){
            super.setupAnim(s);float gait=Mth.clamp(s.walkAnimationSpeed,0,1),sway=Mth.sin(s.ageInTicks*0.07f);
            leftLeg.xRot=Mth.cos(s.walkAnimationPos*0.65f)*0.55f*gait;rightLeg.xRot=-leftLeg.xRot;
            leftArm.xRot=-leftLeg.xRot*0.35f;rightArm.xRot=-rightLeg.xRot*0.35f;head.yRot=Mth.clamp(s.yRot,-35,35)*Mth.DEG_TO_RAD;
            body.yRot=sway*0.025f;leftCrest.zRot=sway*0.035f;rightCrest.zRot=-leftCrest.zRot;
            float windup=1-Mth.clamp(s.remaining/70f,0,1);
            switch(s.action){case 1->{rightArm.xRot=-0.9f*windup;}
                case 2->{rightArm.xRot=-1.45f*windup;body.xRot=0.08f*windup;}
                case 3->{leftArm.xRot=-0.75f*windup;rightArm.xRot=-0.75f*windup;head.xRot=-0.2f*windup;}
                case 4->{leftArm.zRot=-0.6f*windup;rightArm.zRot=0.6f*windup;body.yRot+=0.18f*windup;}
                case 5->{leftArm.xRot=-0.9f;rightArm.xRot=-0.9f;body.y-=0.08f*windup;}
                case 6->{leftArm.xRot=-0.8f;rightArm.xRot=-0.8f;}
                default->{}}
            if(s.phase==2){head.xRot+=Mth.sin(s.ageInTicks*0.12f)*0.045f;}
            if(s.hurt>0){body.zRot=0.04f*Mth.sin(s.hurt*1.5f);head.zRot=body.zRot*1.5f;}
            if(s.deathTime>0){body.xRot=1.2f;leftArm.xRot=-0.35f;rightArm.xRot=-0.35f;}
        }
    }
}
