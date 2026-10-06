package com.murthinext.ae2pr.block.meteorite;

import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.DropExperienceBlock;

/**
 * 陨石矿石方块，挖掘时掉落经验。
 */
public class MeteoriteOreBlock extends DropExperienceBlock {

    public MeteoriteOreBlock(Properties properties, IntProvider xpRange) {
        super(properties, xpRange);
    }
}
