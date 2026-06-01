package com.supermarket.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private Double salary;
    private Integer role;   // 1管理员 0普通用户
    private String remark;
    private Date createTime;
}