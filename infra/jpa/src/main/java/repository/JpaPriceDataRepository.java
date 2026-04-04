package repository;

import model.price.PriceData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPriceDataRepository extends JpaRepository<PriceData, Long> {
}
