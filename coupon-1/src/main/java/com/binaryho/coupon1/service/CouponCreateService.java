package com.binaryho.coupon1.service;

import com.binaryho.coupon1.producer.CouponCreateProducer;
import com.binaryho.coupon1.repository.CouponCountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CouponCreateService {

    private static final Logger log = LoggerFactory.getLogger(CouponCreateService.class);

    private final CouponCountRepository couponCountRepository;
    private final CouponCreateProducer couponCreateProducer;

    public CouponCreateService(
        CouponCountRepository couponCountRepository,
        CouponCreateProducer couponCreateProducer
    ) {
        this.couponCountRepository = couponCountRepository;
        this.couponCreateProducer = couponCreateProducer;
    }

    public void apply(Long userId) {
        Long count = couponCountRepository.increase();
        if (count > 100) {
            log.info("쿠폰 생성 제한 도달 userId: {}, count: {}", userId, count);
            return;
        }
        couponCreateProducer.create(userId);
    }
}
