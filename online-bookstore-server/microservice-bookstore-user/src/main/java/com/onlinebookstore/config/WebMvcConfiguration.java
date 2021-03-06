package com.onlinebookstore.config;

import com.alibaba.cloud.seata.web.SeataHandlerInterceptor;
import com.onlinebookstore.handler.JsonObjectArgResolverHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * SeataHandlerInterceptorConfiguration#addInterceptors
 * @author rkc
 * @date 2020/11/7 22:11
 * @version 1.0
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        //注册JsonObjectArgResolverHandler处理类
        WebMvcConfigurer.super.addArgumentResolvers(resolvers);
        resolvers.add(new JsonObjectArgResolverHandler());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SeataHandlerInterceptor()).addPathPatterns("/**");
    }
}
