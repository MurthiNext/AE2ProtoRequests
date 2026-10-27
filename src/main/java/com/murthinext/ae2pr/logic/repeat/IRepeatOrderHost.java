package com.murthinext.ae2pr.logic.repeat;

import org.jetbrains.annotations.Nullable;

public interface IRepeatOrderHost {
    @Nullable
    RepeatOrderContext ae2pr$getContext();

    void ae2pr$setContext(@Nullable RepeatOrderContext context);
}
