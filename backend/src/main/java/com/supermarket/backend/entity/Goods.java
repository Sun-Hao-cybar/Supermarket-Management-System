package com.supermarket.backend.entity;

import lombok.Data;
import java.util.Date;
import java.math.BigDecimal;

@Data
public class Goods {
    private Long id;
    private String goodsCode;
    private String goodsName;
    private BigDecimal price;
    private Long supplierId;
    private String intro;
    private String remark;
    private Date createTime;
}