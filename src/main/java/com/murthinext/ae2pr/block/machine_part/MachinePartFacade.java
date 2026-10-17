package com.murthinext.ae2pr.block.machine_part;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;

/**
 * 多方块部件的成型外观注册表：STYLE → 贴图 + 连接族。
 */
public final class MachinePartFacade {

    private static final Map<Integer, MachinePartFacade> BY_STYLE = new HashMap<>();

    public static final MachinePartFacade CRYSTAL = register(1,
            "block/crystal_reinforced_composite_machine_casing", 1);

    public static final MachinePartFacade ZIRCONIA = register(2,
            "block/zirconia_corundum_bricks", 4);

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
                ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, texturePath), ctmFamily);
        BY_STYLE.put(style, facade);
        return facade;
    }

    @Nullable
    public static MachinePartFacade byStyle(int style) {
        return BY_STYLE.get(style);
    }

    public static Collection<MachinePartFacade> all() {
        return BY_STYLE.values();
    }

    public int style() {
        return style;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public int ctmFamily() {
        return ctmFamily;
    }
}
