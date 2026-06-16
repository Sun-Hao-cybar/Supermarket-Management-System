package com.supermarket.backend.controller;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.Supplier;
import com.supermarket.backend.service.SupplierService;
import com.supermarket.backend.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/supplier")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping("/list")
    public Result<List<Supplier>> list() {
        return supplierService.list();
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody Supplier supplier) {
        return supplierService.add(supplier);
    }

    @PostMapping("/update")
    public Result<String> update(@RequestBody Supplier supplier) {
        return supplierService.update(supplier);
    }

    @GetMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        return supplierService.delete(id);
    }

    @PostMapping("/import")
    public Result<String> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            String[] headers = {"supplierCode", "supplierName", "shortName", "address", "phone", "email", "contactPerson", "contactPhone", "remark"};
            List<Map<String, Object>> dataList = ExcelUtil.importExcel(file.getInputStream(), headers);

            int successCount = 0;
            for (int i = 0; i < dataList.size(); i++) {
                Map<String, Object> data = dataList.get(i);
                int rowNum = i + 2;
                try {
                    Supplier supplier = new Supplier();
                    supplier.setSupplierCode(ExcelUtil.getString(data, "supplierCode"));
                    supplier.setSupplierName(ExcelUtil.getString(data, "supplierName"));
                    supplier.setShortName(ExcelUtil.getString(data, "shortName"));
                    supplier.setAddress(ExcelUtil.getString(data, "address"));
                    supplier.setPhone(ExcelUtil.getString(data, "phone"));
                    supplier.setEmail(ExcelUtil.getString(data, "email"));
                    supplier.setContactPerson(ExcelUtil.getString(data, "contactPerson"));
                    supplier.setContactPhone(ExcelUtil.getString(data, "contactPhone"));
                    supplier.setRemark(ExcelUtil.getString(data, "remark"));
                    Result<String> res = supplierService.add(supplier);
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
            Result<List<Supplier>> result = supplierService.list();
            List<Supplier> dataList = result.getData();
            
            String[] headers = {"供应商编号", "供应商名称", "简称", "地址", "公司电话", "邮件", "联系人", "联系人电话", "备注"};
            String[] fieldNames = {"supplierCode", "supplierName", "shortName", "address", "phone", "email", "contactPerson", "contactPhone", "remark"};
            
            byte[] excelData = ExcelUtil.exportExcel(headers, fieldNames, dataList);
            
            String fileName = "供应商数据.xlsx";
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