package com.murthinext.ae2pr.client.ctm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.assembly_line.MachinePartFacade;

/**
 * 连接纹理图集登记表：建立「基础贴图 → CTM 图集」的映射，并缓存基础 sprite
 * （多方块成型换装需要按贴图定位并替换四边形）。
 * <p>
 * 图集为 8×8 共 64 格，用前 47 格（47-tile：每格即一张按边/角连接状态
 * 去掉若干边框、并视对角状态画凹内角弯的基础贴图），
 * 由 {@code misc/styled/generate_textures.py} 生成。
 */
public final class CtmSprites {

    /** 部件未成型基础贴图（成型换装时用于匹配需要替换的四边形） */
    public static final ResourceLocation CERTUS_PART_CASING = id("block/certus_quartz_crystal/casing");
    public static final ResourceLocation AEV_PART_CASING = id("block/aev/aev_machine_casing");

    /** 基础贴图 → CTM 图集 */
    private static final Map<ResourceLocation, ResourceLocation> ATLASES = new HashMap<>();

    /** 已登记的基础贴图（含无图集者），拼接时逐一解析 */
    private static final Set<ResourceLocation> REGISTERED = new LinkedHashSet<>();

    /** 基础贴图 → 已拼接的基础 sprite */
    private static final Map<ResourceLocation, TextureAtlasSprite> BASES = new HashMap<>();

    /** 基础贴图名 → 已拼接的图集 sprite */
    private static final Map<ResourceLocation, TextureAtlasSprite> CACHE = new HashMap<>();

    static {
        // 与 CtmConfig 的登记保持一致：控制器 / 装配线外壳 / 控制外壳不启用连接纹理
        register("block/crystal/crystal_reinforced_composite_machine_casing");
        register("block/crystal/assembly_line_grating");
        register("block/crystal/crystal_glass");
        register("block/crystal/fireproof_crystal_glass");
        register("block/certus_quartz_crystal/casing");
        register("block/aev/aev_machine_casing");
        register("block/zircon/zirconia_corundum_bricks");
        register("block/meteor_steel_pipe_block/side");
        // 多方块成型外观的贴图随注册表自动登记（需要连接纹理时附 _ctm 图集，见 blocks.json）
        for (MachinePartFacade facade : MachinePartFacade.all()) {
            registerFacade(facade);
        }
    }

    private CtmSprites() {
    }

    private static void register(String basePath) {
        ResourceLocation base = id(basePath);
        ATLASES.put(base, id(basePath + "_ctm"));
        REGISTERED.add(base);
    }

    /** 登记一条成型外观：基础贴图（必选）与 `_ctm` 图集（连接族非 0 时）。 */
    private static void registerFacade(MachinePartFacade facade) {
        ResourceLocation base = facade.texture();
        if (!REGISTERED.add(base)) {
            return;
        }
        if (facade.ctmFamily() != 0) {
            ATLASES.put(base, new ResourceLocation(base.getNamespace(), base.getPath() + "_ctm"));
        }
    }

    /** 仅登记基础贴图（无连接纹理图集），供不参与连接的成型外观使用。 */
    public static void registerBase(String basePath) {
        REGISTERED.add(id(basePath));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ae2pr.MODID, path);
    }

    /** 图集拼接完成后缓存基础 sprite 与图集 sprite 引用。 */
    public static void stitch(TextureAtlas atlas) {
        BASES.clear();
        CACHE.clear();
        List<ResourceLocation> missing = new ArrayList<>();
        for (ResourceLocation base : REGISTERED) {
            TextureAtlasSprite baseSprite = spriteOrNull(atlas, base);
            if (baseSprite == null) {
                missing.add(base);
                continue;
            }
            BASES.put(base, baseSprite);
            ResourceLocation ctm = ATLASES.get(base);
            if (ctm == null) {
                continue;
            }
            TextureAtlasSprite ctmSprite = spriteOrNull(atlas, ctm);
            if (ctmSprite == null) {
                missing.add(base);
                continue;
            }
            CACHE.put(baseSprite.contents().name(), ctmSprite);
        }
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

    /** 已登记的基础贴图 sprite（图集拼接后可用）。 */
    @Nullable
    public static TextureAtlasSprite baseSprite(ResourceLocation base) {
        return BASES.get(base);
    }

    /** 返回该基础贴图对应的 CTM 图集 sprite；没有则返回 null（保持原样）。 */
    @Nullable
    public static TextureAtlasSprite atlasFor(TextureAtlasSprite base) {
        return CACHE.get(base.contents().name());
    }
}
