package com.murthinext.ae2pr.item.level_emitter;

import com.murthinext.ae2pr.block.level_emitter.MultiThresholdLevelEmitterPart;

import appeng.items.parts.PartItem;

public class MultiThresholdLevelEmitterPartItem extends PartItem<MultiThresholdLevelEmitterPart> {

    public MultiThresholdLevelEmitterPartItem(Properties properties) {
        super(properties, MultiThresholdLevelEmitterPart.class, MultiThresholdLevelEmitterPart::new);
    }
}
