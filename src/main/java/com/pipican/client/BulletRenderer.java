package com.pipican.client;

import com.pipican.Pipican;
import com.pipican.firearm.Bullet;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

/** Bullets: a short dark slug on the arrow model. */
public class BulletRenderer extends ArrowRenderer<Bullet, ArrowRenderState> {

    private static final Identifier TEXTURE = Pipican.id("textures/entity/bullet.png");

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected Identifier getTextureLocation(ArrowRenderState state) {
        return TEXTURE;
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }
}
