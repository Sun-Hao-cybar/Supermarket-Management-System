package com.supermarket.backend.controller;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.PurchaseMain;
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

    @GetMapping("/list")
    public Result<List<PurchaseMain>> list(){
        return purchaseMainService.list();
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody PurchaseMain main){
        return purchaseMainService.add(main);
    }

    @PostMapping("/update")
    public Result<String> update(@RequestBody PurchaseMain main){
        return purchaseMainService.update(main);
    }

    @GetMapping("/delete")
    public Result<String> delete(@RequestParam Long id){
        return purchaseMainService.delete(id);
    }

    @PostMapping("/import")
    public Result<String> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            String[] headers = {"purchaseNo", "userId", "totalNum", "totalPrice", "purchaseTime", "remark"};
            List<Map<String, Object>> dataList = ExcelUtil.importExcel(file.getInputStream(), headers);
            
            for (Map<String, Object> data : dataList) {
                PurchaseMain main = new PurchaseMain();
                main.setPurchaseNo(String.valueOf(data.get("purchaseNo")));
                if (data.get("userId") != null) {
                    main.setUserId(((Number) data.get("userId")).longValue());
                }
                if (data.get("totalNum") != null) {
                    main.setTotalNum(((Number) data.get("totalNum")).intValue());
                }
                if (data.get("totalPrice") != null) {
                    main.setTotalPrice(new BigDecimal(String.valueOf(data.get("totalPrice"))));
                }
                if (data.get("purchaseTime") != null) {
                    String timeStr = String.valueOf(data.get("purchaseTime"));
                    try {
                        LocalDateTime dateTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        main.setPurchaseTime(java.sql.Timestamp.valueOf(dateTime));
                    } catch (Exception e) {
                        main.setPurchaseTime(new Date());
                    }
                }
                main.setRemark(String.valueOf(data.get("remark")));
                purchaseMainService.add(main);
            }
            return Result.success("导入成功，共导入 " + dataList.size() + " 条数据");
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
            String[] fieldNames = {"purchaseNo", "userId", "totalNum", "totalPrice", "purchaseTime", "remark"};
            
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