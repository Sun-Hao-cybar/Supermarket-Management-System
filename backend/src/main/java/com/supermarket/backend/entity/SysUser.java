package com.supermarket.backend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private BigDecimal salary;
    private Integer role;   // 1管理员 0普通用户
    private String remark;
    private String avatar;      // 头像 base64
    private String gender;      // 性别
    private Integer age;        // 年龄
    private String address;     // 住址
    private Date createTime;
}