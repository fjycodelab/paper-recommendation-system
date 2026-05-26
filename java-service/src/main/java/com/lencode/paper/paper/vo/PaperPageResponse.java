package com.lencode.paper.paper.vo;

import java.util.List;

public class PaperPageResponse {

    private final List<PaperResponse> items;
    private final long total;
    private final int page;
    private final int pageSize;

    public PaperPageResponse(List<PaperResponse> items, long total, int page, int pageSize) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<PaperResponse> getItems() {
        return items;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }
}

