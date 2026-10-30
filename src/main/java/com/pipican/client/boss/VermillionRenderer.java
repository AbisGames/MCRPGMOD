package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.VermillionEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native wing, channel and blade poses for the preserved Blood Monarch geometry. */
public final class VermillionRenderer extends MobRenderer<VermillionEntity,VermillionRenderer.State,VermillionRenderer.Model>{
    public VermillionRenderer(EntityRendererProvider.Context context){super(context,new Model(BossGeometry.load("vermillion_blood_monarch")),1.05f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;boolean exposed;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/vermillion_"+(s.phase==2?"bloodmoon":"monarch")+".png");}
    @Override public void extractRenderState(VermillionEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.vermillionAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;s.exposed=e.isExposed();}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(VermillionEntity e){return super.getBoundingBoxForCulling(e).inflate(2.0);}
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,leftWing,rightWing;
        Model(ModelPart root){super(root);var base=root.getChild("root");body=base.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=base.getChild("left_leg");rightLeg=base.getChild("right_leg");leftWing=body.getChild("left_wing");rightWing=body.getChild("right_wing");}
        @Override public void setupAnim(State s){super.setupAnim(s);float gait=Mth.clamp(s.walkAnimationSpeed,0,1),pulse=Mth.sin(s.ageInTicks*0.11f);leftLeg.xRot=Mth.cos(s.walkAnimationPos*0.65f)*0.5f*gait;rightLeg.xRot=-leftLeg.xRot;head.yRot=Mth.clamp(s.yRot,-34,34)*Mth.DEG_TO_RAD;
            leftArm.xRot=-leftLeg.xRot*0.25f;rightArm.xRot=-rightLeg.xRot*0.25f;leftWing.zRot=0.1f+pulse*0.07f;rightWing.zRot=-leftWing.zRot;float tell=1-Mth.clamp(s.remaining/70f,0,1);
            switch(s.action){case 1->{rightArm.xRot=-0.8f*tell;leftWing.xRot=-0.1f*tell;}
                case 2->{leftArm.xRot=-0.75f*tell;rightArm.xRot=-0.75f*tell;leftWing.zRot+=0.25f*tell;rightWing.zRot-=0.25f*tell;}
                case 3->{body.yRot+=Mth.sin(s.ageInTicks*0.28f)*0.08f;leftArm.zRot=-0.35f*tell;rightArm.zRot=0.35f*tell;}
                case 4->{leftArm.xRot=-0.85f*tell;rightArm.xRot=-0.85f*tell;body.xRot=0.18f*tell;}
                case 5->{leftArm.xRot=-1.0f;rightArm.xRot=-1.0f;body.y-=0.08f*tell;}
                case 6->{leftWing.zRot=0.28f;rightWing.zRot=-0.28f;}
                default->{}}
            if(s.phase==2){body.yRot+=pulse*0.035f;head.xRot+=pulse*0.035f;}if(s.exposed){leftWing.zRot=-0.42f;rightWing.zRot=0.42f;}
            if(s.hurt>0){body.zRot=0.04f*Mth.sin(s.hurt*1.4f);head.zRot=body.zRot*1.4f;}if(s.deathTime>0){body.xRot=1.15f;leftWing.zRot=-0.5f;rightWing.zRot=0.5f;}}
    }
}
