package com.murthinext.ae2pr.requester.abstraction;

import net.minecraft.network.chat.Component;

import com.murthinext.ae2pr.requester.Requests;

public interface RequestHost {

    void saveChanges();

    void requestChanged(int index);

    boolean isClientSide();

    Requests getRequests();

    Component getTerminalName();
}
