/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.abstraction;

import com.murthinext.ae2pr.requester.Requests;
import net.minecraft.network.chat.Component;

public interface RequestHost {

    void saveChanges();

    void requestChanged(int index);

    boolean isClientSide();

    Requests getRequests();

    Component getTerminalName();
}
