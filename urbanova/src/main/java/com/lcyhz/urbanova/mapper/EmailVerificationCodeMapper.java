package com.lcyhz.urbanova.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lcyhz.urbanova.entity.EmailVerificationCodeEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailVerificationCodeMapper extends BaseMapper<EmailVerificationCodeEntity> {
}
