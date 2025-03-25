package com.binaryho.coupon1.repository;

import com.binaryho.coupon1.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}
