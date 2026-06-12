package com.supermarket.backend.service;

import com.supermarket.backend.common.Result;
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
public class PurchaseMainService {
    @Autowired
    private PurchaseMainMapper purchaseMainMapper;
    @Autowired
    private PurchaseDetailMapper purchaseDetailMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    public Result<List<PurchaseMain>> list(){
        return Result.success(purchaseMainMapper.selectAll());
    }

    public Result<String> add(PurchaseMain main){
        if (main.getPurchaseNo() == null || main.getPurchaseNo().isEmpty())
            return Result.error("采购清单号不能为空");
        if (main.getUserId() == null)
            return Result.error("请选择员工");
        // 检查采购清单号是否重复
        if (purchaseMainMapper.selectByPurchaseNo(main.getPurchaseNo()) != null)
            return Result.error("采购清单号已存在，请使用其他编号");
        // 自动计算总数量和总金额（如果未手动设置）
        if (main.getTotalNum() == null) main.setTotalNum(0);
        if (main.getTotalPrice() == null) main.setTotalPrice(java.math.BigDecimal.ZERO);
        purchaseMainMapper.insert(main);
        return Result.success("采购单新增成功");
    }

    public Result<String> update(PurchaseMain main, Long operatorUserId, Integer adminLevel){
        Result<String> permissionCheck = checkPurchasePermission(main.getId(), operatorUserId, adminLevel);
        if (permissionCheck != null) return permissionCheck;
        // 检查采购清单号是否与其他记录重复
        if (main.getPurchaseNo() != null && !main.getPurchaseNo().isEmpty()) {
            PurchaseMain exist = purchaseMainMapper.selectByPurchaseNo(main.getPurchaseNo());
            if (exist != null && !exist.getId().equals(main.getId()))
                return Result.error("采购清单号已存在，请使用其他编号");
        }
        purchaseMainMapper.update(main);
        return Result.success("采购单修改成功");
    }

    @Transactional
    public Result<String> delete(Long id, Long operatorUserId, Integer adminLevel){
        Result<String> permissionCheck = checkPurchasePermission(id, operatorUserId, adminLevel);
        if (permissionCheck != null) return permissionCheck;
        // 先查询采购主表获取 purchaseNo
        PurchaseMain main = purchaseMainMapper.selectById(id);
        if (main != null && main.getPurchaseNo() != null) {
            // 级联删除关联的采购明细
            purchaseDetailMapper.deleteByPurchaseNo(main.getPurchaseNo());
        }
        purchaseMainMapper.deleteById(id);
        return Result.success("采购单及其明细已删除");
    }

    /**
     * 权限校验：11管理员可操作所有采购，01管理员只能操作自己和普通用户的采购
     * @return null 表示通过，否则返回错误 Result
     */
    private Result<String> checkPurchasePermission(Long purchaseId, Long operatorUserId, Integer adminLevel) {
        if (adminLevel == null || operatorUserId == null) return Result.error("缺少操作者信息");
        if (adminLevel == 1) return null; // 一号管理员：全部权限
        if (adminLevel == 3) {
            PurchaseMain purchase = purchaseMainMapper.selectById(purchaseId);
            if (purchase == null) return Result.error("采购单不存在");
            SysUser purchaseUser = sysUserMapper.selectById(purchase.getUserId());
            if (purchaseUser == null) return Result.error("采购员工不存在");
            // 只能操作自己或普通用户的采购
            if (purchaseUser.getRole() != null && purchaseUser.getRole() == 1
                    && !purchaseUser.getId().equals(operatorUserId)) {
                return Result.error("无权操作该管理员的采购单");
            }
            return null;
        }
        return Result.error("无权操作采购单");
    }
}