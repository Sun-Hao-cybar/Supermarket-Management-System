package com.supermarket.backend.controller;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.Member;
import com.supermarket.backend.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        byte[] data = memberService.export();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "member_data.xlsx");
        return ResponseEntity.ok().headers(headers).body(data);
    }
}