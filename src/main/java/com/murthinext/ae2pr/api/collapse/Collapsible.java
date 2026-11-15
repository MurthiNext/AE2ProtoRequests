package com.murthinext.ae2pr.api.collapse;

import appeng.api.stacks.GenericStack;
import java.util.List;

public interface Collapsible {

    List<GenericStack> readContents();

    void clearContents();
}
