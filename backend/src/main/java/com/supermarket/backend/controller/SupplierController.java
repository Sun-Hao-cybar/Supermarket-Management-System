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
            
            for (Map<String, Object> data : dataList) {
                Supplier supplier = new Supplier();
                supplier.setSupplierCode(String.valueOf(data.get("supplierCode")));
                supplier.setSupplierName(String.valueOf(data.get("supplierName")));
                supplier.setShortName(String.valueOf(data.get("shortName")));
                supplier.setAddress(String.valueOf(data.get("address")));
                supplier.setPhone(String.valueOf(data.get("phone")));
                supplier.setEmail(String.valueOf(data.get("email")));
                supplier.setContactPerson(String.valueOf(data.get("contactPerson")));
                supplier.setContactPhone(String.valueOf(data.get("contactPhone")));
                supplier.setRemark(String.valueOf(data.get("remark")));
                supplierService.add(supplier);
            }
            return Result.success("导入成功，共导入 " + dataList.size() + " 条数据");
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