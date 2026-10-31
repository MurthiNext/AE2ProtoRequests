package com.murthinext.ae2pr.machine;

public interface ModeMachine<T extends Enum<T> & Mode> {
    T getMode();

    boolean switchMode();
}
