package com.lencode.paper.importer.vo;

public class ArxivImportResponse {

    private final int requested;
    private final int imported;
    private final int skipped;
    private final int failed;
    private final String message;

    public ArxivImportResponse(int requested, int imported, int skipped, int failed, String message) {
        this.requested = requested;
        this.imported = imported;
        this.skipped = skipped;
        this.failed = failed;
        this.message = message;
    }

    public int getRequested() {
        return requested;
    }

    public int getImported() {
        return imported;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getFailed() {
        return failed;
    }

    public String getMessage() {
        return message;
    }
}
