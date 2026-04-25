package com.lcyhz.urbanova.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.entity.PaymentMethodEntity;
import com.lcyhz.urbanova.mapper.PaymentMethodMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentMethodService {
    private final PaymentMethodMapper paymentMethodMapper;

    public PaymentMethodService(PaymentMethodMapper paymentMethodMapper) {
        this.paymentMethodMapper = paymentMethodMapper;
    }

    public List<Map<String, Object>> listPaymentMethods(String userId) {
        return paymentMethodMapper.selectList(new LambdaQueryWrapper<PaymentMethodEntity>()
                        .eq(PaymentMethodEntity::getUserId, userId)
                        .ne(PaymentMethodEntity::getStatus, DomainConstants.PaymentMethodStatus.REMOVED)
                        .orderByDesc(PaymentMethodEntity::getIsDefault)
                        .orderByDesc(PaymentMethodEntity::getUpdatedAt))
                .stream()
                .map(this::toMap)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createPaymentMethod(String userId, Map<String, Object> request) {
        String brand = requireText(request.get("brand"), "brand").toUpperCase(Locale.ROOT);
        String cardNumber = requireText(request.get("cardNumber"), "cardNumber").replaceAll("\\s+", "");
        if (cardNumber.length() < 4) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "cardNumber must contain at least 4 digits");
        }

        PaymentMethodEntity entity = new PaymentMethodEntity();
        entity.setPaymentMethodId("PM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
        entity.setUserId(userId);
        entity.setBrand(brand);
        entity.setLast4(cardNumber.substring(cardNumber.length() - 4));
        entity.setExpiryMonth(requireInteger(request.get("expiryMonth"), "expiryMonth"));
        entity.setExpiryYear(requireInteger(request.get("expiryYear"), "expiryYear"));
        entity.setLabel(trimToNull((String) request.get("label")));
        entity.setStatus(DomainConstants.PaymentMethodStatus.ACTIVE);
        entity.setIsDefault(parseBoolean(request.get("isDefault")) ? 1 : 0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        if (entity.getIsDefault() == 1 || !hasAnyDefault(userId)) {
            clearDefault(userId);
            entity.setIsDefault(1);
        }
        paymentMethodMapper.insert(entity);
        return toMap(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updatePaymentMethod(String userId, String paymentMethodId, Map<String, Object> request) {
        PaymentMethodEntity entity = requireOwnedMethod(userId, paymentMethodId);
        if (request.containsKey("expiryMonth")) {
            entity.setExpiryMonth(requireInteger(request.get("expiryMonth"), "expiryMonth"));
        }
        if (request.containsKey("expiryYear")) {
            entity.setExpiryYear(requireInteger(request.get("expiryYear"), "expiryYear"));
        }
        if (request.containsKey("label")) {
            entity.setLabel(trimToNull((String) request.get("label")));
        }
        if (request.containsKey("isDefault") && parseBoolean(request.get("isDefault"))) {
            clearDefault(userId);
            entity.setIsDefault(1);
        }
        entity.setUpdatedAt(LocalDateTime.now());
        paymentMethodMapper.updateById(entity);
        return toMap(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deletePaymentMethod(String userId, String paymentMethodId) {
        PaymentMethodEntity entity = requireOwnedMethod(userId, paymentMethodId);
        entity.setStatus(DomainConstants.PaymentMethodStatus.REMOVED);
        entity.setIsDefault(0);
        entity.setUpdatedAt(LocalDateTime.now());
        paymentMethodMapper.updateById(entity);

        if (!hasAnyDefault(userId)) {
            PaymentMethodEntity replacement = paymentMethodMapper.selectOne(new LambdaQueryWrapper<PaymentMethodEntity>()
                    .eq(PaymentMethodEntity::getUserId, userId)
                    .eq(PaymentMethodEntity::getStatus, DomainConstants.PaymentMethodStatus.ACTIVE)
                    .orderByDesc(PaymentMethodEntity::getUpdatedAt)
                    .last("LIMIT 1"));
            if (replacement != null) {
                replacement.setIsDefault(1);
                replacement.setUpdatedAt(LocalDateTime.now());
                paymentMethodMapper.updateById(replacement);
            }
        }
        return toMap(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> setDefault(String userId, String paymentMethodId) {
        PaymentMethodEntity entity = requireOwnedMethod(userId, paymentMethodId);
        clearDefault(userId);
        entity.setIsDefault(1);
        entity.setUpdatedAt(LocalDateTime.now());
        paymentMethodMapper.updateById(entity);
        return toMap(entity);
    }

    public PaymentMethodEntity requireActiveOwnedMethod(String userId, String paymentMethodId) {
        PaymentMethodEntity entity = requireOwnedMethod(userId, paymentMethodId);
        if (!DomainConstants.PaymentMethodStatus.ACTIVE.equals(entity.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.BOOKING_CONFLICT, "Payment method is not active");
        }
        return entity;
    }

    public Map<String, Object> toMap(PaymentMethodEntity entity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentMethodId", entity.getPaymentMethodId());
        data.put("userId", entity.getUserId());
        data.put("brand", entity.getBrand());
        data.put("last4", entity.getLast4());
        data.put("expiryMonth", entity.getExpiryMonth());
        data.put("expiryYear", entity.getExpiryYear());
        data.put("label", entity.getLabel());
        data.put("isDefault", entity.getIsDefault() != null && entity.getIsDefault() == 1);
        data.put("status", entity.getStatus());
        data.put("createdAt", entity.getCreatedAt());
        data.put("updatedAt", entity.getUpdatedAt());
        return data;
    }

    private PaymentMethodEntity requireOwnedMethod(String userId, String paymentMethodId) {
        PaymentMethodEntity entity = paymentMethodMapper.selectOne(new LambdaQueryWrapper<PaymentMethodEntity>()
                .eq(PaymentMethodEntity::getPaymentMethodId, paymentMethodId)
                .eq(PaymentMethodEntity::getUserId, userId));
        if (entity == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Payment method not found");
        }
        return entity;
    }

    private boolean hasAnyDefault(String userId) {
        return paymentMethodMapper.selectCount(new LambdaQueryWrapper<PaymentMethodEntity>()
                .eq(PaymentMethodEntity::getUserId, userId)
                .eq(PaymentMethodEntity::getIsDefault, 1)
                .eq(PaymentMethodEntity::getStatus, DomainConstants.PaymentMethodStatus.ACTIVE)) > 0;
    }

    private void clearDefault(String userId) {
        List<PaymentMethodEntity> methods = paymentMethodMapper.selectList(new LambdaQueryWrapper<PaymentMethodEntity>()
                .eq(PaymentMethodEntity::getUserId, userId)
                .eq(PaymentMethodEntity::getIsDefault, 1));
        for (PaymentMethodEntity method : methods) {
            method.setIsDefault(0);
            method.setUpdatedAt(LocalDateTime.now());
            paymentMethodMapper.updateById(method);
        }
    }

    private String requireText(Object value, String field) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " is required");
        }
        return String.valueOf(value).trim();
    }

    private Integer requireInteger(Object value, String field) {
        if (value == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " is required");
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    field + " must be an integer");
        }
    }

    private boolean parseBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
