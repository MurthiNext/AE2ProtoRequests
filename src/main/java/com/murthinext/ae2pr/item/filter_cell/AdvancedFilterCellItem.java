package com.murthinext.ae2pr.item.filter_cell;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.items.AEBaseItem;

/**
 * 高级过滤元件：以标签表达式配置白名单与黑名单过滤规则。
 */
public class AdvancedFilterCellItem extends AEBaseItem implements ICellWorkbenchItem, FilterCell {

    public static final String ACTION_SET_WHITELIST = "ae2pr:set_adv_whitelist";
    public static final String ACTION_SET_BLACKLIST = "ae2pr:set_adv_blacklist";

    private static final String WHITELIST_TAG = "Whitelist";
    private static final String BLACKLIST_TAG = "Blacklist";

    private static final Map<String, Optional<TagExpression>> PARSE_CACHE = new ConcurrentHashMap<>();

    public AdvancedFilterCellItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isEditable(ItemStack is) {
        return true;
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack is) {
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public void setFuzzyMode(ItemStack is, FuzzyMode fzMode) {
    }

    public static String getWhitelist(ItemStack cell) {
        return getString(cell, WHITELIST_TAG);
    }

    public static String getBlacklist(ItemStack cell) {
        return getString(cell, BLACKLIST_TAG);
    }

    public static void setWhitelist(ItemStack cell, String value) {
        setString(cell, WHITELIST_TAG, value);
    }

    public static void setBlacklist(ItemStack cell, String value) {
        setString(cell, BLACKLIST_TAG, value);
    }

    /**
     * 校验单条表达式是否合法，空白视为合法。
     */
    public static boolean isValid(@Nullable String text) {
        return text == null || text.isBlank() || parse(text) != null;
    }

    /**
     * 编译为过滤谓词；无表达式时返回 null，白名单非法则全不匹配，黑名单非法则忽略。
     */
    @Nullable
    public static Predicate<AEKey> createPredicate(ItemStack cell) {
        if (!(cell.getItem() instanceof AdvancedFilterCellItem)) {
            return null;
        }

        var whiteText = getWhitelist(cell);
        var blackText = getBlacklist(cell);
        boolean hasWhite = !whiteText.isBlank();
        boolean hasBlack = !blackText.isBlank();
        if (!hasWhite && !hasBlack) {
            return null;
        }

        var white = hasWhite ? parse(whiteText) : null;
        var black = hasBlack ? parse(blackText) : null;
        return key -> {
            if (hasWhite && (white == null || !white.test(key))) {
                return false;
            }
            return !hasBlack || black == null || !black.test(key);
        };
    }

    @Nullable
    private static TagExpression parse(String text) {
        if (PARSE_CACHE.size() > 512) {
            PARSE_CACHE.clear();
        }
        return PARSE_CACHE.computeIfAbsent(text, t -> Optional.ofNullable(TagExpression.parse(t))).orElse(null);
    }

    private static String getString(ItemStack cell, String key) {
        var tag = cell.getTag();
        return tag != null ? tag.getString(key) : "";
    }

    private static void setString(ItemStack cell, String key, @Nullable String value) {
        if (value == null || value.isBlank()) {
            var tag = cell.getTag();
            if (tag != null) {
                tag.remove(key);
            }
        } else {
            cell.getOrCreateTag().putString(key, value);
        }
    }
}
