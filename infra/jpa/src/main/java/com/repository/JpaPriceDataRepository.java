package com.repository;

import java.time.LocalDate;
import java.util.List;
import model.price.PriceData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPriceDataRepository extends JpaRepository<PriceData, Long> {

  List<PriceData> findAllByRegDayOrderByCreatedAtDescIdDesc(LocalDate regDay);

  List<PriceData> findAllByItemNameContainingIgnoreCaseOrderByCreatedAtDescIdDesc(String itemName);

  List<PriceData> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
