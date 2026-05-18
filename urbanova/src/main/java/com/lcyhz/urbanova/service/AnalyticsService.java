package com.lcyhz.urbanova.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.entity.BookingEntity;
import com.lcyhz.urbanova.entity.HireOptionEntity;
import com.lcyhz.urbanova.entity.PaymentEntity;
import com.lcyhz.urbanova.entity.UserEntity;
import com.lcyhz.urbanova.mapper.BookingMapper;
import com.lcyhz.urbanova.mapper.HireOptionMapper;
import com.lcyhz.urbanova.mapper.PaymentMapper;
import com.lcyhz.urbanova.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final PaymentMapper paymentMapper;
    private final BookingMapper bookingMapper;
    private final HireOptionMapper hireOptionMapper;
    private final UserMapper userMapper;
    private final DiscountRuleService discountRuleService;

    public AnalyticsService(PaymentMapper paymentMapper,
                            BookingMapper bookingMapper,
                            HireOptionMapper hireOptionMapper,
                            UserMapper userMapper,
                            DiscountRuleService discountRuleService) {
        this.paymentMapper = paymentMapper;
        this.bookingMapper = bookingMapper;
        this.hireOptionMapper = hireOptionMapper;
        this.userMapper = userMapper;
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
        int threshold = discountRuleService.resolveFrequentUserThreshold();
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getRole, DomainConstants.ROLE_CUSTOMER)
                        .eq(UserEntity::getAccountStatus, DomainConstants.ACCOUNT_ACTIVE))
                .stream()
                .map(user -> {
                    int completedBookings = discountRuleService.countCompletedBookings(user.getUserId());
                    if (completedBookings < threshold) {
                        return null;
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("userId", user.getUserId());
                    row.put("email", user.getEmail());
                    row.put("fullName", user.getFullName());
                    row.put("completedBookings", completedBookings);
                    row.put("thresholdBookings", threshold);
                    row.put("hoursLast7Days", discountRuleService.calculateRecentHours(user.getUserId()));
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

    public List<Map<String, Object>> dailyOptionIncome(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedStart = startDate == null ? LocalDate.now().minusDays(30) : startDate;
        LocalDate resolvedEnd = endDate == null ? LocalDate.now() : endDate;

        LocalDateTime startAt = resolvedStart.atStartOfDay();
        LocalDateTime endAt = resolvedEnd.plusDays(1).atStartOfDay();

        // 1. 获取所有租赁选项，建立 id -> code 的映射 (HIRE-H1 -> H1, HIRE-H4 -> H4, HIRE-D1 -> D1, HIRE-W1 -> W1)
        Map<String, String> hireOptionCodeMap = hireOptionMapper.selectList(new LambdaQueryWrapper<>())
                .stream()
                .collect(Collectors.toMap(
                        HireOptionEntity::getHireOptionId,
                        HireOptionEntity::getCode
                ));

        // 2. 直接查询订单表，状态为 COMPLETED 或 PAID 的订单
        List<BookingEntity> bookings = bookingMapper.selectList(new LambdaQueryWrapper<BookingEntity>()
                .in(BookingEntity::getPaymentStatus,
                        DomainConstants.PAYMENT_STATUS_PAID)
                .ge(BookingEntity::getCreatedAt, startAt)
                .lt(BookingEntity::getCreatedAt, endAt));

        if (bookings.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 按日期和方案代码分组统计收入
        // 数据结构: Map<日期, Map<方案代码, 收入>>
        Map<String, Map<String, BigDecimal>> dailyOptionRevenue = new LinkedHashMap<>();

        // 用于记录所有出现的方案代码
        Set<String> allOptionCodes = new LinkedHashSet<>();

        for (BookingEntity booking : bookings) {
            String hireOptionId = booking.getHireOptionId();
            String optionCode = hireOptionCodeMap.get(hireOptionId);
            if (optionCode == null) continue;

            allOptionCodes.add(optionCode);

            LocalDate date = booking.getCreatedAt().toLocalDate();
            String dateStr = date.toString();
            BigDecimal amount = booking.getPriceFinal() != null ? booking.getPriceFinal() : BigDecimal.ZERO;

            dailyOptionRevenue.computeIfAbsent(dateStr, k -> new LinkedHashMap<>())
                    .merge(optionCode, amount, BigDecimal::add);
        }

        // 4. 构建返回数据，确保每天都有所有方案代码的字段
        List<Map<String, Object>> result = new ArrayList<>();

        // 对日期排序
        List<String> sortedDates = new ArrayList<>(dailyOptionRevenue.keySet());
        Collections.sort(sortedDates);

        for (String dateStr : sortedDates) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", dateStr);

            Map<String, BigDecimal> optionRevenues = dailyOptionRevenue.get(dateStr);
            BigDecimal dailyTotal = BigDecimal.ZERO;

            for (String optionCode : allOptionCodes) {
                BigDecimal revenue = optionRevenues.getOrDefault(optionCode, BigDecimal.ZERO);
                row.put(optionCode, revenue.setScale(2, RoundingMode.HALF_UP));
                dailyTotal = dailyTotal.add(revenue);
            }

            row.put("total", dailyTotal.setScale(2, RoundingMode.HALF_UP));
            result.add(row);
        }

        return result;
    }

    public List<Map<String, Object>> dailyTimeScooter(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedStart = startDate == null ? LocalDate.now().minusDays(7) : startDate;
        LocalDate resolvedEnd = endDate == null ? LocalDate.now() : endDate;

        LocalDateTime queryStartAt = resolvedStart.atStartOfDay();
        LocalDateTime queryEndAt = resolvedEnd.plusDays(1).atStartOfDay();

        // 查询所有与查询时间范围有交集的 booking
        List<BookingEntity> bookings = bookingMapper.selectList(new LambdaQueryWrapper<BookingEntity>()
                .lt(BookingEntity::getStartAt, queryEndAt)   // 开始时间 < 查询结束时间
                .gt(BookingEntity::getEndAt, queryStartAt)   // 结束时间 > 查询开始时间
                .eq(BookingEntity::getStatus, DomainConstants.BookingStatus.COMPLETED));

        // 按小时统计用车数量（0-23）
        Map<Integer, Integer> hourlyCount = new LinkedHashMap<>();
        for (int i = 0; i < 24; i++) {
            hourlyCount.put(i, 0);
        }

        for (BookingEntity booking : bookings) {
            // 确定实际统计的开始时间：取订单开始时间和查询开始时间的较大值
            LocalDateTime actualStart = booking.getStartAt().isBefore(queryStartAt) ? queryStartAt : booking.getStartAt();
            // 确定实际统计的结束时间：取订单结束时间和查询结束时间的较小值
            LocalDateTime actualEnd = booking.getEndAt().isAfter(queryEndAt) ? queryEndAt : booking.getEndAt();

            if (actualStart.isAfter(actualEnd) || actualStart.isEqual(actualEnd)) {
                continue;
            }

            // 从 actualStart 开始，按小时递增，每个小时段都 +1
            // 注意：14:30 属于 14:00-15:00 这个时段，所以取 hour
            LocalDateTime current = actualStart;
            while (current.isBefore(actualEnd)) {
                int hour = current.getHour();
                hourlyCount.merge(hour, 1, Integer::sum);
                current = current.plusHours(1);
            }
        }

        // 构建返回数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : hourlyCount.entrySet()) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("hour", entry.getKey());
            point.put("hourLabel", String.format("%02d:00", entry.getKey()));
            point.put("scooterCount", entry.getValue());
            result.add(point);
        }

        return result;
    }
}
