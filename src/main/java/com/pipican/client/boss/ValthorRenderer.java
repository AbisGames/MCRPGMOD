package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.ValthorEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Broad storm-wing poses for the original Stormbringer geometry. */
public final class ValthorRenderer extends MobRenderer<ValthorEntity,ValthorRenderer.State,ValthorRenderer.Model>{
    public ValthorRenderer(EntityRendererProvider.Context c){super(c,new Model(BossGeometry.load("valthor_stormbringer")),1f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}@Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/valthor_"+(s.phase==2?"tempest":"stormbringer")+".png");}
    @Override public void extractRenderState(ValthorEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.valthorAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(ValthorEntity e){return super.getBoundingBoxForCulling(e).inflate(2);}
    static final class Model extends EntityModel<State>{private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,leftWing,rightWing;
        Model(ModelPart r){super(r);var root=r.getChild("root");body=root.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");leftWing=body.getChild("left_wing");rightWing=body.getChild("right_wing");}
        @Override public void setupAnim(State s){super.setupAnim(s);float g=Mth.clamp(s.walkAnimationSpeed,0,1),flap=Mth.sin(s.ageInTicks*.28f),tell=1-Mth.clamp(s.remaining/65f,0,1);head.yRot=Mth.clamp(s.yRot,-32,32)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(s.xRot,-12,12)*Mth.DEG_TO_RAD;leftArm.xRot=Mth.cos(s.ageInTicks*.67f)*.65f*g;rightArm.xRot=Mth.cos(s.ageInTicks*.67f+(float)Math.PI)*.65f*g;leftLeg.xRot=Mth.cos(s.ageInTicks*.67f+(float)Math.PI)*.7f*g;rightLeg.xRot=Mth.cos(s.ageInTicks*.67f)*.7f*g;leftWing.zRot=-.18f+flap*.12f;rightWing.zRot=.18f-flap*.12f;
            switch(s.action){case 1->{rightArm.xRot=-.85f*tell;body.xRot=.06f*tell;}case 2->{leftWing.zRot=-.5f*tell;rightWing.zRot=.5f*tell;}case 3->{leftWing.zRot=-.8f*tell;rightWing.zRot=.8f*tell;body.y-=.04f*tell;}case 4->{leftWing.xRot=-.25f*tell;rightWing.xRot=-.25f*tell;body.xRot=.08f*tell;}case 5->{leftWing.zRot=-.7f;rightWing.zRot=.7f;body.y-=.06f;}case 6->{leftWing.zRot=-.4f;rightWing.zRot=.4f;}default->{}}
            if(s.phase==2){leftWing.xRot+=flap*.04f;rightWing.xRot-=flap*.04f;}if(s.hurt>0)body.zRot=.035f*Mth.sin(s.hurt*1.5f);if(s.deathTime>0)body.xRot=.7f;}
    }
}
