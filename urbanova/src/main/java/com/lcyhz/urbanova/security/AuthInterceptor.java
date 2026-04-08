package com.lcyhz.urbanova.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.entity.UserEntity;
import com.lcyhz.urbanova.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (isPublicEndpoint(path, method)) {
            return true;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED.value(), ErrorCodes.AUTH_FORBIDDEN, "Missing Bearer token");
        }

        String token = authorization.substring("Bearer ".length()).trim();
        JwtPrincipal principal = jwtService.parseToken(token);

        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId, principal.getUserId()));
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED.value(), ErrorCodes.AUTH_INVALID_CREDENTIALS, "User not found");
        }
        if (!DomainConstants.ACCOUNT_ACTIVE.equals(user.getAccountStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), ErrorCodes.AUTH_FORBIDDEN, "Account is not active");
        }

        AuthContext.setCurrentUser(new AuthUser(user.getUserId(), user.getRole(), user.getEmail()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private boolean isPublicEndpoint(String path, String method) {
        if ("POST".equalsIgnoreCase(method) && "/api/v1/auth/register".equals(path)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/v1/auth/login".equals(path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && "/api/v1/hire-options".equals(path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && "/api/v1/scooters/ids".equals(path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && "/api/v1/scooters/map-points".equals(path)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/v1/pricing/quotes".equals(path)) {
            return true;
        }
        return "GET".equalsIgnoreCase(method) && "/api/v1/health".equals(path);
    }
}
