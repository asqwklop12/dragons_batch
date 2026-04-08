package com.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.client")
@ConfigurationPropertiesScan(basePackages = "com.properties")
public class InfraClientConfiguration {
}
