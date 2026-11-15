package com.murthinext.ae2pr.mixin.ae2.accessor;

import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.WidgetContainer;
import java.util.Map;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// 读取 WidgetContainer 实际持有的控件表，用于同名控件去重
@Mixin(value = WidgetContainer.class, remap = false)
public interface WidgetContainerAccessor {

    @Accessor("widgets")
    Map<String, AbstractWidget> ae2pr$getWidgets();

    @Accessor("compositeWidgets")
    Map<String, ICompositeWidget> ae2pr$getCompositeWidgets();
}
