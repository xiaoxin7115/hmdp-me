package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result sendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号错误");
        }
        String code = RandomUtil.randomNumbers(6);

//        session.setAttribute("code",code); 解决集群session问题
        //保存短信验证码到value
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY +phone , code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        return Result.ok();
    }

    //登录
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1、接收手机号并校验
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号错误");
        }
//        Object code = session.getAttribute("code"); 不再从session获取
        String chchecode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if(chchecode == null || !chchecode.equals(loginForm.getCode())){
            return Result.fail("验证码不对");
        }
//        query()  相当与select * from tb_user
        User user = query().eq("phone", phone).one();
        //快捷 user.null
        if (user == null) {
            //确保一定登陆成功
            user = createUserWithPhone(phone);
            save(user);
        }
        String token = UUID.randomUUID().toString(true);//不带下划线的数字
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        HashMap<Object, Object> userMap = new HashMap<>();
        //这里不适用工具类，id为Long类型，要先转化为string再存，否则会类型转换异常
        userMap.put("id", userDTO.getId().toString());
        userMap.put("nickName", userDTO.getNickName());
        userMap.put("icon", userDTO.getIcon());
        Map<String, Object> usermap = BeanUtil.beanToMap(userDTO);
        stringRedisTemplate.opsForHash().putAll(LOGIN_TOKEN_KEY+token,usermap);
        //模仿session，有效期为30minutes
        stringRedisTemplate.expire(LOGIN_TOKEN_KEY+token,LOGIN_TOKEN_TTL,TimeUnit.MINUTES);


//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
//        log.info("用户信息已存入session");

        //不需要返回用户凭证，因为用cookid中的sessionid

        //返回用户登陆凭证token
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(9));
        //也可自己设置其他的属性，也可让用户自己弄
        return user;
    }
}
