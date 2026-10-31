package com.pipican.client.boss;
import com.pipican.Pipican;
import com.pipican.boss.ThalassorEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Pressure pulse and crest poses for the preserved Thalassor model. */
public final class ThalassorRenderer extends MobRenderer<ThalassorEntity,ThalassorRenderer.State,ThalassorRenderer.Model>{
 public ThalassorRenderer(EntityRendererProvider.Context c){super(c,new Model(BossGeometry.load("thalassor_deep_tyrant")),1f);}
 public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
 @Override public State createRenderState(){return new State();}@Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/thalassor_"+(s.phase==2?"abyssal":"deep_tyrant")+".png");}
 @Override public void extractRenderState(ThalassorEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.thalassorAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
 @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(ThalassorEntity e){return super.getBoundingBoxForCulling(e).inflate(1.5);}
 static final class Model extends EntityModel<State>{private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,core,leftCrest,rightCrest;
  Model(ModelPart r){super(r);var root=r.getChild("root");body=root.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");core=body.getChild("chest_core");leftCrest=head.getChild("left_crest");rightCrest=head.getChild("right_crest");}
  @Override public void setupAnim(State s){super.setupAnim(s);float g=Mth.clamp(s.walkAnimationSpeed,0,1),pulse=Mth.sin(s.ageInTicks*.18f),tell=1-Mth.clamp(s.remaining/60f,0,1);head.yRot=Mth.clamp(s.yRot,-32,32)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(s.xRot,-12,12)*Mth.DEG_TO_RAD;leftArm.xRot=Mth.cos(s.ageInTicks*.58f)*.6f*g;rightArm.xRot=Mth.cos(s.ageInTicks*.58f+(float)Math.PI)*.6f*g;leftLeg.xRot=Mth.cos(s.ageInTicks*.58f+(float)Math.PI)*.65f*g;rightLeg.xRot=Mth.cos(s.ageInTicks*.58f)*.65f*g;core.xRot=pulse*.04f;leftCrest.zRot=-.12f+pulse*.04f;rightCrest.zRot=.12f-pulse*.04f;
   switch(s.action){case 1->{rightArm.xRot=-.82f*tell;}case 2->{rightArm.xRot=-1.0f*tell;leftArm.xRot=.3f*tell;}case 3->{body.xRot=.08f*tell;leftCrest.zRot=-.38f*tell;rightCrest.zRot=.38f*tell;core.xRot=-.16f*tell;}case 4->{leftArm.zRot=-.35f*tell;rightArm.zRot=.35f*tell;core.xRot=.14f*tell;}case 5->{body.xRot=.12f;leftCrest.zRot=-.42f;rightCrest.zRot=.42f;}case 6->{leftArm.xRot=-.3f;rightArm.xRot=-.3f;core.xRot=-.12f;}default->{}}
   if(s.phase==2)core.xRot+=pulse*.09f;if(s.hurt>0)body.zRot=.04f*Mth.sin(s.hurt*1.4f);if(s.deathTime>0)body.xRot=.7f;}
 }
}
