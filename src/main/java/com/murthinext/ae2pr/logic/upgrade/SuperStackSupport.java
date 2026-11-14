package com.murthinext.ae2pr.logic.upgrade;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import appeng.helpers.InterfaceLogic;
import appeng.helpers.externalstorage.GenericStackInv;

import com.murthinext.ae2pr.registry.ModItems;

/**
 * 超级堆叠卡的接入点：登记可用机器，并把返回/存储槽的容量放大判定绑到宿主上。
 * <p>
 * 附属自带接口/供应器实现，这里一律通过反射探测其升级库存。
 */
public final class SuperStackSupport {

    // 目标上限
    public static final long BOOSTED_AMOUNT = Integer.MAX_VALUE;
    public static final String NBT_PROVIDER_UPGRADES = "ae2pr_upgrades";

    private static final String INTERFACE_GROUP = "group.ae2pr.interface.name";
    private static final String PROVIDER_GROUP = "group.ae2pr.pattern_provider.name";

    private static final String AE2CS = "ae2cs";
    private static final String EXTENDED_AE = "expatternprovider";
    private static final String ADVANCED_AE = "advanced_ae";

    // 已登记的宿主机器物品
    private static final Set<Item> SUPPORTED_MACHINES = ConcurrentHashMap.newKeySet();

    // 升级槽数量由宿主界面自行绘制的供应器物品，不允许本模组再扩容
    private static final Set<Item> PROVIDER_MACHINES = ConcurrentHashMap.newKeySet();

    // 视作返回/存储槽的库存字段名
    private static final List<String> STORAGE_FIELDS = List.of("returnInv", "storage", "storageInv");

    private static final String[] AE2CS_INTERFACES = {
        "ender_interface",           "extended_ender_interface",
        "integrated_interface",      "extended_integrated_interface",
        "ender_interface_part",      "extended_ender_interface_part",
        "integrated_interface_part", "extended_integrated_interface_part",
    };

    private static final String[] AE2CS_PROVIDERS = {
        "resonating_pattern_provider",
        "extended_resonating_pattern_provider",
        "simple_pattern_provider",
        "mirror_pattern_provider",
        "meteorite_pattern_provider",
        "resonating_pattern_provider_part",
        "extended_resonating_pattern_provider_part",
        "simple_pattern_provider_part",
        "mirror_pattern_provider_part",
        "meteorite_pattern_provider_part",
    };

    private static final String[] EXTENDED_AE_INTERFACES = {
        "ex_interface",
        "ex_interface_part",
        "oversize_interface",
        "oversize_interface_part",
    };

    private static final String[] EXTENDED_AE_PROVIDERS = { "ex_pattern_provider", "ex_pattern_provider_part" };

    private static final String[] ADVANCED_AE_PROVIDERS = {
        "adv_pattern_provider",
        "adv_pattern_provider_part",
        "small_adv_pattern_provider",
        "small_adv_pattern_provider_part",
    };

    private static final Map<Class<?>, Field[]> UPGRADE_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field[]> STORAGE_FIELDS_BY_TYPE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> LISTENER_FIELDS = new ConcurrentHashMap<>();

    /** 由 GenericStackInv 的 Mixin 实现，把宿主存到库存自身。 */
    public interface Target {
        void ae2pr$setOwner(@Nullable Object owner);

        boolean ae2pr$isManaged();

        /** 仅客户端同步时直写原始槽位。 */
        void ae2pr$setSyncedStack(int slot, @Nullable GenericStack stack);

        boolean ae2pr$isBoosted(AEKeyType type);
    }

    /** 由本模组对宿主升级库存的扩容 Mixin 实现：原地追加一个槽位。 */
    public interface AppendableUpgrades {
        void ae2pr$appendUpgradeSlot();
    }

    /** 由 PatternProviderLogic 的 Mixin 实现，暴露本模组补出的升级库存。 */
    public interface ProviderUpgrades {
        IUpgradeInventory ae2pr$getProviderUpgrades();
    }

    private SuperStackSupport() {
    }

    public static void registerUpgrades() {
        ItemLike card = ModItems.SUPER_STACK_CARD.get();
        registerSupported(card, AEBlocks.INTERFACE, INTERFACE_GROUP);
        registerSupported(card, AEParts.INTERFACE, INTERFACE_GROUP);
        registerSupported(card, AEBlocks.PATTERN_PROVIDER, PROVIDER_GROUP);
        registerSupported(card, AEParts.PATTERN_PROVIDER, PROVIDER_GROUP);

        if (ModList.get().isLoaded(AE2CS)) {
            for (var id : AE2CS_INTERFACES) {
                registerModItem(card, AE2CS, id, INTERFACE_GROUP);
            }
            for (var id : AE2CS_PROVIDERS) {
                registerModItem(card, AE2CS, id, PROVIDER_GROUP);
            }
        }
        if (ModList.get().isLoaded(EXTENDED_AE)) {
            for (var id : EXTENDED_AE_INTERFACES) {
                registerModItem(card, EXTENDED_AE, id, INTERFACE_GROUP);
            }
            for (var id : EXTENDED_AE_PROVIDERS) {
                registerModItem(card, EXTENDED_AE, id, PROVIDER_GROUP);
            }
        }
        if (ModList.get().isLoaded(ADVANCED_AE)) {
            for (var id : ADVANCED_AE_PROVIDERS) {
                registerModItem(card, ADVANCED_AE, id, PROVIDER_GROUP);
            }
        }
    }

    private static void registerSupported(ItemLike card, ItemLike machine, String group) {
        Upgrades.add(card, machine, 1, group);
        SUPPORTED_MACHINES.add(machine.asItem());
        if (PROVIDER_GROUP.equals(group)) {
            PROVIDER_MACHINES.add(machine.asItem());
        }
    }

    private static void registerModItem(ItemLike card, String modId, String path, String group) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(modId, path));
        if (item != null && item != Items.AIR) {
            registerSupported(card, item, group);
        }
    }

    public static boolean isSupportedMachine(@Nullable ItemLike machine) {
        return machine != null && SUPPORTED_MACHINES.contains(machine.asItem());
    }

    public static boolean isProviderMachine(@Nullable ItemLike machine) {
        return machine != null && PROVIDER_MACHINES.contains(machine.asItem());
    }

    public static void bind(@Nullable GenericStackInv inventory, @Nullable Object owner) {
        if (inventory instanceof Target target && owner != null) {
            target.ae2pr$setOwner(owner);
        }
    }

    /**
     * 兜底：从库存自身的监听器反查宿主。
     * {@link GenericStackInv} 的监听器多为 {@code this::onStorageChanged} 这类绑定方法引用，
     * 其捕获对象即宿主；再确认该宿主确实声明了这个库存字段。
     */
    @Nullable
    public static Object resolveStorageOwner(GenericStackInv inventory) {
        var listenerField = listenerField(inventory.getClass());
        if (listenerField == null) {
            return null;
        }
        Object listener;
        try {
            listener = listenerField.get(inventory);
        } catch (Throwable ignored) {
            return null;
        }
        if (!(listener instanceof Runnable runnable)) {
            return null;
        }
        for (Field field : runnable.getClass().getDeclaredFields()) {
            var name = field.getName();
            if (!name.startsWith("arg$") && !name.equals("this$0")) {
                continue;
            }
            try {
                field.setAccessible(true);
                var owner = field.get(runnable);
                if (owner != null && owner != runnable && ownsStorage(owner, inventory)) {
                    return owner;
                }
            } catch (Throwable ignored) {
                // 跳过不可访问的字段
            }
        }
        return null;
    }

    public static boolean isCardInstalled(@Nullable IUpgradeInventory upgrades) {
        return upgrades != null && upgrades.isInstalled(ModItems.SUPER_STACK_CARD.get());
    }

    public static boolean hasCard(@Nullable Object owner) {
        return isCardInstalled(upgradesOf(owner));
    }

    /** 解析宿主真正生效的升级库存。 */
    @Nullable
    public static IUpgradeInventory upgradesOf(@Nullable Object owner) {
        if (owner == null) {
            return null;
        }
        if (owner instanceof IUpgradeableObject upgradable) {
            try {
                var inventory = upgradable.getUpgrades();
                if (isRealInventory(inventory)) {
                    return inventory;
                }
            } catch (Throwable ignored) {
                // 升级库存尚未初始化时继续字段探测
            }
        }
        for (var field : upgradeFields(owner.getClass())) {
            // 跳过本模组 mixin 自己的 fallback 字段
            if (field.getName().startsWith("ae2pr$")) {
                continue;
            }
            try {
                if (field.get(owner) instanceof IUpgradeInventory inventory && isRealInventory(inventory)) {
                    return inventory;
                }
            } catch (Throwable ignored) {
                // 跳过不可访问的字段
            }
        }
        if (owner instanceof ProviderUpgrades provider) {
            var inventory = provider.ae2pr$getProviderUpgrades();
            if (isRealInventory(inventory)) {
                return inventory;
            }
        }
        return null;
    }

    private static boolean isRealInventory(@Nullable IUpgradeInventory inventory) {
        return inventory != null && inventory != UpgradeInventories.empty();
    }

    public static IUpgradeInventory providerUpgrades(@Nullable Object logic) {
        var inventory = upgradesOf(logic);
        return inventory != null ? inventory : UpgradeInventories.empty();
    }

    public static void appendUpgradeSlot(IUpgradeInventory inventory) {
        if (inventory instanceof AppendableUpgrades appendable) {
            appendable.ae2pr$appendUpgradeSlot();
        }
    }

    private static boolean ownsStorage(Object owner, GenericStackInv inventory) {
        // 附属可能替换掉接口的 config 实例，按当前 getter 取值判定归属
        if (owner instanceof InterfaceLogic logic && logic.getConfig() == inventory) {
            return true;
        }
        for (var field : storageFields(owner.getClass())) {
            try {
                if (field.get(owner) == inventory) {
                    return true;
                }
            } catch (Throwable ignored) {
                // 跳过不可访问的字段
            }
        }
        return false;
    }

    @Nullable
    private static Field listenerField(Class<?> type) {
        var cached = LISTENER_FIELDS.get(type);
        if (cached != null) {
            return cached;
        }
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField("listener");
                field.setAccessible(true);
                LISTENER_FIELDS.put(type, field);
                return field;
            } catch (Throwable ignored) {
                // 继续查找父类
            }
        }
        return null;
    }

    private static Field[] upgradeFields(Class<?> type) {
        return UPGRADE_FIELDS.computeIfAbsent(type, key -> collect(key, IUpgradeInventory.class));
    }

    private static Field[] storageFields(Class<?> type) {
        return STORAGE_FIELDS_BY_TYPE.computeIfAbsent(type, key -> {
            var fields = new ArrayList<Field>();
            for (var field : collect(key, GenericStackInv.class)) {
                if (STORAGE_FIELDS.contains(field.getName())) {
                    fields.add(field);
                }
            }
            return fields.toArray(new Field[0]);
        });
    }

    private static Field[] collect(Class<?> type, Class<?> wanted) {
        var fields = new ArrayList<Field>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!wanted.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                fields.add(field);
            }
        }
        return fields.toArray(new Field[0]);
    }
}
