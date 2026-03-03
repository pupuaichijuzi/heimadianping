package com.puffyna.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.puffyna.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RefreshTokenInteceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInteceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //TODO 1.从redis中获取用户
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            //不存在，放行，拦截操作不在这个拦截器中进行
            return true;
        }

        //TODO 2.基于token获取redis的用户
        String key=RedisConstants.LOGIN_USER_KEY+token;
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(key);
        if(map.isEmpty()){
            return true;
        }
        //TODO 3.将map转化成为UserDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);
        //4.用户存在，将用户保存到ThreadLocal中
        UserHolder.saveUser(userDTO);
        //TODO 5.修改过期时间
        stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //6.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
