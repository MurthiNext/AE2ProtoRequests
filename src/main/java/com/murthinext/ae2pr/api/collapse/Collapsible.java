package com.murthinext.ae2pr.api.collapse;

import java.util.List;

import appeng.api.stacks.GenericStack;

public interface Collapsible {

    List<GenericStack> readContents();

    void clearContents();
}
