package com.pipican.client.boss;

import com.pipican.Pipican;
import com.pipican.boss.SkollvarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native wolf model poses for Sköllvar's charge, pounce and prowl attacks. */
public final class SkollvarRenderer extends MobRenderer<SkollvarEntity,SkollvarRenderer.State,SkollvarRenderer.Model>{
    public SkollvarRenderer(EntityRendererProvider.Context context){ super(context,new Model(BossGeometry.load("skollvar_winterfang")),0.9f); }
    public static final class State extends LivingEntityRenderState{int phase,action;float remaining;}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return Pipican.id("textures/entity/boss/skollvar_"+(state.phase==2?"alpha":"winterfang")+".png");}
    @Override public void extractRenderState(SkollvarEntity entity,State state,float partial){super.extractRenderState(entity,state,partial);state.phase=entity.phase();state.action=entity.skollAction();state.remaining=Math.max(0,entity.abilityTicksRemaining()-partial);}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(SkollvarEntity entity){return super.getBoundingBoxForCulling(entity).inflate(1.5);}
    static final class Model extends EntityModel<State>{
        private final ModelPart body,head,frontLeft,frontRight,backLeft,backRight;
        Model(ModelPart root){super(root);var base=root.getChild("root");body=base.getChild("body");head=body.getChild("head");frontLeft=body.getChild("left_arm");frontRight=body.getChild("right_arm");backLeft=base.getChild("left_leg");backRight=base.getChild("right_leg");}
        @Override public void setupAnim(State s){
            super.setupAnim(s);float gait=Mth.clamp(s.walkAnimationSpeed,0,1);
            frontLeft.xRot=Mth.cos(s.walkAnimationPos*0.8f)*0.8f*gait;frontRight.xRot=-frontLeft.xRot;
            backLeft.xRot=-frontLeft.xRot;backRight.xRot=frontLeft.xRot;head.yRot=Mth.clamp(s.yRot,-40,40)*Mth.DEG_TO_RAD;
            float pounce=1-Mth.clamp(s.remaining/60,0,1);
            switch(s.action){case 1->{body.xRot=-0.45f*pounce;frontLeft.xRot=-0.8f*pounce;frontRight.xRot=-0.8f*pounce;}
                case 2->{body.xRot=-0.2f*pounce;head.xRot=-0.4f*pounce;}
                case 3->{body.yRot=0.15f*pounce;head.yRot+=0.25f*pounce;}
                case 4->{body.xRot=0.3f*pounce;head.xRot=-0.5f*pounce;}
                case 5->{body.y-=0.12f*pounce;head.xRot=-0.3f*pounce;}
                default->{}}
            if(s.phase==2)body.y+=Mth.sin(s.ageInTicks*0.12f)*0.08f;
            if(s.deathTime>0)body.xRot=1.2f;
        }
    }
}
