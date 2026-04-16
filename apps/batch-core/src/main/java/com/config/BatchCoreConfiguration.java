package com.config;

import com.application.PriceReadService;
import com.process.KamisItemProcessor;
import com.read.KamisItemReader;
import com.write.KamisItemWriter;
import java.time.Clock;
import java.time.LocalDate;
import model.price.PriceData;
import model.price.PriceDataRepository;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import model.price.PriceReadItem;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class BatchCoreConfiguration {

  private static final int CHUNK_SIZE = 100;

  @Bean
  Clock batchClock() {
    return Clock.systemDefaultZone();
  }

  @Bean
  @StepScope
  ItemReader<PriceReadItem> kamisItemReader(
      PriceReadService priceReadService,
      @Value("#{jobParameters['itemCategoryCode']}") String itemCategoryCode,
      @Value("#{jobParameters['regDay']}") String regDay
  ) {
    return new KamisItemReader(
        priceReadService,
        itemCategoryCode == null || itemCategoryCode.isBlank() ? "200" : itemCategoryCode,
        regDay == null || regDay.isBlank() ? LocalDate.now() : LocalDate.parse(regDay)
    );
  }

  @Bean
  ItemProcessor<PriceReadItem, PriceData> kamisItemProcessor(Clock batchClock) {
    return new KamisItemProcessor(batchClock);
  }

  @Bean
  ItemWriter<PriceData> kamisItemWriter(PriceDataRepository priceDataRepository) {
    return new KamisItemWriter(priceDataRepository);
  }

  @Bean
  Step kamisPriceStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      ItemReader<PriceReadItem> kamisItemReader,
      ItemProcessor<PriceReadItem, PriceData> kamisItemProcessor,
      ItemWriter<PriceData> kamisItemWriter
  ) {
    return new StepBuilder("kamisPriceStep", jobRepository)
        .<PriceReadItem, PriceData>chunk(CHUNK_SIZE)
        .transactionManager(transactionManager)
        .reader(kamisItemReader)
        .processor(kamisItemProcessor)
        .writer(kamisItemWriter)
        .build();
  }

  @Bean
  Job kamisPriceJob(JobRepository jobRepository, Step kamisPriceStep) {
    return new JobBuilder("kamisPriceJob", jobRepository)
        .start(kamisPriceStep)
        .build();
  }
}
