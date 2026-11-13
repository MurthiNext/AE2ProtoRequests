package com.murthinext.ae2pr.item.level_emitter;

import appeng.items.parts.PartItem;

import com.murthinext.ae2pr.block.level_emitter.MultiThresholdLevelEmitterPart;

public class MultiThresholdLevelEmitterPartItem extends PartItem<MultiThresholdLevelEmitterPart> {

    public MultiThresholdLevelEmitterPartItem(Properties properties) {
        super(properties, MultiThresholdLevelEmitterPart.class, MultiThresholdLevelEmitterPart::new);
    }
}
