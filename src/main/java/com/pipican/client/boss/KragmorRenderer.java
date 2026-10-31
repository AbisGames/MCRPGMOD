package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.KragmorEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native heavy-limb poses and phase palettes for the canyon warlord. */
public final class KragmorRenderer extends MobRenderer<KragmorEntity,KragmorRenderer.State,KragmorRenderer.Model>{
    public KragmorRenderer(EntityRendererProvider.Context context){super(context,new Model(BossGeometry.load("kragmor_canyon_warlord")),1.25f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/kragmor_"+(s.phase==2?"awakened":"canyon_iron")+".png");}
    @Override public void extractRenderState(KragmorEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.kragmorAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(KragmorEntity e){return super.getBoundingBoxForCulling(e).inflate(2.2);}
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,core,leftCrest,rightCrest;
        Model(ModelPart root){super(root);var base=root.getChild("root");body=base.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=base.getChild("left_leg");rightLeg=base.getChild("right_leg");core=body.getChild("chest_core");leftCrest=head.getChild("left_crest");rightCrest=head.getChild("right_crest");}
        @Override public void setupAnim(State s){super.setupAnim(s);float gait=Mth.clamp(s.walkAnimationSpeed,0,1),sway=Mth.sin(s.ageInTicks*0.055f);
            leftLeg.xRot=Mth.cos(s.walkAnimationPos*0.58f)*0.46f*gait;rightLeg.xRot=-leftLeg.xRot;leftArm.xRot=-leftLeg.xRot*0.4f;rightArm.xRot=-rightLeg.xRot*0.4f;
            head.yRot=Mth.clamp(s.yRot,-34,34)*Mth.DEG_TO_RAD;body.yRot=sway*0.02f;leftCrest.zRot=sway*0.03f;rightCrest.zRot=-leftCrest.zRot;
            float tell=1-Mth.clamp(s.remaining/80f,0,1);
            switch(s.action){case 1->{rightArm.xRot=-0.95f*tell;body.xRot=0.08f*tell;}
                case 2->{leftArm.xRot=-0.8f*tell;rightArm.xRot=-0.8f*tell;body.y+=0.04f*tell;}
                case 3->{leftArm.zRot=-0.38f*tell;rightArm.zRot=0.38f*tell;body.yRot+=0.1f*tell;}
                case 4->{leftArm.xRot=-1.1f*tell;rightArm.xRot=-1.1f*tell;body.xRot=0.18f*tell;}
                case 5->{leftArm.xRot=-0.5f;rightArm.xRot=-0.5f;body.y-=0.1f*tell;core.xRot=sway*0.08f;}
                case 6->{leftArm.xRot=-0.7f;rightArm.xRot=-0.7f;}
                default->{}}
            if(s.phase==2){core.xRot+=Mth.sin(s.ageInTicks*0.14f)*0.06f;body.y+=Mth.sin(s.ageInTicks*0.12f)*0.05f;}
            if(s.hurt>0){body.zRot=0.04f*Mth.sin(s.hurt*1.5f);head.zRot=body.zRot*1.5f;}
            if(s.deathTime>0){body.xRot=1.2f;leftArm.xRot=-0.35f;rightArm.xRot=-0.35f;}}
    }
}
