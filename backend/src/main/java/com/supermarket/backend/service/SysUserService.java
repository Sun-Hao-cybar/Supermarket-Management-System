package com.supermarket.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.CaptchaVO;
import com.supermarket.backend.entity.SysUser;
import com.supermarket.backend.mapper.MemberMapper;
import com.supermarket.backend.mapper.SupplierMapper;
import com.supermarket.backend.mapper.SysUserMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class SysUserService {
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private SupplierMapper supplierMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired(required = false)
    private RedissonClient redissonClient;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~])[A-Za-z\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]{8,}$");
    private static final String[] ADMIN_PREFIXES = {"11", "10", "01"};

    // 验证码缓存（Caffeine 本地缓存，1分钟过期）
    private final Cache<String, String> verificationCodeCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    // CAPTCHA 人机验证缓存（2分钟过期）
    private final Cache<String, Integer> captchaCache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    // ==================== 读取操作（带缓存） ====================

    @Cacheable(value = "userList", unless = "#result == null || #result.isEmpty()")
    public List<SysUser> listAll() {
        return sysUserMapper.selectAll();
    }

    public SysUser login(String username, String rawPassword, String captchaId, String captchaAnswer) {
        // 先验证 CAPTCHA
        Result<String> captchaCheck = verifyCaptcha(captchaId, captchaAnswer);
        if (captchaCheck != null) {
            // captchaCheck 不为 null 说明验证失败，需要通过 Controller 返回错误
            // 这里返回 null 并让 Controller 检查 loginFailedReason
            loginFailedReason = captchaCheck.getMsg();
            return null;
        }
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) { loginFailedReason = "用户名或密码错误"; return null; }
        if (verifyPassword(rawPassword, user.getPassword())) {
            // 旧明文密码自动升级为 BCrypt
            String stored = user.getPassword();
            if (!stored.startsWith("$2a$") && !stored.startsWith("$2b$") && !stored.startsWith("$2y$")) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                sysUserMapper.update(user);
            }
            loginFailedReason = null;
            return user;
        }
        loginFailedReason = "用户名或密码错误";
        return null;
    }

    /** 登录失败原因（供 Controller 区分是 CAPTCHA 错误还是密码错误） */
    private String loginFailedReason;
    public String getLoginFailedReason() { return loginFailedReason; }

    @Cacheable(value = "user", key = "#id", unless = "#result == null")
    public SysUser getById(Long id) {
        return sysUserMapper.selectById(id);
    }

    // ==================== 写操作（带事务+锁+缓存清除） ====================

    @Transactional
    @CacheEvict(value = {"user", "userList", "memberList"}, allEntries = true)
    public Result<String> add(SysUser user) {
        return add(user, false);
    }

    /**
     * @param isSelfRegistration true=自行注册, false=管理员通过员工管理添加
     */
    @Transactional
    public Result<String> add(SysUser user, boolean isSelfRegistration) {
        String phoneWithCode = user.getPhone();
        String areaCode = phoneWithCode != null && phoneWithCode.contains("|") ? phoneWithCode.split("\\|")[0] : "+86";
        String phone = phoneWithCode != null && phoneWithCode.contains("|") ? phoneWithCode.split("\\|")[1] : phoneWithCode;

        Integer role = user.getRole();
        if (role == null) { role = 0; user.setRole(0); }

        if (role == 1 && !isSelfRegistration) {
            return Result.error("管理员只能通过自行注册创建，请联系管理员自行注册");
        }

        if (role == 1) {
            // 分布式锁保护管理员注册竞态条件
            return addAdminWithLock(user, areaCode, phone);
        } else {
            return addEmployee(user, isSelfRegistration, areaCode, phone);
        }
    }

    /**
     * 管理员注册 —— Redisson 分布式锁保护
     */
    private Result<String> addAdminWithLock(SysUser user, String areaCode, String phone) {
        String lockKey = "admin:register:" + user.getUsername().substring(0, 2);
        RLock lock = null;
        if (redissonClient != null) {
            lock = redissonClient.getLock(lockKey);
        }

        try {
            if (lock != null) {
                if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                    return Result.error("系统繁忙，请稍后重试");
                }
            }

            Result<String> adminCheck = validateAdminRegistration(user.getUsername());
            if (!adminCheck.getCode().equals(200)) return adminCheck;
            user.setUsername(truncateUsername(user.getUsername(), 6));

            // 密码加密
            Result<String> passwordCheck = validatePassword(user.getPassword());
            if (!passwordCheck.getCode().equals(200)) return passwordCheck;
            if (isPasswordExists(user.getPassword())) return Result.error("密码已被使用，请使用其他密码");
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            if (phone != null && !phone.isEmpty()) {
                Result<String> phoneCheckResult = validatePhone(areaCode, phone);
                if (!phoneCheckResult.getCode().equals(200)) return phoneCheckResult;
                if (isPhoneExists(areaCode, phone)) return Result.error("该地区的电话号码已被注册");
            }

            SysUser existing = sysUserMapper.selectByUsername(user.getUsername());
            if (existing != null) return Result.error("用户名已存在");

            user.setCreateTime(new Date());
            sysUserMapper.insert(user);

            // 管理员注册后自动加入会员表
            String memberLevel = user.getUsername().startsWith("11") ? "SVIP" : "VIP";
            com.supermarket.backend.entity.Member member = new com.supermarket.backend.entity.Member();
            member.setMemberNo("M" + user.getUsername());
            member.setName(user.getRealName());
            member.setPhone(user.getPhone());
            member.setLevel(memberLevel);
            member.setRegisterTime(new Date());
            memberMapper.insert(member);

            return Result.success("添加成功");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("系统繁忙，请稍后重试");
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 员工添加/注册
     */
    private Result<String> addEmployee(SysUser user, boolean isSelfRegistration, String areaCode, String phone) {
        user.setUsername(truncateUsername(user.getUsername(), 9));

        if (isSelfRegistration) {
            SysUser existingEmp = sysUserMapper.selectByUsername(user.getUsername());
            if (existingEmp == null)
                return Result.error("此员工编号不存在，请联系管理员录入员工信息后再注册");
            if (existingEmp.getPassword() != null && !existingEmp.getPassword().isEmpty())
                return Result.error("此员工已注册，请直接登录");

            Result<String> passwordCheck = validatePassword(user.getPassword());
            if (!passwordCheck.getCode().equals(200)) return passwordCheck;
            if (isPasswordExists(user.getPassword())) return Result.error("密码已被使用，请使用其他密码");
            if (phone != null && !phone.isEmpty()) {
                Result<String> phoneCheckResult = validatePhone(areaCode, phone);
                if (!phoneCheckResult.getCode().equals(200)) return phoneCheckResult;
                if (isPhoneExists(areaCode, phone)) return Result.error("该地区的电话号码已被注册");
            }

            existingEmp.setPassword(passwordEncoder.encode(user.getPassword()));
            existingEmp.setRealName(user.getRealName());
            existingEmp.setPhone(user.getPhone());
            existingEmp.setCreateTime(new Date());
            sysUserMapper.update(existingEmp);

            // 只有管理员分配了会员等级才加入/更新会员表
            if (existingEmp.getMemberLevel() != null && !existingEmp.getMemberLevel().isEmpty()) {
                String memberNo = "M" + existingEmp.getUsername();
                com.supermarket.backend.entity.Member existingMember = memberMapper.selectByMemberNo(memberNo);
                if (existingMember == null) {
                    com.supermarket.backend.entity.Member member = new com.supermarket.backend.entity.Member();
                    member.setMemberNo(memberNo);
                    member.setName(user.getRealName());
                    member.setPhone(user.getPhone());
                    member.setLevel(existingEmp.getMemberLevel());
                    member.setRegisterTime(new Date());
                    memberMapper.insert(member);
                } else {
                    // 管理员可能已通过编辑功能创建了会员记录，这里更新姓名和电话即可
                    existingMember.setName(user.getRealName());
                    existingMember.setPhone(user.getPhone());
                    memberMapper.update(existingMember);
                }
            }

            return Result.success("注册成功！请登录");
        }

        // 管理员新增员工：仅校验用户名格式
        Result<String> employeeCheck = validateEmployeeUsername(user.getUsername());
        if (!employeeCheck.getCode().equals(200)) return employeeCheck;

        if (phone != null && !phone.isEmpty()) {
            Result<String> phoneCheckResult = validatePhone(areaCode, phone);
            if (!phoneCheckResult.getCode().equals(200)) return phoneCheckResult;
            if (isPhoneExists(areaCode, phone)) return Result.error("该地区的电话号码已被注册");
        }

        if (user.getPassword() == null) user.setPassword("");

        SysUser existing = sysUserMapper.selectByUsername(user.getUsername());
        if (existing != null) return Result.error("用户名已存在");

        user.setCreateTime(new Date());
        sysUserMapper.insert(user);
        return Result.success("添加成功");
    }

    @Transactional
    @CacheEvict(value = {"user", "userList", "memberList"}, allEntries = true)
    public Result<String> update(SysUser user) {
        String phone = user.getPhone();
        if (phone != null && !phone.isEmpty()) {
            SysUser existingByPhone = sysUserMapper.selectByPhone(phone);
            if (existingByPhone != null && !existingByPhone.getId().equals(user.getId()))
                return Result.error("该电话号已在员工中使用");
            if (phone.contains("|")) {
                String[] parts = phone.split("\\|");
                if (parts.length >= 2) {
                    String fullPhone = parts[0] + "|" + parts[1];
                    // 检查会员表（排除员工自己对应的会员记录）
                    com.supermarket.backend.entity.Member memberByPhone = memberMapper.selectByPhone(fullPhone);
                    if (memberByPhone != null) {
                        String ownMemberNo = "M" + user.getUsername();
                        if (!ownMemberNo.equals(memberByPhone.getMemberNo())) {
                            return Result.error("该电话号已在会员中使用");
                        }
                    }
                    // 检查供应商表（排除自己）
                    if (supplierMapper.selectByContactPhone(fullPhone) != null)
                        return Result.error("该电话号已在供应商中使用");
                }
            }
        }
        sysUserMapper.update(user);

        // 同步会员等级
        syncMemberLevel(user);

        return Result.success("修改成功");
    }

    /**
     * 同步会员等级：管理员通过员工管理设置/修改会员等级时
     * 只有员工已完成自行注册（有密码）后才同步到会员表，未注册员工不创建会员记录
     */
    private void syncMemberLevel(SysUser user) {
        // 从数据库取最新记录，判断员工是否已注册
        SysUser dbUser = sysUserMapper.selectById(user.getId());
        if (dbUser == null) return;
        boolean isRegistered = dbUser.getPassword() != null && !dbUser.getPassword().isEmpty();

        String memberNo = "M" + dbUser.getUsername();
        com.supermarket.backend.entity.Member member = memberMapper.selectByMemberNo(memberNo);
        String newLevel = user.getMemberLevel();

        if (newLevel != null && !newLevel.isEmpty() && !"无".equals(newLevel)) {
            if (!isRegistered) {
                // 员工尚未注册，不创建会员记录，但保留 memberLevel 到 sys_user 待注册时同步
                return;
            }
            // 创建或更新会员记录
            if (member == null) {
                member = new com.supermarket.backend.entity.Member();
                member.setMemberNo(memberNo);
                String name = dbUser.getRealName();
                member.setName(name != null && !name.isEmpty() ? name : dbUser.getUsername());
                String phone = dbUser.getPhone();
                member.setPhone(phone != null ? phone : "");
                member.setRegisterTime(new Date());
                member.setLevel(newLevel);
                memberMapper.insert(member);
            } else {
                member.setLevel(newLevel);
                memberMapper.update(member);
            }
        } else {
            // 管理员取消了会员身份 → 删除会员记录
            if (member != null) {
                memberMapper.delete(member.getId());
            }
        }
    }

    @Transactional
    @CacheEvict(value = {"user", "userList", "memberList"}, allEntries = true)
    public Result<String> delete(Long id) {
        // 级联删除对应的会员记录
        SysUser user = sysUserMapper.selectById(id);
        if (user != null) {
            String memberNo = "M" + user.getUsername();
            com.supermarket.backend.entity.Member member = memberMapper.selectByMemberNo(memberNo);
            if (member != null) {
                memberMapper.delete(member.getId());
            }
        }
        sysUserMapper.delete(id);
        return Result.success("删除成功");
    }

    @Transactional
    @CacheEvict(value = {"user", "userList"}, allEntries = true)
    public Result<String> selfUpdate(Long userId, SysUser updateData, String oldPassword, String confirmPassword) {
        SysUser current = sysUserMapper.selectById(userId);
        if (current == null) return Result.error("用户不存在");

        if (updateData.getUsername() != null && !updateData.getUsername().equals(current.getUsername()))
            return Result.error("员工编号不可修改");
        if (updateData.getRole() != null && !updateData.getRole().equals(current.getRole()))
            return Result.error("角色不可修改");

        // 修改密码：验证旧密码（兼容明文→BCrypt升级）
        if (updateData.getPassword() != null && !updateData.getPassword().isEmpty()) {
            if (oldPassword == null || !verifyPassword(oldPassword, current.getPassword()))
                return Result.error("旧密码不正确");
            if (!updateData.getPassword().equals(confirmPassword))
                return Result.error("两次输入的新密码不一致");
            Result<String> pwdCheck = validatePassword(updateData.getPassword());
            if (!pwdCheck.getCode().equals(200)) return pwdCheck;
            if (isPasswordExists(updateData.getPassword()))
                return Result.error("密码已被使用，请使用其他密码");
            updateData.setPassword(passwordEncoder.encode(updateData.getPassword()));
        } else {
            updateData.setPassword(current.getPassword());
        }

        if (updateData.getPhone() != null && !updateData.getPhone().isEmpty()
                && !updateData.getPhone().equals(current.getPhone())) {
            if (updateData.getPhone().contains("|")) {
                String[] parts = updateData.getPhone().split("\\|");
                if (parts.length >= 2 && isPhoneExists(parts[0], parts[1]))
                    return Result.error("该电话号已被使用");
            }
        }

        // 年龄校验：公司规定员工年龄须在18~66岁之间
        if (updateData.getAge() != null && (updateData.getAge() < 18 || updateData.getAge() > 66)) {
            return Result.error("年龄不符合公司规定，请重新输入");
        }

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

    // ==================== CAPTCHA 人机验证 ====================

    /**
     * 生成数学算式 CAPTCHA
     */
    public CaptchaVO generateCaptcha() {
        int a = new Random().nextInt(10) + 1;  // 1-10
        int b = new Random().nextInt(10) + 1;  // 1-10
        int operator = new Random().nextInt(3); // 0=+, 1=−, 2=×
        String question;
        int answer;
        switch (operator) {
            case 0: question = a + " + " + b + " = ?"; answer = a + b; break;
            case 1: question = (a + b) + " − " + Math.min(a, b) + " = ?"; answer = Math.max(a, b); break;
            default: question = a + " × " + b + " = ?"; answer = a * b; break;
        }
        String captchaId = java.util.UUID.randomUUID().toString().substring(0, 8);
        captchaCache.put(captchaId, answer);
        return new CaptchaVO(captchaId, question);
    }

    /**
     * 验证 CAPTCHA 答案。返回 null 表示验证通过，否则返回错误 Result
     */
    private Result<String> verifyCaptcha(String captchaId, String captchaAnswer) {
        if (captchaId == null || captchaId.isEmpty() || captchaAnswer == null || captchaAnswer.isEmpty()) {
            return Result.error("请输入人机验证码");
        }
        Integer expected = captchaCache.getIfPresent(captchaId);
        if (expected == null) {
            return Result.error("验证码已过期，请刷新后重试");
        }
        try {
            if (Integer.parseInt(captchaAnswer.trim()) == expected) {
                captchaCache.invalidate(captchaId); // 一次性使用
                return null; // 验证通过
            }
        } catch (NumberFormatException ignored) {}
        return Result.error("人机验证码错误");
    }

    // ==================== 密码找回 ====================

    /**
     * 发送验证码到手机号
     */
    public Result<String> sendResetCode(String phone, String captchaId, String captchaAnswer) {
        // 先验证 CAPTCHA
        Result<String> captchaCheck = verifyCaptcha(captchaId, captchaAnswer);
        if (captchaCheck != null) return captchaCheck;

        if (phone == null || phone.isEmpty()) {
            return Result.error("请输入手机号");
        }
        // 查找该手机号对应的用户
        SysUser user = sysUserMapper.selectByPhone(phone);
        if (user == null) {
            return Result.error("该手机号未注册");
        }
        // 生成6位数字验证码
        String code = String.format("%06d", new Random().nextInt(1000000));
        verificationCodeCache.put(phone, code);
        // 开发环境：返回验证码（生产环境应通过短信发送）
        System.out.println("========================================");
        System.out.println("密码找回验证码: " + code);
        System.out.println("手机号: " + phone);
        System.out.println("用户名: " + user.getUsername());
        System.out.println("有效时间: 1分钟");
        System.out.println("========================================");
        return Result.success("验证码已发送（开发模式：请查看后端控制台）");
    }

    /**
     * 验证验证码并重置密码
     */
    @Transactional
    @CacheEvict(value = {"user", "userList"}, allEntries = true)
    public Result<String> resetPassword(String phone, String code, String newPassword) {
        if (phone == null || code == null || newPassword == null) {
            return Result.error("参数不完整");
        }
        // 验证验证码
        String storedCode = verificationCodeCache.getIfPresent(phone);
        if (storedCode == null) {
            return Result.error("验证码已过期或未发送");
        }
        if (!storedCode.equals(code)) {
            return Result.error("验证码错误");
        }
        // 验证新密码
        Result<String> pwdCheck = validatePassword(newPassword);
        if (!pwdCheck.getCode().equals(200)) return pwdCheck;
        if (isPasswordExists(newPassword)) return Result.error("密码已被使用，请使用其他密码");

        // 查找用户并重置密码
        SysUser user = sysUserMapper.selectByPhone(phone);
        if (user == null) return Result.error("用户不存在");

        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.update(user);

        // 清除验证码
        verificationCodeCache.invalidate(phone);

        return Result.success("密码重置成功，请使用新密码登录");
    }

    // ==================== 异步任务（多线程） ====================

    @Async("taskExecutor")
    public CompletableFuture<Void> asyncImportUsers(List<SysUser> users) {
        for (SysUser user : users) {
            add(user);
        }
        return CompletableFuture.completedFuture(null);
    }

    // ==================== 验证方法 ====================

    public Result<String> validateAdminRegistration(String username) {
        if (username == null || username.length() != 6)
            return Result.error("管理员账号必须为6位");

        boolean validPrefix = false;
        for (String prefix : ADMIN_PREFIXES) {
            if (username.startsWith(prefix)) { validPrefix = true; break; }
        }
        if (!validPrefix) return Result.error("管理员账号必须以11、10或01开头");

        if (sysUserMapper.countAdmins() >= 3)
            return Result.error("管理员数量已达上限（3人）");

        for (String prefix : ADMIN_PREFIXES) {
            if (username.startsWith(prefix)) {
                if (sysUserMapper.countAdminsByPrefix(prefix) > 0)
                    return Result.error(prefix + "开头的管理员已存在");
            }
        }
        return Result.success("验证通过");
    }

    public Result<String> validateEmployeeUsername(String username) {
        if (username == null || username.length() != 9)
            return Result.error("员工账号必须为9位");
        if (!username.matches("^00\\d{7}$"))
            return Result.error("员工账号必须以00开头且为9位纯数字");
        return Result.success("验证通过");
    }

    public String truncateUsername(String username, int maxLength) {
        if (username == null || username.length() <= maxLength) return username;
        return username.substring(0, maxLength);
    }

    public Result<String> checkHasEmployees() {
        if (sysUserMapper.countEmployees() == 0)
            return Result.error("员工表为空，系统将退出到登录页面");
        return Result.success("有员工数据");
    }

    public Result<String> validatePassword(String password) {
        if (password == null || password.length() < 8)
            return Result.error("密码长度至少为8位");
        if (!PASSWORD_PATTERN.matcher(password).matches())
            return Result.error("密码必须包含至少一个字母、数字和特殊字符");
        return Result.success("验证通过");
    }

    public boolean isPasswordExists(String password) {
        // BCrypt 后密码哈希不同，此方法改为检查用户输入明文是否不可重复
        // 改为通过遍历判断（生产中更建议去掉密码全局唯一限制）
        List<SysUser> allUsers = sysUserMapper.selectAll();
        for (SysUser u : allUsers) {
            if (passwordEncoder.matches(password, u.getPassword())) {
                return true;
            }
        }
        return false;
    }

    public Result<String> validatePhone(String areaCode, String phone) {
        String cleanedPhone = phone.replaceAll("\\s|-", "");
        switch (areaCode) {
            case "+86": if (!cleanedPhone.matches("^1\\d{10}$")) return Result.error("中国大陆手机号必须为11位且以1开头"); break;
            case "+852": if (!cleanedPhone.matches("^(5|6|9)\\d{7}$")) return Result.error("中国香港手机号必须为8位且以5、6或9开头"); break;
            case "+853": if (!cleanedPhone.matches("^6\\d{7}$")) return Result.error("中国澳门手机号必须为8位且以6开头"); break;
            case "+886": if (!cleanedPhone.matches("^09\\d{8}$")) return Result.error("中国台湾手机号必须为10位且以09开头"); break;
            case "+81": if (!cleanedPhone.matches("^\\d{10,11}$")) return Result.error("日本手机号必须为10-11位"); break;
            case "+82": if (!cleanedPhone.matches("^\\d{10,11}$")) return Result.error("韩国手机号必须为10-11位"); break;
            case "+65": if (!cleanedPhone.matches("^\\d{8}$")) return Result.error("新加坡手机号必须为8位"); break;
            case "+66": if (!cleanedPhone.matches("^\\d{10}$")) return Result.error("泰国手机号必须为10位"); break;
            case "+60": if (!cleanedPhone.matches("^\\d{10}$")) return Result.error("马来西亚手机号必须为10位"); break;
            case "+84": if (!cleanedPhone.matches("^\\d{10}$")) return Result.error("越南手机号必须为10位"); break;
            case "+91": if (!cleanedPhone.matches("^\\d{10}$")) return Result.error("印度手机号必须为10位"); break;
            case "+971": if (!cleanedPhone.matches("^\\d{9}$")) return Result.error("阿联酋手机号必须为9位"); break;
            case "+966": if (!cleanedPhone.matches("^\\d{9}$")) return Result.error("沙特手机号必须为9位"); break;
            case "+62": if (!cleanedPhone.matches("^\\d{10,12}$")) return Result.error("印尼手机号必须为10-12位"); break;
            case "+63": if (!cleanedPhone.matches("^\\d{10}$")) return Result.error("菲律宾手机号必须为10位"); break;
            case "+1": if (!cleanedPhone.matches("^\\d{10}$")) return Result.error("美国/加拿大手机号必须为10位"); break;
            case "+7": if (!cleanedPhone.matches("^\\d{10}$")) return Result.error("俄罗斯手机号必须为10位"); break;
            case "+44": if (!cleanedPhone.matches("^\\d{11}$")) return Result.error("英国手机号必须为11位"); break;
            case "+49": if (!cleanedPhone.matches("^\\d{10,11}$")) return Result.error("德国手机号必须为10-11位"); break;
            case "+33": if (!cleanedPhone.matches("^\\d{9}$")) return Result.error("法国手机号必须为9位"); break;
            case "+39": if (!cleanedPhone.matches("^\\d{10}$")) return Result.error("意大利手机号必须为10位"); break;
            case "+34": if (!cleanedPhone.matches("^\\d{9}$")) return Result.error("西班牙手机号必须为9位"); break;
            case "+41": if (!cleanedPhone.matches("^\\d{9}$")) return Result.error("瑞士手机号必须为9位"); break;
            case "+46": if (!cleanedPhone.matches("^\\d{9}$")) return Result.error("瑞典手机号必须为9位"); break;
            case "+47": if (!cleanedPhone.matches("^\\d{8}$")) return Result.error("挪威手机号必须为8位"); break;
            case "+61": if (!cleanedPhone.matches("^\\d{9}$")) return Result.error("澳大利亚手机号必须为9位"); break;
            case "+64": if (!cleanedPhone.matches("^\\d{8,9}$")) return Result.error("新西兰手机号必须为8-9位"); break;
            case "+55": if (!cleanedPhone.matches("^\\d{11}$")) return Result.error("巴西手机号必须为11位"); break;
            case "+54": if (!cleanedPhone.matches("^\\d{10}$")) return Result.error("阿根廷手机号必须为10位"); break;
        }
        return Result.success("验证通过");
    }

    /**
     * 密码验证（兼容明文自动升级为 BCrypt）
     */
    private boolean verifyPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null) return false;
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    public boolean isPhoneExists(String areaCode, String phone) {
        String fullPhone = areaCode + "|" + phone;
        if (sysUserMapper.countByPhone(fullPhone) > 0) return true;
        if (memberMapper.selectByPhone(fullPhone) != null) return true;
        if (supplierMapper.selectByContactPhone(fullPhone) != null) return true;
        return false;
    }

    // ==================== 权限判断 ====================

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

    public boolean canManageSupplier(int level) { return level == 1 || level == 2; }
    public boolean canManageGoods(int level) { return level == 1 || level == 2; }
    public boolean canManageEmployee(int level) { return level == 1 || level == 3; }
    public boolean canManagePurchase(int level) { return level == 1 || level == 3; }
    public boolean canViewEmployee(int level) { return true; }
    public boolean canViewPurchase(int level) { return true; }
    public boolean canViewSupplier(int level) { return true; }
    public boolean canViewGoods(int level) { return true; }
}
