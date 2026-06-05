package com.supermarket.backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class PurchaseMain {
    private Long id;
    private String purchaseNo;
    private Long userId;
    private Integer totalNum;
    private BigDecimal totalPrice;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Shanghai")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    private Date purchaseTime;
    private String remark;
    private Date createTime;
}
