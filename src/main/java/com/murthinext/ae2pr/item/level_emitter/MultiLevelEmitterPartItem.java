package com.murthinext.ae2pr.item.level_emitter;

import com.murthinext.ae2pr.block.level_emitter.MultiLevelEmitterPart;

import appeng.items.parts.PartItem;

public class MultiLevelEmitterPartItem extends PartItem<MultiLevelEmitterPart> {

    public MultiLevelEmitterPartItem(Properties properties) {
        super(properties, MultiLevelEmitterPart.class, MultiLevelEmitterPart::new);
    }
}
