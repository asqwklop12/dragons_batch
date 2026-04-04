package repository;

import lombok.RequiredArgsConstructor;
import model.price.PriceDataRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PriceDataRepositoryImpl implements PriceDataRepository {
  private final JpaPriceDataRepository repository;
}
