package com.murthinext.ae2pr.api.machine;

import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

// 带工作模式的控制器，模式列表由机器定义提供
public abstract class ModeMachineBlockEntity<T extends Enum<T> & Mode> extends AbstractMultiblockControllerBlockEntity {

    private static final String MODE_ID = "mode";

    private final T[] modes;
    private T mode;

    @SuppressWarnings("unchecked")
    protected ModeMachineBlockEntity(MultiblockMachineDefinition<?> definition, BlockPos pos, BlockState state) {
        super(definition, pos, state);
        this.modes = (T[]) definition.modeType().getEnumConstants();
        this.mode = modes[0];
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
        int next = mode.ordinal() + 1;
        mode = modes[next >= modes.length ? 0 : next];
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
        mode = modes[0];
    }
}
