package com.supermarket.backend.service;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.PurchaseDetail;
import com.supermarket.backend.entity.PurchaseMain;
import com.supermarket.backend.entity.SysUser;
import com.supermarket.backend.mapper.PurchaseDetailMapper;
import com.supermarket.backend.mapper.PurchaseMainMapper;
import com.supermarket.backend.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class PurchaseDetailService {
    @Autowired
    private PurchaseDetailMapper purchaseDetailMapper;
    @Autowired
    private PurchaseMainMapper purchaseMainMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    public Result<List<PurchaseDetail>> list(){
        return Result.success(purchaseDetailMapper.selectAll());
    }

    @Transactional
    public Result<String> add(PurchaseDetail detail){
        if (detail.getDetailNo() == null || detail.getDetailNo().isEmpty())
            return Result.error("明细号不能为空");
        // 检查明细号是否重复
        if (purchaseDetailMapper.selectByDetailNo(detail.getDetailNo()) != null)
            return Result.error("明细号已存在，请使用其他编号");
        purchaseDetailMapper.insert(detail);
        return Result.success("采购明细新增成功");
    }

    @Transactional
    public Result<String> update(PurchaseDetail detail, Long operatorUserId, Integer adminLevel){
        Result<String> permissionCheck = checkDetailPermission(detail.getId(), operatorUserId, adminLevel);
        if (permissionCheck != null) return permissionCheck;
        // 检查明细号是否与其他记录重复
        if (detail.getDetailNo() != null && !detail.getDetailNo().isEmpty()) {
            PurchaseDetail exist = purchaseDetailMapper.selectByDetailNo(detail.getDetailNo());
            if (exist != null && !exist.getId().equals(detail.getId()))
                return Result.error("明细号已存在，请使用其他编号");
        }
        purchaseDetailMapper.update(detail);
        return Result.success("采购明细修改成功");
    }

    @Transactional
    public Result<String> delete(Long id, Long operatorUserId, Integer adminLevel){
        Result<String> permissionCheck = checkDetailPermission(id, operatorUserId, adminLevel);
        if (permissionCheck != null) return permissionCheck;
        purchaseDetailMapper.deleteById(id);
        return Result.success("采购明细删除成功");
    }

    /**
     * 权限校验：通过明细关联的采购单判断权限
     */
    private Result<String> checkDetailPermission(Long detailId, Long operatorUserId, Integer adminLevel) {
        if (adminLevel == null || operatorUserId == null) return Result.error("缺少操作者信息");
        if (adminLevel == 1) return null; // 一号管理员：全部权限
        if (adminLevel == 3) {
            PurchaseDetail detail = purchaseDetailMapper.selectById(detailId);
            if (detail == null) return Result.error("采购明细不存在");
            List<PurchaseMain> mains = purchaseMainMapper.selectAll();
            PurchaseMain main = mains.stream()
                    .filter(m -> m.getPurchaseNo() != null && m.getPurchaseNo().equals(detail.getPurchaseNo()))
                    .findFirst().orElse(null);
            if (main == null) return Result.error("关联采购单不存在");
            SysUser purchaseUser = sysUserMapper.selectById(main.getUserId());
            if (purchaseUser == null) return Result.error("采购员工不存在");
            if (purchaseUser.getRole() != null && purchaseUser.getRole() == 1
                    && !purchaseUser.getId().equals(operatorUserId)) {
                return Result.error("无权操作该管理员的采购明细");
            }
            return null;
        }
        return Result.error("无权操作采购明细");
    }
}