package com.ohgiraffers.loggingprac.config;

import com.ohgiraffers.loggingprac.log.ClientLogInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public ClientLogInterceptor clientLogInterceptor() {
        return new ClientLogInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clientLogInterceptor())
                .addPathPatterns("/**");
    }
}
