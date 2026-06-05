package com.supermarket.backend.entity;

import lombok.Data;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class Member {
    private Long id;
    private String memberNo;
    private String name;
    private String phone;
    private String level;       // 会员等级: SVIP/VIP/普通会员
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Shanghai")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    private Date registerTime;
    private String remark;
}
