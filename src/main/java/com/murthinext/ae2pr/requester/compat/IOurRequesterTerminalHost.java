package com.murthinext.ae2pr.requester.compat;

import org.jetbrains.annotations.Nullable;

import com.murthinext.ae2pr.requester.RedstoneRequesterBlockEntity;

/**
 * 由 {@code RequesterTerminalMenuMixin} 实现的鸭子接口，
 * 供 {@code AbstractRequesterMenuMixin} 通过 {@code id} 解析本模组的红石请求器。
 */
public interface IOurRequesterTerminalHost {

    @Nullable
    RedstoneRequesterBlockEntity ae2pr$getOurRequester(long id);
}
