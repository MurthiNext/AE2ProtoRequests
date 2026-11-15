package com.murthinext.ae2pr.client.model;

import appeng.client.render.cablebus.CubeBuilder;
import appeng.parts.automation.PlaneConnections;
import appeng.parts.automation.PlaneModel;
import appeng.parts.automation.PlaneModelData;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.Nullable;

public final class SkyDomeAnnihilationPlaneModel implements IUnbakedGeometry<SkyDomeAnnihilationPlaneModel> {

    public static final IGeometryLoader<SkyDomeAnnihilationPlaneModel> LOADER =
            (json, context) -> new SkyDomeAnnihilationPlaneModel();

    @Override
    public BakedModel bake(
            IGeometryBakingContext context,
            ModelBaker baker,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            ItemOverrides overrides,
            ResourceLocation modelLocation) {
        var front = context.getMaterial("plane");
        var sides = context.getMaterial("sides");
        var back = context.getMaterial("back");
        var original = new PlaneModel(front.texture(), sides.texture(), back.texture())
                .bake(baker, spriteGetter, modelState, modelLocation);
        return new ConnectedModel(original, spriteGetter.apply(front));
    }

    private static final class ConnectedModel extends BakedModelWrapper<BakedModel> {

        private final Map<PlaneConnections, List<BakedQuad>> quads = new HashMap<>();

        private ConnectedModel(BakedModel original, TextureAtlasSprite front) {
            super(original);
            var random = RandomSource.create(0);
            for (var connections : PlaneConnections.PERMUTATIONS) {
                var data = ModelData.builder()
                        .with(PlaneModelData.CONNECTIONS, connections)
                        .build();
                var output = new ArrayList<>(original.getQuads(null, null, random, data, null));
                output.removeIf(quad -> quad.getDirection() == Direction.NORTH);
                var builder = new CubeBuilder(output);
                builder.setDrawFaces(EnumSet.of(Direction.NORTH));
                builder.setTexture(front);

                int[] x = {connections.isRight() ? 0 : 1, 2, 14, connections.isLeft() ? 16 : 15};
                int[] y = {connections.isDown() ? 0 : 1, 2, 14, connections.isUp() ? 16 : 15};
                // 外沿保留一像素包边，连接方向延展并采样内部纹理。
                int[][] u = {
                    {connections.isRight() ? 2 : 1, connections.isRight() ? 4 : 2},
                    {2, 14},
                    {connections.isLeft() ? 12 : 14, connections.isLeft() ? 14 : 15}
                };
                int[][] v = {
                    {connections.isDown() ? 2 : 1, connections.isDown() ? 4 : 2},
                    {2, 14},
                    {connections.isUp() ? 12 : 14, connections.isUp() ? 14 : 15}
                };
                for (int column = 0; column < 3; column++) {
                    for (int row = 0; row < 3; row++) {
                        builder.setCustomUv(
                                Direction.NORTH, 16 - u[column][1], 16 - v[row][0], 16 - u[column][0], 16 - v[row][1]);
                        builder.addCube(x[column], y[row], 0, x[column + 1], y[row + 1], 1);
                    }
                }
                quads.put(connections, List.copyOf(output));
            }
        }

        @Override
        public List<BakedQuad> getQuads(
                @Nullable BlockState state,
                @Nullable Direction side,
                RandomSource random,
                ModelData data,
                @Nullable RenderType renderType) {
            if (side != null) {
                return List.of();
            }
            var connections = data.get(PlaneModelData.CONNECTIONS);
            return quads.get(connections != null ? connections : PlaneConnections.of(false, false, false, false));
        }
    }
}
