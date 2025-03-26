package com.binaryho.coupon1.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AppliedUserRepository {

    public static final String APPLIED_USER = "applied_user";
    private static final int ADDED_COUNT = 1;
    private final RedisTemplate<String, String> redisTemplate;

    public AppliedUserRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean add(Long userId) {
        Long appliedUser = redisTemplate
            .opsForSet()
            .add(APPLIED_USER, userId.toString());
        System.out.println("appliedUser = " + appliedUser);
        return isApplied(appliedUser);
    }

    private boolean isApplied(Long result) {
        return result != null && result == ADDED_COUNT;
    }

    public void resetAppliedUser() {
        redisTemplate.delete(APPLIED_USER);
    }
}
