package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.XaltekEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Emerald devourer locomotion, snare, tongue and pounce poses on its source geometry. */
public final class XaltekRenderer extends MobRenderer<XaltekEntity,XaltekRenderer.State,XaltekRenderer.Model>{
    public XaltekRenderer(EntityRendererProvider.Context c){super(c,new Model(BossGeometry.load("xaltek_emerald_devourer")),0.85f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/xaltek_"+(s.phase==2?"overgrowth":"devourer")+".png");}
    @Override public void extractRenderState(XaltekEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.xaltekAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(XaltekEntity e){return super.getBoundingBoxForCulling(e).inflate(1.4);}
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,leftCrest,rightCrest;
        Model(ModelPart root){super(root);var base=root.getChild("root");body=base.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=base.getChild("left_leg");rightLeg=base.getChild("right_leg");leftCrest=head.getChild("left_crest");rightCrest=head.getChild("right_crest");}
        @Override public void setupAnim(State s){super.setupAnim(s);float gait=Mth.clamp(s.walkAnimationSpeed,0,1),wave=Mth.sin(s.ageInTicks*.13f),tell=1-Mth.clamp(s.remaining/65f,0,1);head.yRot=Mth.clamp(s.yRot,-32,32)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(s.xRot,-12,12)*Mth.DEG_TO_RAD;leftArm.xRot=Mth.cos(s.ageInTicks*.6662f)*.62f*gait;rightArm.xRot=Mth.cos(s.ageInTicks*.6662f+(float)Math.PI)*.62f*gait;leftLeg.xRot=Mth.cos(s.ageInTicks*.6662f+(float)Math.PI)*.9f*gait;rightLeg.xRot=Mth.cos(s.ageInTicks*.6662f)*.9f*gait;
            switch(s.action){case 1->{rightArm.xRot=-.88f*tell;head.xRot=-.12f*tell;}case 2->{leftArm.xRot=-.72f*tell;rightArm.xRot=-.72f*tell;body.xRot=.08f*tell;}case 3->{head.xRot=.22f*tell;body.xRot=-.1f*tell;rightArm.xRot=-.38f*tell;}case 4->{body.xRot=-.4f*tell;leftArm.xRot=-1.05f*tell;rightArm.xRot=-1.05f*tell;head.xRot=.18f*tell;}case 5->{body.y-=.04f*tell;leftCrest.zRot=-.3f;rightCrest.zRot=.3f;}case 6->{leftArm.xRot=-.35f;rightArm.xRot=-.35f;}default->{}}
            if(s.phase==2){leftCrest.zRot+=wave*.09f;rightCrest.zRot-=wave*.09f;body.zRot=wave*.025f;}if(s.hurt>0)body.zRot=.04f*Mth.sin(s.hurt*1.5f);if(s.deathTime>0){body.xRot=.95f;head.xRot=.28f;}}
    }
}
