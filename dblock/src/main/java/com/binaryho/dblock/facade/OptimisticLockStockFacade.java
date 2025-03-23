package com.binaryho.dblock.facade;

import com.binaryho.dblock.service.OptimisticLockStockService;
import org.springframework.stereotype.Component;

@Component
public class OptimisticLockStockFacade {

    private final OptimisticLockStockService optimisticLockStockService;

    public OptimisticLockStockFacade(OptimisticLockStockService optimisticLockStockService) {
        this.optimisticLockStockService = optimisticLockStockService;
    }

    public void decrease(Long id, Long quantity) throws InterruptedException {
        int maxRetryCount = 50; // 최대 재시도 횟수 설정
        int retryCount = 0;
        
        while (retryCount < maxRetryCount) {
            try {
                optimisticLockStockService.decrease(id, quantity);
                return; // 성공 시 즉시 반환
            } catch (Exception e) {
                /* 실패한 경우 50 milli second 후에 재시도된다. */
                Thread.sleep(50);
                retryCount++;
            }
        }
        
        // 최대 재시도 횟수를 초과한 경우 예외 발생
        throw new RuntimeException("재고 감소 작업 실패: 최대 재시도 횟수(" + maxRetryCount + ")를 초과했습니다.");
    }
}
