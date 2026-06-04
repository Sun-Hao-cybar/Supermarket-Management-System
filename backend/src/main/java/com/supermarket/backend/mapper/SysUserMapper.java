package com.supermarket.backend.mapper;

import com.supermarket.backend.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface SysUserMapper {
    List<SysUser> selectAll();
    SysUser selectByUsername(String username);
    SysUser selectById(Long id);
    int insert(SysUser sysUser);
    int update(SysUser sysUser);
    int delete(Long id);
    long countAdmins();
    long countAdminsByPrefix(String prefix);
    long countByPassword(String password);
    long countByPhone(String phone);
    long countEmployees();
}