package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.ZahiraEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Saber-ready captain poses and fuse-lit corsair attack animation on preserved geometry. */
public final class ZahiraRenderer extends MobRenderer<ZahiraEntity,ZahiraRenderer.State,ZahiraRenderer.Model>{
    public ZahiraRenderer(EntityRendererProvider.Context context){super(context,new Model(BossGeometry.load("zahira_crimson_corsair")),0.95f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/zahira_"+(s.phase==2?"bloodtide":"corsair")+".png");}
    @Override public void extractRenderState(ZahiraEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.zahiraAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(ZahiraEntity e){return super.getBoundingBoxForCulling(e).inflate(1.7);}
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg;
        Model(ModelPart root){super(root);var base=root.getChild("root");body=base.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=base.getChild("left_leg");rightLeg=base.getChild("right_leg");}
        @Override public void setupAnim(State s){super.setupAnim(s);float wave=Mth.sin(s.ageInTicks*0.12f),gait=Mth.clamp(s.walkAnimationSpeed,0,1);head.yRot=Mth.clamp(s.yRot,-35,35)*Mth.DEG_TO_RAD;body.xRot=Mth.clamp(s.xRot,-14,14)*Mth.DEG_TO_RAD;leftArm.xRot=wave*0.045f-gait*0.42f;rightArm.xRot=-wave*0.045f+gait*0.42f;leftLeg.xRot=Mth.cos(s.ageInTicks*0.6662f)*1.05f*gait;rightLeg.xRot=Mth.cos(s.ageInTicks*0.6662f+(float)Math.PI)*1.05f*gait;float tell=1-Mth.clamp(s.remaining/65f,0,1);
            switch(s.action){case 1->{rightArm.xRot=-1.0f*tell;body.xRot=0.08f*tell;}case 2->{leftArm.xRot=-0.75f*tell;rightArm.xRot=-0.9f*tell;body.yRot=wave*0.08f*tell;}case 3->{leftArm.xRot=-0.18f*tell;rightArm.xRot=-0.12f*tell;body.xRot=0.04f*tell;}case 4->{leftArm.xRot=-0.6f*tell;rightArm.xRot=0.42f*tell;head.xRot=-0.12f*tell;}case 5->{body.y-=0.055f*tell;head.xRot=-0.12f;}case 6->{leftArm.xRot=-0.4f;rightArm.xRot=0.25f;}default->{}}
            if(s.phase==2){leftArm.zRot+=wave*0.06f;rightArm.zRot-=wave*0.06f;}if(s.hurt>0){body.zRot=0.04f*Mth.sin(s.hurt*1.5f);head.zRot=body.zRot;}if(s.deathTime>0){body.xRot=0.88f;head.xRot=0.32f;leftArm.zRot=-0.5f;rightArm.zRot=0.5f;}}
    }
}
