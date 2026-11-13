package com.murthinext.ae2pr.item.level_emitter;

import appeng.items.parts.PartItem;

import com.murthinext.ae2pr.block.level_emitter.MultiLevelEmitterPart;

public class MultiLevelEmitterPartItem extends PartItem<MultiLevelEmitterPart> {

    public MultiLevelEmitterPartItem(Properties properties) {
        super(properties, MultiLevelEmitterPart.class, MultiLevelEmitterPart::new);
    }
}
