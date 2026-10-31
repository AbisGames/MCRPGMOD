package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.NetharisEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Crown crests, gill motion and readable river-current attack poses on retained geometry. */
public final class NetharisRenderer extends MobRenderer<NetharisEntity,NetharisRenderer.State,NetharisRenderer.Model>{
    public NetharisRenderer(EntityRendererProvider.Context context){super(context,new Model(BossGeometry.load("netharis_river_guardian")),1.05f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/netharis_"+(s.phase==2?"flooded":"river_guardian")+".png");}
    @Override public void extractRenderState(NetharisEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.netharisAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(NetharisEntity e){return super.getBoundingBoxForCulling(e).inflate(2.1);}
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,muzzle,leftArm,rightArm,leftLeg,rightLeg,leftEar,rightEar,leftCrest,rightCrest;
        Model(ModelPart root){super(root);var base=root.getChild("root");body=base.getChild("body");head=body.getChild("head");muzzle=head.getChild("muzzle");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=base.getChild("left_leg");rightLeg=base.getChild("right_leg");leftEar=head.getChild("left_ear");rightEar=head.getChild("right_ear");leftCrest=head.getChild("left_crest");rightCrest=head.getChild("right_crest");}
        @Override public void setupAnim(State s){super.setupAnim(s);float wave=Mth.sin(s.ageInTicks*0.115f),gait=Mth.clamp(s.walkAnimationSpeed,0,1);head.yRot=Mth.clamp(s.yRot,-36,36)*Mth.DEG_TO_RAD;body.xRot=Mth.clamp(s.xRot,-14,14)*Mth.DEG_TO_RAD;leftArm.xRot=wave*0.04f-gait*0.42f;rightArm.xRot=-wave*0.04f+gait*0.42f;leftLeg.xRot=Mth.cos(s.ageInTicks*0.6662f)*1.05f*gait;rightLeg.xRot=Mth.cos(s.ageInTicks*0.6662f+(float)Math.PI)*1.05f*gait;leftEar.zRot=wave*0.045f;rightEar.zRot=-leftEar.zRot;leftCrest.zRot=wave*0.03f;rightCrest.zRot=-leftCrest.zRot;float tell=1-Mth.clamp(s.remaining/65f,0,1);
            switch(s.action){case 1->{head.xRot=-0.25f*tell;body.xRot=0.07f*tell;}case 2->{leftArm.xRot=-0.8f*tell;rightArm.xRot=-0.8f*tell;body.yRot=wave*0.075f*tell;}case 3->{rightArm.xRot=-0.95f*tell;head.xRot=-0.12f*tell;}case 4->{leftArm.xRot=-0.28f*tell;rightArm.xRot=-0.28f*tell;head.xRot=-0.18f*tell;muzzle.xRot=-0.12f*tell;}case 5->{body.y-=0.055f*tell;head.xRot=-0.15f;}case 6->{leftArm.xRot=-0.4f;rightArm.xRot=0.28f;}default->{}}
            if(s.phase==2){leftCrest.zRot+=wave*0.075f;rightCrest.zRot-=wave*0.075f;leftEar.xRot=wave*0.04f;rightEar.xRot=-leftEar.xRot;}if(s.hurt>0){body.zRot=0.038f*Mth.sin(s.hurt*1.5f);head.zRot=body.zRot;}if(s.deathTime>0){body.xRot=0.95f;head.xRot=0.3f;leftArm.zRot=-0.55f;rightArm.zRot=0.55f;}}
    }
}
