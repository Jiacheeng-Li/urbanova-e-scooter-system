package com.lcyhz.urbanova.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.entity.BookingEntity;
import com.lcyhz.urbanova.entity.DiscountRuleEntity;
import com.lcyhz.urbanova.entity.HireOptionEntity;
import com.lcyhz.urbanova.entity.PaymentEntity;
import com.lcyhz.urbanova.entity.UserEntity;
import com.lcyhz.urbanova.mapper.BookingMapper;
import com.lcyhz.urbanova.mapper.DiscountRuleMapper;
import com.lcyhz.urbanova.mapper.HireOptionMapper;
import com.lcyhz.urbanova.mapper.PaymentMapper;
import com.lcyhz.urbanova.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final PaymentMapper paymentMapper;
    private final BookingMapper bookingMapper;
    private final HireOptionMapper hireOptionMapper;
    private final UserMapper userMapper;
    private final DiscountRuleMapper discountRuleMapper;
    private final DiscountRuleService discountRuleService;

    public AnalyticsService(PaymentMapper paymentMapper,
                            BookingMapper bookingMapper,
                            HireOptionMapper hireOptionMapper,
                            UserMapper userMapper,
                            DiscountRuleMapper discountRuleMapper,
                            DiscountRuleService discountRuleService) {
        this.paymentMapper = paymentMapper;
        this.bookingMapper = bookingMapper;
        this.hireOptionMapper = hireOptionMapper;
        this.userMapper = userMapper;
        this.discountRuleMapper = discountRuleMapper;
        this.discountRuleService = discountRuleService;
    }

    public Map<String, Object> revenueEstimate(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedStart = startDate == null ? LocalDate.now().minusDays(6) : startDate;
        LocalDate resolvedEnd = endDate == null ? LocalDate.now() : endDate;
        BigDecimal total = paymentsInRange(resolvedStart, resolvedEnd).stream()
                .map(this::netPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("startDate", resolvedStart);
        data.put("endDate", resolvedEnd);
        data.put("currency", DomainConstants.CURRENCY_GBP);
        data.put("estimatedRevenue", total);
        return data;
    }

    public List<Map<String, Object>> weeklyByHireOption(LocalDate startDate) {
        LocalDate resolvedStart = startDate == null ? LocalDate.now().minusDays(6) : startDate;
        LocalDate resolvedEnd = resolvedStart.plusDays(6);
        Map<String, BookingEntity> bookingMap = bookingMapper.selectList(new LambdaQueryWrapper<BookingEntity>())
                .stream().collect(Collectors.toMap(BookingEntity::getBookingId, Function.identity()));
        Map<String, HireOptionEntity> hireMap = hireOptionMapper.selectList(new LambdaQueryWrapper<HireOptionEntity>())
                .stream().collect(Collectors.toMap(HireOptionEntity::getHireOptionId, Function.identity()));
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (HireOptionEntity hireOption : hireMap.values()) {
            totals.put(hireOption.getCode(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        for (PaymentEntity payment : paymentsInRange(resolvedStart, resolvedEnd)) {
            BookingEntity booking = bookingMap.get(payment.getBookingId());
            if (booking == null) {
                continue;
            }
            HireOptionEntity hireOption = hireMap.get(booking.getHireOptionId());
            if (hireOption == null) {
                continue;
            }
            totals.put(hireOption.getCode(), totals.getOrDefault(hireOption.getCode(), BigDecimal.ZERO).add(netPayment(payment)).setScale(2, RoundingMode.HALF_UP));
        }
        List<Map<String, Object>> data = new ArrayList<>();
        totals.forEach((code, total) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("hireOptionCode", code);
            row.put("weeklyRevenue", total);
            row.put("startDate", resolvedStart);
            row.put("endDate", resolvedEnd);
            data.add(row);
        });
        return data;
    }

    public List<Map<String, Object>> dailyCombined(LocalDate startDate) {
        LocalDate resolvedStart = startDate == null ? LocalDate.now().minusDays(6) : startDate;
        LocalDate resolvedEnd = resolvedStart.plusDays(6);
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = resolvedStart.plusDays(i);
            BigDecimal total = paymentsInRange(day, day).stream().map(this::netPayment).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", day);
            row.put("dailyRevenue", total);
            data.add(row);
        }
        return data;
    }

    public Map<String, Object> weeklyChart(LocalDate startDate) {
        List<Map<String, Object>> points = dailyCombined(startDate);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("series", points);
        data.put("currency", DomainConstants.CURRENCY_GBP);
        return data;
    }

    public List<Map<String, Object>> frequentUsers() {
        DiscountRuleEntity frequentRule = discountRuleMapper.selectOne(new LambdaQueryWrapper<DiscountRuleEntity>()
                .eq(DiscountRuleEntity::getType, DomainConstants.DiscountRuleType.FREQUENT_USER)
                .eq(DiscountRuleEntity::getActive, 1));
        BigDecimal threshold = frequentRule == null || frequentRule.getThresholdHoursPerWeek() == null
                ? BigDecimal.valueOf(8).setScale(2, RoundingMode.HALF_UP)
                : frequentRule.getThresholdHoursPerWeek();
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getRole, DomainConstants.ROLE_CUSTOMER)
                        .eq(UserEntity::getAccountStatus, DomainConstants.ACCOUNT_ACTIVE))
                .stream()
                .map(user -> {
                    BigDecimal hours = discountRuleService.calculateRecentHours(user.getUserId());
                    if (hours.compareTo(threshold) < 0) {
                        return null;
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("userId", user.getUserId());
                    row.put("email", user.getEmail());
                    row.put("fullName", user.getFullName());
                    row.put("hoursLast7Days", hours);
                    row.put("thresholdHours", threshold);
                    return row;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<PaymentEntity> paymentsInRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();
        return paymentMapper.selectList(new LambdaQueryWrapper<PaymentEntity>()
                .in(PaymentEntity::getStatus, DomainConstants.PaymentStatus.SUCCEEDED, DomainConstants.PaymentStatus.REFUNDED)
                .ge(PaymentEntity::getCreatedAt, startAt)
                .lt(PaymentEntity::getCreatedAt, endAt));
    }

    private BigDecimal netPayment(PaymentEntity payment) {
        return payment.getAmount().subtract(payment.getRefundedAmount() == null ? BigDecimal.ZERO : payment.getRefundedAmount())
                .setScale(2, RoundingMode.HALF_UP);
    }
}
