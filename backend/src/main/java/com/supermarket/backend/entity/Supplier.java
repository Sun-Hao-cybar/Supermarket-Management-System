package com.supermarket.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Supplier {
    private Long id;
    private String supplierCode;  // 供应商编码
    private String supplierName;  // 供应商名称
    private String shortName;     // 简称
    private String address;       // 地址
    private String phone;         // 电话
    private String email;         // 邮箱
    private String contactPerson; // 联系人
    private String contactPhone;  // 联系电话
    private String remark;        // 备注
    private Date createTime;      // 创建时间
}