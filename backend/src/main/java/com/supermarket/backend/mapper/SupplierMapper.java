package com.supermarket.backend.mapper;

import com.supermarket.backend.entity.Supplier;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface SupplierMapper {
    List<Supplier> selectAll();
    Supplier selectById(Long id);
    Supplier selectByContactPhone(String contactPhone);
    int insert(Supplier supplier);
    int update(Supplier supplier);
    int deleteById(Long id);
}