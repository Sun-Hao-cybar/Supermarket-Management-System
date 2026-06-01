package com.supermarket.backend.controller;

import com.supermarket.backend.entity.SysUser;
import com.supermarket.backend.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/user")
public class SysUserController {
    @Autowired
    private SysUserService sysUserService;

    @GetMapping("/list")
    public List<SysUser> list() {
        return sysUserService.listAll();
    }

    @PostMapping("/login")
    public Object login(@RequestParam String username, @RequestParam String password) {
        SysUser user = sysUserService.login(username, password);
        if (user != null) {
            return user;
        }
        return "登录失败";
    }
}