아래 예시는 Redisson을 이용해 coupon:apply라는 Redis Stream에 메시지를 넣는(Producer 역할) 기본 코드와 관련 안내 문서 초안입니다.
(소비자 쪽 소비/ACK 로직, 고급 기능 등은 뒤에서 확장할 수 있도록 최소한의 구성만 담았습니다.)

1. 전반적 구조 및 개념
MQ 역할:

Redis Streams를 통해 쿠폰 적용 요청(Apply)을 메시지처럼 적재합니다.

Producer는 쿠폰 요청을 받아 Stream에 기록만 담당하며, 추후 Consumer(별도 서비스/스레드)가 이 메시지를 읽어 실제로 쿠폰을 발급·처리합니다.

Redis 설정:

Redis 단일 인스턴스 연결

**Persistence(AOF/RDB)**를 활성화해두면 재시작 후에도 스트림 데이터가 유지됩니다.

메모리 관리 상 필요하면 XTRIM, TTL, 또는 MAXLEN 등을 활용하여 오래된 데이터나 불필요한 대량 데이터를 트리밍할 수 있습니다.

Consumer Group (추후 확장):

여러 Consumer가 동시에 메시지를 분산 처리하려면 Redis의 Consumer Group 기능(XGROUP)을 사용할 예정입니다.

현재는 Producer 쪽 코드 및 간단한 지침만 작성하고, Consumer Group 생성/소비 로직은 후에 추가합니다.

주기적 백업 (기본 아이디어):

Redis에 쌓인 스트림 및 처리 상태(예: 어느 메시지가 처리 완료인지) 등을 주기적으로 다른 스토리지(파일 DB 등)에 덤프해야 합니다.

최소한 Redis 서버의 AOF/RDB 옵션을 켜두어야 재시작 시 데이터가 보존됩니다.
둘 중 적절한 옵션을 켜 주세요

필요 시 stream.range(...)(Redisson API)나 Redis 명령(XREAD, XRANGE 등)을 통해 일정 기간 메시지를 읽어와 로컬 파일이나 별도 DB에 백업하는 방식을 구현할 수 있습니다. (추후 구현합시다.)


1. Maven/Gradle 의존성 (예시)
Maven 기준:

xml
복사
편집
<dependency>
  <groupId>org.redisson</groupId>
  <artifactId>redisson</artifactId>
  <version>3.20.0</version> <!-- 버전 예시 -->
</dependency>
Spring Boot를 사용한다면 redisson-spring-boot-starter 등을 함께 고려하실 수 있습니다.

4. Redisson 기본 설정 예시 (싱글 모드)
java
복사
편집
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedissonConfig {

    public RedissonClient createRedissonClient() {
        Config config = new Config();
        // 단일 인스턴스용 URI
        config.useSingleServer()
              .setAddress("redis://127.0.0.1:6379")
              .setConnectionMinimumIdleSize(1)
              .setConnectionPoolSize(10);
        
        // 필요 시 password, timeout 등 추가 설정
        return Redisson.create(config);
    }
}
추후 클러스터로 확장 시 config.useClusterServers()로 전환 가능합니다.

Spring Boot 환경에서는 RedissonAutoConfiguration 등을 통해 빈(bean)으로 등록하면 됩니다.

5. Producer 예시 코드
다음 코드는 쿠폰 적용 요청 정보를 스트림 coupon:apply에 기록하는 간단한 Producer 예시입니다.

CouponApplyRequest : 쿠폰 적용에 필요한 데이터(예: userId, couponCode, 요청 시각 등)를 담는 DTO.

CouponQueueProducer : RStream<K,V>를 사용해 Redis Streams에 레코드를 추가.

```java
package com.example.couponservice.producer;

import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamAddArgs;

import java.util.HashMap;
import java.util.Map;

/**
 * 쿠폰 서비스의 Producer 역할:
 * Redis Streams("coupon:apply")에 메시지를 넣어줌.
 */
public class CouponQueueProducer {

    private final RStream<String, String> couponStream;

    public CouponQueueProducer(RedissonClient redissonClient) {
        // "coupon:apply" 라는 Redis Stream 키
        this.couponStream = redissonClient.getStream("coupon:apply");
    }

    /**
     * 쿠폰 적용 요청 메시지를 Redis Stream에 추가.
     */
    public void sendCouponApplyRequest(CouponApplyRequest request) {
        // RStream에 넣을 Map<String, String> 형태 구성
        Map<String, String> data = new HashMap<>();
        data.put("userId", request.getUserId());
        data.put("couponCode", request.getCouponCode());
        data.put("requestedAt", String.valueOf(System.currentTimeMillis()));

        // 필요시 더 많은 필드를 넣을 수 있음
        // ex) data.put("channel", request.getChannel());

        // Redisson의 add() 메서드 (XADD에 해당)
        // StreamAddArgs.entry(...) 로 Map 생성
        couponStream.add(StreamAddArgs.entry(data));
    }
}
```

```java
package com.example.couponservice.producer;

/**
 * 쿠폰 적용 요청 DTO
 */
public class CouponApplyRequest {
    
    private String userId;
    private String couponCode;
    // 기타 필요한 필드들...

    public CouponApplyRequest(String userId, String couponCode) {
        this.userId = userId;
        this.couponCode = couponCode;
    }

    public String getUserId() {
        return userId;
    }

    public String getCouponCode() {
        return couponCode;
    }
    
    // setter, toString 등...
}
```
위 코드에서 couponStream.add(...)는 내부적으로 XADD coupon:apply ... 명령을 실행하여 Redis에 메시지를 쌓습니다.

명시적으로 Stream을 생성하지 않아도 존재하지 않을 경우 자동 생성됩니다.

6. 단순 사용 흐름 예시
```java
public class MainApp {
    public static void main(String[] args) {
        // 1) RedissonClient 준비
        RedissonConfig config = new RedissonConfig();
        RedissonClient redisson = config.createRedissonClient();
        
        // 2) Producer 인스턴스 생성
        CouponQueueProducer producer = new CouponQueueProducer(redisson);

        // 3) 쿠폰 적용 요청을 스트림에 기록
        CouponApplyRequest request = new CouponApplyRequest("user123", "COUPON-ABC");
        producer.sendCouponApplyRequest(request);

        // 4) 종료 시 클라이언트 닫기
        redisson.shutdown();
    }
}
```