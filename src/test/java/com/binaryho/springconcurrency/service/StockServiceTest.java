package com.binaryho.springconcurrency.service;


import static org.assertj.core.api.Assertions.assertThat;

import com.binaryho.springconcurrency.domain.Stock;
import com.binaryho.springconcurrency.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StockServiceTest {

    public final long STOCK_QUANTITY = 100L;
    public final long PRODUCT_ID = 1L;

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
}
