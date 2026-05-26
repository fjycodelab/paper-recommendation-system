package com.lencode.paper.health;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class HealthResponse {

    private String status;
    private String mysql;
    private String redis;
    private String pythonInfer;
    private Map<String, String> details;
    private Instant checkedAt;

    public HealthResponse() {
        this.details = new LinkedHashMap<>();
    }

    public HealthResponse(String status, String mysql, String redis, String pythonInfer,
                          Map<String, String> details, Instant checkedAt) {
        this.status = status;
        this.mysql = mysql;
        this.redis = redis;
        this.pythonInfer = pythonInfer;
        this.details = details;
        this.checkedAt = checkedAt;
    }

    public static HealthResponse from(ComponentHealth mysql, ComponentHealth redis, ComponentHealth pythonInfer) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("mysql", mysql.getDetail());
        details.put("redis", redis.getDetail());
        details.put("pythonInfer", pythonInfer.getDetail());

        String overall = resolveOverallStatus(mysql, redis, pythonInfer);
        return new HealthResponse(
                overall,
                mysql.getStatus(),
                redis.getStatus(),
                pythonInfer.getStatus(),
                details,
                Instant.now()
        );
    }

    private static String resolveOverallStatus(ComponentHealth mysql, ComponentHealth redis,
                                               ComponentHealth pythonInfer) {
        if (!mysql.isUp() || !redis.isUp()) {
            return "DOWN";
        }
        if (!pythonInfer.isUp()) {
            return "DEGRADED";
        }
        return "UP";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMysql() {
        return mysql;
    }

    public void setMysql(String mysql) {
        this.mysql = mysql;
    }

    public String getRedis() {
        return redis;
    }

    public void setRedis(String redis) {
        this.redis = redis;
    }

    public String getPythonInfer() {
        return pythonInfer;
    }

    public void setPythonInfer(String pythonInfer) {
        this.pythonInfer = pythonInfer;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }
}

