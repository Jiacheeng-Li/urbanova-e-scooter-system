package com.lcyhz.urbanova.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lcyhz.urbanova.entity.IdempotencyKeyEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IdempotencyKeyMapper extends BaseMapper<IdempotencyKeyEntity> {
}
