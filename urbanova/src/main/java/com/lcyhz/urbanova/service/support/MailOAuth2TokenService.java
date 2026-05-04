package com.lcyhz.urbanova.service.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MailOAuth2TokenService {
    private static final Duration TOKEN_REFRESH_BUFFER = Duration.ofMinutes(1);

    private final RestClient restClient;
    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();
    private final AtomicReference<String> currentRefreshToken = new AtomicReference<>();

    @Value("${app.mail.oauth2.enabled:false}")
    private boolean oauth2Enabled;

    @Value("${app.mail.oauth2.provider:outlook}")
    private String provider;

    @Value("${app.mail.oauth2.tenant:consumers}")
    private String tenant;

    @Value("${app.mail.oauth2.client-id:}")
    private String clientId;

    @Value("${app.mail.oauth2.client-secret:}")
    private String clientSecret;

    @Value("${app.mail.oauth2.refresh-token:}")
    private String refreshToken;

    @Value("${app.mail.oauth2.scope:https://outlook.office.com/SMTP.Send offline_access}")
    private String scope;

    @Value("${app.mail.oauth2.token-uri:}")
    private String tokenUri;

    public MailOAuth2TokenService() {
        this.restClient = RestClient.builder().build();
    }

    public boolean isEnabled() {
        return oauth2Enabled;
    }

    public String getAccessToken() {
        if (!oauth2Enabled) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE.value(), ErrorCodes.EMAIL_DELIVERY_NOT_CONFIGURED,
                    "Mail OAuth2 is not enabled");
        }
        ensureConfigured();
        CachedToken existingToken = cachedToken.get();
        if (existingToken != null && !existingToken.isExpiringSoon()) {
            return existingToken.accessToken();
        }

        synchronized (this) {
            existingToken = cachedToken.get();
            if (existingToken != null && !existingToken.isExpiringSoon()) {
                return existingToken.accessToken();
            }
            TokenResponse response = requestToken();
            Instant expiresAt = Instant.now().plusSeconds(Math.max(response.expiresIn(), 300));
            cachedToken.set(new CachedToken(response.accessToken(), expiresAt));
            if (!isBlank(response.refreshToken())) {
                currentRefreshToken.set(response.refreshToken());
            }
            return response.accessToken();
        }
    }

    private void ensureConfigured() {
        if (!"outlook".equalsIgnoreCase(provider)
                && !"microsoft".equalsIgnoreCase(provider)
                && !"graph".equalsIgnoreCase(provider)) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE.value(), ErrorCodes.EMAIL_DELIVERY_NOT_CONFIGURED,
                    "Unsupported mail OAuth2 provider: " + provider);
        }
        if (isBlank(clientId) || isBlank(clientSecret) || isBlank(refreshToken)) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE.value(), ErrorCodes.EMAIL_DELIVERY_NOT_CONFIGURED,
                    "Mail OAuth2 is missing required Microsoft configuration");
        }
        currentRefreshToken.compareAndSet(null, refreshToken);
    }

    private TokenResponse requestToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", currentRefreshToken.get());
        if (!isBlank(scope)) {
            form.add("scope", scope);
        }

        try {
            TokenResponse response = restClient.post()
                    .uri(resolveTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null || isBlank(response.accessToken())) {
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE.value(), ErrorCodes.EMAIL_DELIVERY_FAILED,
                        "Mail OAuth2 token endpoint returned an empty access token");
            }
            return response;
        } catch (RestClientException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE.value(), ErrorCodes.EMAIL_DELIVERY_FAILED,
                    "Failed to obtain Microsoft OAuth2 access token: " + ex.getMessage());
        }
    }

    private String resolveTokenUri() {
        if (!isBlank(tokenUri)) {
            return tokenUri;
        }
        return "https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/token";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record CachedToken(String accessToken, Instant expiresAt) {
        boolean isExpiringSoon() {
            return Instant.now().plus(TOKEN_REFRESH_BUFFER).isAfter(expiresAt);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }
}
