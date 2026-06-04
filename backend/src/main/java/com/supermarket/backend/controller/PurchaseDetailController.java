package com.supermarket.backend.controller;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.PurchaseDetail;
import com.supermarket.backend.service.PurchaseDetailService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/purchaseDetail")
public class PurchaseDetailController {
    @Autowired
    private PurchaseDetailService purchaseDetailService;

    @GetMapping("/list")
    public Result<List<PurchaseDetail>> list(){
        return purchaseDetailService.list();
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody PurchaseDetail detail){
        return purchaseDetailService.add(detail);
    }

    @PostMapping("/update")
    public Result<String> update(@RequestBody PurchaseDetail detail){
        return purchaseDetailService.update(detail);
    }

    @GetMapping("/delete")
    public Result<String> delete(@RequestParam Long id){
        return purchaseDetailService.delete(id);
    }

    @PostMapping("/import")
    public Result<String> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            String[] headers = {"detailNo", "purchaseNo", "goodsId", "goodsNum", "goodsPrice", "totalPrice", "remark"};
            List<Map<String, Object>> dataList = ExcelUtil.importExcel(file.getInputStream(), headers);
            
            for (Map<String, Object> data : dataList) {
                PurchaseDetail detail = new PurchaseDetail();
                detail.setDetailNo(String.valueOf(data.get("detailNo")));
                detail.setPurchaseNo(String.valueOf(data.get("purchaseNo")));
                if (data.get("goodsId") != null) {
                    detail.setGoodsId(((Number) data.get("goodsId")).longValue());
                }
                if (data.get("goodsNum") != null) {
                    detail.setGoodsNum(((Number) data.get("goodsNum")).intValue());
                }
                if (data.get("goodsPrice") != null) {
                    detail.setGoodsPrice(new BigDecimal(String.valueOf(data.get("goodsPrice"))));
                }
                if (data.get("totalPrice") != null) {
                    detail.setTotalPrice(new BigDecimal(String.valueOf(data.get("totalPrice"))));
                }
                detail.setRemark(String.valueOf(data.get("remark")));
                purchaseDetailService.add(detail);
            }
            return Result.success("导入成功，共导入 " + dataList.size() + " 条数据");
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() {
        try {
            Result<List<PurchaseDetail>> result = purchaseDetailService.list();
            List<PurchaseDetail> dataList = result.getData();
            
            String[] headers = {"明细号", "采购清单号", "商品编号", "采购数量", "商品单价", "商品总价", "备注"};
            String[] fieldNames = {"detailNo", "purchaseNo", "goodsId", "goodsNum", "goodsPrice", "totalPrice", "remark"};
            
            byte[] excelData = ExcelUtil.exportExcel(headers, fieldNames, dataList);
            
            String fileName = "采购明细数据.xlsx";
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