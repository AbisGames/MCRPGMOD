package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.LongweiEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Segment-by-segment jade dragon tail motion and readable ability poses. */
public final class LongweiRenderer extends MobRenderer<LongweiEntity,LongweiRenderer.State,LongweiRenderer.Model>{
    public LongweiRenderer(EntityRendererProvider.Context context){super(context,new Model(BossGeometry.load("longwei_jade_dragon")),1.2f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/longwei_"+(s.phase==2?"ascendant":"jade")+".png");}
    @Override public void extractRenderState(LongweiEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.longweiAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(LongweiEntity e){return super.getBoundingBoxForCulling(e).inflate(2.5);}
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,tail0,tail1,tail2,tail3,leftCrest,rightCrest;
        Model(ModelPart root){super(root);var base=root.getChild("root");body=base.getChild("body");head=body.getChild("head");tail0=body.getChild("tail_0");tail1=tail0.getChild("tail_1");tail2=tail1.getChild("tail_2");tail3=tail2.getChild("tail_3");leftCrest=head.getChild("left_crest");rightCrest=head.getChild("right_crest");}
        @Override public void setupAnim(State s){super.setupAnim(s);float gait=Mth.clamp(s.walkAnimationSpeed,0,1),wave=Mth.sin(s.ageInTicks*0.12f);head.yRot=Mth.clamp(s.yRot,-38,38)*Mth.DEG_TO_RAD;body.xRot=Mth.clamp(s.xRot,-18,18)*Mth.DEG_TO_RAD;
            tail0.yRot=wave*0.15f+gait*0.08f;tail1.yRot=Mth.sin(s.ageInTicks*0.12f-0.55f)*0.2f;tail2.yRot=Mth.sin(s.ageInTicks*0.12f-1.1f)*0.24f;tail3.yRot=Mth.sin(s.ageInTicks*0.12f-1.65f)*0.28f;
            leftCrest.zRot=wave*0.035f;rightCrest.zRot=-leftCrest.zRot;float tell=1-Mth.clamp(s.remaining/60f,0,1);
            switch(s.action){case 1->{head.xRot=-0.24f*tell;body.xRot+=0.08f*tell;}
                case 2->{head.xRot=-0.32f*tell;body.xRot=0.12f*tell;tail0.yRot+=0.1f*tell;}
                case 3->{body.xRot=-0.55f*tell;tail0.xRot=0.22f*tell;tail1.xRot=0.18f*tell;}
                case 4->{head.yRot+=wave*0.08f;head.xRot=-0.12f*tell;}
                case 5->{body.y-=0.06f*tell;head.xRot=-0.15f;}
                case 6->{body.xRot=-0.08f;tail0.yRot=0.18f;tail1.yRot=-0.15f;}
                default->{}}
            if(s.phase==2){leftCrest.zRot+=wave*0.045f;rightCrest.zRot-=wave*0.045f;tail3.yRot+=wave*0.08f;}
            if(s.hurt>0){body.zRot=0.035f*Mth.sin(s.hurt*1.5f);head.zRot=body.zRot;}
            if(s.deathTime>0){body.xRot=0.9f;tail0.xRot=0.25f;tail1.xRot=0.3f;tail2.xRot=0.35f;tail3.xRot=0.4f;}}
    }
}
