package com.lcyhz.urbanova.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.entity.WalletAccountEntity;
import com.lcyhz.urbanova.entity.WalletTransactionEntity;
import com.lcyhz.urbanova.mapper.WalletAccountMapper;
import com.lcyhz.urbanova.mapper.WalletTransactionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class WalletService {
    private static final Set<String> SUPPORTED_TOP_UP_METHODS = Set.of(
            DomainConstants.WalletTopUpMethod.APPLE_PAY,
            DomainConstants.WalletTopUpMethod.ALIPAY,
            DomainConstants.WalletTopUpMethod.SAVED_CARD
    );

    private final WalletAccountMapper walletAccountMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final PaymentMethodService paymentMethodService;

    public WalletService(WalletAccountMapper walletAccountMapper,
                         WalletTransactionMapper walletTransactionMapper,
                         PaymentMethodService paymentMethodService) {
        this.walletAccountMapper = walletAccountMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.paymentMethodService = paymentMethodService;
    }

    public Map<String, Object> getWallet(String userId) {
        WalletAccountEntity account = ensureWalletAccount(userId);
        return toWalletMap(account, currentBalance(userId));
    }

    public List<Map<String, Object>> listTransactions(String userId) {
        WalletAccountEntity account = ensureWalletAccount(userId);
        return walletTransactionMapper.selectList(new LambdaQueryWrapper<WalletTransactionEntity>()
                        .eq(WalletTransactionEntity::getUserId, userId)
                        .orderByDesc(WalletTransactionEntity::getCreatedAt)
                        .orderByDesc(WalletTransactionEntity::getWalletTransactionId))
                .stream()
                .map(tx -> toTransactionMap(tx, account.getCurrency()))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> topUp(String userId, Map<String, Object> request) {
        WalletAccountEntity account = ensureWalletAccount(userId);
        BigDecimal amount = requireAmount(request.get("amount"), "amount");
        String method = requireTopUpMethod(request.get("method"));
        String paymentMethodId = null;
        if (DomainConstants.WalletTopUpMethod.SAVED_CARD.equals(method)) {
            paymentMethodId = requireText(request.get("paymentMethodId"), "paymentMethodId");
            paymentMethodService.requireActiveOwnedMethod(userId, paymentMethodId);
        }

        LocalDateTime now = LocalDateTime.now();
        WalletTransactionEntity transaction = new WalletTransactionEntity();
        transaction.setWalletTransactionId("WTX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
        transaction.setWalletAccountId(account.getWalletAccountId());
        transaction.setUserId(userId);
        transaction.setType(DomainConstants.WalletTransactionType.TOP_UP);
        transaction.setAmount(amount);
        transaction.setMethod(method);
        transaction.setPaymentMethodId(paymentMethodId);
        transaction.setDescription(buildTopUpDescription(method, amount));
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);
        walletTransactionMapper.insert(transaction);

        account.setUpdatedAt(now);
        walletAccountMapper.updateById(account);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("wallet", toWalletMap(account, currentBalance(userId)));
        data.put("transaction", toTransactionMap(transaction, account.getCurrency()));
        return data;
    }

    private WalletAccountEntity ensureWalletAccount(String userId) {
        WalletAccountEntity existing = walletAccountMapper.selectOne(new LambdaQueryWrapper<WalletAccountEntity>()
                .eq(WalletAccountEntity::getUserId, userId));
        if (existing != null) {
            return existing;
        }

        WalletAccountEntity created = new WalletAccountEntity();
        created.setWalletAccountId("WAL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
        created.setUserId(userId);
        created.setCurrency(DomainConstants.CURRENCY_GBP);
        created.setStatus(DomainConstants.WalletStatus.ACTIVE);
        created.setCreatedAt(LocalDateTime.now());
        created.setUpdatedAt(LocalDateTime.now());
        try {
            walletAccountMapper.insert(created);
            return created;
        } catch (DuplicateKeyException ex) {
            WalletAccountEntity reloaded = walletAccountMapper.selectOne(new LambdaQueryWrapper<WalletAccountEntity>()
                    .eq(WalletAccountEntity::getUserId, userId));
            if (reloaded != null) {
                return reloaded;
            }
            throw ex;
        }
    }

    private BigDecimal currentBalance(String userId) {
        BigDecimal value = walletTransactionMapper.sumAmountByUserId(userId);
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> toWalletMap(WalletAccountEntity entity, BigDecimal balance) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("walletAccountId", entity.getWalletAccountId());
        data.put("userId", entity.getUserId());
        data.put("balance", balance);
        data.put("currency", entity.getCurrency());
        data.put("status", entity.getStatus());
        data.put("createdAt", entity.getCreatedAt());
        data.put("updatedAt", entity.getUpdatedAt());
        return data;
    }

    private Map<String, Object> toTransactionMap(WalletTransactionEntity entity, String currency) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("walletTransactionId", entity.getWalletTransactionId());
        data.put("walletAccountId", entity.getWalletAccountId());
        data.put("userId", entity.getUserId());
        data.put("type", entity.getType());
        data.put("direction", entity.getAmount().compareTo(BigDecimal.ZERO) >= 0 ? "CREDIT" : "DEBIT");
        data.put("title", titleForType(entity.getType()));
        data.put("amount", entity.getAmount().setScale(2, RoundingMode.HALF_UP));
        data.put("currency", currency);
        data.put("method", entity.getMethod());
        data.put("paymentMethodId", entity.getPaymentMethodId());
        data.put("description", entity.getDescription());
        data.put("createdAt", entity.getCreatedAt());
        data.put("updatedAt", entity.getUpdatedAt());
        return data;
    }

    private String titleForType(String type) {
        return switch (type) {
            case DomainConstants.WalletTransactionType.TOP_UP -> "Wallet top-up";
            case DomainConstants.WalletTransactionType.BOOKING_CHARGE -> "Ride charge";
            case DomainConstants.WalletTransactionType.REFUND -> "Wallet refund";
            default -> "Wallet adjustment";
        };
    }

    private String buildTopUpDescription(String method, BigDecimal amount) {
        return "Top up via " + method.replace('_', ' ').toLowerCase(Locale.ROOT) + " for GBP " + amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal requireAmount(Object value, String field) {
        if (value == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " is required");
        }
        try {
            BigDecimal amount = new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " must be greater than 0");
            }
            if (amount.compareTo(new BigDecimal("5000.00")) > 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " exceeds allowed limit");
            }
            return amount;
        } catch (NumberFormatException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " must be a decimal amount");
        }
    }

    private String requireTopUpMethod(Object value) {
        String method = requireText(value, "method").toUpperCase(Locale.ROOT);
        if (!SUPPORTED_TOP_UP_METHODS.contains(method)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "method must be APPLE_PAY, ALIPAY, or SAVED_CARD");
        }
        return method;
    }

    private String requireText(Object value, String field) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " is required");
        }
        return String.valueOf(value).trim();
    }
}
