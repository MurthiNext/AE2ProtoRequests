package com.murthinext.ae2pr.client.ctm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;

/**
 * 连接纹理图集登记表：建立「基础贴图 → CTM 图集」的映射。
 * <p>
 * 图集为 8×8 共 64 格，用前 47 格（47-tile：每格即一张按边/角连接状态
 * 去掉若干边框、并视对角状态画凹内角弯的基础贴图），
 * 由 {@code misc/styled/generate_textures.py} 生成。
 */
public final class CtmSprites {

    /** 基础贴图 → CTM 图集 */
    private static final Map<ResourceLocation, ResourceLocation> ATLASES = new HashMap<>();

    /** 基础贴图名 → 已拼接的图集 sprite */
    private static final Map<ResourceLocation, TextureAtlasSprite> CACHE = new HashMap<>();

    static {
        // 与 CtmConfig 的登记保持一致：控制器 / 装配线外壳 / 控制外壳不启用连接纹理
        register("block/crystal/crystal_reinforced_composite_machine_casing");
        register("block/crystal/assembly_line_grating");
        register("block/crystal/crystal_glass");
        register("block/certus_quartz_crystal/casing");
        register("block/zircon/zirconia_corundum_bricks");
        register("block/meteor_steel_pipe_block/side");
    }

    private CtmSprites() {
    }

    private static void register(String basePath) {
        ATLASES.put(id(basePath), id(basePath + "_ctm"));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ae2pr.MODID, path);
    }

    /** 图集拼接完成后缓存 sprite 引用。 */
    public static void stitch(TextureAtlas atlas) {
        CACHE.clear();
        List<ResourceLocation> missing = new ArrayList<>();
        ATLASES.forEach((base, ctm) -> {
            TextureAtlasSprite baseSprite = spriteOrNull(atlas, base);
            TextureAtlasSprite ctmSprite = spriteOrNull(atlas, ctm);
            if (baseSprite == null || ctmSprite == null) {
                missing.add(base);
                return;
            }
            CACHE.put(baseSprite.contents().name(), ctmSprite);
        });
        if (!missing.isEmpty()) {
            ae2pr.LOGGER.warn("连接纹理图集缺失，相关方块将使用普通贴图: {}", missing);
        }
    }

    @Nullable
    private static TextureAtlasSprite spriteOrNull(TextureAtlas atlas, ResourceLocation location) {
        try {
            TextureAtlasSprite sprite = atlas.getSprite(location);
            // 缺失时 getSprite 可能返回 missing 占位贴图，需显式排除
            if (sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
                return null;
            }
            return sprite;
        } catch (Exception e) {
            return null;
        }
    }

    /** 返回该基础贴图对应的 CTM 图集 sprite；没有则返回 null（保持原样）。 */
    @Nullable
    public static TextureAtlasSprite atlasFor(TextureAtlasSprite base) {
        return CACHE.get(base.contents().name());
    }
}
