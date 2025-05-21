package com.ohgiraffers.loggingprac.log;

import java.time.LocalDateTime;

public class LogEntry {
    private LocalDateTime timestamp;
    private String clientIp;
    private String method;
    private String uri;
    private String userAgent;

    public LogEntry(LocalDateTime timestamp, String clientIp, String method, String uri, String userAgent) {
        this.timestamp = timestamp;
        this.clientIp = clientIp;
        this.method = method;
        this.uri = uri;
        this.userAgent = userAgent;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "timestamp=" + timestamp +
                ", clientIp='" + clientIp + '\'' +
                ", method='" + method + '\'' +
                ", uri='" + uri + '\'' +
                ", userAgent='" + userAgent + '\'' +
                '}';
    }
}
