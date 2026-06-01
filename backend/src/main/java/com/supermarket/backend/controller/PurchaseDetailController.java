package com.supermarket.backend.controller;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.PurchaseDetail;
import com.supermarket.backend.service.PurchaseDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
}