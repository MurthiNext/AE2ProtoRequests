package com.murthinext.ae2pr.api.machine;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.api.machine.Mode;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;

// 带工作模式的控制器，模式列表由机器定义提供
public abstract class ModeMachineBlockEntity<T extends Enum<T> & Mode>
        extends AbstractMultiblockControllerBlockEntity {

    private static final String MODE_ID = "mode";

    private final List<T> modes;
    private T mode;

    protected ModeMachineBlockEntity(MultiblockMachineDefinition<?, ?> definition, BlockPos pos, BlockState state,
            List<T> modes) {
        super(definition, pos, state);
        this.modes = List.copyOf(modes);
        this.mode = this.modes.get(0);
    }

    public final T getMode() {
        return mode;
    }

    @Override
    protected final RecipeType<?> recipeType() {
        return mode.recipeType();
    }

    // 作业期间禁止切换，避免配置与已扣料作业不一致
    public boolean switchMode() {
        if (level == null || level.isClientSide || isBusy()) {
            return false;
        }
        int next = modes.indexOf(mode) + 1;
        mode = modes.get(next >= modes.size() ? 0 : next);
        clearPause();
        markStateChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(MODE_ID, mode.id());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        String id = tag.getString(MODE_ID);
        for (T candidate : modes) {
            if (candidate.id().equals(id)) {
                mode = candidate;
                return;
            }
        }
        mode = modes.get(0);
    }
}
