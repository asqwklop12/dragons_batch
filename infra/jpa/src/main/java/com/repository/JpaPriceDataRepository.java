package com.repository;

import java.time.LocalDate;
import java.util.List;
import model.price.PriceData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPriceDataRepository extends JpaRepository<PriceData, Long> {

  List<PriceData> findAllByRegDayOrderByCreatedAtDesc(LocalDate regDay);

  List<PriceData> findAllByItemNameContainingIgnoreCaseOrderByCreatedAtDesc(String itemName);

  List<PriceData> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
