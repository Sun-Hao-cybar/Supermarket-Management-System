package com.supermarket.backend.controller;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.Goods;
import com.supermarket.backend.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
}