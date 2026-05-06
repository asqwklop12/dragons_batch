package com.config;

import com.application.MonthlyPriceReadService;
import com.application.PriceReadService;
import com.process.KamisItemProcessor;
import com.read.KamisItemReader;
import com.read.KamisMonthlyItemReader;
import com.write.KamisItemWriter;
import constant.Constants;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import model.price.PriceData;
import model.price.PriceDataRepository;
import model.price.PriceReadItem;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class BatchCoreConfiguration {



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
  @StepScope
  ItemReader<PriceReadItem> kamisMonthlyItemReader(
      MonthlyPriceReadService monthlyPriceReadService,
      @Value("#{jobParameters['itemCategoryCode']}") String itemCategoryCode,
      @Value("#{jobParameters['yyyy']}") String year,
      @Value("#{jobParameters['mm']}") String month
  ) {
    YearMonth yearMonth = (year == null || year.isBlank() || month == null || month.isBlank())
        ? YearMonth.now()
        : YearMonth.of(Integer.parseInt(year), Integer.parseInt(month));

    return new KamisMonthlyItemReader(
        monthlyPriceReadService,
        itemCategoryCode == null || itemCategoryCode.isBlank() ? "200" : itemCategoryCode,
        yearMonth
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
  SkipPolicy batchSkipPolicy() {
    return new BatchSkipPolicy(Constants.SKIP_LIMIT);
  }

  @Bean
  BatchStepMonitoringListener batchStepMonitoringListener() {
    return new BatchStepMonitoringListener();
  }

  @Bean
  Step kamisPriceStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      @Qualifier("kamisItemReader") ItemReader<PriceReadItem> kamisItemReader,
      ItemProcessor<PriceReadItem, PriceData> kamisItemProcessor,
      ItemWriter<PriceData> kamisItemWriter,
      SkipPolicy batchSkipPolicy,
      BatchStepMonitoringListener batchStepMonitoringListener
  ) {
    return new StepBuilder("kamisPriceStep", jobRepository)
        .<PriceReadItem, PriceData>chunk(Constants.CHUNK_SIZE)
        .transactionManager(transactionManager)
        .faultTolerant()
        .skipPolicy(batchSkipPolicy)
        .reader(kamisItemReader)
        .processor(kamisItemProcessor)
        .writer(kamisItemWriter)
        .listener((ItemWriteListener<PriceData>) batchStepMonitoringListener)
        .listener((SkipListener<PriceReadItem, PriceData>) batchStepMonitoringListener)
        .listener(batchStepMonitoringListener)
        .build();
  }

  @Bean
  Job kamisPriceJob(JobRepository jobRepository, @Qualifier("kamisPriceStep") Step kamisPriceStep) {
    return new JobBuilder("kamisPriceJob", jobRepository)
        .start(kamisPriceStep)
        .build();
  }

  @Bean
  Step kamisMonthlyPriceStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      @Qualifier("kamisMonthlyItemReader") ItemReader<PriceReadItem> kamisMonthlyItemReader,
      ItemProcessor<PriceReadItem, PriceData> kamisItemProcessor,
      ItemWriter<PriceData> kamisItemWriter,
      SkipPolicy batchSkipPolicy,
      BatchStepMonitoringListener batchStepMonitoringListener
  ) {
    return new StepBuilder("kamisMonthlyPriceStep", jobRepository)
        .<PriceReadItem, PriceData>chunk(Constants.CHUNK_SIZE)
        .transactionManager(transactionManager)
        .faultTolerant()
        .skipPolicy(batchSkipPolicy)
        .reader(kamisMonthlyItemReader)
        .processor(kamisItemProcessor)
        .writer(kamisItemWriter)
        .listener((ItemWriteListener<PriceData>) batchStepMonitoringListener)
        .listener((SkipListener<PriceReadItem, PriceData>) batchStepMonitoringListener)
        .listener(batchStepMonitoringListener)
        .build();
  }

  @Bean
  Job kamisMonthlyPriceJob(
      JobRepository jobRepository,
      @Qualifier("kamisMonthlyPriceStep") Step kamisMonthlyPriceStep
  ) {
    return new JobBuilder("kamisMonthlyPriceJob", jobRepository)
        .start(kamisMonthlyPriceStep)
        .build();
  }
}
