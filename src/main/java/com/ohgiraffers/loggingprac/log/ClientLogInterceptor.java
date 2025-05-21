package com.ohgiraffers.loggingprac.log;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

public class ClientLogInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ClientLogInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = request.getRemoteAddr();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String userAgent = request.getHeader("User-Agent");

        logger.info("Client Request - IP: {}, Method: {}, URI: {}, User-Agent: {}",
                clientIp, method, uri, userAgent);

        return true;
    }
}
