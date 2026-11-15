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
