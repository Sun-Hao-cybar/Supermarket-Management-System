package com.supermarket.backend.controller;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.Member;
import com.supermarket.backend.service.MemberService;
import com.supermarket.backend.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @GetMapping("/list")
    public Result<List<Member>> list() {
        return memberService.list();
    }

    @GetMapping("/getById")
    public Result<Member> getById(@RequestParam Long id) {
        return memberService.getById(id);
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody Member member) {
        return memberService.add(member);
    }

    @PostMapping("/update")
    public Result<String> update(@RequestBody Member member) {
        return memberService.update(member);
    }

    @GetMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        return memberService.delete(id);
    }

    @PostMapping("/import")
    public Result<String> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            String[] headers = {"memberNo", "name", "phone", "level", "registerTime", "remark"};
            List<Map<String, Object>> dataList = ExcelUtil.importExcel(file.getInputStream(), headers);

            int successCount = 0;
            for (int i = 0; i < dataList.size(); i++) {
                Map<String, Object> data = dataList.get(i);
                int rowNum = i + 2;
                try {
                    Member member = new Member();
                    member.setMemberNo(ExcelUtil.getString(data, "memberNo"));
                    member.setName(ExcelUtil.getString(data, "name"));
                    member.setPhone(ExcelUtil.getString(data, "phone"));
                    member.setLevel(ExcelUtil.getString(data, "level"));
                    member.setRemark(ExcelUtil.getString(data, "remark"));
                    // 注册时间：Excel中可能为日期对象或字符串
                    member.setRegisterTime(ExcelUtil.getDate(data, "registerTime"));
                    Result<String> res = memberService.add(member);
                    if (res.getCode() == 200) {
                        successCount++;
                    } else {
                        return Result.error("第" + rowNum + "行导入失败：" + res.getMsg());
                    }
                } catch (Exception e) {
                    return Result.error("第" + rowNum + "行导入失败：" + e.getMessage());
                }
            }
            return Result.success("导入成功，共导入 " + successCount + " 条数据");
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        byte[] data = memberService.export();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "member_data.xlsx");
        return ResponseEntity.ok().headers(headers).body(data);
    }
}