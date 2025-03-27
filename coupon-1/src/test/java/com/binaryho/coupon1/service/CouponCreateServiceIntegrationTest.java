package com.binaryho.coupon1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.binaryho.coupon1.repository.CouponCountRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.client.RedisException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class CouponCreateServiceIntegrationTest {

    private static final String COUPON_STREAM_NAME = "coupon:create";
    @Autowired
    private CouponCreateService couponCreateService;
    @Autowired
    private CouponCountRepository couponCountRepository;
    @Autowired
    private RedissonClient redissonClient;
    private RStream<String, String> couponStream;

    @BeforeEach
    public void setUp() {
        couponCountRepository.resetCouponCount();
        couponStream = redissonClient.getStream(COUPON_STREAM_NAME);
        // 스트림이 존재하면 모든 메시지 삭제
        try {
            couponStream.delete();
        } catch (RedisException e) {
            // 스트림이 존재하지 않는 경우 무시
        }
    }

    @AfterEach
    public void tearDown() {
        // 테스트 후 스트림 정리
        try {
            couponStream.delete();
        } catch (RedisException e) {
            // 스트림이 존재하지 않는 경우 무시
        }
    }

    @Test
    @DisplayName("쿠폰 생성 요청이 Redis 스트림에 정상적으로 추가된다")
    public void createCouponTest() {
        // given
        Long userId = 1L;

        // when
        couponCreateService.apply(userId);

        // then
        long streamLength = couponStream.size();
        assertThat(streamLength).isEqualTo(1);

        // 스트림에 추가된 메시지 확인 - 모든 메시지 가져오기
        Map<StreamMessageId, Map<String, String>> entries = couponStream.range(StreamMessageId.MIN,
            StreamMessageId.MAX);
        assertThat(entries.size()).isEqualTo(1);

        Map<String, String> messageData = entries.values().iterator().next();
        assertThat(messageData.get("userId")).isEqualTo(String.valueOf(userId));
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
        long streamLength = couponStream.size();
        assertThat(streamLength).isEqualTo(0);
    }

    @Test
    @DisplayName("여러 사용자가 동시에 요청해도 최대 100개만 생성된다")
    public void concurrentCreateCouponTest() throws InterruptedException {
        // given
        int threadCount = 1000;
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
        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        long streamLength = couponStream.size();
        assertThat(streamLength).isEqualTo(100);

        // 스트림에 추가된 메시지의 userId 값들이 모두 다른지 확인
        Map<StreamMessageId, Map<String, String>> entries = couponStream.range(StreamMessageId.MIN,
            StreamMessageId.MAX);
        List<String> userIds = entries.values().stream()
            .map(entry -> entry.get("userId"))
            .collect(Collectors.toList());

        long distinctUserIds = userIds.stream().distinct().count();
        assertThat(distinctUserIds).isEqualTo(100);
    }

    @Test
    @DisplayName("쿠폰 카운트가 이미 100을 초과하면 스트림에 메시지가 추가되지 않는다")
    public void exceedLimitTest() {
        // given
        for (int i = 0; i < 101; i++) {
            couponCountRepository.increase();
        }

        // when
        couponCreateService.apply(1L);

        // then
        long streamLength = couponStream.size();
        assertThat(streamLength).isEqualTo(0);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
        }
    }
}
