package com.devhub.ocr.app.systems.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import com.devhub.ocr.app.systems.config.AuthorizationInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve files under the project 'uploads' directory at URL path /uploads/**
        // Maps requests like /uploads/example.png -> file:uploads/example.png
    registry.addResourceHandler("/uploads/**")
        // Use an explicit relative path to the uploads directory
        .addResourceLocations("file:./uploads/")
                .setCachePeriod(3600);
    }

    @Autowired
    private AuthorizationInterceptor authorizationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
    // Theme interceptor: handle ?theme=dark|light, persist it in a cookie and redirect
    registry.addInterceptor(new ThemeInterceptor())
        .addPathPatterns("/**")
        .excludePathPatterns("/css/**", "/js/**", "/images/**", "/auth/**", "/uploads/**", "/error", "/");

        // run authorization interceptor for application routes
        registry.addInterceptor(authorizationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/auth/**", "/uploads/**", "/error", "/");
    }
}
