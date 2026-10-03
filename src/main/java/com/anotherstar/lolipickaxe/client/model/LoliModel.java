package com.anotherstar.lolipickaxe.client.model;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.entity.LoliEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

 
public final class LoliModel extends EntityModel<LoliEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(LoliPickaxe.MOD_ID, "loli"), "main");
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;

    public LoliModel(ModelPart root) {
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
        body = root.getChild("body");
        head = root.getChild("head");
        leftArm = root.getChild("left_arm");
        rightArm = root.getChild("right_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.5F, -1.0F, -1.5F, 3.0F, 8.0F, 3.0F), PartPose.offset(2.0F, 17.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(12, 0)
                .addBox(-1.5F, -1.0F, -1.5F, 3.0F, 8.0F, 3.0F), PartPose.offset(-2.0F, 17.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 41).addBox(-5.0F, 2.0F, -5.0F, 10.0F, 2.0F, 10.0F)
                .texOffs(4, 31).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 2.0F, 8.0F)
                .texOffs(8, 20).addBox(-3.0F, -5.0F, -3.0F, 6.0F, 5.0F, 6.0F)
                .texOffs(12, 13).addBox(-2.0F, -8.0F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.offset(0.0F, 14.0F, 0.0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(48, 0)
                .addBox(-4.0F, -4.5F, -4.0F, 8.0F, 8.0F, 8.0F, new net.minecraft.client.model.geom.builders.CubeDeformation(-0.5F)), PartPose.offset(0.0F, 5.0F, 0.0F));
        head.addOrReplaceChild("hair", CubeListBuilder.create()
                .texOffs(80, 0).addBox(-4.0F, -4.5F, -4.0F, 8.0F, 1.0F, 8.0F)
                .texOffs(80, 9).addBox(-4.0F, -3.5F, 3.0F, 8.0F, 9.0F, 1.0F)
                .texOffs(112, 24).addBox(-3.0F, 5.5F, 3.0F, 6.0F, 2.0F, 1.0F)
                .texOffs(80, 19).addBox(-2.0F, 7.5F, 3.5F, 4.0F, 1.0F, 1.0F)
                .texOffs(90, 19).addBox(-2.0F, 8.5F, 4.0F, 4.0F, 1.0F, 1.0F)
                .texOffs(80, 21).addBox(-1.0F, 9.5F, 4.5F, 2.0F, 1.0F, 1.0F)
                .texOffs(86, 21).addBox(-1.0F, 10.5F, 5.0F, 2.0F, 1.0F, 1.0F)
                .texOffs(104, 14).addBox(0.0F, -1.5F, -4.0F, 1.0F, 1.0F, 1.0F)
                .texOffs(98, 15).addBox(1.0F, -3.5F, -4.0F, 1.0F, 1.0F, 1.0F)
                .texOffs(104, 12).addBox(-2.0F, -3.5F, -4.0F, 1.0F, 1.0F, 1.0F)
                .texOffs(98, 17).addBox(-4.0F, -1.5F, -4.0F, 1.0F, 1.0F, 1.0F)
                .texOffs(104, 16).addBox(3.0F, -1.5F, -4.0F, 1.0F, 1.0F, 1.0F)
                .texOffs(104, 9).addBox(-1.0F, -3.5F, -4.0F, 2.0F, 2.0F, 1.0F)
                .texOffs(98, 9).addBox(2.0F, -3.5F, -4.0F, 2.0F, 2.0F, 1.0F)
                .texOffs(98, 12).addBox(-4.0F, -3.5F, -4.0F, 2.0F, 2.0F, 1.0F)
                .texOffs(112, 0).addBox(-4.0F, -3.5F, -3.0F, 1.0F, 4.0F, 6.0F)
                .texOffs(102, 18).addBox(-4.0F, 0.5F, -1.0F, 1.0F, 1.0F, 4.0F)
                .texOffs(112, 20).addBox(-4.0F, 1.5F, 1.0F, 1.0F, 1.0F, 2.0F)
                .texOffs(124, 20).addBox(-4.0F, 2.5F, 2.0F, 1.0F, 1.0F, 1.0F)
                .texOffs(102, 23).addBox(3.0F, 0.5F, -1.0F, 1.0F, 1.0F, 4.0F)
                .texOffs(118, 20).addBox(3.0F, 1.5F, 1.0F, 1.0F, 1.0F, 2.0F)
                .texOffs(124, 22).addBox(3.0F, 2.5F, 2.0F, 1.0F, 1.0F, 1.0F)
                .texOffs(112, 10).addBox(3.0F, -3.5F, -3.0F, 1.0F, 4.0F, 6.0F), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(26, 0).addBox(0.0F, 2.0F, -1.0F, 2.0F, 5.0F, 2.0F)
                .texOffs(24, 7).addBox(-0.5F, -1.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(3.0F, 10.0F, 0.0F, 0.0F, 0.0F, -0.3491F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(38, 0).addBox(-2.0F, 2.0F, -1.0F, 2.0F, 5.0F, 2.0F)
                .texOffs(36, 7).addBox(-2.5F, -1.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(-3.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.3491F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(LoliEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float amount = Mth.clamp(limbSwingAmount, 0.0F, 0.8F);
        float walk = limbSwing * 0.6662F;
        leftArm.xRot = Mth.cos(walk + Mth.PI) * amount;
        rightArm.xRot = Mth.cos(walk) * amount;
        leftLeg.xRot = Mth.cos(walk) * amount * 1.4F;
        rightLeg.xRot = Mth.cos(walk + Mth.PI) * amount * 1.4F;
        head.yRot = Mth.clamp(netHeadYaw, -75.0F, 75.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(headPitch, -45.0F, 45.0F) * Mth.DEG_TO_RAD;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        leftLeg.render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        rightLeg.render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        body.render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        head.render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        leftArm.render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        rightArm.render(poseStack, consumer, light, overlay, red, green, blue, alpha);
    }
}
