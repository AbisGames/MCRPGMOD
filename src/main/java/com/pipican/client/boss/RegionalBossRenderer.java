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
    public RegionalBossRenderer(EntityRendererProvider.Context context){ super(context,new Model(BossGeometry.load("morvath_ashen_king")),1.1f);
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<State,Model>(this){
            @Override public void submit(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,int light,State state,float yaw,float pitch){
                var texture=Pipican.id("textures/entity/boss/morvath_"+(state.phase==2 ? "enraged" : "armored")+"_glow.png");
                collector.order(1).submitModel(getParentModel(),state,pose,net.minecraft.client.renderer.rendertype.RenderTypes.eyes(texture),light,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,state.outlineColor,null);
            }
        }); }
    public static final class State extends LivingEntityRenderState { public int phase=1,ability,remaining; public float hurtTime; }
    @Override public State createRenderState(){ return new State(); }
    @Override public Identifier getTextureLocation(State state){ return Pipican.id("textures/entity/boss/morvath_"+(state.phase==2 ? "enraged" : "armored")+".png"); }
    @Override public void extractRenderState(RegionalBossEntity entity,State state,float partial){ super.extractRenderState(entity,state,partial); state.phase=entity.phase(); state.ability=entity.ability().ordinal(); state.remaining=entity.abilityTicksRemaining(); state.hurtTime=entity.hurtTime-partial; }
    static final class Model extends EntityModel<State> {
        private final ModelPart head,leftArm,rightArm,leftLeg,rightLeg,eyes;
        Model(ModelPart modelRoot){
            super(modelRoot); var root=modelRoot.getChild("root");
            var body=root.getChild("body");
            head=body.getChild("head"); eyes=head.getChild("animated_eyes"); leftArm=body.getChild("left_arm"); rightArm=body.getChild("right_arm");
            leftLeg=root.getChild("left_leg"); rightLeg=root.getChild("right_leg");
        }
        @Override public void setupAnim(State state){
            super.setupAnim(state);
            eyes.visible=((int)state.ageInTicks%100)<95;
            head.yRot=state.yRot*Mth.DEG_TO_RAD; head.xRot=state.xRot*Mth.DEG_TO_RAD;
            leftLeg.xRot=Mth.cos(state.walkAnimationPos*0.6662f)*1.4f*state.walkAnimationSpeed;
            rightLeg.xRot=-leftLeg.xRot;
            leftArm.xRot=-leftLeg.xRot*0.65f; rightArm.xRot=-rightLeg.xRot*0.65f;
            leftArm.zRot=0.035f*Mth.sin(state.ageInTicks*0.09f); rightArm.zRot=-leftArm.zRot;
            var ability=com.pipican.boss.BossCombatState.Ability.values()[state.ability];
            float windup=Mth.clamp((ability.windup+ability.recovery-state.remaining)/(float)Math.max(1,ability.windup),0,1);
            switch(ability){
                case CLEAVE -> { rightArm.xRot=state.remaining>ability.recovery ? -2.5f*windup : 0.9f; rightArm.yRot=-0.4f; }
                case METEOR_RAIN -> { leftArm.xRot=-2.6f*windup; rightArm.xRot=-2.6f*windup; }
                case ERUPTION -> { leftArm.xRot=-1.6f*windup; rightArm.xRot=-1.6f*windup; head.xRot=0.45f; }
                case PHASE_CHANGE -> { leftArm.zRot=-0.7f*windup; rightArm.zRot=0.7f*windup; head.xRot=-0.5f; }
                case SUMMONING -> { leftArm.xRot=-1.2f*(1-windup); rightArm.xRot=-1.2f*(1-windup); head.xRot=0.7f*(1-windup); }
                default -> {}
            }
            if(state.hurtTime>0) head.zRot=0.15f*Mth.sin(state.hurtTime*1.5f);
            if(state.deathTime>0){ leftArm.xRot=-0.8f; rightArm.xRot=-0.8f; head.xRot=0.6f; }
        }
    }
}
