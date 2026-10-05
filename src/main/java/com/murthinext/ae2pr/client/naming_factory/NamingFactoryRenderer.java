package com.murthinext.ae2pr.client.naming_factory;

import java.util.Map;
import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.naming_factory.NamingFactoryBlockEntity;

/**
 * 名称压印工厂渲染器：绘制下压的 forge、随行程平移材质的活塞连杆以及平台上的待压物品。
 * <p>
 * 静态外壳由方块模型绘制；forge 使用 {@code RegisterAdditional} 注册的独立模型，连杆按当前行程手工绘制四边形，
 * 其 UV 以底端为材质原点随行程平移，因此不会被纵向拉伸。
 */
public class NamingFactoryRenderer implements BlockEntityRenderer<NamingFactoryBlockEntity> {

    public static final ResourceLocation FORGE_MODEL = new ResourceLocation(ae2pr.MODID, "block/naming_factory_forge");

    /** 活塞连杆专用贴图 */
    private static final ResourceLocation ROD_TEXTURE = new ResourceLocation(ae2pr.MODID, "block/naming_factory_rod");

    /** 连杆截面（像素）：8x8，几乎与 forge（10x10）同宽 */
    private static final float ROD_MIN = 4.0F;
    private static final float ROD_MAX = 12.0F;
    /** 连杆固定端：顶框底面 */
    private static final float ROD_ANCHOR = 14.0F;
    /** 连杆截面宽度对应的 UV 宽度（1 材质像素 = 1 模型像素） */
    private static final float ROD_U = ROD_MAX - ROD_MIN;

    /** 原版各朝向的漫反射系数：南北 / 东西 */
    private static final float SHADE_NS = 0.8F;
    private static final float SHADE_EW = 0.6F;

    /** 平台顶面高度（像素），物品摆放面 */
    private static final float PLATFORM_TOP = 6.0F;

    private static BakedModel forgeModel;

    private final ModelBlockRenderer blockRenderer;
    private final Function<ResourceLocation, TextureAtlasSprite> atlas;

    public NamingFactoryRenderer(BlockEntityRendererProvider.Context context) {
        Minecraft minecraft = Minecraft.getInstance();
        this.blockRenderer = minecraft.getBlockRenderer().getModelRenderer();
        this.atlas = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
    }

    /** 由客户端初始化在模型烘焙完成时注入附加模型。 */
    public static void acceptModels(Map<ResourceLocation, BakedModel> models) {
        forgeModel = models.get(FORGE_MODEL);
    }

    @Override
    public void render(NamingFactoryBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffers, int combinedLight, int combinedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        float offset = blockEntity.getPressOffset(partialTicks);
        VertexConsumer buffer = buffers.getBuffer(RenderType.cutout());

        if (forgeModel != null) {
            poseStack.pushPose();
            poseStack.translate(0, -offset / 16.0F, 0);
            blockRenderer.renderModel(poseStack.last(), buffer, blockEntity.getBlockState(), forgeModel,
                    1.0F, 1.0F, 1.0F, combinedLight, combinedOverlay);
            poseStack.popPose();
        }
        if (offset > 0.01F) {
            renderRod(poseStack, buffer, atlas.apply(ROD_TEXTURE), offset, combinedLight, combinedOverlay);
        }

        renderPlatformItem(blockEntity, poseStack, buffers, combinedLight, combinedOverlay);
    }

    /**
     * 绘制活塞连杆：顶端固定在顶框底面，底端随 forge 下移；材质 V 以底端为原点，图案随行程平移而不拉伸。
     */
    private void renderRod(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite, float offset,
            int light, int overlay) {
        float top = ROD_ANCHOR;
        float bottom = ROD_ANCHOR - offset;
        PoseStack.Pose pose = poseStack.last();

        // 北面（-Z）
        addVertex(buffer, pose, sprite, ROD_MAX, top, ROD_MIN, ROD_U, offset, 0, 0, -1, light, overlay, SHADE_NS);
        addVertex(buffer, pose, sprite, ROD_MAX, bottom, ROD_MIN, ROD_U, 0, 0, 0, -1, light, overlay, SHADE_NS);
        addVertex(buffer, pose, sprite, ROD_MIN, bottom, ROD_MIN, 0, 0, 0, 0, -1, light, overlay, SHADE_NS);
        addVertex(buffer, pose, sprite, ROD_MIN, top, ROD_MIN, 0, offset, 0, 0, -1, light, overlay, SHADE_NS);

        // 南面（+Z）
        addVertex(buffer, pose, sprite, ROD_MIN, top, ROD_MAX, 0, offset, 0, 0, 1, light, overlay, SHADE_NS);
        addVertex(buffer, pose, sprite, ROD_MIN, bottom, ROD_MAX, 0, 0, 0, 0, 1, light, overlay, SHADE_NS);
        addVertex(buffer, pose, sprite, ROD_MAX, bottom, ROD_MAX, ROD_U, 0, 0, 0, 1, light, overlay, SHADE_NS);
        addVertex(buffer, pose, sprite, ROD_MAX, top, ROD_MAX, ROD_U, offset, 0, 0, 1, light, overlay, SHADE_NS);

        // 西面（-X）
        addVertex(buffer, pose, sprite, ROD_MIN, top, ROD_MIN, 0, offset, -1, 0, 0, light, overlay, SHADE_EW);
        addVertex(buffer, pose, sprite, ROD_MIN, bottom, ROD_MIN, 0, 0, -1, 0, 0, light, overlay, SHADE_EW);
        addVertex(buffer, pose, sprite, ROD_MIN, bottom, ROD_MAX, ROD_U, 0, -1, 0, 0, light, overlay, SHADE_EW);
        addVertex(buffer, pose, sprite, ROD_MIN, top, ROD_MAX, ROD_U, offset, -1, 0, 0, light, overlay, SHADE_EW);

        // 东面（+X）
        addVertex(buffer, pose, sprite, ROD_MAX, top, ROD_MAX, ROD_U, offset, 1, 0, 0, light, overlay, SHADE_EW);
        addVertex(buffer, pose, sprite, ROD_MAX, bottom, ROD_MAX, ROD_U, 0, 1, 0, 0, light, overlay, SHADE_EW);
        addVertex(buffer, pose, sprite, ROD_MAX, bottom, ROD_MIN, 0, 0, 1, 0, 0, light, overlay, SHADE_EW);
        addVertex(buffer, pose, sprite, ROD_MAX, top, ROD_MIN, 0, offset, 1, 0, 0, light, overlay, SHADE_EW);
    }

    private static void addVertex(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            float x, float y, float z, float u, float v, float nx, float ny, float nz,
            int light, int overlay, float shade) {
        buffer.vertex(pose.pose(), x / 16.0F, y / 16.0F, z / 16.0F);
        buffer.color(shade, shade, shade, 1.0F);
        buffer.uv(sprite.getU(u), sprite.getV(v));
        buffer.overlayCoords(overlay);
        buffer.uv2(light);
        buffer.normal(pose.normal(), nx, ny, nz);
        buffer.endVertex();
    }

    /** 在平台顶面平放渲染当前输入的整组物品（压印器的待压物料效果）。 */
    private void renderPlatformItem(NamingFactoryBlockEntity blockEntity, PoseStack poseStack,
            MultiBufferSource buffers, int combinedLight, int combinedOverlay) {
        ItemStack stack = blockEntity.getInputStack();
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, PLATFORM_TOP / 16.0F + 0.02F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                combinedLight, combinedOverlay, poseStack, buffers, blockEntity.getLevel(), 0);
        poseStack.popPose();
    }
}
