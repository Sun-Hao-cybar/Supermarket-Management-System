package com.supermarket.backend.controller;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.PurchaseMain;
import com.supermarket.backend.service.PurchaseMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
}