package com.hmdp.dto;

import lombok.Data;
//同时支持密码和手机验证码登录
@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
