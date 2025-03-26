package com.binaryho.coupon1.producer;

import java.time.LocalDateTime;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CouponCreateProducer {

    private static final Logger log = LoggerFactory.getLogger(CouponCreateProducer.class);
    private static final String STREAM_KEY = "coupon:create";

    private final RStream<String, String> couponStream;

    public CouponCreateProducer(RedissonClient redissonClient) {
        this.couponStream = redissonClient.getStream(STREAM_KEY);
        log.info("CouponQueueProducer initialized with stream key: {}", STREAM_KEY);
    }

    public void create(Long userId) {
        CouponCreateRequest request = new CouponCreateRequest(userId, LocalDateTime.now());
        StreamAddArgs<String, String> entries = StreamAddArgs.entries(request.toMap());
        StreamMessageId streamMessageId = couponStream.add(entries);
        log.info("coupon apply request added MessageID: {}, Request: {}", streamMessageId, request);
    }
}
