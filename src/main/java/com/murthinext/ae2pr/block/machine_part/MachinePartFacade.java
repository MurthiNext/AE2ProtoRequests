package com.murthinext.ae2pr.block.machine_part;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;

/**
 * 机器部件的多方块成型外观注册表：STYLE 值 → 基础贴图 + 连接族。
 * <p>
 * 结构成型时把部件的 STYLE 设为对应 id；渲染端（见 {@code CtmConfig} 与 {@code CtmBakedModel}）
 * 在成型状态下把基础面贴图换成该外观，并按连接族继续绘制连接纹理。
 */
public final class MachinePartFacade {

    private static final Map<Integer, MachinePartFacade> BY_STYLE = new HashMap<>();

    /** 水晶装配线：水晶强化复合机械方块（连接族 1） */
    public static final MachinePartFacade CRYSTAL = register(1,
            "block/crystal/crystal_reinforced_composite_machine_casing", 1);

    /** 高反应性熔岩冶炼炉：锆刚玉砖块（连接族 4） */
    public static final MachinePartFacade ZIRCONIA = register(2,
            "block/zircon/zirconia_corundum_bricks", 4);

    /** STYLE 属性上限：覆盖全部已登记外观 */
    public static final int MAX_STYLE = BY_STYLE.keySet().stream().mapToInt(Integer::intValue).max().orElse(1);

    private final int style;
    private final ResourceLocation texture;
    private final int ctmFamily;

    private MachinePartFacade(int style, ResourceLocation texture, int ctmFamily) {
        this.style = style;
        this.texture = texture;
        this.ctmFamily = ctmFamily;
    }

    private static MachinePartFacade register(int style, String texturePath, int ctmFamily) {
        MachinePartFacade facade = new MachinePartFacade(style,
                new ResourceLocation(ae2pr.MODID, texturePath), ctmFamily);
        BY_STYLE.put(style, facade);
        return facade;
    }

    /** 按 STYLE 值取外观；未登记返回 null。 */
    @Nullable
    public static MachinePartFacade byStyle(int style) {
        return BY_STYLE.get(style);
    }

    /** 已登记的全部外观（渲染端据此自动登记贴图）。 */
    public static Collection<MachinePartFacade> all() {
        return BY_STYLE.values();
    }

    public int style() {
        return style;
    }

    /** 成型后基础面的贴图。 */
    public ResourceLocation texture() {
        return texture;
    }

    /** 成型后的连接纹理族（见 {@code CtmConfig}）。 */
    public int ctmFamily() {
        return ctmFamily;
    }
}
