package com.binaryho.coupon1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.binaryho.coupon1.repository.CouponRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ApplyServiceTest {

    @Autowired
    private ApplyService applyService;

    @Autowired
    private CouponRepository couponRepository;

    @Test
    @DisplayName("한번만 응모하기")
    public void applyTest() {
        applyService.apply(1L);

        long count = couponRepository.count();

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("여러명 동시 응모")
    // 여러명 동시 응모 test
    public void applyTest2() throws InterruptedException {
        int nThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);

        int threadCount = 1000;
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            long userId = i;
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
        assertThat(count).isEqualTo(100);
    }
}
