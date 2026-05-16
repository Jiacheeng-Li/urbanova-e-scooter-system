package com.lcyhz.urbanova.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lcyhz.urbanova.entity.WalletTransactionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface WalletTransactionMapper extends BaseMapper<WalletTransactionEntity> {
    @Select("SELECT COALESCE(SUM(amount), 0) FROM wallet_transactions WHERE user_id = #{userId}")
    BigDecimal sumAmountByUserId(@Param("userId") String userId);
}
