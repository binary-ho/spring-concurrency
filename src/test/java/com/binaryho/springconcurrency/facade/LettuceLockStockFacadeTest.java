package com.binaryho.springconcurrency.facade;


import static org.assertj.core.api.Assertions.assertThat;

import com.binaryho.springconcurrency.domain.Stock;
import com.binaryho.springconcurrency.repository.StockRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LettuceLockStockFacadeTest {

    public final long STOCK_QUANTITY = 100L;
    public final long PRODUCT_ID = 1L;
    /* ThreadPool 에 Thread 의 개수가 너무 많다고 에러가 생기기도 한다.
    * 강의에선 32개로 설명해줘서 32개로 진행하였더니 재고가 하나도 줄어들지 않는 문제가 발생했다. */
    public final int THREAD_POOL_SIZE = 32;
    public final long DECREASE_QUANTITY = 1L;

    @Autowired private LettuceLockStockFacade lettuceLockStockFacade;
    @Autowired private StockRepository stockRepository;

    @BeforeEach
    public void before() {
        stockRepository.deleteAll();
        Stock stock = new Stock(PRODUCT_ID, STOCK_QUANTITY);
        stockRepository.saveAndFlush(stock);
        System.out.println(stock);
    }

    @AfterEach
    public void after() {
        stockRepository.deleteAll();
    }

    @Test
    public void 동시에_여러_요청을_처리할_수_있다() throws InterruptedException {
        final long decreaseQuantity = 1L;
        int threadCount = (int) STOCK_QUANTITY;

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    lettuceLockStockFacade.decrease(PRODUCT_ID, DECREASE_QUANTITY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Stock stock = stockRepository.findById(PRODUCT_ID).orElseThrow();
//        assertThat(stock.getQuantity()).isEqualTo(0L);
        Assertions.assertEquals(0, stock.getQuantity());
    }

}
