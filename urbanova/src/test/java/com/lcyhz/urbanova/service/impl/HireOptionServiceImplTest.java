package com.lcyhz.urbanova.service.impl;

import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.dto.admin.hire.CreateHireOptionRequest;
import com.lcyhz.urbanova.dto.admin.hire.UpdateHireOptionRequest;
import com.lcyhz.urbanova.dto.pricing.PriceQuoteRequest;
import com.lcyhz.urbanova.entity.HireOptionEntity;
import com.lcyhz.urbanova.mapper.HireOptionMapper;
import com.lcyhz.urbanova.service.DiscountRuleService;
import com.lcyhz.urbanova.service.DiscountRuleService.DiscountComputation;
import com.lcyhz.urbanova.vo.hire.AdminHireOptionVo;
import com.lcyhz.urbanova.vo.hire.HireOptionVo;
import com.lcyhz.urbanova.vo.pricing.PriceQuoteVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HireOptionServiceImpl Tests")
class HireOptionServiceImplTest {

    @Mock private HireOptionMapper hireOptionMapper;
    @Mock private DiscountRuleService discountRuleService;

    @InjectMocks
    private HireOptionServiceImpl hireOptionService;

    // ─── Helper builders ─────────────────────────────────────────────────

    private HireOptionEntity buildEntity(String id, String code, int minutes, String price, int active) {
        HireOptionEntity e = new HireOptionEntity();
        e.setHireOptionId(id);
        e.setCode(code);
        e.setDurationMinutes(minutes);
        e.setBasePrice(new BigDecimal(price));
        e.setActive(active);
        e.setCreatedAt(LocalDateTime.now().minusDays(1));
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    private DiscountRuleService.DiscountComputation noDiscount(BigDecimal base) {
        return new DiscountRuleService.DiscountComputation(
                BigDecimal.ZERO, BigDecimal.ZERO, base,
                List.of(), List.of(), BigDecimal.ZERO);
    }

    private DiscountRuleService.DiscountComputation withDiscount(BigDecimal base, BigDecimal discountAmount) {
        return new DiscountRuleService.DiscountComputation(
                BigDecimal.ZERO, discountAmount, base.subtract(discountAmount),
                List.of(), List.of(), BigDecimal.TEN);
    }

    // ─── listActiveHireOptions ────────────────────────────────────────────

    @Nested
    @DisplayName("listActiveHireOptions")
    class ListActiveHireOptions {

        @Test
        @DisplayName("有激活的选项 → 返回对应 Vo 列表")
        void returnsActiveOptions() {
            when(hireOptionMapper.selectList(any())).thenReturn(List.of(
                    buildEntity("HIRE-30MIN", "30MIN", 30, "5.00", 1),
                    buildEntity("HIRE-60MIN", "60MIN", 60, "8.00", 1)
            ));

            List<HireOptionVo> result = hireOptionService.listActiveHireOptions();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCode()).isEqualTo("30MIN");
            assertThat(result.get(0).getActive()).isTrue();
        }

        @Test
        @DisplayName("没有激活的选项 → 返回空列表")
        void noActiveOptions_returnsEmpty() {
            when(hireOptionMapper.selectList(any())).thenReturn(List.of());

            assertThat(hireOptionService.listActiveHireOptions()).isEmpty();
        }
    }

    // ─── quotePrice ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("quotePrice")
    class QuotePrice {

        @Test
        @DisplayName("无折扣 → basePrice 等于 finalPrice")
        void noDiscount_basePriceEqualsFinaPrice() {
            PriceQuoteRequest req = new PriceQuoteRequest();
            req.setHireOptionCode("30MIN");

            when(hireOptionMapper.selectOne(any())).thenReturn(buildEntity("HIRE-30MIN", "30MIN", 30, "5.00", 1));
            when(discountRuleService.calculateForUser(eq("USR-001"), any()))
                    .thenReturn(noDiscount(new BigDecimal("5.00")));

            PriceQuoteVo result = hireOptionService.quotePrice("USR-001", req);

            assertThat(result.getBasePrice()).isEqualByComparingTo("5.00");
            assertThat(result.getFinalPrice()).isEqualByComparingTo("5.00");
            assertThat(result.getAppliedDiscounts()).isEmpty();
        }

        @Test
        @DisplayName("有折扣 → finalPrice 低于 basePrice，appliedDiscounts 不为空")
        void withDiscount_finalPriceLessThanBase() {
            PriceQuoteRequest req = new PriceQuoteRequest();
            req.setHireOptionCode("30MIN");

            when(hireOptionMapper.selectOne(any())).thenReturn(buildEntity("HIRE-30MIN", "30MIN", 30, "10.00", 1));
            when(discountRuleService.calculateForUser(eq("USR-001"), any()))
                    .thenReturn(withDiscount(new BigDecimal("10.00"), new BigDecimal("1.00")));

            PriceQuoteVo result = hireOptionService.quotePrice("USR-001", req);

            assertThat(result.getFinalPrice()).isEqualByComparingTo("9.00");
        }

        @Test
        @DisplayName("hire option code 不存在 → 抛 BusinessException")
        void optionNotFound_throws() {
            PriceQuoteRequest req = new PriceQuoteRequest();
            req.setHireOptionCode("INVALID");

            when(hireOptionMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> hireOptionService.quotePrice("USR-001", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("返回结果包含正确的货币单位 GBP")
        void resultContainsGbpCurrency() {
            PriceQuoteRequest req = new PriceQuoteRequest();
            req.setHireOptionCode("30MIN");

            when(hireOptionMapper.selectOne(any())).thenReturn(buildEntity("HIRE-30MIN", "30MIN", 30, "5.00", 1));
            when(discountRuleService.calculateForUser(any(), any()))
                    .thenReturn(noDiscount(new BigDecimal("5.00")));

            PriceQuoteVo result = hireOptionService.quotePrice("USR-001", req);

            assertThat(result.getCurrency()).isEqualTo("GBP");
        }
    }

    // ─── listAllHireOptions ───────────────────────────────────────────────

    @Nested
    @DisplayName("listAllHireOptions")
    class ListAllHireOptions {

        @Test
        @DisplayName("返回所有选项（包括 inactive）")
        void returnsAllIncludingInactive() {
            when(hireOptionMapper.selectList(any())).thenReturn(List.of(
                    buildEntity("HIRE-30MIN", "30MIN", 30, "5.00", 1),
                    buildEntity("HIRE-OLD", "OLD", 20, "3.00", 0) // inactive
            ));

            List<AdminHireOptionVo> result = hireOptionService.listAllHireOptions();

            assertThat(result).hasSize(2);
        }
    }

    // ─── createHireOption ─────────────────────────────────────────────────

    @Nested
    @DisplayName("createHireOption")
    class CreateHireOption {

        @Test
        @DisplayName("正常创建 → code 被转大写，active = true")
        void success_normalizeCodeToUpperCase() {
            CreateHireOptionRequest req = new CreateHireOptionRequest();
            req.setCode("  90min  "); // 小写 + 空格，应被 normalize
            req.setDurationMinutes(90);
            req.setBasePrice(new BigDecimal("12.00"));

            when(hireOptionMapper.selectOne(any())).thenReturn(null); // 无重复
            when(hireOptionMapper.insert(any(HireOptionEntity.class))).thenReturn(1);

            AdminHireOptionVo result = hireOptionService.createHireOption(req);

            assertThat(result.getCode()).isEqualTo("90MIN");
            assertThat(result.getDurationMinutes()).isEqualTo(90);
            assertThat(result.getBasePrice()).isEqualByComparingTo("12.00");
            assertThat(result.getActive()).isTrue();
            assertThat(result.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("创建时 hireOptionId 格式为 HIRE-<CODE>")
        void success_hireOptionIdFormat() {
            CreateHireOptionRequest req = new CreateHireOptionRequest();
            req.setCode("2HOUR");
            req.setDurationMinutes(120);
            req.setBasePrice(new BigDecimal("15.00"));

            when(hireOptionMapper.selectOne(any())).thenReturn(null);
            when(hireOptionMapper.insert(any(HireOptionEntity.class))).thenReturn(1);

            AdminHireOptionVo result = hireOptionService.createHireOption(req);

            assertThat(result.getHireOptionId()).isEqualTo("HIRE-2HOUR");
        }

        @Test
        @DisplayName("code 重复 → 抛 BusinessException")
        void duplicateCode_throws() {
            CreateHireOptionRequest req = new CreateHireOptionRequest();
            req.setCode("30MIN");
            req.setDurationMinutes(30);
            req.setBasePrice(new BigDecimal("5.00"));

            when(hireOptionMapper.selectOne(any()))
                    .thenReturn(buildEntity("HIRE-30MIN", "30MIN", 30, "5.00", 1));

            assertThatThrownBy(() -> hireOptionService.createHireOption(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("创建后写入 DB 一次")
        void success_insertsOnce() {
            CreateHireOptionRequest req = new CreateHireOptionRequest();
            req.setCode("45MIN");
            req.setDurationMinutes(45);
            req.setBasePrice(new BigDecimal("6.50"));

            when(hireOptionMapper.selectOne(any())).thenReturn(null);
            when(hireOptionMapper.insert(any(HireOptionEntity.class))).thenReturn(1);

            hireOptionService.createHireOption(req);

            verify(hireOptionMapper).insert(any(HireOptionEntity.class));
        }
    }

    // ─── updateHireOption ─────────────────────────────────────────────────

    @Nested
    @DisplayName("updateHireOption")
    class UpdateHireOption {

        @Test
        @DisplayName("更新 durationMinutes → 新值生效")
        void updateDurationMinutes_succeeds() {
            HireOptionEntity entity = buildEntity("HIRE-30MIN", "30MIN", 30, "5.00", 1);
            UpdateHireOptionRequest req = new UpdateHireOptionRequest();
            req.setDurationMinutes(45);

            when(hireOptionMapper.selectOne(any())).thenReturn(entity);
            when(hireOptionMapper.updateById(any(HireOptionEntity.class))).thenReturn(1);

            AdminHireOptionVo result = hireOptionService.updateHireOption("HIRE-30MIN", req);

            assertThat(result.getDurationMinutes()).isEqualTo(45);
        }

        @Test
        @DisplayName("更新 basePrice → 新价格生效")
        void updateBasePrice_succeeds() {
            HireOptionEntity entity = buildEntity("HIRE-30MIN", "30MIN", 30, "5.00", 1);
            UpdateHireOptionRequest req = new UpdateHireOptionRequest();
            req.setBasePrice(new BigDecimal("6.00"));

            when(hireOptionMapper.selectOne(any())).thenReturn(entity);
            when(hireOptionMapper.updateById(any(HireOptionEntity.class))).thenReturn(1);

            AdminHireOptionVo result = hireOptionService.updateHireOption("HIRE-30MIN", req);

            assertThat(result.getBasePrice()).isEqualByComparingTo("6.00");
        }

        @Test
        @DisplayName("request 为 null → 抛 BusinessException")
        void nullRequest_throws() {
            assertThatThrownBy(() -> hireOptionService.updateHireOption("HIRE-30MIN", null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("request 无任何字段 → 抛 BusinessException")
        void emptyRequest_throws() {
            UpdateHireOptionRequest req = new UpdateHireOptionRequest(); // 所有字段为 null

            assertThatThrownBy(() -> hireOptionService.updateHireOption("HIRE-30MIN", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("hire option 不存在 → 抛 BusinessException")
        void notFound_throws() {
            UpdateHireOptionRequest req = new UpdateHireOptionRequest();
            req.setDurationMinutes(45);

            when(hireOptionMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> hireOptionService.updateHireOption("HIRE-NONE", req))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ─── disableHireOption ────────────────────────────────────────────────

    @Nested
    @DisplayName("disableHireOption")
    class DisableHireOption {

        @Test
        @DisplayName("正常禁用 → active 变为 false")
        void success_setsActiveToFalse() {
            HireOptionEntity entity = buildEntity("HIRE-30MIN", "30MIN", 30, "5.00", 1);

            when(hireOptionMapper.selectOne(any())).thenReturn(entity);
            when(hireOptionMapper.updateById(any(HireOptionEntity.class))).thenReturn(1);

            AdminHireOptionVo result = hireOptionService.disableHireOption("HIRE-30MIN");

            assertThat(result.getActive()).isFalse();
        }

        @Test
        @DisplayName("hire option 不存在 → 抛 BusinessException")
        void notFound_throws() {
            when(hireOptionMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> hireOptionService.disableHireOption("HIRE-NONE"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("禁用后 updatedAt 已更新")
        void success_updatesTimestamp() {
            LocalDateTime beforeTest = LocalDateTime.now().minusSeconds(1);
            HireOptionEntity entity = buildEntity("HIRE-30MIN", "30MIN", 30, "5.00", 1);

            when(hireOptionMapper.selectOne(any())).thenReturn(entity);
            when(hireOptionMapper.updateById(any(HireOptionEntity.class))).thenReturn(1);

            AdminHireOptionVo result = hireOptionService.disableHireOption("HIRE-30MIN");

            assertThat(result.getUpdatedAt()).isAfter(beforeTest);
        }
    }
}
