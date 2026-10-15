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
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;

/**
 * 连接纹理图集登记表：基础贴图 → CTM 图集，并缓存基础 sprite。
 */
public final class CtmSprites {

    public static final ResourceLocation CERTUS_PART_CASING = id("block/certus_quartz_crystal/casing");
    public static final ResourceLocation AEV_PART_CASING = id("block/aev/aev_machine_casing");
    public static final ResourceLocation LAVA_SMELTER_CONTROLLER_CASING = id("block/lava_smelter/controller_casing");
    public static final ResourceLocation MATTER_PHASE_TRANSFORMER_CONTROLLER_CASING = id("block/matter_phase_transformer/controller_casing");
    public static final ResourceLocation CRYSTAL_REACTION_CONTROLLER_CASING = id("block/crystal_reaction_chamber/controller_casing");

    private static final Map<ResourceLocation, ResourceLocation> ATLASES = new HashMap<>();

    private static final Set<ResourceLocation> REGISTERED = new LinkedHashSet<>();

    private static final Map<ResourceLocation, TextureAtlasSprite> BASES = new HashMap<>();

    private static final Map<ResourceLocation, TextureAtlasSprite> CACHE = new HashMap<>();

    static {
        // 与 CtmConfig 的登记保持一致
        register("block/crystal/crystal_reinforced_composite_machine_casing");
        register("block/crystal/crystal_glass");
        register("block/crystal/fireproof_crystal_glass");
        register("block/certus_quartz_crystal/casing");
        register("block/aev/aev_machine_casing");
        register("block/reinforced_high_power_casing");
        register("block/zircon/zirconia_corundum_bricks");
        register("block/meteor_steel_pipe_block/side");
        REGISTERED.add(LAVA_SMELTER_CONTROLLER_CASING);
        REGISTERED.add(MATTER_PHASE_TRANSFORMER_CONTROLLER_CASING);
        REGISTERED.add(CRYSTAL_REACTION_CONTROLLER_CASING);
        // 成型外观贴图随注册表自动登记
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

    private static void registerFacade(MachinePartFacade facade) {
        ResourceLocation base = facade.texture();
        if (!REGISTERED.add(base)) {
            return;
        }
        if (facade.ctmFamily() != 0) {
            ATLASES.put(base, ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "_ctm"));
        }
    }

    public static void registerBase(String basePath) {
        REGISTERED.add(id(basePath));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, path);
    }

    /**
     * 图集拼接完成后缓存基础与图集 sprite。
     */
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
        return CACHE.get(base.contents().name());
    }
}
