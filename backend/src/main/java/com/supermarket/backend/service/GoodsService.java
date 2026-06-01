package com.supermarket.backend.service;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.entity.Goods;
import com.supermarket.backend.mapper.GoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    public Result<List<Goods>> list() {
        return Result.success(goodsMapper.selectAll());
    }

    public Result<String> add(Goods goods) {
        goodsMapper.insert(goods);
        return Result.success("商品添加成功");
    }

    public Result<String> update(Goods goods) {
        goodsMapper.update(goods);
        return Result.success("商品修改成功");
    }

    public Result<String> delete(Long id) {
        goodsMapper.deleteById(id);
        return Result.success("商品删除成功");
    }
}