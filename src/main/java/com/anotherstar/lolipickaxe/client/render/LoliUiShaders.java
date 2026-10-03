package com.anotherstar.lolipickaxe.client.render;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

 
@Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LoliUiShaders {
    private static ShaderInstance shape;

    public static boolean isReady() { return shape != null; }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation(LoliPickaxe.MOD_ID, "loli_rounded_shape"),
                    DefaultVertexFormat.POSITION_COLOR_TEX), shader -> shape = shader);
        } catch (Exception error) {
            LoliPickaxe.LOGGER.warn("Unable to register rounded UI shader; using rectangle fallback", error);
        }
    }

    public static boolean drawRoundedRect(GuiGraphics graphics, float x, float y, float width, float height,
                                          float radius, int fill, float borderWidth, int border) {
        if (graphics == null || shape == null || width <= 0 || height <= 0) return false;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(() -> shape);
        Uniform size = shape.getUniform("rectSize");
        if (size != null) size.set(width, height);
        scalar("cornerRadius", radius);
        scalar("guiScale", (float) Minecraft.getInstance().getWindow().getGuiScale());
        scalar("borderWidth", borderWidth);
        color("fillColor", fill);
        color("borderColor", border);

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        builder.vertex(matrix, x, y + height, 0).color(255,255,255,255).uv(0,1).endVertex();
        builder.vertex(matrix, x + width, y + height, 0).color(255,255,255,255).uv(1,1).endVertex();
        builder.vertex(matrix, x + width, y, 0).color(255,255,255,255).uv(1,0).endVertex();
        builder.vertex(matrix, x, y, 0).color(255,255,255,255).uv(0,0).endVertex();
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.enableDepthTest();
        return true;
    }

    private static void scalar(String name, float value) {
        Uniform uniform = shape.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void color(String name, int argb) {
        Uniform uniform = shape.getUniform(name);
        if (uniform != null) uniform.set(((argb >>> 16) & 255) / 255f,
                ((argb >>> 8) & 255) / 255f, (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
    }

    private LoliUiShaders() {}
}
