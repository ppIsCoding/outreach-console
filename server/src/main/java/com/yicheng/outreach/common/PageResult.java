package com.yicheng.outreach.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应结构：{@code {items, page, size, total, totalPages}}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> items;
    private int page;
    private int size;
    private long total;
    private int totalPages;

    public static <T> PageResult<T> of(List<T> items, int page, int size, long total) {
        int totalPages = size <= 0 ? 0 : (int) ((total + size - 1) / size);
        return new PageResult<>(items, page, size, total, totalPages);
    }
}
