package com.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DataSourceConfig {

  @Bean
  @ConfigurationProperties(prefix = "datasource.mysql-jpa.primary")
  HikariConfig primaryHikariConfig() {
    return new HikariConfig();
  }

  @Bean
  DataSource dataSource(HikariConfig config) {
    return new HikariDataSource(config);
  }

}
