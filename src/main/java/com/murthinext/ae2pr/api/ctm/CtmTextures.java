package com.murthinext.ae2pr.api.ctm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.Ids;

/**
 * 连接纹理的图集登记表：基础贴图 → 图集贴图，图集路径为底图路径加 {@code _ctm}。
 *
 * <p>两种连接方式都只需登记一张底图：
 * <ul>
 * <li>{@link #registerTile47} —— 8×8 图集，按边与角选块（{@link CtmMeshBuilder}）</li>
 * <li>{@link #registerQuadrant} —— 4×4 图集，一个面拆四象限取块（{@link QuadrantCtmMeshBuilder}）</li>
 * </ul>
 */
public final class CtmTextures {

    public static final String ATLAS_SUFFIX = "_ctm";

    // 8×8 图集共 64 格，用前 47 格
    public static final int TILE_47_GRID = 8;

    // 4×4 图集共 16 格，一个面拆四象限
    public static final int QUADRANT_GRID = 4;

    // 底图 → 图集每边格数，0 表示只登记底图而不参与连接
    private static final Map<ResourceLocation, Integer> GRIDS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, TextureAtlasSprite> BASES = new HashMap<>();
    private static final Map<ResourceLocation, TextureAtlasSprite> ATLASES = new HashMap<>();
    private static final Map<ResourceLocation, Integer> GRIDS_BY_SPRITE = new HashMap<>();

    private CtmTextures() {
    }

    // 登记 47-tile 连接底图，重复登记以先登记者为准
    public static void registerTile47(String basePath) {
        registerTile47(Ids.of(basePath));
    }

    public static void registerTile47(ResourceLocation base) {
        GRIDS.putIfAbsent(base, TILE_47_GRID);
    }

    // 登记四象限连接底图
    public static void registerQuadrant(String basePath) {
        registerQuadrant(Ids.of(basePath));
    }

    public static void registerQuadrant(ResourceLocation base) {
        GRIDS.putIfAbsent(base, QUADRANT_GRID);
    }

    // 只登记底图，供换装时取用，不参与连接
    public static void registerSprite(String basePath) {
        registerSprite(Ids.of(basePath));
    }

    public static void registerSprite(ResourceLocation base) {
        GRIDS.putIfAbsent(base, 0);
    }

    // 图集拼接完成后缓存基础与图集 sprite
    public static void stitch(TextureAtlas atlas) {
        BASES.clear();
        ATLASES.clear();
        GRIDS_BY_SPRITE.clear();
        List<ResourceLocation> missing = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Integer> entry : GRIDS.entrySet()) {
            ResourceLocation base = entry.getKey();
            TextureAtlasSprite baseSprite = spriteOrNull(atlas, base);
            if (baseSprite == null) {
                missing.add(base);
                continue;
            }
            BASES.put(base, baseSprite);
            int grid = entry.getValue();
            if (grid == 0) {
                continue;
            }
            ResourceLocation ctm = ResourceLocation.fromNamespaceAndPath(base.getNamespace(),
                                                                         base.getPath() + ATLAS_SUFFIX);
            TextureAtlasSprite atlasSprite = spriteOrNull(atlas, ctm);
            if (atlasSprite == null) {
                missing.add(base);
                continue;
            }
            ATLASES.put(baseSprite.contents().name(), atlasSprite);
            GRIDS_BY_SPRITE.put(baseSprite.contents().name(), grid);
        }
        if (!missing.isEmpty()) {
            ae2pr.LOGGER.warn("Connected texture atlas is missing, affected blocks will use plain textures: {}",
                              missing);
        }
    }

    @Nullable
    private static TextureAtlasSprite spriteOrNull(TextureAtlas atlas, ResourceLocation location) {
        try {
            TextureAtlasSprite sprite = atlas.getSprite(location);
            // 缺失时 getSprite 返回 missing 占位贴图，需排除
            if (sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
                return null;
            }
            return sprite;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static TextureAtlasSprite baseSprite(ResourceLocation base) {
        return BASES.get(base);
    }

    @Nullable
    public static TextureAtlasSprite atlasFor(TextureAtlasSprite base) {
        return ATLASES.get(base.contents().name());
    }

    // 该底图是否使用四象限图集，未登记连接图集时按 47-tile 处理
    public static boolean isQuadrant(TextureAtlasSprite base) {
        return GRIDS_BY_SPRITE.getOrDefault(base.contents().name(), TILE_47_GRID) == QUADRANT_GRID;
    }
}
