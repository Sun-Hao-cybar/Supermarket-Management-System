package com.supermarket.backend.controller;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.CaptchaVO;
import com.supermarket.backend.entity.SysUser;
import com.supermarket.backend.service.SysUserService;
import com.supermarket.backend.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class SysUserController {
    @Autowired
    private SysUserService sysUserService;

    @GetMapping("/list")
    public Result<List<SysUser>> list() {
        return Result.success(sysUserService.listAll());
    }

    @GetMapping("/checkHasEmployees")
    public Result<String> checkHasEmployees() {
        return sysUserService.checkHasEmployees();
    }

    @GetMapping("/getById")
    public Result<SysUser> getById(@RequestParam Long id) {
        return Result.success(sysUserService.getById(id));
    }

    @PostMapping("/login")
    public Result<SysUser> login(@RequestParam String username, @RequestParam String password,
                                  @RequestParam(required = false) String captchaId,
                                  @RequestParam(required = false) String captchaAnswer) {
        SysUser user = sysUserService.login(username, password, captchaId, captchaAnswer);
        if (user != null) {
            return Result.success(user);
        }
        String reason = sysUserService.getLoginFailedReason();
        return Result.error(reason != null ? reason : "用户名或密码错误");
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody SysUser user) {
        return sysUserService.add(user, true);  // 自行注册允许创建管理员
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody SysUser user) {
        return sysUserService.add(user);  // 员工管理添加：禁止创建管理员
    }

    @PostMapping("/update")
    public Result<String> update(@RequestBody SysUser user) {
        return sysUserService.update(user);
    }

    @PostMapping("/updateProfile")
    public Result<String> updateProfile(@RequestBody SysUser user,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String oldPassword,
            @RequestParam(required = false) String confirmPassword) {
        return sysUserService.selfUpdate(userId, user, oldPassword, confirmPassword);
    }

    @GetMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        return sysUserService.delete(id);
    }

    @PostMapping("/import")
    public Result<String> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            String[] headers = {"username", "password", "realName", "phone", "salary", "role", "remark"};
            List<Map<String, Object>> dataList = ExcelUtil.importExcel(file.getInputStream(), headers);
            
            for (Map<String, Object> data : dataList) {
                SysUser user = new SysUser();
                user.setUsername(String.valueOf(data.get("username")));
                user.setPassword(String.valueOf(data.get("password")));
                user.setRealName(String.valueOf(data.get("realName")));
                user.setPhone(String.valueOf(data.get("phone")));
                if (data.get("salary") != null) {
                    user.setSalary(new BigDecimal(String.valueOf(data.get("salary"))));
                }
                if (data.get("role") != null) {
                    user.setRole(((Number) data.get("role")).intValue());
                }
                user.setRemark(String.valueOf(data.get("remark")));
                sysUserService.add(user);
            }
            return Result.success("导入成功，共导入 " + dataList.size() + " 条数据");
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    // ==================== CAPTCHA ====================

    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.success(sysUserService.generateCaptcha());
    }

    // ==================== 密码找回 ====================

    @PostMapping("/sendResetCode")
    public Result<String> sendResetCode(@RequestParam String phone,
                                         @RequestParam(required = false) String captchaId,
                                         @RequestParam(required = false) String captchaAnswer) {
        return sysUserService.sendResetCode(phone, captchaId, captchaAnswer);
    }

    @PostMapping("/resetPassword")
    public Result<String> resetPassword(@RequestParam String phone,
                                         @RequestParam String code,
                                         @RequestParam String newPassword) {
        return sysUserService.resetPassword(phone, code, newPassword);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() {
        try {
            List<SysUser> dataList = sysUserService.listAll();
            
            String[] headers = {"账号", "姓名", "电话", "工资", "角色", "备注"};
            String[] fieldNames = {"username", "realName", "phone", "salary", "role", "remark"};
            
            byte[] excelData = ExcelUtil.exportExcel(headers, fieldNames, dataList);
            
            String fileName = "员工数据.xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDispositionFormData("attachment", encodedFileName);
            
            return new ResponseEntity<>(excelData, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}