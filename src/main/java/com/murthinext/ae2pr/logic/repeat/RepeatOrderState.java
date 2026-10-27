package com.murthinext.ae2pr.logic.repeat;

// 重复订单状态机的状态
public enum RepeatOrderState {
    RUNNING,
    PENDING_NEXT,
    PLANNING,
    RETRY_WAIT,
    FAILED,
    DONE,
    CANCELLED
}
