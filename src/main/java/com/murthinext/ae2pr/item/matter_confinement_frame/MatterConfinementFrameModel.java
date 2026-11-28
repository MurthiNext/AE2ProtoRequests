package com.murthinext.ae2pr.item.matter_confinement_frame;

import appeng.api.util.AEColor;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.CompositeModel;
import net.minecraftforge.client.model.ElementsModel;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.Nullable;

public final class MatterConfinementFrameModel implements IUnbakedGeometry<MatterConfinementFrameModel> {

    public static final IGeometryLoader<MatterConfinementFrameModel> LOADER =
            (json, context) -> new MatterConfinementFrameModel(
                    ElementsModel.Loader.INSTANCE.read(json, context),
                    new Crystal(json.getAsJsonObject("crystal")));

    private final ElementsModel frame;
    private final Crystal crystal;

    private MatterConfinementFrameModel(ElementsModel frame, Crystal crystal) {
        this.frame = frame;
        this.crystal = crystal;
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
        var resolver = new ContentsOverrides(base, spriteGetter.apply(context.getMaterial("matter")), crystal);
        var builder = resolver.builder(resolver);
        return builder.build();
    }

    private static final class ContentsOverrides extends ItemOverrides {

        private final BakedModel frame;
        private final TextureAtlasSprite matter;
        private final Crystal crystal;
        private final Cache<Map<AEColor, Integer>, FilledModel> models =
                CacheBuilder.newBuilder().maximumSize(32).build();

        private ContentsOverrides(BakedModel frame, TextureAtlasSprite matter, Crystal crystal) {
            this.frame = frame;
            this.matter = matter;
            this.crystal = crystal;
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
            var surface = crystal.createSurface(contents, matter);
            var builder = builder(ItemOverrides.EMPTY);
            builder.addQuads(
                    new RenderTypeGroup(RenderType.translucent(), ForgeRenderTypes.ITEM_UNLIT_TRANSLUCENT.get()),
                    surface.quads());
            return new FilledModel(builder.build(), surface);
        }

        private CompositeModel.Baked.Builder builder(ItemOverrides overrides) {
            var builder = CompositeModel.Baked.builder(
                    frame.useAmbientOcclusion(),
                    frame.isGui3d(),
                    frame.usesBlockLight(),
                    frame.getParticleIcon(),
                    overrides,
                    frame.getTransforms());
            builder.addLayer(frame);
            return builder;
        }
    }

    private record FilledModel(BakedModel model, Crystal.Surface surface) {}
}
