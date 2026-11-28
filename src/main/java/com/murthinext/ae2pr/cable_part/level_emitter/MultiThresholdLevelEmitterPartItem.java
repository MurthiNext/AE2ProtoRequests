package com.murthinext.ae2pr.cable_part.level_emitter;

import appeng.items.parts.PartItem;

public class MultiThresholdLevelEmitterPartItem extends PartItem<MultiThresholdLevelEmitterPart> {

    public MultiThresholdLevelEmitterPartItem(Properties properties) {
        super(properties, MultiThresholdLevelEmitterPart.class, MultiThresholdLevelEmitterPart::new);
    }
}
