package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.NhalkorrEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Articulated tide motion and attack poses for the preserved leviathan geometry. */
public final class NhalkorrRenderer extends MobRenderer<NhalkorrEntity,NhalkorrRenderer.State,NhalkorrRenderer.Model>{
    public NhalkorrRenderer(EntityRendererProvider.Context c){super(c,new Model(BossGeometry.load("nhalkorr_leviathan")),1.1f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/nhalkorr_"+(s.phase==2?"abyssal":"glacial")+".png");}
    @Override public void extractRenderState(NhalkorrEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.nhalkorrAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(NhalkorrEntity e){return super.getBoundingBoxForCulling(e).inflate(2.2);}
    static final class Model extends EntityModel<State>{private final ModelPart body,head,tail0,tail1,tail2,tail3;
        Model(ModelPart root){super(root);var b=root.getChild("root").getChild("body");body=b;head=b.getChild("head");tail0=b.getChild("tail_0");tail1=tail0.getChild("tail_1");tail2=tail1.getChild("tail_2");tail3=tail2.getChild("tail_3");}
        @Override public void setupAnim(State s){super.setupAnim(s);float swish=Mth.sin(s.ageInTicks*.16f),gait=Mth.clamp(s.walkAnimationSpeed,0,1),tell=1-Mth.clamp(s.remaining/65f,0,1);head.yRot=Mth.clamp(s.yRot,-34,34)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(s.xRot,-12,12)*Mth.DEG_TO_RAD;tail0.yRot=swish*.12f;tail1.yRot=swish*.2f;tail2.yRot=swish*.29f;tail3.yRot=swish*.37f;body.xRot=.025f*Mth.sin(s.ageInTicks*.09f);
            switch(s.action){case 1->{head.xRot-=.16f*tell;body.xRot=.08f*tell;}case 2->{head.xRot=-.22f*tell;tail0.yRot+=.12f*tell;}case 3->{for(int i=0;i<4;i++){}body.y+=.08f*tell;tail0.xRot=-.16f*tell;tail1.xRot=-.13f*tell;}case 4->{head.xRot=.12f*tell;body.xRot=-.11f*tell;}case 5->{body.y-=.08f;tail0.yRot=-.2f;tail1.yRot=.17f;}case 6->{body.y-=.06f;tail0.yRot=-.16f;}default->{}}
            if(s.phase==2){tail0.yRot+=swish*.035f;tail1.yRot-=swish*.05f;tail2.yRot+=swish*.065f;tail3.yRot-=swish*.08f;}if(gait>0)body.y+=Mth.sin(s.ageInTicks*.4f)*.025f*gait;if(s.hurt>0)body.zRot=.035f*Mth.sin(s.hurt*1.4f);if(s.deathTime>0)body.xRot=.7f;}
    }
}
