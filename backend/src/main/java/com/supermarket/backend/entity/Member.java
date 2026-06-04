package com.supermarket.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Member {
    private Long id;
    private String memberNo;
    private String name;
    private String phone;
    private Date registerTime;
    private String remark;
}