package com.binaryho.coupon1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.binaryho.coupon1.repository.AppliedUserRepository;
import com.binaryho.coupon1.repository.CouponCountRepository;
import com.binaryho.coupon1.repository.CouponRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UniqueCouponApplyServiceTest {

    @Autowired
    private UniqueCouponApplyService applyService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponCountRepository couponCountRepository;

    @Autowired
    private AppliedUserRepository appliedUserRepository;

    // 테스트 이후 쿠폰 갯수 초기화
    @BeforeEach
    public void tearDown() {
        couponRepository.deleteAll();
        couponCountRepository.resetCouponCount();
        appliedUserRepository.resetAppliedUser();
    }

    @Test
    @DisplayName("응모가 가능하다")
    public void applyTest() {
        applyService.apply(1L);
        long count = couponRepository.count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("한 유저는 여러번 응모해도 1번개의 쿠폰이 발급된다.")
    public void applyTest2() throws InterruptedException {
        int nThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);

        int threadCount = 100;
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        long userId = 1L;
        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    applyService.apply(userId);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();
        long count = couponRepository.count();
        assertThat(count).isEqualTo(1);
    }
}
