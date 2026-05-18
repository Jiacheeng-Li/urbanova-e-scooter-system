package com.lcyhz.urbanova.service.impl;

import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.auth.LoginRequest;
import com.lcyhz.urbanova.dto.auth.RegisterRequest;
import com.lcyhz.urbanova.entity.AuthSessionEntity;
import com.lcyhz.urbanova.entity.EmailVerificationCodeEntity;
import com.lcyhz.urbanova.entity.UserEntity;
import com.lcyhz.urbanova.mapper.AuthSessionMapper;
import com.lcyhz.urbanova.mapper.EmailVerificationCodeMapper;
import com.lcyhz.urbanova.mapper.UserMapper;
import com.lcyhz.urbanova.security.JwtService;
import com.lcyhz.urbanova.service.support.EmailDeliveryService;
import com.lcyhz.urbanova.vo.auth.AuthPayload;
import com.lcyhz.urbanova.vo.auth.UserProfileVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthSessionMapper authSessionMapper;
    @Mock private EmailVerificationCodeMapper emailVerificationCodeMapper;
    @Mock private EmailDeliveryService emailDeliveryService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpirationDays", 14L);
        ReflectionTestUtils.setField(authService, "emailVerificationExpirationMinutes", 10L);
        ReflectionTestUtils.setField(authService, "emailVerificationResendCooldownSeconds", 60L);
        ReflectionTestUtils.setField(authService, "passwordResetExpirationMinutes", 10L);
        ReflectionTestUtils.setField(authService, "passwordResetResendCooldownSeconds", 60L);
    }

    // ─── Helper builders ─────────────────────────────────────────────────

    private UserEntity buildActiveUser(String userId, String email) {
        UserEntity u = new UserEntity();
        u.setUserId(userId);
        u.setEmail(email);
        u.setFullName("Test User");
        u.setPasswordHash("$hashed$");
        u.setRole(DomainConstants.ROLE_CUSTOMER);
        u.setDiscountCategory(DomainConstants.DISCOUNT_NONE);
        u.setAccountStatus(DomainConstants.ACCOUNT_ACTIVE);
        u.setCreatedAt(LocalDateTime.now().minusDays(30));
        u.setUpdatedAt(LocalDateTime.now());
        return u;
    }

    private AuthSessionEntity buildSession(String userId, boolean expired, boolean revoked) {
        AuthSessionEntity s = new AuthSessionEntity();
        s.setSessionId("SES-TESTTEST1234");
        s.setUserId(userId);
        s.setRefreshToken("refresh-token-abc");
        s.setExpiresAt(expired ? LocalDateTime.now().minusDays(1) : LocalDateTime.now().plusDays(14));
        s.setRevoked(revoked ? 1 : 0);
        s.setCreatedAt(LocalDateTime.now().minusDays(1));
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }

    /** 返回一个已通过验证、未过期、未消费的邮箱验证码 entity */
    private EmailVerificationCodeEntity buildVerifiedCode(String email, String purpose) {
        EmailVerificationCodeEntity e = new EmailVerificationCodeEntity();
        e.setVerificationCodeId("EMV-TEST12345");
        e.setEmail(email);
        e.setPurpose(purpose);
        e.setCodeHash("$hashedcode$");
        e.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        e.setVerifiedAt(LocalDateTime.now().minusMinutes(1));
        e.setConsumed(0);
        e.setAttemptCount(0);
        e.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    /** 模拟 JWT + Session 创建（login/register 都需要） */
    private void stubAuthPayloadCreation() {
        when(authSessionMapper.insert(any(AuthSessionEntity.class))).thenReturn(1);
        when(jwtService.generateToken(anyString(), anyString(), anyString())).thenReturn("mock-access-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);
    }

    // ─── login ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("邮箱和密码正确 → 返回含 accessToken 的 AuthPayload")
        void validCredentials_returnsAuthPayload() {
            UserEntity user = buildActiveUser("USR-001", "user@example.com");
            LoginRequest req = new LoginRequest();
            req.setEmail("user@example.com");
            req.setPassword("password123");

            when(userMapper.selectOne(any())).thenReturn(user);
            when(passwordEncoder.matches("password123", "$hashed$")).thenReturn(true);
            stubAuthPayloadCreation();

            AuthPayload result = authService.login(req);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("mock-access-token");
            assertThat(result.getRefreshToken()).isNotBlank();
            assertThat(result.getUser().getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("邮箱大写 → 自动转小写后查询")
        void uppercaseEmail_normalizedToLowercase() {
            UserEntity user = buildActiveUser("USR-001", "user@example.com");
            LoginRequest req = new LoginRequest();
            req.setEmail("USER@EXAMPLE.COM");
            req.setPassword("password123");

            when(userMapper.selectOne(any())).thenReturn(user);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            stubAuthPayloadCreation();

            AuthPayload result = authService.login(req);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("用户不存在 → 抛 BusinessException（凭证无效）")
        void userNotFound_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail("ghost@example.com");
            req.setPassword("password123");

            when(userMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("密码错误 → 抛 BusinessException（凭证无效）")
        void wrongPassword_throws() {
            UserEntity user = buildActiveUser("USR-001", "user@example.com");
            LoginRequest req = new LoginRequest();
            req.setEmail("user@example.com");
            req.setPassword("wrongpassword");

            when(userMapper.selectOne(any())).thenReturn(user);
            when(passwordEncoder.matches("wrongpassword", "$hashed$")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("账号已被封禁（SUSPENDED）→ 抛 BusinessException")
        void suspendedAccount_throws() {
            UserEntity user = buildActiveUser("USR-001", "user@example.com");
            user.setAccountStatus(DomainConstants.ACCOUNT_SUSPENDED);

            LoginRequest req = new LoginRequest();
            req.setEmail("user@example.com");
            req.setPassword("password123");

            when(userMapper.selectOne(any())).thenReturn(user);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ─── register ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("邮箱已注册 → 抛 BusinessException")
        void emailAlreadyRegistered_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("existing@example.com");
            req.setPassword("password123");
            req.setFullName("Test User");

            when(userMapper.selectOne(any())).thenReturn(buildActiveUser("USR-EXIST", "existing@example.com"));

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("未完成邮箱验证（无验证码记录）→ 抛 BusinessException")
        void noVerification_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("new@example.com");
            req.setPassword("password123");
            req.setFullName("New User");

            when(userMapper.selectOne(any())).thenReturn(null);
            when(emailVerificationCodeMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("验证码已过期 → 抛 BusinessException")
        void expiredVerification_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("new@example.com");
            req.setPassword("password123");
            req.setFullName("New User");

            EmailVerificationCodeEntity expired = buildVerifiedCode("new@example.com", "REGISTER");
            expired.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // 已过期

            when(userMapper.selectOne(any())).thenReturn(null);
            when(emailVerificationCodeMapper.selectOne(any())).thenReturn(expired);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("验证码已消费（consumed=1）→ 抛 BusinessException")
        void consumedVerification_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("new@example.com");
            req.setPassword("password123");
            req.setFullName("New User");

            EmailVerificationCodeEntity consumed = buildVerifiedCode("new@example.com", "REGISTER");
            consumed.setConsumed(1);

            when(userMapper.selectOne(any())).thenReturn(null);
            when(emailVerificationCodeMapper.selectOne(any())).thenReturn(consumed);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("verifiedAt 为 null（尚未验证）→ 抛 BusinessException")
        void notYetVerified_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("new@example.com");
            req.setPassword("password123");
            req.setFullName("New User");

            EmailVerificationCodeEntity notVerified = buildVerifiedCode("new@example.com", "REGISTER");
            notVerified.setVerifiedAt(null); // 尚未验证

            when(userMapper.selectOne(any())).thenReturn(null);
            when(emailVerificationCodeMapper.selectOne(any())).thenReturn(notVerified);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("未来的 birthDate → 抛 BusinessException")
        void futureBirthDate_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("new@example.com");
            req.setPassword("password123");
            req.setFullName("New User");
            req.setBirthDate(LocalDate.now().plusDays(1)); // 未来日期

            EmailVerificationCodeEntity valid = buildVerifiedCode("new@example.com", "REGISTER");

            when(userMapper.selectOne(any())).thenReturn(null);
            when(emailVerificationCodeMapper.selectOne(any())).thenReturn(valid);
            when(passwordEncoder.encode(anyString())).thenReturn("$hashed$");

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("正常注册 → 写入 DB，返回 AuthPayload")
        void success_returnsAuthPayload() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("new@example.com");
            req.setPassword("password123");
            req.setFullName("New User");

            EmailVerificationCodeEntity valid = buildVerifiedCode("new@example.com", "REGISTER");

            when(userMapper.selectOne(any())).thenReturn(null);
            when(emailVerificationCodeMapper.selectOne(any())).thenReturn(valid);
            when(passwordEncoder.encode(anyString())).thenReturn("$hashed$");
            when(userMapper.insert(any(UserEntity.class))).thenReturn(1);
            when(emailVerificationCodeMapper.updateById(any(EmailVerificationCodeEntity.class))).thenReturn(1);
            stubAuthPayloadCreation();

            AuthPayload result = authService.register(req);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isNotBlank();
            assertThat(result.getUser().getRole()).isEqualTo(DomainConstants.ROLE_CUSTOMER);
            verify(userMapper).insert(any(UserEntity.class));
        }

        @Test
        @DisplayName("注册成功后验证码被标记为 consumed")
        void success_marksVerificationConsumed() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("new@example.com");
            req.setPassword("password123");
            req.setFullName("New User");

            EmailVerificationCodeEntity valid = buildVerifiedCode("new@example.com", "REGISTER");

            when(userMapper.selectOne(any())).thenReturn(null);
            when(emailVerificationCodeMapper.selectOne(any())).thenReturn(valid);
            when(passwordEncoder.encode(anyString())).thenReturn("$hashed$");
            when(userMapper.insert(any(UserEntity.class))).thenReturn(1);
            when(emailVerificationCodeMapper.updateById(any(EmailVerificationCodeEntity.class))).thenReturn(1);
            stubAuthPayloadCreation();

            authService.register(req);

            verify(emailVerificationCodeMapper).updateById(any(EmailVerificationCodeEntity.class));
        }
    }

    // ─── sendRegistrationVerificationCode ────────────────────────────────

    @Nested
    @DisplayName("sendRegistrationVerificationCode")
    class SendRegistrationVerificationCode {

        @Test
        @DisplayName("邮箱已注册 → 抛 BusinessException")
        void emailAlreadyRegistered_throws() {
            when(userMapper.selectOne(any())).thenReturn(buildActiveUser("USR-001", "existing@example.com"));

            assertThatThrownBy(() -> authService.sendRegistrationVerificationCode("existing@example.com"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("冷却期内（60秒内发过验证码）→ 抛 BusinessException")
        void cooldownActive_throws() {
            when(userMapper.selectOne(any())).thenReturn(null);

            EmailVerificationCodeEntity recentCode = buildVerifiedCode("new@example.com", "REGISTER");
            recentCode.setCreatedAt(LocalDateTime.now().minusSeconds(30)); // 30秒前发过，冷却60秒

            when(emailVerificationCodeMapper.selectOne(any())).thenReturn(recentCode);

            assertThatThrownBy(() -> authService.sendRegistrationVerificationCode("new@example.com"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("无历史验证码 → 发送成功，返回含 sent=true 的 Map")
        void noRecentCode_sendSuccessfully() {
            when(userMapper.selectOne(any())).thenReturn(null);
            when(emailVerificationCodeMapper.selectOne(any())).thenReturn(null); // 无历史
            when(passwordEncoder.encode(anyString())).thenReturn("$hashedcode$");
            when(emailVerificationCodeMapper.insert(any(EmailVerificationCodeEntity.class))).thenReturn(1);

            Map<String, Object> result = authService.sendRegistrationVerificationCode("new@example.com");

            assertThat(result.get("sent")).isEqualTo(true);
            assertThat(result.get("email")).isEqualTo("new@example.com");
            assertThat(result.get("expiresAt")).isNotNull();
        }

        @Test
        @DisplayName("冷却期已过（超过60秒）→ 可以重新发送")
        void afterCooldown_canResend() {
            when(userMapper.selectOne(any())).thenReturn(null);

            EmailVerificationCodeEntity oldCode = buildVerifiedCode("new@example.com", "REGISTER");
            oldCode.setCreatedAt(LocalDateTime.now().minusSeconds(90)); // 90秒前，超过60秒冷却

            when(emailVerificationCodeMapper.selectOne(any())).thenReturn(oldCode);
            when(passwordEncoder.encode(anyString())).thenReturn("$hashedcode$");
            when(emailVerificationCodeMapper.insert(any(EmailVerificationCodeEntity.class))).thenReturn(1);

            Map<String, Object> result = authService.sendRegistrationVerificationCode("new@example.com");

            assertThat(result.get("sent")).isEqualTo(true);
        }
    }

    // ─── refresh ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("refreshToken 为空 → 抛 BusinessException")
        void blankToken_throws() {
            assertThatThrownBy(() -> authService.refresh(""))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("refreshToken 为 null → 抛 BusinessException")
        void nullToken_throws() {
            assertThatThrownBy(() -> authService.refresh(null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("session 不存在 → 抛 BusinessException")
        void sessionNotFound_throws() {
            when(authSessionMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> authService.refresh("invalid-refresh-token"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("session 已过期 → 抛 BusinessException")
        void expiredSession_throws() {
            AuthSessionEntity expired = buildSession("USR-001", true, false);

            when(authSessionMapper.selectOne(any())).thenReturn(expired);

            assertThatThrownBy(() -> authService.refresh("refresh-token-abc"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("有效 session + 活跃用户 → 返回新的 AuthPayload")
        void validSession_returnsAuthPayload() {
            AuthSessionEntity session = buildSession("USR-001", false, false);
            UserEntity user = buildActiveUser("USR-001", "user@example.com");

            when(authSessionMapper.selectOne(any())).thenReturn(session);
            when(authSessionMapper.updateById(any(AuthSessionEntity.class))).thenReturn(1);
            when(userMapper.selectOne(any())).thenReturn(user);
            stubAuthPayloadCreation();

            AuthPayload result = authService.refresh("refresh-token-abc");

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("mock-access-token");
        }
    }

    // ─── logout ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("传入 refreshToken → 只撤销该 session，revokedSessions=1")
        void withRefreshToken_revokesSpecificSession() {
            AuthSessionEntity session = buildSession("USR-001", false, false);

            when(authSessionMapper.selectOne(any())).thenReturn(session);
            when(authSessionMapper.updateById(any(AuthSessionEntity.class))).thenReturn(1);

            Map<String, Object> result = authService.logout("USR-001", "refresh-token-abc");

            assertThat(result.get("revokedSessions")).isEqualTo(1);
            assertThat(result.get("loggedOut")).isEqualTo(true);
        }

        @Test
        @DisplayName("不传 refreshToken → 撤销该用户所有 session")
        void withoutRefreshToken_revokesAllSessions() {
            List<AuthSessionEntity> sessions = List.of(
                    buildSession("USR-001", false, false),
                    buildSession("USR-001", false, false)
            );

            when(authSessionMapper.selectList(any())).thenReturn(sessions);
            when(authSessionMapper.updateById(any(AuthSessionEntity.class))).thenReturn(1);

            Map<String, Object> result = authService.logout("USR-001", null);

            assertThat(result.get("revokedSessions")).isEqualTo(2);
            assertThat(result.get("loggedOut")).isEqualTo(true);
        }

        @Test
        @DisplayName("session 已被撤销 → revokedSessions=0，不重复撤销")
        void alreadyRevokedSession_revokedCountIsZero() {
            AuthSessionEntity revokedSession = buildSession("USR-001", false, true); // 已撤销

            when(authSessionMapper.selectOne(any())).thenReturn(revokedSession);

            Map<String, Object> result = authService.logout("USR-001", "refresh-token-abc");

            assertThat(result.get("revokedSessions")).isEqualTo(0);
            verify(authSessionMapper, never()).updateById(any(AuthSessionEntity.class));
        }
    }

    // ─── getCurrentUser ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {

        @Test
        @DisplayName("用户存在 → 返回 UserProfileVo")
        void userExists_returnsProfile() {
            UserEntity user = buildActiveUser("USR-001", "user@example.com");
            user.setBirthDate(LocalDate.now().minusYears(25));

            when(userMapper.selectOne(any())).thenReturn(user);

            UserProfileVo result = authService.getCurrentUser("USR-001");

            assertThat(result.getUserId()).isEqualTo("USR-001");
            assertThat(result.getEmail()).isEqualTo("user@example.com");
            assertThat(result.getRole()).isEqualTo(DomainConstants.ROLE_CUSTOMER);
            assertThat(result.getAccountStatus()).isEqualTo(DomainConstants.ACCOUNT_ACTIVE);
        }

        @Test
        @DisplayName("用户不存在 → 抛 BusinessException")
        void userNotFound_throws() {
            when(userMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> authService.getCurrentUser("USR-GHOST"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("没有 birthDate 的用户 → age 和 ageGroup 不影响返回结果")
        void noBirthDate_returnsProfileWithoutAge() {
            UserEntity user = buildActiveUser("USR-001", "user@example.com");
            user.setBirthDate(null); // 没有填生日

            when(userMapper.selectOne(any())).thenReturn(user);

            UserProfileVo result = authService.getCurrentUser("USR-001");

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo("USR-001");
        }
    }

    // ─── forgotPassword ───────────────────────────────────────────────────

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        @Test
        @DisplayName("邮箱不存在 → 仍返回 accepted=true（不暴露是否注册）")
        void emailNotFound_returnsAccepted() {
            when(userMapper.selectOne(any())).thenReturn(null);

            Map<String, Object> result = authService.forgotPassword("ghost@example.com");

            assertThat(result.get("accepted")).isEqualTo(true);
            assertThat(result.get("email")).isEqualTo("ghost@example.com");
        }

        @Test
        @DisplayName("邮箱存在且无历史重置码 → 发送重置码，返回 accepted=true")
        void emailExists_noRecentCode_sendsResetCode() {
            UserEntity user = buildActiveUser("USR-001", "user@example.com");

            when(userMapper.selectOne(any())).thenReturn(user);
            when(emailVerificationCodeMapper.selectOne(any())).thenReturn(null); // 无历史重置码
            when(passwordEncoder.encode(anyString())).thenReturn("$hashedcode$");
            when(emailVerificationCodeMapper.insert(any(EmailVerificationCodeEntity.class))).thenReturn(1);

            Map<String, Object> result = authService.forgotPassword("user@example.com");

            assertThat(result.get("accepted")).isEqualTo(true);
        }

        @Test
        @DisplayName("冷却期内 → 静默忽略，仍返回 accepted=true（不暴露冷却状态）")
        void cooldownActive_silentlyReturnsAccepted() {
            UserEntity user = buildActiveUser("USR-001", "user@example.com");
            EmailVerificationCodeEntity recentCode = buildVerifiedCode("user@example.com", "PASSWORD_RESET");
            recentCode.setCreatedAt(LocalDateTime.now().minusSeconds(30)); // 30秒内

            when(userMapper.selectOne(any())).thenReturn(user);
            when(emailVerificationCodeMapper.selectOne(any())).thenReturn(recentCode);

            Map<String, Object> result = authService.forgotPassword("user@example.com");

            assertThat(result.get("accepted")).isEqualTo(true);
            verify(emailVerificationCodeMapper, never()).insert(any(EmailVerificationCodeEntity.class)); // 不发送
        }
    }
}
