package com.supermarket.backend.service;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.SysUser;
import com.supermarket.backend.mapper.MemberMapper;
import com.supermarket.backend.mapper.SupplierMapper;
import com.supermarket.backend.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SysUserService {
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private SupplierMapper supplierMapper;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    
    private static final String[] ADMIN_PREFIXES = {"11", "10", "01"};

    public List<SysUser> listAll() {
        return sysUserMapper.selectAll();
    }

    public SysUser login(String username, String password) {
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public SysUser getById(Long id) {
        return sysUserMapper.selectById(id);
    }

    public Result<String> add(SysUser user) {
        return add(user, false);
    }

    /**
     * @param isSelfRegistration true=自行注册, false=管理员通过员工管理添加
     */
    public Result<String> add(SysUser user, boolean isSelfRegistration) {
        String phoneWithCode = user.getPhone();
        String areaCode = phoneWithCode != null && phoneWithCode.contains("|") ? phoneWithCode.split("\\|")[0] : "+86";
        String phone = phoneWithCode != null && phoneWithCode.contains("|") ? phoneWithCode.split("\\|")[1] : phoneWithCode;

        Integer role = user.getRole();
        if (role == null) {
            role = 0;
            user.setRole(0);
        }

        // 管理员只能通过自行注册创建，不可通过员工管理添加
        if (role == 1 && !isSelfRegistration) {
            return Result.error("管理员只能通过自行注册创建，请联系管理员自行注册");
        }

        if (role == 1) {
            Result<String> adminCheck = validateAdminRegistration(user.getUsername());
            if (!adminCheck.getCode().equals(200)) {
                return adminCheck;
            }
            user.setUsername(truncateUsername(user.getUsername(), 6));
        } else {
            user.setUsername(truncateUsername(user.getUsername(), 9));

            // 员工自行注册：更新已存在的员工记录
            if (isSelfRegistration) {
                SysUser existingEmp = sysUserMapper.selectByUsername(user.getUsername());
                if (existingEmp == null) {
                    return Result.error("此员工编号不存在，请联系管理员录入员工信息后再注册");
                }
                if (existingEmp.getPassword() != null && !existingEmp.getPassword().isEmpty()) {
                    return Result.error("此员工已注册，请直接登录");
                }
                // 注册时校验密码和电话
                Result<String> passwordCheck = validatePassword(user.getPassword());
                if (!passwordCheck.getCode().equals(200)) return passwordCheck;
                if (isPasswordExists(user.getPassword())) return Result.error("密码已被使用，请使用其他密码");
                if (phone != null && !phone.isEmpty()) {
                    Result<String> phoneCheckResult = validatePhone(areaCode, phone);
                    if (!phoneCheckResult.getCode().equals(200)) return phoneCheckResult;
                    if (isPhoneExists(areaCode, phone)) return Result.error("该地区的电话号码已被注册");
                }
                // 更新员工信息
                existingEmp.setPassword(user.getPassword());
                existingEmp.setRealName(user.getRealName());
                existingEmp.setPhone(user.getPhone());
                existingEmp.setCreateTime(new Date());
                sysUserMapper.update(existingEmp);

                // 注册后自动加入会员表
                String memberLevel = "普通会员";
                com.supermarket.backend.entity.Member member = new com.supermarket.backend.entity.Member();
                member.setMemberNo("M" + existingEmp.getUsername());
                member.setName(user.getRealName());
                member.setPhone(user.getPhone());
                member.setLevel(memberLevel);
                member.setRegisterTime(new Date());
                memberMapper.insert(member);

                return Result.success("注册成功！请登录");
            }

            // 管理员新增员工：仅校验用户名格式，不校验密码/电话
            Result<String> employeeCheck = validateEmployeeUsername(user.getUsername());
            if (!employeeCheck.getCode().equals(200)) {
                return employeeCheck;
            }
        }

        // 管理员新增或管理员注册：密码校验
        if (isSelfRegistration) {
            Result<String> passwordCheck = validatePassword(user.getPassword());
            if (!passwordCheck.getCode().equals(200)) return passwordCheck;
            if (isPasswordExists(user.getPassword())) return Result.error("密码已被使用，请使用其他密码");
        }

        if (phone != null && !phone.isEmpty()) {
            Result<String> phoneCheckResult = validatePhone(areaCode, phone);
            if (!phoneCheckResult.getCode().equals(200)) return phoneCheckResult;
            if (isPhoneExists(areaCode, phone)) return Result.error("该地区的电话号码已被注册");
        }

        // 管理员新增员工：密码留空，等员工注册时自行设置
        if (user.getPassword() == null) {
            user.setPassword("");
        }

        SysUser existing = sysUserMapper.selectByUsername(user.getUsername());
        if (existing != null) {
            return Result.error("用户名已存在");
        }

        user.setCreateTime(new Date());
        sysUserMapper.insert(user);

        // 管理员注册后自动加入会员表
        if (role == 1) {
            String username = user.getUsername();
            String memberLevel;
            if (username.startsWith("11")) {
                memberLevel = "SVIP";
            } else {
                memberLevel = "VIP";
            }
            com.supermarket.backend.entity.Member member = new com.supermarket.backend.entity.Member();
            member.setMemberNo("M" + username);
            member.setName(user.getRealName());
            member.setPhone(user.getPhone());
            member.setLevel(memberLevel);
            member.setRegisterTime(new Date());
            memberMapper.insert(member);
        }

        return Result.success("添加成功");
    }

    public Result<String> update(SysUser user) {
        // 修改时仅检查本表电话不重复（编辑不拦截跨表同步）
        String phone = user.getPhone();
        if (phone != null && !phone.isEmpty()) {
            if (sysUserMapper.countByPhone(phone) > 1) {
                return Result.error("该电话号已在员工中使用");
            }
        }
        sysUserMapper.update(user);
        return Result.success("修改成功");
    }

    public Result<String> delete(Long id) {
        sysUserMapper.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 当前用户自行编辑个人信息
     * - 员工编号不可修改
     * - 角色不可修改
     * - 修改密码需提供旧密码验证+新密码二次确认
     */
    public Result<String> selfUpdate(Long userId, SysUser updateData, String oldPassword, String confirmPassword) {
        SysUser current = sysUserMapper.selectById(userId);
        if (current == null) {
            return Result.error("用户不存在");
        }

        // 员工编号不可修改
        if (updateData.getUsername() != null && !updateData.getUsername().equals(current.getUsername())) {
            return Result.error("员工编号不可修改");
        }

        // 角色不可修改
        if (updateData.getRole() != null && !updateData.getRole().equals(current.getRole())) {
            return Result.error("角色不可修改");
        }

        // 修改密码时需验证旧密码+二次确认
        if (updateData.getPassword() != null && !updateData.getPassword().isEmpty()) {
            if (oldPassword == null || !oldPassword.equals(current.getPassword())) {
                return Result.error("旧密码不正确");
            }
            if (!updateData.getPassword().equals(confirmPassword)) {
                return Result.error("两次输入的新密码不一致");
            }
            Result<String> pwdCheck = validatePassword(updateData.getPassword());
            if (!pwdCheck.getCode().equals(200)) {
                return pwdCheck;
            }
            if (isPasswordExists(updateData.getPassword())) {
                return Result.error("密码已被使用，请使用其他密码");
            }
        } else {
            // 不修改密码时保留原密码
            updateData.setPassword(current.getPassword());
        }

        // 电话校验（如果修改了电话）
        if (updateData.getPhone() != null && !updateData.getPhone().isEmpty()
                && !updateData.getPhone().equals(current.getPhone())) {
            if (updateData.getPhone().contains("|")) {
                String[] parts = updateData.getPhone().split("\\|");
                if (isPhoneExists(parts[0], parts[1])) {
                    return Result.error("该电话号已被使用");
                }
            }
        }

        // 保留不可修改的字段，未传的字段保留原值
        updateData.setId(userId);
        updateData.setUsername(current.getUsername());
        updateData.setRole(current.getRole());
        if (updateData.getRealName() == null) updateData.setRealName(current.getRealName());
        if (updateData.getPhone() == null) updateData.setPhone(current.getPhone());
        if (updateData.getSalary() == null) updateData.setSalary(current.getSalary());
        if (updateData.getRemark() == null) updateData.setRemark(current.getRemark());
        if (updateData.getAvatar() == null) updateData.setAvatar(current.getAvatar());
        if (updateData.getGender() == null) updateData.setGender(current.getGender());
        if (updateData.getAge() == null) updateData.setAge(current.getAge());
        if (updateData.getAddress() == null) updateData.setAddress(current.getAddress());

        sysUserMapper.update(updateData);
        return Result.success("个人信息更新成功");
    }

    public Result<String> validateAdminRegistration(String username) {
        if (username == null || username.length() != 6) {
            return Result.error("管理员账号必须为6位");
        }

        boolean validPrefix = false;
        for (String prefix : ADMIN_PREFIXES) {
            if (username.startsWith(prefix)) {
                validPrefix = true;
                break;
            }
        }
        if (!validPrefix) {
            return Result.error("管理员账号必须以11、10或01开头");
        }

        long adminCount = sysUserMapper.countAdmins();
        if (adminCount >= 3) {
            return Result.error("管理员数量已达上限（3人）");
        }

        for (String prefix : ADMIN_PREFIXES) {
            if (username.startsWith(prefix)) {
                long count = sysUserMapper.countAdminsByPrefix(prefix);
                if (count > 0) {
                    return Result.error(prefix + "开头的管理员已存在");
                }
            }
        }

        return Result.success("验证通过");
    }

    public Result<String> validateEmployeeUsername(String username) {
        if (username == null || username.length() < 2) {
            return Result.error("员工账号必须为9位");
        }
        if (!username.startsWith("00")) {
            return Result.error("员工账号必须以00开头");
        }
        return Result.success("验证通过");
    }

    public Result<String> validateEmployeeExists(String username) {
        SysUser employee = sysUserMapper.selectByUsername(username);
        if (employee == null) {
            return Result.error("员工表中无此员工信息，请联系管理员录入");
        }
        if (employee.getPassword() != null && !employee.getPassword().isEmpty()) {
            return Result.error("该员工已注册，请直接登录");
        }
        return Result.success("验证通过");
    }

    public String truncateUsername(String username, int maxLength) {
        if (username == null || username.length() <= maxLength) {
            return username;
        }
        return username.substring(0, maxLength);
    }

    public Result<String> checkHasEmployees() {
        long count = sysUserMapper.countEmployees();
        if (count == 0) {
            return Result.error("员工表为空，系统将退出到登录页面");
        }
        return Result.success("有员工数据");
    }

    public Result<String> validatePassword(String password) {
        if (password == null || password.length() < 8) {
            return Result.error("密码长度至少为8位");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return Result.error("密码必须包含至少一个字母、数字和特殊字符(@$!%*?&)");
        }
        return Result.success("验证通过");
    }

    public Result<String> validatePhone(String areaCode, String phone) {
        String cleanedPhone = phone.replaceAll("\\s|-", "");
        
        switch (areaCode) {
            case "+86":
                if (!cleanedPhone.matches("^1\\d{10}$")) {
                    return Result.error("中国大陆手机号必须为11位且以1开头");
                }
                break;
            case "+852":
                if (!cleanedPhone.matches("^(5|6|9)\\d{7}$")) {
                    return Result.error("中国香港手机号必须为8位且以5、6或9开头");
                }
                break;
            case "+853":
                if (!cleanedPhone.matches("^6\\d{7}$")) {
                    return Result.error("中国澳门手机号必须为8位且以6开头");
                }
                break;
            case "+886":
                if (!cleanedPhone.matches("^09\\d{8}$")) {
                    return Result.error("中国台湾手机号必须为10位且以09开头");
                }
                break;
            case "+81":
                if (!cleanedPhone.matches("^\\d{10,11}$")) {
                    return Result.error("日本手机号必须为10-11位");
                }
                break;
            case "+82":
                if (!cleanedPhone.matches("^\\d{10,11}$")) {
                    return Result.error("韩国手机号必须为10-11位");
                }
                break;
            case "+65":
                if (!cleanedPhone.matches("^\\d{8}$")) {
                    return Result.error("新加坡手机号必须为8位");
                }
                break;
            case "+66":
                if (!cleanedPhone.matches("^\\d{10}$")) {
                    return Result.error("泰国手机号必须为10位");
                }
                break;
            case "+60":
                if (!cleanedPhone.matches("^\\d{10}$")) {
                    return Result.error("马来西亚手机号必须为10位");
                }
                break;
            case "+84":
                if (!cleanedPhone.matches("^\\d{10}$")) {
                    return Result.error("越南手机号必须为10位");
                }
                break;
            case "+91":
                if (!cleanedPhone.matches("^\\d{10}$")) {
                    return Result.error("印度手机号必须为10位");
                }
                break;
            case "+971":
                if (!cleanedPhone.matches("^\\d{9}$")) {
                    return Result.error("阿联酋手机号必须为9位");
                }
                break;
            case "+966":
                if (!cleanedPhone.matches("^\\d{9}$")) {
                    return Result.error("沙特手机号必须为9位");
                }
                break;
            case "+62":
                if (!cleanedPhone.matches("^\\d{10,12}$")) {
                    return Result.error("印尼手机号必须为10-12位");
                }
                break;
            case "+63":
                if (!cleanedPhone.matches("^\\d{10}$")) {
                    return Result.error("菲律宾手机号必须为10位");
                }
                break;
            case "+1":
                if (!cleanedPhone.matches("^\\d{10}$")) {
                    return Result.error("美国/加拿大手机号必须为10位");
                }
                break;
            case "+7":
                if (!cleanedPhone.matches("^\\d{10}$")) {
                    return Result.error("俄罗斯手机号必须为10位");
                }
                break;
            case "+44":
                if (!cleanedPhone.matches("^\\d{11}$")) {
                    return Result.error("英国手机号必须为11位");
                }
                break;
            case "+49":
                if (!cleanedPhone.matches("^\\d{10,11}$")) {
                    return Result.error("德国手机号必须为10-11位");
                }
                break;
            case "+33":
                if (!cleanedPhone.matches("^\\d{9}$")) {
                    return Result.error("法国手机号必须为9位");
                }
                break;
            case "+39":
                if (!cleanedPhone.matches("^\\d{10}$")) {
                    return Result.error("意大利手机号必须为10位");
                }
                break;
            case "+34":
                if (!cleanedPhone.matches("^\\d{9}$")) {
                    return Result.error("西班牙手机号必须为9位");
                }
                break;
            case "+41":
                if (!cleanedPhone.matches("^\\d{9}$")) {
                    return Result.error("瑞士手机号必须为9位");
                }
                break;
            case "+46":
                if (!cleanedPhone.matches("^\\d{9}$")) {
                    return Result.error("瑞典手机号必须为9位");
                }
                break;
            case "+47":
                if (!cleanedPhone.matches("^\\d{8}$")) {
                    return Result.error("挪威手机号必须为8位");
                }
                break;
            case "+61":
                if (!cleanedPhone.matches("^\\d{9}$")) {
                    return Result.error("澳大利亚手机号必须为9位");
                }
                break;
            case "+64":
                if (!cleanedPhone.matches("^\\d{8,9}$")) {
                    return Result.error("新西兰手机号必须为8-9位");
                }
                break;
            case "+55":
                if (!cleanedPhone.matches("^\\d{11}$")) {
                    return Result.error("巴西手机号必须为11位");
                }
                break;
            case "+54":
                if (!cleanedPhone.matches("^\\d{10}$")) {
                    return Result.error("阿根廷手机号必须为10位");
                }
                break;
        }
        return Result.success("验证通过");
    }

    public boolean isPasswordExists(String password) {
        return sysUserMapper.countByPassword(password) > 0;
    }

    public boolean isPhoneExists(String areaCode, String phone) {
        String fullPhone = areaCode + "|" + phone;
        // 检查员工表
        if (sysUserMapper.countByPhone(fullPhone) > 0) return true;
        // 检查会员表
        if (memberMapper.selectByPhone(fullPhone) != null) return true;
        // 检查供应商联系人电话（非公司电话）
        if (supplierMapper.selectByContactPhone(fullPhone) != null) return true;
        return false;
    }

    public String getAdminType(String username) {
        if (username.startsWith("11")) return "一号管理员";
        if (username.startsWith("10")) return "二号管理员";
        if (username.startsWith("01")) return "三号管理员";
        return "";
    }

    public int getAdminLevel(String username) {
        if (username.startsWith("11")) return 1;
        if (username.startsWith("10")) return 2;
        if (username.startsWith("01")) return 3;
        return 0;
    }

    public boolean canManageSupplier(int level) {
        return level == 1 || level == 2;
    }

    public boolean canManageGoods(int level) {
        return level == 1 || level == 2;
    }

    public boolean canManageEmployee(int level) {
        return level == 1 || level == 3;
    }

    public boolean canManagePurchase(int level) {
        return level == 1 || level == 3;
    }

    public boolean canViewEmployee(int level) {
        return true;
    }

    public boolean canViewPurchase(int level) {
        return true;
    }

    public boolean canViewSupplier(int level) {
        return true;
    }

    public boolean canViewGoods(int level) {
        return true;
    }
}