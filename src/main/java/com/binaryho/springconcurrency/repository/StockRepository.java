package com.binaryho.springconcurrency.repository;

import com.binaryho.springconcurrency.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface StockRepository extends JpaRepository<Stock, Long> {

}
