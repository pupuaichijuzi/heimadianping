package com.puffyna.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puffyna.dto.LoginFormDTO;
import com.puffyna.dto.Result;
import com.puffyna.dto.UserDTO;
import com.puffyna.entity.User;
import com.puffyna.mapper.UserMapper;
import com.puffyna.service.IUserService;
import com.puffyna.utils.RedisConstants;
import com.puffyna.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.puffyna.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.puffyna.utils.RedisConstants.LOGIN_CODE_TTL;
import static com.puffyna.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号
        boolean invalid = RegexUtils.isPhoneInvalid(phone);
        if(!invalid){
            //2.不符合，提交错误信息
            return Result.fail("手机号输入错误，请重新输入");
        }
        //3.生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4.保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //5.发送验证码
        log.debug("发送验证码成功，验证码为{}",code);

        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号
        boolean invalid = RegexUtils.isPhoneInvalid(loginForm.getPhone());
        if(!invalid){
            return Result.fail("手机号格式错误");
        }
        //2.获取验证码
        String code = loginForm.getCode();
        //TODO 3.从redis中获取验证码，手机号和验证码不一致，返回提示信息
        if(code ==null||!code.equals(stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+loginForm.getPhone()))){
            return Result.fail("手机号和验证码不一致");
        }
        //4.根据手机号查询用户是否存在
        User user = query().eq("phone", loginForm.getPhone()).one();
        //5.用户不存在，创建新用户，保存到数据库中
        if(user ==null){
            user=createUser(loginForm.getPhone(),USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
        }
        //TODO 6.保存用户到redis中  存在有效时间问题
        //6.1 使用uuid生成随机token
        String token = UUID.randomUUID().toString();
        //6.2 使用hash的方式保存用户信息
        String key= RedisConstants.LOGIN_USER_KEY+token;
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).
                        setFieldValueEditor((fileName,fileValue)->fileValue.toString()));
        stringRedisTemplate.opsForHash().putAll(key,map);
        //6.3设置有效期
        stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL,TimeUnit.MINUTES);
        //TODO 7.结束，返回登录成功信息以及token信息
        return Result.ok(token);
    }

    private User createUser(String phone, String nickName) {
        //1.创建新用户
        User user=new User();
        user.setPhone(phone);
        user.setNickName(nickName);
        //2.保存用户到数据库中
        save(user);
        return user;
    }
}
