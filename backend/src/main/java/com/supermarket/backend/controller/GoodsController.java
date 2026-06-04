package com.supermarket.backend.controller;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.Goods;
import com.supermarket.backend.service.GoodsService;
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
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @GetMapping("/list")
    public Result<List<Goods>> list() {
        return goodsService.list();
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody Goods goods) {
        return goodsService.add(goods);
    }

    @PostMapping("/update")
    public Result<String> update(@RequestBody Goods goods) {
        return goodsService.update(goods);
    }

    @GetMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        return goodsService.delete(id);
    }

    @PostMapping("/import")
    public Result<String> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            String[] headers = {"goodsCode", "goodsName", "price", "supplierId", "intro", "remark"};
            List<Map<String, Object>> dataList = ExcelUtil.importExcel(file.getInputStream(), headers);
            
            for (Map<String, Object> data : dataList) {
                Goods goods = new Goods();
                goods.setGoodsCode(String.valueOf(data.get("goodsCode")));
                goods.setGoodsName(String.valueOf(data.get("goodsName")));
                if (data.get("price") != null) {
                    goods.setPrice(new BigDecimal(String.valueOf(data.get("price"))));
                }
                if (data.get("supplierId") != null) {
                    goods.setSupplierId(((Number) data.get("supplierId")).longValue());
                }
                goods.setIntro(String.valueOf(data.get("intro")));
                goods.setRemark(String.valueOf(data.get("remark")));
                goodsService.add(goods);
            }
            return Result.success("导入成功，共导入 " + dataList.size() + " 条数据");
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() {
        try {
            Result<List<Goods>> result = goodsService.list();
            List<Goods> dataList = result.getData();
            
            String[] headers = {"商品编号", "商品名称", "商品单价", "供应商编号", "商品简介", "备注"};
            String[] fieldNames = {"goodsCode", "goodsName", "price", "supplierId", "intro", "remark"};
            
            byte[] excelData = ExcelUtil.exportExcel(headers, fieldNames, dataList);
            
            String fileName = "商品数据.xlsx";
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