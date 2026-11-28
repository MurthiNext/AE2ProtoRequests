package com.murthinext.ae2pr.cable_part.level_emitter;

import appeng.items.parts.PartItem;

public class MultiLevelEmitterPartItem extends PartItem<MultiLevelEmitterPart> {

    public MultiLevelEmitterPartItem(Properties properties) {
        super(properties, MultiLevelEmitterPart.class, MultiLevelEmitterPart::new);
    }
}
