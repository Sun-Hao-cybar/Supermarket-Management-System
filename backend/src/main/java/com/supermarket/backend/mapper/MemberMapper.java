package com.supermarket.backend.mapper;

import com.supermarket.backend.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface MemberMapper {
    List<Member> selectAll();
    Member selectById(Long id);
    Member selectByMemberNo(String memberNo);
    int insert(Member member);
    int update(Member member);
    int delete(Long id);
}