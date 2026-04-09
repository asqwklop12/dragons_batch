package com.config;

import com.application.PriceReadService;
import com.process.PriceItemProcessor;
import com.write.PriceItemWriter;
import java.time.Clock;
import java.time.LocalDate;
import model.price.PriceData;
import model.price.PriceDataRepository;
import model.price.PriceReadItem;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;

@Configuration(proxyBeanMethods = false)
public class BatchCoreConfiguration {

  @Bean
  @StepScope
  ItemReader<PriceReadItem> priceItemReader(
      PriceReadService priceReadService,
      @Value("#{jobParameters['itemCategoryCode']}") String itemCategoryCode,
      @Value("#{jobParameters['regDay']}") String regDay
  ) {
    return new ListItemReader<>(
        priceReadService.readItems(
            itemCategoryCode == null || itemCategoryCode.isBlank() ? "200" : itemCategoryCode,
            regDay == null || regDay.isBlank() ? LocalDate.now() : LocalDate.parse(regDay)
        )
    );
  }

  @Bean
  ItemProcessor<PriceReadItem, PriceData> priceItemProcessor() {
    return new PriceItemProcessor(Clock.systemDefaultZone());
  }

  @Bean
  ItemWriter<PriceData> priceItemWriter(PriceDataRepository priceDataRepository) {
    return new PriceItemWriter(priceDataRepository);
  }
}
