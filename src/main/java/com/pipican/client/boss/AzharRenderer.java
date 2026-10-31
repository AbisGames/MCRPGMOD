package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.AzharEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Mane, horn and gilded-plate action poses for the existing savanna beast geometry. */
public final class AzharRenderer extends MobRenderer<AzharEntity,AzharRenderer.State,AzharRenderer.Model>{
    public AzharRenderer(EntityRendererProvider.Context c){super(c,new Model(BossGeometry.load("azhar_golden_behemoth")),0.95f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}@Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/azhar_"+(s.phase==2?"sunheart":"golden")+".png");}
    @Override public void extractRenderState(AzharEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.azharAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(AzharEntity e){return super.getBoundingBoxForCulling(e).inflate(1.7);}
    static final class Model extends EntityModel<State>{private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,leftCrest,rightCrest;
        Model(ModelPart r){super(r);var root=r.getChild("root");body=root.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");leftCrest=head.getChild("left_crest");rightCrest=head.getChild("right_crest");}
        @Override public void setupAnim(State s){super.setupAnim(s);float gait=Mth.clamp(s.walkAnimationSpeed,0,1),wave=Mth.sin(s.ageInTicks*.12f),tell=1-Mth.clamp(s.remaining/65f,0,1);head.yRot=Mth.clamp(s.yRot,-32,32)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(s.xRot,-12,12)*Mth.DEG_TO_RAD;leftArm.xRot=Mth.cos(s.ageInTicks*.6662f)*.76f*gait;rightArm.xRot=Mth.cos(s.ageInTicks*.6662f+(float)Math.PI)*.76f*gait;leftLeg.xRot=Mth.cos(s.ageInTicks*.6662f+(float)Math.PI)*.9f*gait;rightLeg.xRot=Mth.cos(s.ageInTicks*.6662f)*.9f*gait;
            switch(s.action){case 1->{rightArm.xRot=-.95f*tell;body.xRot=.09f*tell;}case 2->{leftArm.zRot=-.18f*tell;rightArm.zRot=.18f*tell;head.xRot=-.08f*tell;}case 3->{body.xRot=-.16f*tell;leftArm.xRot=-.38f*tell;rightArm.xRot=-.38f*tell;}case 4->{body.y+=.055f*tell;head.xRot=.12f*tell;leftArm.xRot=-.52f*tell;rightArm.xRot=-.52f*tell;}case 5->{body.y-=.05f*tell;leftCrest.zRot=-.25f;rightCrest.zRot=.25f;}case 6->{leftArm.xRot=-.4f;rightArm.xRot=-.4f;}default->{}}
            if(s.phase==2){leftCrest.zRot+=wave*.065f;rightCrest.zRot-=wave*.065f;body.zRot=wave*.024f;}if(s.hurt>0)body.zRot=.05f*Mth.sin(s.hurt*1.5f);if(s.deathTime>0)body.xRot=.82f;}
    }
}
