package com.supermarket.backend.controller;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.Supplier;
import com.supermarket.backend.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
}