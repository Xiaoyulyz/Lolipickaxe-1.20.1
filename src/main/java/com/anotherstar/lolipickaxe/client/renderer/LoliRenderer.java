package com.anotherstar.lolipickaxe.client.renderer;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.client.model.LoliModel;
import com.anotherstar.lolipickaxe.entity.LoliEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class LoliRenderer extends MobRenderer<LoliEntity, LoliModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(LoliPickaxe.MOD_ID, "textures/entities/loli.png");

    public LoliRenderer(EntityRendererProvider.Context context) {
        super(context, new LoliModel(context.bakeLayer(LoliModel.LAYER)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(LoliEntity entity) {
        return TEXTURE;
    }
}
