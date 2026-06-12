package com.supermarket.backend.service;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.Supplier;
import com.supermarket.backend.mapper.MemberMapper;
import com.supermarket.backend.mapper.SupplierMapper;
import com.supermarket.backend.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SupplierService {

    @Autowired
    private SupplierMapper supplierMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private MemberMapper memberMapper;

    @Cacheable(value = "supplierList", unless = "#result.data == null || #result.data.isEmpty()")
    public Result<List<Supplier>> list() {
        return Result.success(supplierMapper.selectAll());
    }

    /**
     * 检查联系人电话是否在供应商、员工、会员中已存在（同区号下全局唯一）
     * 公司电话（supplier.phone）不在检查范围内
     */
    private Result<String> checkContactPhoneUnique(String phone) {
        if (phone == null || phone.isEmpty()) return Result.success(null);

        // 检查供应商表
        if (supplierMapper.selectByContactPhone(phone) != null) return Result.error("该联系人电话已在供应商中使用");

        // 检查员工表
        if (sysUserMapper.countByPhone(phone) > 0) return Result.error("该电话号已在员工中使用");

        // 检查会员表
        if (memberMapper.selectByPhone(phone) != null) return Result.error("该电话号已在会员中使用");

        return Result.success(null);
    }

    @Transactional
    @CacheEvict(value = "supplierList", allEntries = true)
    public Result<String> add(Supplier supplier) {
        Result<String> phoneCheck = checkContactPhoneUnique(supplier.getContactPhone());
        if (!phoneCheck.getCode().equals(200)) {
            return phoneCheck;
        }
        supplierMapper.insert(supplier);
        return Result.success("添加成功");
    }

    @Transactional
    @CacheEvict(value = "supplierList", allEntries = true)
    public Result<String> update(Supplier supplier) {
        // 只有联系人电话发生变化时才进行唯一性检查
        Supplier existing = supplierMapper.selectById(supplier.getId());
        String newPhone = supplier.getContactPhone();
        String oldPhone = existing != null ? existing.getContactPhone() : null;
        boolean phoneChanged = newPhone != null && !newPhone.equals(oldPhone);

        if (phoneChanged && newPhone != null && !newPhone.isEmpty()) {
            Supplier existSupplier = supplierMapper.selectByContactPhone(newPhone);
            if (existSupplier != null && !existSupplier.getId().equals(supplier.getId())) {
                return Result.error("该联系人电话已在供应商中使用");
            }
            // 跨表检查
            Result<String> crossCheck = checkContactPhoneUnique(newPhone);
            if (!crossCheck.getCode().equals(200)) {
                return crossCheck;
            }
        }
        supplierMapper.update(supplier);
        return Result.success("修改成功");
    }

    @Transactional
    @CacheEvict(value = "supplierList", allEntries = true)
    public Result<String> delete(Long id) {
        supplierMapper.deleteById(id);
        return Result.success("删除成功");
    }
}