package com.murthinext.ae2pr.block.machine_part;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import com.murthinext.ae2pr.api.machine.ControllerHost;
import com.murthinext.ae2pr.api.machine.port.ChannelRequest;
import com.murthinext.ae2pr.api.machine.port.MachinePart;
import com.murthinext.ae2pr.api.machine.port.PartInventory;
import com.murthinext.ae2pr.api.recipe.MultiblockRecipe;
import com.murthinext.ae2pr.logic.collapse.CollapseHandler;
import com.murthinext.ae2pr.core.registry.ModBlockEntities;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public final class MEPatternBufferBlockEntity extends BlockEntity
        implements MachinePart, MachinePart.PatternPart, ICraftingProvider {

    static final int ROWS = 4;
    static final int COLUMNS = 9;
    static final int SIZE = ROWS * COLUMNS;
    static final int SHARED_COLUMNS = 3;
    static final int SHARED_SIZE = SHARED_COLUMNS * SHARED_COLUMNS;
    static final int SHARED_TANK_CAPACITY = FluidType.BUCKET_VOLUME;

    private static final String PATTERNS = "patterns";
    private static final String BUFFERS = "buffers";
    private static final String SHARED_ITEMS = "shared_items";
    private static final String SHARED_FLUIDS = "shared_fluids";
    private static final String SLOT = "slot";
    private static final String KEYS = "keys";
    private static final String KEY = "key";
    private static final String AMOUNT = "amount";
    private final ChannelRequest channelRequest = new ChannelRequest(this, 1, new ChannelRequest.Services() {
        @Override
        public void register(IManagedGridNode node) {
            node.addService(ICraftingProvider.class, MEPatternBufferBlockEntity.this);
        }

        @Override
        public void refresh(IManagedGridNode node) {
            ICraftingProvider.requestUpdate(node);
        }
    });
    private final PatternSlot[] buffers = new PatternSlot[SIZE];
    private final ItemStackHandler patterns = new ItemStackHandler(SIZE) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !isLocked(slot) && decode(stack) != null;
        }

        @Override
        protected void onContentsChanged(int slot) {
            buffers[slot].details = decode(getStackInSlot(slot));
            setChanged();
            channelRequest.refreshServices();
        }
    };
    private final ItemStackHandler sharedItems = new ItemStackHandler(SHARED_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final List<FluidTank> sharedFluids = new ArrayList<>(SHARED_SIZE);

    private int nextSlot;

    public MEPatternBufferBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_PATTERN_BUFFER.get(), pos, state);
        for (int i = 0; i < SIZE; i++) {
            buffers[i] = new PatternSlot();
        }
        for (int i = 0; i < SHARED_SIZE; i++) {
            sharedFluids.add(new FluidTank(SHARED_TANK_CAPACITY) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                }
            });
        }
    }

    @Nullable
    private IPatternDetails decode(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return null;
        }
        IPatternDetails details = PatternDetailsHelper.decodePattern(stack, level);
        return details != null && details.supportsPushInputsToExternalInventory() ? details : null;
    }

    ItemStackHandler patterns() {
        return patterns;
    }

    ItemStackHandler sharedItems() {
        return sharedItems;
    }

    FluidTank sharedTank(int slot) {
        return sharedFluids.get(slot);
    }

    boolean isLocked(int slot) {
        return !buffers[slot].keys.isEmpty();
    }

    boolean isOnline() {
        return channelRequest.isOnline();
    }

    @Override
    public ChannelRequest channelRequest() {
        return channelRequest;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            for (int i = 0; i < SIZE; i++) {
                buffers[i].details = decode(patterns.getStackInSlot(i));
            }
            channelRequest.refreshServices();
        }
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        if (!isOnline()) {
            return List.of();
        }
        var available = new LinkedHashSet<IPatternDetails>();
        for (var slot : buffers) {
            if (slot.details != null) {
                available.add(slot.details);
            }
        }
        return List.copyOf(available);
    }

    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
        if (!isOnline()) {
            return false;
        }
        for (int offset = 0; offset < SIZE; offset++) {
            int index = (nextSlot + offset) % SIZE;
            var slot = buffers[index];
            if (details.equals(slot.details)) {
                details.pushInputsToExternalInventory(inputs, slot::add);
                nextSlot = (index + 1) % SIZE;
                setChanged();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBusy() {
        return !isOnline();
    }

    @Override
    public List<BufferedPattern> bufferedPatterns(ControllerHost host) {
        if (!channelRequest.isOnlineFor(host)) {
            return List.of();
        }
        var pending = new ArrayList<BufferedPattern>();
        for (var slot : buffers) {
            if (slot.details != null && !slot.keys.isEmpty()) {
                pending.add(slot);
            }
        }
        return pending;
    }

    void dropContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        var id = BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock());
        for (int i = 0; i < SIZE; i++) {
            Block.popResource(level, worldPosition, patterns.getStackInSlot(i));
            if (isLocked(i)) {
                CollapseHandler.dropBufferedContents(level, worldPosition, id, buffers[i].keys);
            }
        }
        for (int i = 0; i < SHARED_SIZE; i++) {
            Block.popResource(level, worldPosition, sharedItems.getStackInSlot(i));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(PATTERNS, patterns.serializeNBT());
        tag.put(SHARED_ITEMS, sharedItems.serializeNBT());
        if (sharedFluids.stream().anyMatch(tank -> !tank.isEmpty())) {
            var fluids = new ListTag();
            for (FluidTank tank : sharedFluids) {
                fluids.add(tank.writeToNBT(new CompoundTag()));
            }
            tag.put(SHARED_FLUIDS, fluids);
        }
        var list = new ListTag();
        for (int i = 0; i < SIZE; i++) {
            if (!isLocked(i)) {
                continue;
            }
            var entry = new CompoundTag();
            entry.putInt(SLOT, i);
            var keys = new ListTag();
            buffers[i].keys.forEach((key, amount) -> {
                var keyTag = new CompoundTag();
                keyTag.put(KEY, key.toTagGeneric());
                keyTag.putByteArray(AMOUNT, amount.toByteArray());
                keys.add(keyTag);
            });
            entry.put(KEYS, keys);
            list.add(entry);
        }
        tag.put(BUFFERS, list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadItems(tag, PATTERNS, patterns);
        loadItems(tag, SHARED_ITEMS, sharedItems);
        var fluids = tag.getList(SHARED_FLUIDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < SHARED_SIZE; i++) {
            sharedTank(i).setFluid(FluidStack.loadFluidStackFromNBT(fluids.getCompound(i)));
        }
        for (var buffer : buffers) {
            buffer.keys.clear();
            buffer.details = null;
        }
        var list = tag.getList(BUFFERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i);
            int index = entry.getInt(SLOT);
            if (index < 0 || index >= SIZE) {
                continue;
            }
            var keys = entry.getList(KEYS, Tag.TAG_COMPOUND);
            for (int j = 0; j < keys.size(); j++) {
                var keyTag = keys.getCompound(j);
                AEKey key = AEKey.fromTagGeneric(keyTag.getCompound(KEY));
                byte[] amount = keyTag.getByteArray(AMOUNT);
                if (key != null && amount.length > 0) {
                    buffers[index].add(key, new BigInteger(amount));
                }
            }
        }
    }

    private static void loadItems(CompoundTag tag, String name, ItemStackHandler inventory) {
        var saved = tag.getCompound(name).copy();
        saved.putInt("Size", inventory.getSlots());
        inventory.deserializeNBT(saved);
    }

    @Override
    public BlockPos getPartPos() {
        return worldPosition;
    }

    @Override
    public ItemPart items() {
        return null;
    }

    @Override
    public FluidPart fluids() {
        return null;
    }

    @Override
    public NetworkPart network() {
        return null;
    }

    @Override
    public PatternPart patternBuffer() {
        return this;
    }

    @Override
    public boolean isRecipeIsolated() {
        return true;
    }

    private final class PatternSlot implements BufferedPattern {
        private final Map<AEKey, BigInteger> keys = new LinkedHashMap<>();

        @Nullable
        private IPatternDetails details;

        private void add(AEKey key, long amount) {
            add(key, BigInteger.valueOf(amount));
        }

        private void add(AEKey key, BigInteger amount) {
            if (amount.signum() > 0) {
                keys.merge(key, amount, BigInteger::add);
            }
        }

        @Override
        public boolean matchesOutputs(MultiblockRecipe recipe) {
            if (details == null) {
                return false;
            }
            for (var output : details.getOutputs()) {
                AEKey key = output.what();
                if (recipe.getItemOutputs().stream().noneMatch(item -> key.equals(AEItemKey.of(item.stack())))
                        && recipe.getFluidOutputs().stream()
                                .noneMatch(fluid -> key.equals(AEFluidKey.of(fluid.stack())))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean consume(MultiblockRecipe recipe, int parallel, boolean simulate) {
            boolean matched = PartInventory.consumeBuffered(
                    recipe,
                    parallel,
                    keys,
                    sharedItems,
                    sharedFluids,
                    simulate,
                    (key, amount) -> keys.computeIfPresent(key, (ignored, stock) -> {
                        var remaining = stock.subtract(BigInteger.valueOf(amount));
                        return remaining.signum() > 0 ? remaining : null;
                    }));
            if (matched && !simulate) {
                setChanged();
            }
            return matched;
        }
    }
}
