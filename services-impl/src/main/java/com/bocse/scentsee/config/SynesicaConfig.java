package com.bocse.scentsee.config;

import com.bocse.scentsee.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Created by bocse on 11.02.2016.
 */
@Configuration
@ComponentScan(basePackages = "com.bocse.scentsee")
public class SynesicaConfig extends WebMvcConfigurerAdapter {

    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        super.addInterceptors(registry);
        registry.addInterceptor(rateLimitInterceptor()).addPathPatterns("/rest/recommendation/**");
        registry.addInterceptor(rateLimitInterceptor()).addPathPatterns("/rest/visualization/**");
        registry.addInterceptor(rateLimitInterceptor()).addPathPatterns("/rest/authentication/**");
        registry.addInterceptor(rateLimitInterceptor()).addPathPatterns("/rest/composition/**");
        registry.addInterceptor(rateLimitInterceptor()).addPathPatterns("/rest/navigation/**");
        registry.addInterceptor(rateLimitInterceptor()).addPathPatterns("/rest/analytics/**");


    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        super.addCorsMappings(registry);
        //registry.addMapping("/greeting-javaconfig").allowedOrigins("*");
    }
//
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry.addResourceHandler("**/*.css", "**/*.js", "**/*.map", "*.html").addResourceLocations("classpath:META-INF/resources/").setCachePeriod(0);
//    }


}