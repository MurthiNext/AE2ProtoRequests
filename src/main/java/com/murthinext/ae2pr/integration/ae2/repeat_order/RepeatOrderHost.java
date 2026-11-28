package com.murthinext.ae2pr.integration.ae2.repeat_order;

import org.jetbrains.annotations.Nullable;

public interface RepeatOrderHost {
    @Nullable
    RepeatOrderContext ae2pr$getContext();

    void ae2pr$setContext(@Nullable RepeatOrderContext context);
}
