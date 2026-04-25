package com.lcyhz.urbanova.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.entity.BookingEntity;
import com.lcyhz.urbanova.entity.DiscountRuleEntity;
import com.lcyhz.urbanova.entity.UserEntity;
import com.lcyhz.urbanova.mapper.BookingMapper;
import com.lcyhz.urbanova.mapper.DiscountRuleMapper;
import com.lcyhz.urbanova.mapper.UserMapper;
import com.lcyhz.urbanova.vo.pricing.AppliedDiscountVo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DiscountRuleService {
    private final DiscountRuleMapper discountRuleMapper;
    private final BookingMapper bookingMapper;
    private final UserMapper userMapper;

    public DiscountRuleService(DiscountRuleMapper discountRuleMapper, BookingMapper bookingMapper, UserMapper userMapper) {
        this.discountRuleMapper = discountRuleMapper;
        this.bookingMapper = bookingMapper;
        this.userMapper = userMapper;
    }

    public DiscountComputation calculateForUser(String userId, BigDecimal basePrice) {
        if (userId == null) {
            return new DiscountComputation(basePrice, BigDecimal.ZERO, basePrice, List.of(), List.of(), BigDecimal.ZERO);
        }

        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId, userId));
        if (user == null) {
            return new DiscountComputation(basePrice, BigDecimal.ZERO, basePrice, List.of(), List.of(), BigDecimal.ZERO);
        }

        Map<String, DiscountRuleEntity> activeRuleMap = discountRuleMapper.selectList(new LambdaQueryWrapper<DiscountRuleEntity>()
                        .eq(DiscountRuleEntity::getActive, 1))
                .stream()
                .collect(Collectors.toMap(DiscountRuleEntity::getType, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        BigDecimal recentHours = calculateRecentHours(userId);
        List<AppliedDiscountVo> applied = new ArrayList<>();
        LinkedHashSet<String> eligibleTypes = new LinkedHashSet<>();
        BigDecimal totalPercentage = BigDecimal.ZERO;

        DiscountRuleEntity frequentRule = activeRuleMap.get(DomainConstants.DiscountRuleType.FREQUENT_USER);
        if (frequentRule != null && frequentRule.getThresholdHoursPerWeek() != null
                && recentHours.compareTo(frequentRule.getThresholdHoursPerWeek()) >= 0) {
            eligibleTypes.add(frequentRule.getType());
            totalPercentage = totalPercentage.add(zeroIfNull(frequentRule.getPercentage()));
        }

        String category = user.getDiscountCategory();
        if (DomainConstants.DISCOUNT_STUDENT.equals(category)) {
            DiscountRuleEntity rule = activeRuleMap.get(DomainConstants.DiscountRuleType.STUDENT);
            if (rule != null) {
                eligibleTypes.add(rule.getType());
                totalPercentage = totalPercentage.add(zeroIfNull(rule.getPercentage()));
            }
        }
        if (DomainConstants.DISCOUNT_SENIOR.equals(category)) {
            DiscountRuleEntity rule = activeRuleMap.get(DomainConstants.DiscountRuleType.SENIOR);
            if (rule != null) {
                eligibleTypes.add(rule.getType());
                totalPercentage = totalPercentage.add(zeroIfNull(rule.getPercentage()));
            }
        }

        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (String type : eligibleTypes) {
            DiscountRuleEntity rule = activeRuleMap.get(type);
            BigDecimal amount = basePrice.multiply(zeroIfNull(rule.getPercentage()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            applied.add(new AppliedDiscountVo(type, amount));
            totalDiscount = totalDiscount.add(amount);
        }
        BigDecimal finalPrice = basePrice.subtract(totalDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new DiscountComputation(recentHours, totalDiscount, finalPrice, applied, List.copyOf(eligibleTypes), totalPercentage);
    }

    public Map<String, Object> getEligibility(String userId) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId, userId));
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
        }
        DiscountComputation computation = calculateForUser(userId, BigDecimal.valueOf(100));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("discountCategory", user.getDiscountCategory());
        data.put("hoursLast7Days", computation.recentHours());
        data.put("eligibleTypes", computation.eligibleTypes());
        data.put("estimatedPercentage", computation.totalPercentage());
        data.put("activeRules", listRules());
        return data;
    }

    public List<Map<String, Object>> listRules() {
        return discountRuleMapper.selectList(new LambdaQueryWrapper<DiscountRuleEntity>()
                        .orderByAsc(DiscountRuleEntity::getType))
                .stream()
                .map(this::toRuleMap)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createRule(Map<String, Object> request) {
        String type = normalizeType(requireText(request.get("type"), "type"));
        DiscountRuleEntity existing = discountRuleMapper.selectOne(new LambdaQueryWrapper<DiscountRuleEntity>()
                .eq(DiscountRuleEntity::getType, type));
        if (existing != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "Discount rule already exists");
        }

        DiscountRuleEntity entity = new DiscountRuleEntity();
        entity.setDiscountRuleId("DISC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT));
        entity.setType(type);
        entity.setThresholdHoursPerWeek(parseBigDecimal(request.get("thresholdHoursPerWeek")));
        entity.setPercentage(requireBigDecimal(request.get("percentage"), "percentage"));
        entity.setActive(parseBooleanDefaultTrue(request.get("active")) ? 1 : 0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        discountRuleMapper.insert(entity);
        return toRuleMap(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateRule(String discountRuleId, Map<String, Object> request) {
        DiscountRuleEntity entity = discountRuleMapper.selectOne(new LambdaQueryWrapper<DiscountRuleEntity>()
                .eq(DiscountRuleEntity::getDiscountRuleId, discountRuleId));
        if (entity == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Discount rule not found");
        }

        if (request.containsKey("thresholdHoursPerWeek")) {
            entity.setThresholdHoursPerWeek(parseBigDecimal(request.get("thresholdHoursPerWeek")));
        }
        if (request.containsKey("percentage")) {
            entity.setPercentage(requireBigDecimal(request.get("percentage"), "percentage"));
        }
        if (request.containsKey("active")) {
            entity.setActive(parseBooleanDefaultTrue(request.get("active")) ? 1 : 0);
        }
        entity.setUpdatedAt(LocalDateTime.now());
        discountRuleMapper.updateById(entity);
        return toRuleMap(entity);
    }

    public BigDecimal calculateRecentHours(String userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<BookingEntity> bookings = bookingMapper.selectList(new LambdaQueryWrapper<BookingEntity>()
                .eq(BookingEntity::getUserId, userId)
                .ge(BookingEntity::getCreatedAt, since)
                .in(BookingEntity::getStatus,
                        DomainConstants.BookingStatus.PENDING_PAYMENT,
                        DomainConstants.BookingStatus.CONFIRMED,
                        DomainConstants.BookingStatus.ACTIVE,
                        DomainConstants.BookingStatus.COMPLETED));

        long totalMinutes = 0L;
        for (BookingEntity booking : bookings) {
            LocalDateTime start = booking.getActualStartAt() != null ? booking.getActualStartAt() : booking.getStartAt();
            LocalDateTime end = booking.getActualEndAt() != null ? booking.getActualEndAt() : booking.getEndAt();
            if (start != null && end != null && !end.isBefore(start)) {
                totalMinutes += Duration.between(start, end).toMinutes();
            }
        }
        return BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public Map<String, Object> toRuleMap(DiscountRuleEntity entity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("discountRuleId", entity.getDiscountRuleId());
        data.put("type", entity.getType());
        data.put("thresholdHoursPerWeek", entity.getThresholdHoursPerWeek());
        data.put("percentage", entity.getPercentage());
        data.put("active", entity.getActive() != null && entity.getActive() == 1);
        data.put("createdAt", entity.getCreatedAt());
        data.put("updatedAt", entity.getUpdatedAt());
        return data;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String requireText(Object value, String field) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " is required");
        }
        return String.valueOf(value).trim();
    }

    private String normalizeType(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        Set<String> allowed = Set.of(
                DomainConstants.DiscountRuleType.FREQUENT_USER,
                DomainConstants.DiscountRuleType.STUDENT,
                DomainConstants.DiscountRuleType.SENIOR
        );
        if (!allowed.contains(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "type must be FREQUENT_USER, STUDENT, or SENIOR");
        }
        return normalized;
    }

    private BigDecimal requireBigDecimal(Object value, String field) {
        BigDecimal parsed = parseBigDecimal(value);
        if (parsed == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " is required");
        }
        return parsed.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "Invalid decimal value: " + value);
        }
    }

    private boolean parseBooleanDefaultTrue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public record DiscountComputation(BigDecimal recentHours,
                                      BigDecimal totalDiscount,
                                      BigDecimal finalPrice,
                                      List<AppliedDiscountVo> appliedDiscounts,
                                      List<String> eligibleTypes,
                                      BigDecimal totalPercentage) {
    }
}
