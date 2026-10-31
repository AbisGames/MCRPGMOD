package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.HrothgarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Axe-and-shield poses for the preserved drowned jarl model. */
public final class HrothgarRenderer extends MobRenderer<HrothgarEntity,HrothgarRenderer.State,HrothgarRenderer.Model>{
    public HrothgarRenderer(EntityRendererProvider.Context c){super(c,new Model(BossGeometry.load("hrothgar_drowned_jarl")),.9f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}@Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/hrothgar_"+(s.phase==2?"drowned":"jarl")+".png");}
    @Override public void extractRenderState(HrothgarEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.hrothgarAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(HrothgarEntity e){return super.getBoundingBoxForCulling(e).inflate(1.5);}
    static final class Model extends EntityModel<State>{private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,leftCrest,rightCrest;
        Model(ModelPart r){super(r);var root=r.getChild("root");body=root.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");leftCrest=head.getChild("left_crest");rightCrest=head.getChild("right_crest");}
        @Override public void setupAnim(State s){super.setupAnim(s);float g=Mth.clamp(s.walkAnimationSpeed,0,1),wave=Mth.sin(s.ageInTicks*.1f),tell=1-Mth.clamp(s.remaining/65f,0,1);head.yRot=Mth.clamp(s.yRot,-32,32)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(s.xRot,-12,12)*Mth.DEG_TO_RAD;leftArm.xRot=Mth.cos(s.ageInTicks*.67f)*.7f*g;rightArm.xRot=Mth.cos(s.ageInTicks*.67f+(float)Math.PI)*.7f*g;leftLeg.xRot=Mth.cos(s.ageInTicks*.67f+(float)Math.PI)*.8f*g;rightLeg.xRot=Mth.cos(s.ageInTicks*.67f)*.8f*g;
            switch(s.action){case 1->{rightArm.xRot=-1.05f*tell;body.xRot=.08f*tell;}case 2->{rightArm.xRot=-1.2f*tell;body.yRot=.08f*tell;}case 3->{leftArm.zRot=-.38f*tell;rightArm.zRot=.38f*tell;body.xRot=.05f*tell;}case 4->{leftArm.xRot=-.6f*tell;rightArm.xRot=-.6f*tell;body.xRot=-.07f*tell;}case 5->{body.y-=.06f;leftCrest.zRot=-.18f;rightCrest.zRot=.18f;}case 6->{leftArm.xRot=-.3f;rightArm.xRot=-.45f;}default->{}}
            if(s.phase==2){leftCrest.zRot+=wave*.04f;rightCrest.zRot-=wave*.04f;body.zRot=wave*.02f;}if(s.hurt>0)body.zRot=.04f*Mth.sin(s.hurt*1.4f);if(s.deathTime>0)body.xRot=.72f;}
    }
}
