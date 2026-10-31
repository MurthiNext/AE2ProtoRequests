package com.murthinext.ae2pr.machine.multi_factory;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.machine.ModeMachine;
import com.murthinext.ae2pr.logic.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.recipe.MultiblockRecipe;

public class MultiFactoryControllerBlockEntity extends AbstractMultiblockControllerBlockEntity
        implements ModeMachine<MultiFactoryMode> {

    private static final String MODE_ID = "mode";

    private MultiFactoryMode mode = MultiFactoryMode.INSCRIBING;

    public MultiFactoryControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.MULTI_FACTORY, pos, state);
    }

    @Override
    protected RecipeType<?> recipeType() {
        return mode.recipeType();
    }

    @Override
    public MultiFactoryMode getMode() {
        return mode;
    }

    // 作业期间禁止切换，避免配置与已扣料作业不一致
    @Override
    public boolean switchMode() {
        if (level == null || level.isClientSide || isBusy()) {
            return false;
        }
        mode = mode.next();
        clearPauseState();
        markStateChanged();
        return true;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<MultiblockRecipe> recipes() {
        if (level == null) {
            return List.of();
        }
        List<MultiblockRecipe> wrapped = new ArrayList<>();
        List<? extends Recipe<?>> all = (List<? extends Recipe<?>>) (List<?>) level.getRecipeManager()
                .getAllRecipesFor((RecipeType) mode.recipeType());
        for (Recipe<?> recipe : all) {
            MultiFactoryRecipe adapter = MultiFactoryRecipe.wrap(recipe, mode);
            if (adapter != null) {
                wrapped.add(adapter);
            }
        }
        wrapped.sort(AbstractMultiblockControllerBlockEntity::compareRecipePriority);
        return wrapped;
    }

    @Override
    @Nullable
    protected MultiblockRecipe resolveRecipe(ResourceLocation id) {
        if (level == null) {
            return null;
        }
        return MultiFactoryRecipe.wrap(level.getRecipeManager().byKey(id).orElse(null), mode);
    }

    @Override
    protected boolean isRecipeApplicable(MultiblockRecipe recipe) {
        return recipe instanceof MultiFactoryRecipe wrapped && wrapped.mode() == mode;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(MODE_ID, mode.id());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mode = MultiFactoryMode.byId(tag.getString(MODE_ID));
    }

    @Override
    public BlockPos getCorePos() {
        return MultiFactoryStructure.corePos(worldPosition, getFacing(), mirrorFront);
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos core = getCorePos();
        return new AABB(core).inflate(2.0);
    }
}
