package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.AureliosEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native mirror, pressure and treasury poses over Aurelios's source geometry. */
public final class AureliosRenderer extends MobRenderer<AureliosEntity,AureliosRenderer.State,AureliosRenderer.Model>{
    public AureliosRenderer(EntityRendererProvider.Context context){super(context,new Model(BossGeometry.load("aurelios_sunken_emperor")),1.0f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/aurelios_"+(s.phase==2?"tidal_emperor":"sunken_emperor")+".png");}
    @Override public void extractRenderState(AureliosEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.aureliosAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(AureliosEntity e){return super.getBoundingBoxForCulling(e).inflate(1.8);}
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,leftCrest,rightCrest;
        Model(ModelPart root){super(root);var base=root.getChild("root");body=base.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=base.getChild("left_leg");rightLeg=base.getChild("right_leg");leftCrest=head.getChild("left_crest");rightCrest=head.getChild("right_crest");}
        @Override public void setupAnim(State s){super.setupAnim(s);float gait=Mth.clamp(s.walkAnimationSpeed,0,1),wave=Mth.sin(s.ageInTicks*0.1f);leftLeg.xRot=Mth.cos(s.walkAnimationPos*0.7f)*0.4f*gait;rightLeg.xRot=-leftLeg.xRot;head.yRot=Mth.clamp(s.yRot,-35,35)*Mth.DEG_TO_RAD;leftArm.xRot=-0.1f+wave*0.03f;rightArm.xRot=-leftArm.xRot;leftCrest.zRot=wave*0.04f;rightCrest.zRot=-leftCrest.zRot;float tell=1-Mth.clamp(s.remaining/65f,0,1);
            switch(s.action){case 1->{rightArm.xRot=-0.9f*tell;body.xRot=0.06f*tell;}case 2->{leftArm.xRot=-0.65f*tell;rightArm.xRot=-0.65f*tell;leftCrest.yRot=0.18f*tell;rightCrest.yRot=-0.18f*tell;}case 3->{leftArm.zRot=-0.15f*tell;rightArm.zRot=0.15f*tell;body.yRot=wave*0.06f*tell;}case 4->{leftArm.xRot=-0.75f*tell;rightArm.xRot=-0.75f*tell;head.xRot=-0.1f*tell;}case 5->{body.y-=0.07f*tell;leftArm.xRot=-0.8f;rightArm.xRot=-0.8f;}case 6->{leftArm.xRot=-0.3f;rightArm.xRot=-0.5f;}default->{}}
            if(s.phase==2){body.yRot+=wave*0.025f;leftCrest.zRot+=0.04f;rightCrest.zRot-=0.04f;}if(s.hurt>0){body.zRot=0.04f*Mth.sin(s.hurt*1.5f);head.zRot=body.zRot;}if(s.deathTime>0){body.xRot=0.95f;leftArm.zRot=-0.2f;rightArm.zRot=0.2f;}}
    }
}
