package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.NoctharEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native robes, staff and void-spike poses on the preserved Sovereign geometry. */
public final class NoctharRenderer extends MobRenderer<NoctharEntity,NoctharRenderer.State,NoctharRenderer.Model>{
    public NoctharRenderer(EntityRendererProvider.Context context){super(context,new Model(BossGeometry.load("nocthar_void_sovereign")),1.0f);}
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining,hurt;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State s){return Pipican.id("textures/entity/boss/nocthar_"+(s.phase==2?"eclipsed":"void_sovereign")+".png");}
    @Override public void extractRenderState(NoctharEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.phase=e.phase();s.action=e.noctharAction();s.remaining=Math.max(0,e.abilityTicksRemaining()-partial);s.hurt=e.hurtTime-partial;}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(NoctharEntity e){return super.getBoundingBoxForCulling(e).inflate(2.2);}
    static final class Model extends EntityModel<State>{
        private final ModelPart root,body,head,leftArm,rightArm,leftLeg,rightLeg,robe,staff;private final ModelPart[] spikes;
        Model(ModelPart model){super(model);root=model.getChild("root");body=root.getChild("body");head=body.getChild("head");leftArm=body.getChild("left_arm");rightArm=body.getChild("right_arm");leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");robe=root.getChild("robe");staff=rightArm.getChild("staff");spikes=new ModelPart[]{body.getChild("void_spike_0"),body.getChild("void_spike_1"),body.getChild("void_spike_2"),body.getChild("void_spike_3")};}
        @Override public void setupAnim(State s){super.setupAnim(s);float gait=Mth.clamp(s.walkAnimationSpeed,0,1),wave=s.ageInTicks*0.07f;
            root.y=24+Mth.sin(wave)*0.16f;body.yRot=Mth.sin(wave*0.7f)*0.025f;robe.yRot=-body.yRot*0.7f;
            leftLeg.xRot=Mth.cos(s.walkAnimationPos*0.7f)*0.3f*gait;rightLeg.xRot=-leftLeg.xRot;
            leftArm.xRot=-0.2f;rightArm.xRot=-0.25f;head.yRot=Mth.clamp(s.yRot,-32,32)*Mth.DEG_TO_RAD;
            float tell=1-Mth.clamp(s.remaining/90f,0,1);
            switch(s.action){case 1->{rightArm.xRot=-1.05f*tell;staff.xRot=-0.6f*tell;body.yRot+=0.12f*tell;}
                case 2->{leftArm.xRot=-0.75f*tell;rightArm.xRot=-0.75f*tell;body.xRot=-0.08f*tell;}
                case 3->{leftArm.xRot=-1.1f*tell;rightArm.xRot=-1.1f*tell;head.xRot=-0.12f*tell;}
                case 4->{rightArm.xRot=-1.35f*tell;staff.zRot=0.35f*Mth.sin(s.ageInTicks*0.18f)*tell;}
                case 5->{body.y-=0.12f*tell;for(int i=0;i<spikes.length;i++)spikes[i].yRot=(i%2==0?1:-1)*tell*0.25f;}
                case 6->{leftArm.xRot=-0.75f;rightArm.xRot=-0.75f;}
                default->{}}
            if(s.phase==2)for(int i=0;i<spikes.length;i++)spikes[i].yRot+=Mth.sin(wave+i)*0.06f;
            if(s.hurt>0){body.zRot=0.045f*Mth.sin(s.hurt*1.5f);head.zRot=body.zRot*1.4f;}
            if(s.deathTime>0){body.xRot=1.15f;robe.xRot=1.15f;}}
    }
}
