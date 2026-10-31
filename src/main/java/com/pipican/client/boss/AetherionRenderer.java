package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.AetherionEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Explicit wingbeat, glide and dive poses for Aetherion's airborne navigation. */
public final class AetherionRenderer extends MobRenderer<AetherionEntity,AetherionRenderer.State,AetherionRenderer.Model>{
    public AetherionRenderer(EntityRendererProvider.Context context){super(context,new Model(BossGeometry.load("aetherion_sky_guardian")),1.1f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/aetherion_"+(s.phase==2?"radiant":"sky_guardian")+".png");}
    @Override public void extractRenderState(AetherionEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.aetherionAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(AetherionEntity e){return super.getBoundingBoxForCulling(e).inflate(2.4);}
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,leftWing,rightWing,halo;
        Model(ModelPart root){super(root);var base=root.getChild("root");body=base.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=base.getChild("left_leg");rightLeg=base.getChild("right_leg");leftWing=body.getChild("left_wing");rightWing=body.getChild("right_wing");halo=head.getChild("halo");}
        @Override public void setupAnim(State s){super.setupAnim(s);float flap=Mth.sin(s.ageInTicks*0.32f),gait=Mth.clamp(s.walkAnimationSpeed,0,1);
            leftWing.zRot=0.16f+flap*0.34f;rightWing.zRot=-leftWing.zRot;leftWing.xRot=-0.12f;rightWing.xRot=0.12f;
            body.xRot=Mth.clamp(s.xRot,-24,24)*Mth.DEG_TO_RAD;head.yRot=Mth.clamp(s.yRot,-32,32)*Mth.DEG_TO_RAD;
            leftLeg.xRot=Mth.cos(s.ageInTicks*0.18f)*0.08f+gait*0.1f;rightLeg.xRot=-leftLeg.xRot;leftArm.xRot=-0.12f;rightArm.xRot=-0.12f;
            float tell=1-Mth.clamp(s.remaining/80f,0,1);
            switch(s.action){case 1->{leftWing.zRot+=0.45f*tell;rightWing.zRot-=0.45f*tell;body.xRot=0.38f*tell;}
                case 2->{leftWing.zRot+=0.32f*tell;rightWing.zRot-=0.32f*tell;leftArm.xRot=-0.7f*tell;rightArm.xRot=-0.7f*tell;}
                case 3->{body.xRot=-0.9f*tell;leftWing.xRot=-0.8f*tell;rightWing.xRot=-0.8f*tell;}
                case 4->{leftWing.zRot=0.5f+flap*0.55f;rightWing.zRot=-leftWing.zRot;}
                case 5->{body.y-=0.08f*tell;halo.yRot+=0.1f*tell;}
                case 6->{leftWing.zRot=0.7f;rightWing.zRot=-0.7f;}
                default->{}}
            if(s.phase==2){halo.zRot+=Mth.sin(s.ageInTicks*0.12f)*0.08f;body.y-=0.04f;}
            if(s.hurt>0){body.zRot=0.045f*Mth.sin(s.hurt*1.5f);head.zRot=body.zRot*1.4f;}
            if(s.deathTime>0){body.xRot=1.1f;leftWing.zRot=-0.4f;rightWing.zRot=0.4f;}}
    }
}
