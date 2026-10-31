package com.pipican.client.boss;
import com.pipican.Pipican;
import com.pipican.boss.TengriEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native steppe charge, banner call and phase-two crest poses for Tengri's preserved geometry. */
public final class TengriRenderer extends MobRenderer<TengriEntity,TengriRenderer.State,TengriRenderer.Model>{
 public TengriRenderer(EntityRendererProvider.Context c){super(c,new Model(BossGeometry.load("tengri_champion")),.9f);}
 public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
 @Override public State createRenderState(){return new State();}@Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/tengri_"+(s.phase==2?"bannerlord":"champion")+".png");}
 @Override public void extractRenderState(TengriEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.tengriAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
 static final class Model extends EntityModel<State>{private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,leftCrest,rightCrest;
  Model(ModelPart r){super(r);var root=r.getChild("root");body=root.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");leftCrest=head.getChild("left_crest");rightCrest=head.getChild("right_crest");}
  @Override public void setupAnim(State s){super.setupAnim(s);float g=Mth.clamp(s.walkAnimationSpeed,0,1),sway=Mth.sin(s.ageInTicks*.17f),tell=1-Mth.clamp(s.remaining/60f,0,1);head.yRot=Mth.clamp(s.yRot,-32,32)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(s.xRot,-12,12)*Mth.DEG_TO_RAD;leftArm.xRot=Mth.cos(s.ageInTicks*.64f)*.62f*g;rightArm.xRot=Mth.cos(s.ageInTicks*.64f+(float)Math.PI)*.62f*g;leftLeg.xRot=Mth.cos(s.ageInTicks*.64f+(float)Math.PI)*.7f*g;rightLeg.xRot=Mth.cos(s.ageInTicks*.64f)*.7f*g;leftCrest.zRot=-.12f+sway*.045f;rightCrest.zRot=.12f-sway*.045f;
   switch(s.action){case 1->{rightArm.xRot=-.95f*tell;body.xRot=.08f*tell;}case 2->{body.xRot=-.18f*tell;leftArm.xRot=.5f*tell;rightArm.xRot=-.65f*tell;}case 3->{leftArm.xRot=-.9f*tell;rightArm.xRot=-.9f*tell;body.y-=.035f*tell;}case 4->{leftArm.zRot=-.45f*tell;rightArm.zRot=.45f*tell;body.xRot=.06f*tell;}case 5->{body.xRot=-.12f;leftCrest.zRot=-.35f;rightCrest.zRot=.35f;}case 6->{leftArm.xRot=-.35f;rightArm.xRot=-.35f;}default->{}}
   if(s.phase==2){leftCrest.zRot-=.08f;rightCrest.zRot+=.08f;}if(s.hurt>0)body.zRot=.035f*Mth.sin(s.hurt*1.5f);if(s.deathTime>0)body.xRot=.7f;}
 }
}
