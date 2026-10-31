package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.CorallithEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Shell-heavy reef giant animation preserving its authored geometry and layered coral silhouette. */
public final class CorallithRenderer extends MobRenderer<CorallithEntity,CorallithRenderer.State,CorallithRenderer.Model>{
    public CorallithRenderer(EntityRendererProvider.Context c){super(c,new Model(BossGeometry.load("corallith_reef_colossus")),1.1f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/corallith_"+(s.phase==2?"undertow":"reef")+".png");}
    @Override public void extractRenderState(CorallithEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.corallithAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(CorallithEntity e){return super.getBoundingBoxForCulling(e).inflate(1.6);}
    static final class Model extends EntityModel<State>{private final ModelPart body,head,leftArm,rightArm,leftLeg,rightLeg,core,leftShell,rightShell;
        Model(ModelPart r){super(r);var root=r.getChild("root");body=root.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");core=body.getChild("chest_core");leftShell=body.getChild("left_shoulder");rightShell=body.getChild("right_shoulder");}
        @Override public void setupAnim(State s){super.setupAnim(s);float gait=Mth.clamp(s.walkAnimationSpeed,0,1),wave=Mth.sin(s.ageInTicks*.09f),tell=1-Mth.clamp(s.remaining/65f,0,1);head.yRot=Mth.clamp(s.yRot,-30,30)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(s.xRot,-10,10)*Mth.DEG_TO_RAD;leftArm.xRot=-.12f+Mth.cos(s.ageInTicks*.62f)*.25f*gait;rightArm.xRot=-.12f+Mth.cos(s.ageInTicks*.62f+(float)Math.PI)*.25f*gait;leftLeg.xRot=Mth.cos(s.ageInTicks*.56f+(float)Math.PI)*.45f*gait;rightLeg.xRot=Mth.cos(s.ageInTicks*.56f)*.45f*gait;
            switch(s.action){case 1->{rightArm.xRot=-.52f*tell;body.xRot=.08f*tell;}case 2->{leftArm.xRot=-.7f*tell;rightArm.xRot=-.7f*tell;body.xRot=-.04f*tell;}case 3->{body.xRot=.11f*tell;head.xRot=-.09f*tell;core.y+=wave*.35f*tell;}case 4->{leftArm.zRot=-.35f*tell;rightArm.zRot=.35f*tell;body.y-=.08f*tell;}case 5->{body.y-=.06f*tell;leftShell.zRot=-.15f;rightShell.zRot=.15f;}case 6->{leftArm.xRot=-.42f;rightArm.xRot=-.42f;}default->{}}
            if(s.phase==2){leftShell.zRot+=wave*.035f;rightShell.zRot-=wave*.035f;core.xRot=wave*.07f;}if(s.hurt>0)body.zRot=.025f*Mth.sin(s.hurt*1.3f);if(s.deathTime>0)body.xRot=.55f;}
    }
}
