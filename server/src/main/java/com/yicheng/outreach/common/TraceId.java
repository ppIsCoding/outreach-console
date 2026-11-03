package com.yicheng.outreach.common;

import java.util.UUID;

/**
 * 请求链路 ID 的线程上下文。
 */
public final class TraceId {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private TraceId() {
    }

    public static void set(String traceId) {
        HOLDER.set(traceId);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static String current() {
        String traceId = HOLDER.get();
        if (traceId == null) {
            traceId = generate();
            HOLDER.set(traceId);
        }
        return traceId;
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
