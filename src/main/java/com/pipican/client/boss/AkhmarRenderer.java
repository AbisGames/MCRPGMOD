package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.AkhmarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Crown, mantle, and sand-swept attack poses on Akhmar's preserved bone layout. */
public final class AkhmarRenderer extends MobRenderer<AkhmarEntity,AkhmarRenderer.State,AkhmarRenderer.Model>{
    public AkhmarRenderer(EntityRendererProvider.Context context){super(context,new Model(BossGeometry.load("akhmar_sand_emperor")),1.0f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/akhmar_"+(s.phase==2?"sunfire":"sand_emperor")+".png");}
    @Override public void extractRenderState(AkhmarEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.akhmarAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(AkhmarEntity e){return super.getBoundingBoxForCulling(e).inflate(1.8);}
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,leftCrest,rightCrest;
        Model(ModelPart root){super(root);var base=root.getChild("root");body=base.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=base.getChild("left_leg");rightLeg=base.getChild("right_leg");leftCrest=head.getChild("left_crest");rightCrest=head.getChild("right_crest");}
        @Override public void setupAnim(State s){super.setupAnim(s);float wave=Mth.sin(s.ageInTicks*0.1f),gait=Mth.clamp(s.walkAnimationSpeed,0,1);head.yRot=Mth.clamp(s.yRot,-35,35)*Mth.DEG_TO_RAD;body.xRot=Mth.clamp(s.xRot,-15,15)*Mth.DEG_TO_RAD;leftArm.xRot=wave*0.035f-gait*0.45f;rightArm.xRot=-wave*0.035f+gait*0.45f;leftLeg.xRot=Mth.cos(s.ageInTicks*0.6662f)*1.2f*gait;rightLeg.xRot=Mth.cos(s.ageInTicks*0.6662f+(float)Math.PI)*1.2f*gait;leftCrest.zRot=wave*0.035f;rightCrest.zRot=-leftCrest.zRot;float tell=1-Mth.clamp(s.remaining/60f,0,1);
            switch(s.action){case 1->{rightArm.xRot=-1.1f*tell;body.xRot=0.08f*tell;}case 2->{leftArm.xRot=-0.75f*tell;rightArm.xRot=-0.75f*tell;body.yRot=wave*0.08f*tell;}case 3->{leftArm.xRot=-0.35f*tell;rightArm.xRot=-0.35f*tell;head.xRot=-0.16f*tell;}case 4->{body.xRot=0.12f*tell;leftLeg.xRot=-0.5f*tell;rightLeg.xRot=-0.5f*tell;}case 5->{body.y-=0.05f*tell;head.xRot=-0.16f;}case 6->{leftArm.xRot=-0.45f;rightArm.xRot=0.2f;}default->{}}
            if(s.phase==2){leftCrest.zRot+=wave*0.06f;rightCrest.zRot-=wave*0.06f;}if(s.hurt>0){body.zRot=0.035f*Mth.sin(s.hurt*1.5f);head.zRot=body.zRot;}if(s.deathTime>0){body.xRot=1.1f;head.xRot=0.3f;leftArm.zRot=-0.6f;rightArm.zRot=0.6f;}}
    }
}
