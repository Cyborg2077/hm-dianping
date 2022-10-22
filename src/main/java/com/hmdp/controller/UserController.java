package com.hmdp.controller;


import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.MailUtils;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) throws MessagingException {
        // TODO 发送短信验证码并保存验证码
        if (RegexUtils.isEmailInvalid(phone)) {
            return Result.fail("邮箱格式不正确");
        }
        String code = MailUtils.achieveCode();
        session.setAttribute(phone, code);
        log.info("发送登录验证码：{}", code);
//        MailUtils.sendTestMail(phone, code);
        return Result.ok();
    }

    /**
     * 登录功能
     *
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
        // TODO 实现登录功能
        //获取登录账号
        String phone = loginForm.getPhone();
        //获取登录验证码
        String code = loginForm.getCode();
        //获取session中的验证码
        Object cacheCode = session.getAttribute(phone);

        //1. 校验邮箱
        if (RegexUtils.isEmailInvalid(phone)) {
            //2. 不符合格式则报错
            return Result.fail("邮箱格式不正确！！");
        }
        //3. 校验验证码
        log.info("code:{},cacheCode{}", code, cacheCode);
        if (code == null || !cacheCode.toString().equals(code)) {
            //4. 不一致则报错
            return Result.fail("验证码不一致！！");
        }
        //5. 根据账号查询用户是否存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = userService.getOne(queryWrapper);
        //6. 如果不存在则创建
        if (user == null) {
            user = createUserWithPhone(phone);
        }
        //7. 保存用户信息到session中
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        session.setAttribute("user", userDTO);
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        //创建用户
        User user = new User();
        //设置手机号
        user.setPhone(phone);
        //设置昵称(默认名)，一个固定前缀+随机字符串
        user.setNickName("user_" + RandomUtil.randomString(8));
        //保存到数据库
        userService.save(user);
        return user;
    }

    /**
     * 登出功能
     *
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout() {
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    @GetMapping("/me")
    public Result me() {
        // TODO 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }
}
