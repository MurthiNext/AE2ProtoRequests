/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * ME Requester（LGPL-3.0）兼容层，纳入本仓库 LGPL-3.0 声明范围。
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.compat;

import com.murthinext.ae2pr.requester.RedstoneRequesterBlockEntity;

/**
 * 由 {@link RequesterTerminalMenuMixin} 实现的鸭子接口，
 * 供 {@link AbstractRequesterMenuMixin} 通过 {@code id} 解析本模组的红石请求器。
 */
public interface IOurRequesterTerminalHost {

    RedstoneRequesterBlockEntity ae2pr$getOurRequester(long id);
}
