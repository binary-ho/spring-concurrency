package com.binaryho.springconcurrency.facade;


import static org.assertj.core.api.Assertions.assertThat;

import com.binaryho.springconcurrency.domain.Stock;
import com.binaryho.springconcurrency.facade.OptimisticLockStockFacade;
import com.binaryho.springconcurrency.repository.StockRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OptimisticStockFacadeeTest {

    public final long STOCK_QUANTITY = 100L;
    public final long PRODUCT_ID = 1L;
    public final int THREAD_POOL_SIZE = 32;

    @Autowired private OptimisticLockStockFacade optimisticLockStockFacade;
    @Autowired private StockRepository stockRepository;

    @BeforeEach
    public void before() {
        stockRepository.deleteAll();
        Stock stock = new Stock(PRODUCT_ID, STOCK_QUANTITY);
        stockRepository.saveAndFlush(stock);
    }

    @AfterEach
    public void after() {
        stockRepository.deleteAll();
    }

    @Test
    public void 동시에_여러_요청을_처리할_수_있다() throws InterruptedException {
        long decreaseQuantity = 1L;
        int threadCount = (int) STOCK_QUANTITY;

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    optimisticLockStockFacade.decrease(PRODUCT_ID, decreaseQuantity);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Stock stock = stockRepository.findById(PRODUCT_ID).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(0L);
    }

}
