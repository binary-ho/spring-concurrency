package com.binaryho.springconcurrency.service;


import static org.assertj.core.api.Assertions.assertThat;

import com.binaryho.springconcurrency.domain.Stock;
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
class StockServiceTest {

    public final long STOCK_QUANTITY = 100L;
    public final long PRODUCT_ID = 1L;
    public final int THREAD_POOL_SIZE = 32;

    @Autowired private StockService stockService;
    @Autowired private StockRepository stockRepository;

    @BeforeEach
    public void before() {
        Stock stock = new Stock(PRODUCT_ID, STOCK_QUANTITY);
        System.out.println(stockService);
        System.out.println(stockRepository);
        stockRepository.saveAndFlush(stock);
    }

    @AfterEach
    public void after() {
        stockRepository.deleteAll();
    }

    @Test
    public void 재고량을_줄일_수_있다() {
        long decreaseQuantity = 1L;
        stockService.decrease(PRODUCT_ID, decreaseQuantity);

        Stock stock = stockRepository.findById(PRODUCT_ID).orElseThrow();

        assertThat(stock.getQuantity()).isEqualTo(STOCK_QUANTITY - decreaseQuantity);
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
                    stockService.decrease(PRODUCT_ID, decreaseQuantity);
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
