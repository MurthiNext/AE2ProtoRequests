package com.murthinext.ae2pr.item.cell.collapsed;

import appeng.api.stacks.GenericStack;
import java.util.List;

public interface Collapsible {

    List<GenericStack> readContents();

    void clearContents();
}
