package com.binaryho.coupon1.service;

import com.binaryho.coupon1.domain.Coupon;
import com.binaryho.coupon1.repository.AppliedUserRepository;
import com.binaryho.coupon1.repository.CouponRepository;
import org.springframework.stereotype.Service;

@Service
public class UniqueCouponApplyService {

    private final CouponRepository couponRepository;
    private final AppliedUserRepository appliedUserRepository;

    public UniqueCouponApplyService(CouponRepository couponRepository,
        AppliedUserRepository appliedUserRepository) {
        this.couponRepository = couponRepository;
        this.appliedUserRepository = appliedUserRepository;
    }

    public void apply(Long userId) {
        boolean add = appliedUserRepository.add(userId);
        if (!add) {
            return;
        }
        couponRepository.save(new Coupon(userId));
    }
}
