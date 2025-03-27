package com.binaryho.coupon1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binaryho.coupon1.producer.CouponCreateProducer;
import com.binaryho.coupon1.repository.CouponCountRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public class CouponCreateServiceTest {

    @Autowired
    private CouponCreateService couponCreateService;

    @Autowired
    private CouponCountRepository couponCountRepository;

    @MockBean
    private CouponCreateProducer couponCreateProducer;

    @Captor
    private ArgumentCaptor<Long> userIdCaptor;

    @BeforeEach
    public void setUp() {
        couponCountRepository.resetCouponCount();
    }

    @Test
    @DisplayName("쿠폰 생성 요청이 정상적으로 처리된다")
    public void createCouponTest() {
        // given
        Long userId = 1L;

        // when
        couponCreateService.apply(userId);

        // then
        verify(couponCreateProducer, times(1)).create(userId);
    }

    @Test
    @DisplayName("쿠폰 생성 요청이 100개를 초과하면 더 이상 생성되지 않는다")
    public void createCouponLimitTest() {
        // given
        for (int i = 0; i < 100; i++) {
            couponCountRepository.increase();
        }

        // when
        couponCreateService.apply(1L);

        // then
        verify(couponCreateProducer, times(0)).create(any());
    }

    @Test
    @DisplayName("여러 사용자가 동시에 요청해도 최대 100개만 생성된다")
    public void concurrentCreateCouponTest() throws InterruptedException {
        // given
        int threadCount = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            long userId = i;
            executorService.execute(() -> {
                try {
                    couponCreateService.apply(userId);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        verify(couponCreateProducer, times(100)).create(userIdCaptor.capture());
        
        // 캡처된 userId 값들이 모두 다른지 확인 (중복 없이 100명에게 발급)
        assertThat(userIdCaptor.getAllValues().stream().distinct().count()).isEqualTo(100);
    }

    @Test
    @DisplayName("쿠폰 카운트가 이미 100을 초과하면 Producer가 호출되지 않는다")
    public void exceedLimitTest() {
        // given
        for (int i = 0; i < 101; i++) {
            couponCountRepository.increase();
        }

        // when
        couponCreateService.apply(1L);

        // then
        verify(couponCreateProducer, times(0)).create(any());
    }
}
