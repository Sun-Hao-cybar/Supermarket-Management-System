package com.supermarket.backend.service;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.PurchaseDetail;
import com.supermarket.backend.mapper.PurchaseDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PurchaseDetailService {
    @Autowired
    private PurchaseDetailMapper purchaseDetailMapper;

    public Result<List<PurchaseDetail>> list(){
        return Result.success(purchaseDetailMapper.selectAll());
    }

    public Result<String> add(PurchaseDetail detail){
        purchaseDetailMapper.insert(detail);
        return Result.success("采购明细新增成功");
    }

    public Result<String> update(PurchaseDetail detail){
        purchaseDetailMapper.update(detail);
        return Result.success("采购明细修改成功");
    }

    public Result<String> delete(Long id){
        purchaseDetailMapper.deleteById(id);
        return Result.success("采购明细删除成功");
    }
}