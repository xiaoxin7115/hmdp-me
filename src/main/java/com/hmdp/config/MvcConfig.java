package com.hmdp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @author xiaoxin
 * @version 1.0
 * @date 2023/2/26 9:37
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())//通过构造方法、函数传递参数，使得拦截器能获得reids模板对象
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/blog/hot",
                        "/shop/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/voucher/**"
                ).order(1);//RefreshTokenInterceptor 先于 LoginInterceptor 执行  值越大，优先级越低
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).order(0);//默认拦截所有请求
    }
}
