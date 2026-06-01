package com.supermarket.backend.service;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.PurchaseMain;
import com.supermarket.backend.mapper.PurchaseMainMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PurchaseMainService {
    @Autowired
    private PurchaseMainMapper purchaseMainMapper;

    public Result<List<PurchaseMain>> list(){
        return Result.success(purchaseMainMapper.selectAll());
    }

    public Result<String> add(PurchaseMain main){
        purchaseMainMapper.insert(main);
        return Result.success("采购单新增成功");
    }

    public Result<String> update(PurchaseMain main){
        purchaseMainMapper.update(main);
        return Result.success("采购单修改成功");
    }

    public Result<String> delete(Long id){
        purchaseMainMapper.deleteById(id);
        return Result.success("采购单删除成功");
    }
}