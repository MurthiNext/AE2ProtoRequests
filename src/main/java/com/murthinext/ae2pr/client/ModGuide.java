package com.murthinext.ae2pr.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import guideme.Guide;
import guideme.GuidesCommon;
import guideme.PageAnchor;

import com.murthinext.ae2pr.ae2pr;

/**
 * 本模组的 GuideME 指南（仅客户端）。
 * <p>
 * 页面资源位于 {@code assets/ae2pr/ae2guide/}，页面 id 形如 {@code ae2pr:index.md}。
 */
public final class ModGuide {

    public static final ResourceLocation GUIDE_ID = new ResourceLocation(ae2pr.MODID, "guide");
    public static final ResourceLocation INDEX_PAGE = new ResourceLocation(ae2pr.MODID, "index.md");
    public static final ResourceLocation EMITTER_PAGE = new ResourceLocation(ae2pr.MODID,
            "devices/multi_level_emitter.md");
    public static final ResourceLocation CRYSTAL_ASSEMBLY_LINE_PAGE = new ResourceLocation(ae2pr.MODID,
            "multiblocks/crystal_assembly_line.md");
    public static final ResourceLocation LAVA_SMELTER_PAGE = new ResourceLocation(ae2pr.MODID,
            "multiblocks/high_reactivity_lava_smelter.md");
    public static final ResourceLocation NAMING_FACTORY_PAGE = new ResourceLocation(ae2pr.MODID,
            "machines/naming_factory.md");
    public static final ResourceLocation CHAMBERS_PAGE = new ResourceLocation(ae2pr.MODID,
            "multiblocks/chambers.md");

    private static Guide guide;

    private ModGuide() {
    }

    /** 在模组构造阶段（客户端）调用，构建并注册指南。 */
    public static void init() {
        if (guide == null) {
            guide = Guide.builder(GUIDE_ID)
                    .folder("ae2guide")
                    .defaultLanguage("zh_cn")
                    .build();
        }
    }

    /** 打开指南并跳转到指定页面（可选锚点）。 */
    public static void open(PageAnchor anchor) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            GuidesCommon.openGuide(player, GUIDE_ID, anchor);
        }
    }

    public static void openAt(ResourceLocation pageId) {
        open(PageAnchor.page(pageId));
    }
}
