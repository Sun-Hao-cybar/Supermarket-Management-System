package com.supermarket.backend.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PurchaseDetail {
    private Long id;
    private String detailNo;
    private String purchaseNo;
    private Long goodsId;
    private String goodsCode;
    private Integer goodsNum;
    private BigDecimal goodsPrice;
    private BigDecimal totalPrice;
    private String remark;
}