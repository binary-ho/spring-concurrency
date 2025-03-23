package com.binaryho.dblock.service;

import com.binaryho.dblock.domain.Stock;
import com.binaryho.dblock.repository.StockRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

//    @Transactional(propagation = Propagation.REQUIRES_NEW) /* 부모의 트랜잭션과 별도로 실행됨 */
//    public synchronized void decrease(Long id, Long quantity) {
    public void decrease(Long id, Long quantity) {
        Stock stock = getStockById(id);
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }

    private Stock getStockById(Long id) {
        return stockRepository.findById(id)
            .orElseThrow(NoSuchElementException::new);
    }
}
