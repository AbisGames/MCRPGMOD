package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.RegionalBossEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class RegionalBossRenderer extends MobRenderer<RegionalBossEntity,RegionalBossRenderer.State,RegionalBossRenderer.Model> {
    public RegionalBossRenderer(EntityRendererProvider.Context context){ super(context,new Model(BossGeometry.load("morvath_ashen_king")),1.1f); }
    public static final class State extends LivingEntityRenderState { public int phase=1; }
    @Override public State createRenderState(){ return new State(); }
    @Override public Identifier getTextureLocation(State state){ return Pipican.id("textures/entity/boss/morvath_"+(state.phase==2 ? "enraged" : "armored")+".png"); }
    @Override public void extractRenderState(RegionalBossEntity entity,State state,float partial){ super.extractRenderState(entity,state,partial); state.phase=entity.phase(); }
    static final class Model extends EntityModel<State> {
        private final ModelPart head,leftArm,rightArm,leftLeg,rightLeg;
        Model(ModelPart modelRoot){
            super(modelRoot); var root=modelRoot.getChild("root");
            var body=root.getChild("body");
            head=body.getChild("head"); leftArm=body.getChild("left_arm"); rightArm=body.getChild("right_arm");
            leftLeg=root.getChild("left_leg"); rightLeg=root.getChild("right_leg");
        }
        @Override public void setupAnim(State state){
            super.setupAnim(state);
            head.yRot=state.yRot*Mth.DEG_TO_RAD; head.xRot=state.xRot*Mth.DEG_TO_RAD;
            leftLeg.xRot=Mth.cos(state.walkAnimationPos*0.6662f)*1.4f*state.walkAnimationSpeed;
            rightLeg.xRot=-leftLeg.xRot;
            leftArm.xRot=-leftLeg.xRot*0.65f; rightArm.xRot=-rightLeg.xRot*0.65f;
            leftArm.zRot=0.035f*Mth.sin(state.ageInTicks*0.09f); rightArm.zRot=-leftArm.zRot;
        }
    }
}
