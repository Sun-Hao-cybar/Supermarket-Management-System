package com.supermarket.backend.service;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.Member;
import com.supermarket.backend.mapper.MemberMapper;
import com.supermarket.backend.mapper.SupplierMapper;
import com.supermarket.backend.mapper.SysUserMapper;
import com.supermarket.backend.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class MemberService {
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SupplierMapper supplierMapper;

    @Cacheable(value = "memberList", unless = "#result.data == null || #result.data.isEmpty()")
    public Result<List<Member>> list() {
        return Result.success(memberMapper.selectAll());
    }

    @Cacheable(value = "member", key = "#id", unless = "#result.data == null")
    public Result<Member> getById(Long id) {
        return Result.success(memberMapper.selectById(id));
    }

    /**
     * 检查电话号码是否在会员、员工、供应商联系人中已存在（同区号下全局唯一）
     * 排除公司电话（supplier.phone），只检查联系人电话（supplier.contact_phone）
     */
    private Result<String> checkPhoneUnique(String phone) {
        if (phone == null || phone.isEmpty()) return Result.success(null);

        // 检查会员表
        Member existMember = memberMapper.selectByPhone(phone);
        if (existMember != null) return Result.error("该电话号已在会员中使用");

        // 检查员工表
        if (sysUserMapper.countByPhone(phone) > 0) return Result.error("该电话号已在员工中使用");

        // 检查供应商联系人电话
        if (supplierMapper.selectByContactPhone(phone) != null) return Result.error("该电话号已在供应商中使用");

        return Result.success(null);
    }

    @Transactional
    @CacheEvict(value = {"member", "memberList"}, allEntries = true)
    public Result<String> add(Member member) {
        Member existing = memberMapper.selectByMemberNo(member.getMemberNo());
        if (existing != null) {
            return Result.error("会员编号已存在");
        }
        Result<String> phoneCheck = checkPhoneUnique(member.getPhone());
        if (!phoneCheck.getCode().equals(200)) {
            return phoneCheck;
        }
        // 会员等级默认为普通会员，只允许 SVIP/VIP/普通会员
        if (member.getLevel() == null || member.getLevel().isEmpty()) {
            member.setLevel("普通会员");
        }
        if (!member.getLevel().matches("^(SVIP|VIP|普通会员)$")) {
            return Result.error("会员等级只允许 SVIP、VIP 或 普通会员");
        }
        member.setRegisterTime(new Date());
        memberMapper.insert(member);
        return Result.success("添加成功");
    }

    @Transactional
    @CacheEvict(value = {"member", "memberList"}, allEntries = true)
    public Result<String> update(Member member) {
        // 三个管理员本人的会员（M11xxx/M10xxx/M01xxx）等级不可修改
        Member existing = memberMapper.selectById(member.getId());
        if (existing != null && existing.getMemberNo() != null
                && existing.getMemberNo().matches("^M(11|10|01).*")) {
            if (member.getLevel() != null && !member.getLevel().equals(existing.getLevel())) {
                return Result.error("管理员本人的会员等级不可修改");
            }
        }
        // 修改时检查跨表电话唯一性
        if (member.getPhone() != null && !member.getPhone().isEmpty()) {
            Member existMember = memberMapper.selectByPhone(member.getPhone());
            if (existMember != null && !existMember.getId().equals(member.getId())) {
                return Result.error("该电话号已在会员中使用");
            }
            // 跨表检查（员工和供应商联系人）
            Result<String> crossCheck = checkPhoneUnique(member.getPhone());
            if (!crossCheck.getCode().equals(200)) {
                return crossCheck;
            }
        }
        memberMapper.update(member);
        return Result.success("修改成功");
    }

    @Transactional
    @CacheEvict(value = {"member", "memberList"}, allEntries = true)
    public Result<String> delete(Long id) {
        memberMapper.delete(id);
        return Result.success("删除成功");
    }

    public byte[] export() {
        try {
            List<Member> list = memberMapper.selectAll();
            String[] headers = {"ID", "会员编号", "姓名", "电话", "会员等级", "注册时间", "备注"};
            String[] fields = {"id", "memberNo", "name", "phone", "level", "registerTime", "remark"};
            return ExcelUtil.exportExcel(headers, fields, list);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}