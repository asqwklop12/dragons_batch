package com.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class MySqlContainerTestSupport {

  @Container
  protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("dragons_db")
      .withUsername("application")
      .withPassword("application");

  @DynamicPropertySource
  static void registerMySqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);

    registry.add("datasource.mysql-jpa.primary.jdbc-url", MYSQL::getJdbcUrl);
    registry.add("datasource.mysql-jpa.primary.username", MYSQL::getUsername);
    registry.add("datasource.mysql-jpa.primary.password", MYSQL::getPassword);
    registry.add("datasource.mysql-jpa.primary.driver-class-name", MYSQL::getDriverClassName);
  }
}
