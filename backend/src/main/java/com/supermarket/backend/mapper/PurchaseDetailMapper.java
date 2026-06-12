package com.supermarket.backend.mapper;

import com.supermarket.backend.entity.PurchaseDetail;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface PurchaseDetailMapper {
    List<PurchaseDetail> selectAll();
    PurchaseDetail selectById(Long id);
    PurchaseDetail selectByDetailNo(String detailNo);
    int insert(PurchaseDetail detail);
    int update(PurchaseDetail detail);
    int deleteById(Long id);
    int deleteByPurchaseNo(String purchaseNo);
}