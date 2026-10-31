package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.EldarothEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native pose animation for Eldaroth's wind-ups, root growth and phase awakening. */
public final class EldarothRenderer extends MobRenderer<EldarothEntity,EldarothRenderer.State,EldarothRenderer.Model>{
    public EldarothRenderer(EntityRendererProvider.Context context){super(context,new Model(BossGeometry.load("eldaroth_treant")),1.15f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return Pipican.id("textures/entity/boss/eldaroth_ancient_"+(state.phase==2?"awakened":"heartwood")+".png");}
    @Override public void extractRenderState(EldarothEntity entity,State state,float partial){super.extractRenderState(entity,state,partial);state.phase=entity.phase();state.action=entity.eldarothAction();state.remaining=Math.max(0,entity.abilityTicksRemaining()-partial);state.hurt=entity.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(EldarothEntity entity){return super.getBoundingBoxForCulling(entity).inflate(2.0);}
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,leftCrest,rightCrest,heart;
        Model(ModelPart root){super(root);var base=root.getChild("root");body=base.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=base.getChild("left_leg");rightLeg=base.getChild("right_leg");heart=body.getChild("chest_core");leftCrest=head.getChild("left_crest");rightCrest=head.getChild("right_crest");}
        @Override public void setupAnim(State s){
            super.setupAnim(s);float gait=Mth.clamp(s.walkAnimationSpeed,0,1),wave=Mth.sin(s.ageInTicks*0.055f);
            leftLeg.xRot=Mth.cos(s.walkAnimationPos*0.6f)*0.42f*gait;rightLeg.xRot=-leftLeg.xRot;
            leftArm.xRot=-0.12f+wave*0.035f;rightArm.xRot=-leftArm.xRot;head.yRot=Mth.clamp(s.yRot,-35,35)*Mth.DEG_TO_RAD;
            leftCrest.zRot=wave*0.04f;rightCrest.zRot=-leftCrest.zRot;body.yRot=wave*0.025f;
            float windup=1-Mth.clamp(s.remaining/80f,0,1);
            switch(s.action){case 1->{rightArm.xRot=-0.9f*windup;body.xRot=0.12f*windup;}
                case 2->{leftArm.xRot=-1.0f*windup;rightArm.xRot=-1.0f*windup;head.xRot=-0.22f*windup;}
                case 3->{leftArm.xRot=-0.8f*windup;rightArm.xRot=-0.8f*windup;heart.xRot=-0.3f*windup;}
                case 4->{leftArm.zRot=-0.75f*windup;rightArm.zRot=0.75f*windup;body.yRot+=0.12f*windup;}
                case 5->{body.y-=0.12f*windup;leftArm.xRot=-0.5f;rightArm.xRot=-0.5f;heart.xRot=wave*0.08f;}
                case 6->{leftArm.xRot=-0.95f;rightArm.xRot=-0.95f;body.y-=0.08f;}
                default->{}}
            if(s.phase==2){body.y+=Mth.sin(s.ageInTicks*0.12f)*0.07f;heart.xRot+=Mth.sin(s.ageInTicks*0.16f)*0.08f;}
            if(s.hurt>0){body.zRot=0.04f*Mth.sin(s.hurt*1.5f);head.zRot=body.zRot*1.5f;}
            if(s.deathTime>0)body.xRot=1.15f;
        }
    }
}
