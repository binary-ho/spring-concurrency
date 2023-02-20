package com.binaryho.springconcurrency.facade;

import com.binaryho.springconcurrency.repository.LockRepository;
import com.binaryho.springconcurrency.service.OptimisticLockStockService;
import com.binaryho.springconcurrency.service.StockService;
import org.springframework.stereotype.Service;

@Service
public class NamedLockStockFacade {

    private final LockRepository lockRepository;
    private final StockService stockService;

    public NamedLockStockFacade(
        LockRepository lockRepository, StockService stockService) {
        this.lockRepository = lockRepository;
        this.stockService = stockService;
    }

    public void decrease(Long id, Long quantity) {
        try {
            lockRepository.getLock(id.toString());
            stockService.decrease(id, quantity);
        } finally {
            lockRepository.releaseLock(id.toString());
        }
    }
}
