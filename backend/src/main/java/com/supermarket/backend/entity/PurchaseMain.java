package com.supermarket.backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PurchaseMain {
    private Long id;
    private String purchaseNo;
    private Long userId;
    private Integer totalNum;
    private BigDecimal totalPrice;
    // 加上这个注解，指定日期格式
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date purchaseTime;
    private String remark;
    private Date createTime;
}