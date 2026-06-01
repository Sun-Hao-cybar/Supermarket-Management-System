package com.supermarket.backend.service;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.Supplier;
import com.supermarket.backend.mapper.SupplierMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SupplierService {

    @Autowired
    private SupplierMapper supplierMapper;

    public Result<List<Supplier>> list() {
        return Result.success(supplierMapper.selectAll());
    }

    public Result<String> add(Supplier supplier) {
        supplierMapper.insert(supplier);
        return Result.success("添加成功");
    }

    public Result<String> update(Supplier supplier) {
        supplierMapper.update(supplier);
        return Result.success("修改成功");
    }

    public Result<String> delete(Long id) {
        supplierMapper.deleteById(id);
        return Result.success("删除成功");
    }
}