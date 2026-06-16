package com.supermarket.backend.mapper;

import com.supermarket.backend.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface GoodsMapper {
    List<Goods> selectAll();
    Goods selectByGoodsCode(String goodsCode);
    int insert(Goods goods);
    int update(Goods goods);
    int deleteById(Long id);
}