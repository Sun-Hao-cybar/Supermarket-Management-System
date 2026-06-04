package com.supermarket.backend.service;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.Member;
import com.supermarket.backend.mapper.MemberMapper;
import com.supermarket.backend.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class MemberService {
    @Autowired
    private MemberMapper memberMapper;

    public Result<List<Member>> list() {
        return Result.success(memberMapper.selectAll());
    }

    public Result<Member> getById(Long id) {
        return Result.success(memberMapper.selectById(id));
    }

    public Result<String> add(Member member) {
        Member existing = memberMapper.selectByMemberNo(member.getMemberNo());
        if (existing != null) {
            return Result.error("会员编号已存在");
        }
        member.setRegisterTime(new Date());
        memberMapper.insert(member);
        return Result.success("添加成功");
    }

    public Result<String> update(Member member) {
        memberMapper.update(member);
        return Result.success("修改成功");
    }

    public Result<String> delete(Long id) {
        memberMapper.delete(id);
        return Result.success("删除成功");
    }

    public byte[] export() {
        try {
            List<Member> list = memberMapper.selectAll();
            String[] headers = {"ID", "会员编号", "姓名", "电话", "注册时间", "备注"};
            String[] fields = {"id", "memberNo", "name", "phone", "registerTime", "remark"};
            return ExcelUtil.exportExcel(headers, fields, list);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}