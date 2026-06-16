package com.supermarket.backend.controller;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.PurchaseMain;
import com.supermarket.backend.entity.SysUser;
import com.supermarket.backend.mapper.SysUserMapper;
import com.supermarket.backend.service.PurchaseMainService;
import com.supermarket.backend.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/purchaseMain")
public class PurchaseMainController {
    @Autowired
    private PurchaseMainService purchaseMainService;
    @Autowired
    private SysUserMapper sysUserMapper;

    @GetMapping("/list")
    public Result<List<PurchaseMain>> list(){
        return purchaseMainService.list();
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody PurchaseMain main){
        return purchaseMainService.add(main);
    }

    @PostMapping("/update")
    public Result<String> update(@RequestBody PurchaseMain main,
            @RequestParam(required = false) Long operatorUserId,
            @RequestParam(required = false) Integer adminLevel){
        return purchaseMainService.update(main, operatorUserId, adminLevel);
    }

    @GetMapping("/delete")
    public Result<String> delete(@RequestParam Long id,
            @RequestParam(required = false) Long operatorUserId,
            @RequestParam(required = false) Integer adminLevel){
        return purchaseMainService.delete(id, operatorUserId, adminLevel);
    }

    @PostMapping("/import")
    public Result<String> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            String[] headers = {"purchaseNo", "userName", "totalNum", "totalPrice", "purchaseTime", "remark"};
            List<Map<String, Object>> dataList = ExcelUtil.importExcel(file.getInputStream(), headers);

            int successCount = 0;
            for (int i = 0; i < dataList.size(); i++) {
                Map<String, Object> data = dataList.get(i);
                int rowNum = i + 2;
                try {
                    PurchaseMain main = new PurchaseMain();
                    main.setPurchaseNo(ExcelUtil.getString(data, "purchaseNo"));
                    // 支持员工编号（username）或数字ID
                    String userNameVal = ExcelUtil.getString(data, "userName");
                    if (!userNameVal.isEmpty()) {
                        SysUser user = sysUserMapper.selectByUsername(userNameVal);
                        if (user != null) {
                            main.setUserId(user.getId());
                        } else {
                            try {
                                main.setUserId(Long.parseLong(userNameVal));
                            } catch (NumberFormatException e) {
                                return Result.error("第" + rowNum + "行：员工编号不存在");
                            }
                        }
                    }
                    if (data.get("totalNum") != null) {
                        main.setTotalNum(((Number) data.get("totalNum")).intValue());
                    }
                    if (data.get("totalPrice") != null) {
                        main.setTotalPrice(new BigDecimal(String.valueOf(data.get("totalPrice"))));
                    }
                    if (data.get("purchaseTime") != null) {
                        java.util.Date pt = ExcelUtil.getDate(data, "purchaseTime");
                        main.setPurchaseTime(pt != null ? pt : new Date());
                    }
                    main.setRemark(ExcelUtil.getString(data, "remark"));
                    Result<String> res = purchaseMainService.add(main);
                    if (res.getCode() == 200) {
                        successCount++;
                    } else {
                        return Result.error("第" + rowNum + "行导入失败：" + res.getMsg());
                    }
                } catch (Exception e) {
                    return Result.error("第" + rowNum + "行导入失败：" + e.getMessage());
                }
            }
            return Result.success("导入成功，共导入 " + successCount + " 条数据");
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() {
        try {
            Result<List<PurchaseMain>> result = purchaseMainService.list();
            List<PurchaseMain> dataList = result.getData();
            
            String[] headers = {"采购清单号", "员工编号", "采购数量", "采购总价", "采购时间", "备注"};
            String[] fieldNames = {"purchaseNo", "userName", "totalNum", "totalPrice", "purchaseTime", "remark"};
            
            byte[] excelData = ExcelUtil.exportExcel(headers, fieldNames, dataList);
            
            String fileName = "采购主表数据.xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDispositionFormData("attachment", encodedFileName);
            
            return new ResponseEntity<>(excelData, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}