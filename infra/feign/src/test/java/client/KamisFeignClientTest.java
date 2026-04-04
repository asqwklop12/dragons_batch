package client;

import static org.assertj.core.api.Assertions.assertThat;

import dto.KamisMonthlyPriceResponse;
import java.lang.reflect.RecordComponent;
import org.junit.jupiter.api.Test;

class KamisFeignClientTest {

  @Test
  void monthlyPriceResponseUsesTypedRecordsInsteadOfMap() {
    RecordComponent[] components = KamisMonthlyPriceResponse.KamisMonthlyPriceItem.class.getRecordComponents();

    assertThat(components).extracting(RecordComponent::getName)
        .contains("itemCode", "itemName", "kindCode", "marketCode", "dpr1", "productClsCode");
  }
}
