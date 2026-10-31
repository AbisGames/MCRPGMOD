package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.LianhuaEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Flowing four-link tail and lotus-petal attack poses on original serpent geometry. */
public final class LianhuaRenderer extends MobRenderer<LianhuaEntity,LianhuaRenderer.State,LianhuaRenderer.Model>{
    public LianhuaRenderer(EntityRendererProvider.Context context){super(context,new Model(BossGeometry.load("lianhua_lotus_serpent")),1.05f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/lianhua_"+(s.phase==2?"river_spirit":"lotus_serpent")+".png");}
    @Override public void extractRenderState(LianhuaEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.lianhuaAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(LianhuaEntity e){return super.getBoundingBoxForCulling(e).inflate(2.2);}
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,t0,t1,t2,t3,leftCrest,rightCrest;
        Model(ModelPart root){super(root);var base=root.getChild("root");body=base.getChild("body");head=body.getChild("head");t0=body.getChild("tail_0");t1=t0.getChild("tail_1");t2=t1.getChild("tail_2");t3=t2.getChild("tail_3");leftCrest=head.getChild("left_crest");rightCrest=head.getChild("right_crest");}
        @Override public void setupAnim(State s){super.setupAnim(s);float wave=Mth.sin(s.ageInTicks*0.12f),gait=Mth.clamp(s.walkAnimationSpeed,0,1);head.yRot=Mth.clamp(s.yRot,-38,38)*Mth.DEG_TO_RAD;body.xRot=Mth.clamp(s.xRot,-18,18)*Mth.DEG_TO_RAD;
            t0.yRot=wave*0.16f+gait*0.08f;t1.yRot=Mth.sin(s.ageInTicks*0.12f-0.5f)*0.21f;t2.yRot=Mth.sin(s.ageInTicks*0.12f-1)*0.25f;t3.yRot=Mth.sin(s.ageInTicks*0.12f-1.5f)*0.29f;leftCrest.zRot=wave*0.06f;rightCrest.zRot=-leftCrest.zRot;float tell=1-Mth.clamp(s.remaining/65f,0,1);
            switch(s.action){case 1->{head.xRot=-0.22f*tell;body.xRot=0.08f*tell;}case 2->{head.xRot=-0.3f*tell;body.yRot=wave*0.07f*tell;}case 3->{body.xRot=-0.26f*tell;t0.xRot=0.18f*tell;t1.xRot=0.16f*tell;t2.xRot=0.12f*tell;}case 4->{head.yRot+=wave*0.08f;head.xRot=-0.12f*tell;}case 5->{body.y-=0.06f*tell;head.xRot=-0.12f;}case 6->{t0.yRot=0.2f;t1.yRot=-0.18f;leftCrest.zRot=0.1f;}default->{}}
            if(s.phase==2){leftCrest.zRot+=wave*0.07f;rightCrest.zRot-=wave*0.07f;t3.yRot+=wave*0.1f;}if(s.hurt>0){body.zRot=0.04f*Mth.sin(s.hurt*1.5f);head.zRot=body.zRot;}if(s.deathTime>0){body.xRot=0.8f;t0.xRot=0.25f;t1.xRot=0.25f;t2.xRot=0.25f;t3.xRot=0.25f;}}
    }
}
