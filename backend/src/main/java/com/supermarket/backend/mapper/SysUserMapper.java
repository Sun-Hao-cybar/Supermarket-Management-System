package com.supermarket.backend.mapper;

import com.supermarket.backend.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface SysUserMapper {
    List<SysUser> selectAll();
    SysUser selectByUsername(String username);
}