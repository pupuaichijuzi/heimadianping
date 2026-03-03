package com.puffyna.config;

import com.puffyna.utils.LoginInteceptor;
import com.puffyna.utils.RefreshTokenInteceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //登录拦截器
        registry.addInterceptor(new LoginInteceptor())
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/shop/**",
                        "shop-type/**",
                        "/blog/hot",
                        "/voucher/**",
                        "upload/**"
                ).order(1);
        //刷新拦截器
        registry.addInterceptor(new RefreshTokenInteceptor(stringRedisTemplate)).order(0);
    }
}
