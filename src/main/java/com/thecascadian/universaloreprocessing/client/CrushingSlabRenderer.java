package com.thecascadian.universaloreprocessing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thecascadian.universaloreprocessing.block.CrushingSlabBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Lays the resting item flat and centred on the slab floor, lit by the light
 * of the open space above the basin rather than the inside of the block.
 */
public class CrushingSlabRenderer implements BlockEntityRenderer<CrushingSlabBlockEntity> {

    private final ItemRenderer itemRenderer;

    public CrushingSlabRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(CrushingSlabBlockEntity slab, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        ItemStack stack = slab.item();
        if (stack.isEmpty() || slab.getLevel() == null)
            return;

        int light = LevelRenderer.getLightColor(slab.getLevel(), slab.getBlockPos().above());
        poseStack.pushPose();
        // the item model is one pixel thick; half a scaled pixel above the floor avoids z-fighting
        poseStack.translate(0.5D, CrushingSlabBlockEntity.FLOOR + 1.0D / 64.0D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.625F, 0.625F, 0.625F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, packedOverlay, poseStack, buffer,
                slab.getLevel(), (int) slab.getBlockPos().asLong());
        poseStack.popPose();
    }
}
