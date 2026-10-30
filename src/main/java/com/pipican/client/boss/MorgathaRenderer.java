package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.MorgathaEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native witch, bubble-casting, veil and charged-decoy poses over source geometry. */
public final class MorgathaRenderer extends MobRenderer<MorgathaEntity,MorgathaRenderer.State,MorgathaRenderer.Model>{
    public MorgathaRenderer(EntityRendererProvider.Context context){super(context,new Model(BossGeometry.load("morgatha_swamp_queen")),0.9f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/morgatha_"+(s.phase==2?"blighted":"swamp")+".png");}
    @Override public void extractRenderState(MorgathaEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.morgathaAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(MorgathaEntity e){return super.getBoundingBoxForCulling(e).inflate(1.8);}
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg;
        Model(ModelPart root){super(root);var base=root.getChild("root");body=base.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=base.getChild("left_leg");rightLeg=base.getChild("right_leg");}
        @Override public void setupAnim(State s){super.setupAnim(s);float gait=Mth.clamp(s.walkAnimationSpeed,0,1),pulse=Mth.sin(s.ageInTicks*0.12f);leftLeg.xRot=Mth.cos(s.walkAnimationPos*0.65f)*0.45f*gait;rightLeg.xRot=-leftLeg.xRot;head.yRot=Mth.clamp(s.yRot,-36,36)*Mth.DEG_TO_RAD;leftArm.xRot=-0.12f+Mth.sin(s.ageInTicks*0.08f)*0.035f;rightArm.xRot=leftArm.xRot;float tell=1-Mth.clamp(s.remaining/65f,0,1);
            switch(s.action){case 1->{rightArm.xRot=-0.8f*tell;body.xRot=-0.06f*tell;}case 2->{leftArm.xRot=-0.85f*tell;rightArm.xRot=-0.85f*tell;body.y+=0.045f*pulse*tell;}case 3->{rightArm.xRot=-1.15f*tell;leftArm.zRot=0.16f*tell;head.xRot=-0.12f*tell;}case 4->{leftArm.xRot=-0.9f*tell;rightArm.xRot=-0.9f*tell;body.yRot=0.08f*pulse*tell;}case 5->{leftArm.xRot=-1.0f;rightArm.xRot=-1.0f;body.y-=0.07f*tell;}case 6->{leftArm.xRot=-0.48f;rightArm.xRot=-0.68f;body.xRot=-0.05f;}default->{}}
            if(s.phase==2){body.yRot+=pulse*0.03f;head.xRot+=pulse*0.025f;}if(s.hurt>0){body.zRot=0.04f*Mth.sin(s.hurt*1.5f);head.zRot=body.zRot;}if(s.deathTime>0){body.xRot=0.85f;leftArm.zRot=-0.25f;rightArm.zRot=0.25f;}}
    }
}
