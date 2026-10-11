package com.murthinext.ae2pr.network;

/**
 * 单个合成 CPU 的重复订单展示数据。
 */
public record CpuRoundInfo(int serial, int round, int total) {
}
