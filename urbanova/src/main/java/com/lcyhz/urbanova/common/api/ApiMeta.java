package com.lcyhz.urbanova.common.api;

import com.lcyhz.urbanova.common.web.RequestIdFilter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiMeta {
    private String requestId;
    private String timestamp;

    public static ApiMeta now() {
        String requestId = null;
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            Object value = attributes.getAttribute(RequestIdFilter.REQUEST_ID_ATTR, RequestAttributes.SCOPE_REQUEST);
            if (value instanceof String requestIdValue) {
                requestId = requestIdValue;
            }
        }
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        return new ApiMeta(requestId, OffsetDateTime.now(ZoneOffset.UTC).toString());
    }
}

