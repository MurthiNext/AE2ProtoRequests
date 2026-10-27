package com.murthinext.ae2pr.logic.repeat;

import org.jetbrains.annotations.Nullable;

import appeng.api.networking.crafting.CraftingSubmitErrorCode;

public enum FailureReason {
    MISSING_MATERIAL,
    CPU_TOO_SMALL,
    CPU_BUSY,
    CPU_OFFLINE,
    NO_CPU,
    UNKNOWN;

    public static FailureReason fromCode(@Nullable CraftingSubmitErrorCode code) {
        if (code == null) {
            return UNKNOWN;
        }
        return switch (code) {
            case CPU_TOO_SMALL -> CPU_TOO_SMALL;
            case CPU_BUSY -> CPU_BUSY;
            case CPU_OFFLINE -> CPU_OFFLINE;
            case NO_CPU_FOUND, NO_SUITABLE_CPU_FOUND -> NO_CPU;
            case INCOMPLETE_PLAN, MISSING_INGREDIENT -> MISSING_MATERIAL;
        };
    }
}
