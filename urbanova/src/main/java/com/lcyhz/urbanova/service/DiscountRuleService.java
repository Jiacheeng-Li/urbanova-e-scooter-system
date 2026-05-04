package com.lcyhz.urbanova.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.entity.BookingEntity;
import com.lcyhz.urbanova.entity.PromotionPolicyEntity;
import com.lcyhz.urbanova.entity.UserEntity;
import com.lcyhz.urbanova.mapper.BookingMapper;
import com.lcyhz.urbanova.mapper.PromotionPolicyMapper;
import com.lcyhz.urbanova.mapper.UserMapper;
import com.lcyhz.urbanova.service.support.UserAgeSupport;
import com.lcyhz.urbanova.vo.pricing.AppliedDiscountVo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DiscountRuleService {
    private static final BigDecimal MAX_TOTAL_PERCENTAGE = BigDecimal.valueOf(30);

    private final PromotionPolicyMapper promotionPolicyMapper;
    private final BookingMapper bookingMapper;
    private final UserMapper userMapper;

    public DiscountRuleService(PromotionPolicyMapper promotionPolicyMapper,
                               BookingMapper bookingMapper,
                               UserMapper userMapper) {
        this.promotionPolicyMapper = promotionPolicyMapper;
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

        PromotionContext context = buildContext(user);
        List<PromotionPolicyEntity> selectedPolicies = selectApplicablePolicies(context);
        if (selectedPolicies.isEmpty()) {
            return new DiscountComputation(context.hoursLast7Days(), BigDecimal.ZERO, basePrice, List.of(), List.of(), BigDecimal.ZERO);
        }

        BigDecimal totalPercentage = selectedPolicies.stream()
                .map(PromotionPolicyEntity::getPercentage)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalPercentage.compareTo(MAX_TOTAL_PERCENTAGE) > 0) {
            totalPercentage = MAX_TOTAL_PERCENTAGE;
        }

        List<AppliedDiscountVo> appliedDiscounts = new ArrayList<>();
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (PromotionPolicyEntity policy : selectedPolicies) {
            BigDecimal amount = basePrice.multiply(zeroIfNull(policy.getPercentage()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            appliedDiscounts.add(new AppliedDiscountVo(policy.getPolicyCode(), amount));
            totalDiscount = totalDiscount.add(amount);
        }

        BigDecimal cappedDiscount = basePrice.multiply(totalPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (totalDiscount.compareTo(cappedDiscount) > 0) {
            totalDiscount = cappedDiscount;
        }

        BigDecimal finalPrice = basePrice.subtract(totalDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        List<String> eligibleTypes = selectedPolicies.stream().map(PromotionPolicyEntity::getPolicyCode).toList();
        return new DiscountComputation(context.hoursLast7Days(), totalDiscount, finalPrice, appliedDiscounts, eligibleTypes, totalPercentage);
    }

    public Map<String, Object> getEligibility(String userId) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId, userId));
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
        }

        PromotionContext context = buildContext(user);
        DiscountComputation computation = calculateForUser(userId, BigDecimal.valueOf(100));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("birthDate", user.getBirthDate());
        data.put("age", context.age());
        data.put("ageGroup", context.ageGroup());
        data.put("completedBookingCount", context.completedBookingCount());
        data.put("hoursLast7Days", context.hoursLast7Days());
        data.put("eligibleTypes", computation.eligibleTypes());
        data.put("estimatedPercentage", computation.totalPercentage());
        data.put("activePolicies", listPolicies());
        return data;
    }

    public List<Map<String, Object>> listPolicies() {
        return promotionPolicyMapper.selectList(new LambdaQueryWrapper<PromotionPolicyEntity>()
                        .orderByAsc(PromotionPolicyEntity::getCategory)
                        .orderByAsc(PromotionPolicyEntity::getPriority)
                        .orderByAsc(PromotionPolicyEntity::getPolicyCode))
                .stream()
                .map(this::toPolicyMap)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createPolicy(Map<String, Object> request) {
        PromotionPolicyEntity entity = new PromotionPolicyEntity();
        entity.setPromotionPolicyId("PRM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
        entity.setPolicyCode(normalizePolicyCode(requireText(request.get("policyCode"), "policyCode")));
        entity.setName(requireText(request.get("name"), "name"));
        entity.setCategory(normalizeCategory(requireText(request.get("category"), "category")));
        entity.setDescription(trimToNull(stringValue(request.get("description"))));
        entity.setPercentage(requirePercentage(request.get("percentage")));
        entity.setMinAge(parseInteger(request.get("minAge")));
        entity.setMaxAge(parseInteger(request.get("maxAge")));
        entity.setMinCompletedBookings(parseInteger(request.get("minCompletedBookings")));
        entity.setMaxCompletedBookings(parseInteger(request.get("maxCompletedBookings")));
        entity.setHolidayCampaign(parseBooleanDefaultFalse(request.get("holidayCampaign")) ? 1 : 0);
        entity.setStackable(parseBooleanDefaultTrue(request.get("stackable")) ? 1 : 0);
        entity.setPriority(parseIntegerDefault(request.get("priority"), 100));
        entity.setStartAt(parseDateTime(request.get("startAt"), "startAt"));
        entity.setEndAt(parseDateTime(request.get("endAt"), "endAt"));
        entity.setActive(parseBooleanDefaultTrue(request.get("active")) ? 1 : 0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        validatePolicy(entity, null);
        PromotionPolicyEntity existing = promotionPolicyMapper.selectOne(new LambdaQueryWrapper<PromotionPolicyEntity>()
                .eq(PromotionPolicyEntity::getPolicyCode, entity.getPolicyCode()));
        if (existing != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "Promotion policy already exists");
        }
        promotionPolicyMapper.insert(entity);
        return toPolicyMap(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updatePolicy(String promotionPolicyId, Map<String, Object> request) {
        PromotionPolicyEntity entity = requirePolicy(promotionPolicyId);
        if (request.containsKey("policyCode")) {
            entity.setPolicyCode(normalizePolicyCode(requireText(request.get("policyCode"), "policyCode")));
        }
        if (request.containsKey("name")) {
            entity.setName(requireText(request.get("name"), "name"));
        }
        if (request.containsKey("category")) {
            entity.setCategory(normalizeCategory(requireText(request.get("category"), "category")));
        }
        if (request.containsKey("description")) {
            entity.setDescription(trimToNull(stringValue(request.get("description"))));
        }
        if (request.containsKey("percentage")) {
            entity.setPercentage(requirePercentage(request.get("percentage")));
        }
        if (request.containsKey("minAge")) {
            entity.setMinAge(parseInteger(request.get("minAge")));
        }
        if (request.containsKey("maxAge")) {
            entity.setMaxAge(parseInteger(request.get("maxAge")));
        }
        if (request.containsKey("minCompletedBookings")) {
            entity.setMinCompletedBookings(parseInteger(request.get("minCompletedBookings")));
        }
        if (request.containsKey("maxCompletedBookings")) {
            entity.setMaxCompletedBookings(parseInteger(request.get("maxCompletedBookings")));
        }
        if (request.containsKey("holidayCampaign")) {
            entity.setHolidayCampaign(parseBooleanDefaultFalse(request.get("holidayCampaign")) ? 1 : 0);
        }
        if (request.containsKey("stackable")) {
            entity.setStackable(parseBooleanDefaultTrue(request.get("stackable")) ? 1 : 0);
        }
        if (request.containsKey("priority")) {
            entity.setPriority(parseIntegerDefault(request.get("priority"), 100));
        }
        if (request.containsKey("startAt")) {
            entity.setStartAt(parseDateTime(request.get("startAt"), "startAt"));
        }
        if (request.containsKey("endAt")) {
            entity.setEndAt(parseDateTime(request.get("endAt"), "endAt"));
        }
        if (request.containsKey("active")) {
            entity.setActive(parseBooleanDefaultTrue(request.get("active")) ? 1 : 0);
        }
        entity.setUpdatedAt(LocalDateTime.now());
        validatePolicy(entity, promotionPolicyId);
        promotionPolicyMapper.updateById(entity);
        return toPolicyMap(entity);
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

    public int countCompletedBookings(String userId) {
        Long count = bookingMapper.selectCount(new LambdaQueryWrapper<BookingEntity>()
                .eq(BookingEntity::getUserId, userId)
                .eq(BookingEntity::getStatus, DomainConstants.BookingStatus.COMPLETED));
        return count == null ? 0 : count.intValue();
    }

    public int resolveFrequentUserThreshold() {
        return promotionPolicyMapper.selectList(new LambdaQueryWrapper<PromotionPolicyEntity>()
                        .eq(PromotionPolicyEntity::getActive, 1)
                        .eq(PromotionPolicyEntity::getCategory, DomainConstants.PromotionCategory.LOYALTY))
                .stream()
                .map(PromotionPolicyEntity::getMinCompletedBookings)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(5);
    }

    public List<Map<String, Object>> listRules() {
        return listPolicies();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createRule(Map<String, Object> request) {
        return createPolicy(request);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateRule(String discountRuleId, Map<String, Object> request) {
        return updatePolicy(discountRuleId, request);
    }

    public Map<String, Object> toPolicyMap(PromotionPolicyEntity entity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("promotionPolicyId", entity.getPromotionPolicyId());
        data.put("policyCode", entity.getPolicyCode());
        data.put("name", entity.getName());
        data.put("category", entity.getCategory());
        data.put("description", entity.getDescription());
        data.put("percentage", entity.getPercentage());
        data.put("minAge", entity.getMinAge());
        data.put("maxAge", entity.getMaxAge());
        data.put("minCompletedBookings", entity.getMinCompletedBookings());
        data.put("maxCompletedBookings", entity.getMaxCompletedBookings());
        data.put("holidayCampaign", entity.getHolidayCampaign() != null && entity.getHolidayCampaign() == 1);
        data.put("stackable", entity.getStackable() != null && entity.getStackable() == 1);
        data.put("priority", entity.getPriority());
        data.put("startAt", entity.getStartAt());
        data.put("endAt", entity.getEndAt());
        data.put("active", entity.getActive() != null && entity.getActive() == 1);
        data.put("activeNow", isActiveNow(entity, LocalDateTime.now()));
        data.put("createdAt", entity.getCreatedAt());
        data.put("updatedAt", entity.getUpdatedAt());
        return data;
    }

    private PromotionContext buildContext(UserEntity user) {
        return new PromotionContext(
                user.getUserId(),
                UserAgeSupport.resolveAge(user.getBirthDate()),
                UserAgeSupport.resolveAgeGroup(user.getBirthDate()),
                countCompletedBookings(user.getUserId()),
                calculateRecentHours(user.getUserId()));
    }

    private List<PromotionPolicyEntity> selectApplicablePolicies(PromotionContext context) {
        LocalDateTime now = LocalDateTime.now();
        List<PromotionPolicyEntity> activePolicies = promotionPolicyMapper.selectList(new LambdaQueryWrapper<PromotionPolicyEntity>()
                .eq(PromotionPolicyEntity::getActive, 1)
                .le(PromotionPolicyEntity::getStartAt, now)
                .ge(PromotionPolicyEntity::getEndAt, now));

        Map<String, PromotionPolicyEntity> bestPerCategory = activePolicies.stream()
                .filter(policy -> isEligible(policy, context))
                .collect(Collectors.toMap(
                        PromotionPolicyEntity::getCategory,
                        policy -> policy,
                        this::pickBetterPolicy,
                        LinkedHashMap::new));

        List<PromotionPolicyEntity> winners = new ArrayList<>(bestPerCategory.values());
        winners.sort(Comparator
                .comparing((PromotionPolicyEntity policy) -> policy.getStackable() == null ? 0 : policy.getStackable()).reversed()
                .thenComparing(PromotionPolicyEntity::getPriority, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PromotionPolicyEntity::getPercentage, Comparator.nullsLast(BigDecimal::compareTo)).reversed());

        List<PromotionPolicyEntity> selected = winners.stream()
                .filter(policy -> policy.getStackable() == null || policy.getStackable() == 1)
                .toList();
        if (!selected.isEmpty()) {
            return selected;
        }
        return winners.isEmpty() ? List.of() : List.of(winners.get(0));
    }

    private PromotionPolicyEntity pickBetterPolicy(PromotionPolicyEntity left, PromotionPolicyEntity right) {
        int byPercentage = zeroIfNull(right.getPercentage()).compareTo(zeroIfNull(left.getPercentage()));
        if (byPercentage != 0) {
            return byPercentage > 0 ? right : left;
        }
        int leftPriority = left.getPriority() == null ? Integer.MAX_VALUE : left.getPriority();
        int rightPriority = right.getPriority() == null ? Integer.MAX_VALUE : right.getPriority();
        return rightPriority < leftPriority ? right : left;
    }

    private boolean isEligible(PromotionPolicyEntity policy, PromotionContext context) {
        Integer age = context.age();
        if (policy.getMinAge() != null && (age == null || age < policy.getMinAge())) {
            return false;
        }
        if (policy.getMaxAge() != null && (age == null || age > policy.getMaxAge())) {
            return false;
        }
        if (policy.getMinCompletedBookings() != null && context.completedBookingCount() < policy.getMinCompletedBookings()) {
            return false;
        }
        if (policy.getMaxCompletedBookings() != null && context.completedBookingCount() > policy.getMaxCompletedBookings()) {
            return false;
        }
        return true;
    }

    private boolean isActiveNow(PromotionPolicyEntity policy, LocalDateTime now) {
        return policy.getActive() != null && policy.getActive() == 1
                && policy.getStartAt() != null && policy.getEndAt() != null
                && !policy.getStartAt().isAfter(now)
                && !policy.getEndAt().isBefore(now);
    }

    private PromotionPolicyEntity requirePolicy(String promotionPolicyId) {
        PromotionPolicyEntity entity = promotionPolicyMapper.selectOne(new LambdaQueryWrapper<PromotionPolicyEntity>()
                .eq(PromotionPolicyEntity::getPromotionPolicyId, promotionPolicyId));
        if (entity == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Promotion policy not found");
        }
        return entity;
    }

    private void validatePolicy(PromotionPolicyEntity entity, String currentPolicyId) {
        if (entity.getStartAt() == null || entity.getEndAt() == null || !entity.getEndAt().isAfter(entity.getStartAt())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "endAt must be later than startAt");
        }
        if (entity.getHolidayCampaign() == null || entity.getHolidayCampaign() == 0) {
            long days = Duration.between(entity.getStartAt(), entity.getEndAt()).toDays();
            if (days < 1 || days > 7) {
                throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                        "Non-holiday promotion policies must last between 1 and 7 days");
            }
        }
        if (entity.getMinAge() != null && entity.getMaxAge() != null && entity.getMinAge() > entity.getMaxAge()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "minAge must not be greater than maxAge");
        }
        if (entity.getMinCompletedBookings() != null && entity.getMaxCompletedBookings() != null
                && entity.getMinCompletedBookings() > entity.getMaxCompletedBookings()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "minCompletedBookings must not be greater than maxCompletedBookings");
        }

        PromotionPolicyEntity duplicate = promotionPolicyMapper.selectOne(new LambdaQueryWrapper<PromotionPolicyEntity>()
                .eq(PromotionPolicyEntity::getPolicyCode, entity.getPolicyCode())
                .ne(currentPolicyId != null, PromotionPolicyEntity::getPromotionPolicyId, currentPolicyId));
        if (duplicate != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "policyCode must be unique");
        }
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

    private String normalizePolicyCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private String normalizeCategory(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!List.of(
                DomainConstants.PromotionCategory.NEW_RIDER,
                DomainConstants.PromotionCategory.AGE_BASED,
                DomainConstants.PromotionCategory.LOYALTY,
                DomainConstants.PromotionCategory.HOLIDAY,
                DomainConstants.PromotionCategory.CUSTOM
        ).contains(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "category must be NEW_RIDER, AGE_BASED, LOYALTY, HOLIDAY, or CUSTOM");
        }
        return normalized;
    }

    private BigDecimal requirePercentage(Object value) {
        BigDecimal percentage = parseBigDecimal(value);
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) <= 0 || percentage.compareTo(BigDecimal.valueOf(50)) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "percentage must be greater than 0 and less than or equal to 50");
        }
        return percentage.setScale(2, RoundingMode.HALF_UP);
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

    private Integer parseInteger(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "Invalid integer value: " + value);
        }
    }

    private Integer parseIntegerDefault(Object value, int fallback) {
        Integer parsed = parseInteger(value);
        return parsed == null ? fallback : parsed;
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

    private boolean parseBooleanDefaultFalse(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private LocalDateTime parseDateTime(Object value, String field) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " is required");
        }
        try {
            return value instanceof LocalDateTime localDateTime ? localDateTime : LocalDateTime.parse(String.valueOf(value).trim());
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    field + " must use ISO-8601 date-time format");
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record DiscountComputation(BigDecimal recentHours,
                                      BigDecimal totalDiscount,
                                      BigDecimal finalPrice,
                                      List<AppliedDiscountVo> appliedDiscounts,
                                      List<String> eligibleTypes,
                                      BigDecimal totalPercentage) {
    }

    private record PromotionContext(String userId,
                                    Integer age,
                                    String ageGroup,
                                    int completedBookingCount,
                                    BigDecimal hoursLast7Days) {
    }
}
