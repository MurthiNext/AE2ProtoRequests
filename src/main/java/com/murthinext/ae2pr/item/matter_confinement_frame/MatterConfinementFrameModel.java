package com.murthinext.ae2pr.item.matter_confinement_frame;

import appeng.api.util.AEColor;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.ElementsModel;
import net.minecraftforge.client.model.IModelBuilder;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.Nullable;

public final class MatterConfinementFrameModel implements IUnbakedGeometry<MatterConfinementFrameModel> {

    public static final IGeometryLoader<MatterConfinementFrameModel> LOADER =
            (json, context) -> new MatterConfinementFrameModel(
                    ElementsModel.Loader.INSTANCE.read(json, context),
                    new MatterSphere(json.getAsJsonObject("sphere")));

    private final ElementsModel frame;
    private final MatterSphere sphere;

    private MatterConfinementFrameModel(ElementsModel frame, MatterSphere sphere) {
        this.frame = frame;
        this.sphere = sphere;
    }

    @Override
    public BakedModel bake(
            IGeometryBakingContext context,
            ModelBaker baker,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            ItemOverrides overrides,
            ResourceLocation modelLocation) {
        var base = frame.bake(context, baker, spriteGetter, modelState, overrides, modelLocation);
        var resolver = new ContentsOverrides(base, spriteGetter.apply(context.getMaterial("matter")), sphere);
        return resolver.createModel(resolver.frameQuads, resolver);
    }

    private static final class ContentsOverrides extends ItemOverrides {

        private final BakedModel frame;
        private final List<BakedQuad> frameQuads;
        private final TextureAtlasSprite matter;
        private final MatterSphere sphere;
        private final Cache<Map<AEColor, Integer>, FilledModel> models =
                CacheBuilder.newBuilder().maximumSize(32).build();

        private ContentsOverrides(BakedModel frame, TextureAtlasSprite matter, MatterSphere sphere) {
            this.frame = frame;
            this.frameQuads = List.copyOf(frame.getQuads(null, null, RandomSource.create(0), ModelData.EMPTY, null));
            this.matter = matter;
            this.sphere = sphere;
        }

        @Override
        public BakedModel resolve(
                BakedModel model,
                ItemStack stack,
                @Nullable ClientLevel level,
                @Nullable LivingEntity entity,
                int seed) {
            var contents = MatterConfinementFrameItem.getContents(stack);
            if (contents.isEmpty()) {
                return model;
            }
            var filled = models.asMap().computeIfAbsent(contents, this::createFilledModel);
            filled.surface().update();
            return filled.model();
        }

        private FilledModel createFilledModel(Map<AEColor, Integer> contents) {
            var surface = sphere.createSurface(contents, matter);
            var quads = new ArrayList<>(frameQuads);
            quads.addAll(surface.quads());
            return new FilledModel(createModel(quads, ItemOverrides.EMPTY), surface);
        }

        private BakedModel createModel(List<BakedQuad> quads, ItemOverrides overrides) {
            var builder = IModelBuilder.of(
                    frame.useAmbientOcclusion(),
                    frame.usesBlockLight(),
                    true,
                    frame.getTransforms(),
                    overrides,
                    frame.getParticleIcon(),
                    new RenderTypeGroup(RenderType.cutout(), ForgeRenderTypes.ITEM_LAYERED_CUTOUT.get()));
            quads.forEach(builder::addUnculledFace);
            return builder.build();
        }
    }

    private record FilledModel(BakedModel model, MatterSphere.Surface surface) {}
}
