package com.puffyna.utils;

import com.puffyna.dto.UserDTO;
import com.puffyna.entity.User;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInteceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.从session中获取用户
        HttpSession session = request.getSession();
        Object user = session.getAttribute("user");
        //2.如果用户不存在，进行拦截
        if(user==null){
            response.setStatus(401);
            return false;
        }
        //3.用户存在，将用户保存到ThreadLocal中
        UserHolder.saveUser((UserDTO) user);
        //4.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
