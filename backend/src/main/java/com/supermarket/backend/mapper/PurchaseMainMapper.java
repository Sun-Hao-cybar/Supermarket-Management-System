package com.supermarket.backend.mapper;

import com.supermarket.backend.entity.PurchaseMain;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface PurchaseMainMapper {
    List<PurchaseMain> selectAll();
    PurchaseMain selectById(Long id);
    PurchaseMain selectByPurchaseNo(String purchaseNo);
    int insert(PurchaseMain purchaseMain);
    int update(PurchaseMain purchaseMain);
    int deleteById(Long id);
}