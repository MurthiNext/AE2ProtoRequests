package com.murthinext.ae2pr.integration.ae2.repeat_order;

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
