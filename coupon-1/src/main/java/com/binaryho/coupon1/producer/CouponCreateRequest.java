package com.binaryho.coupon1.producer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class CouponCreateRequest {

    private final Long userId;
    private final LocalDateTime requestedAt;

    public CouponCreateRequest(Long userId, LocalDateTime requestedAt) {
        this.userId = userId;
        this.requestedAt = requestedAt;
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("userId", String.valueOf(userId));
        map.put("requestedAt", requestedAt.toString());
        return map;
    }
}
