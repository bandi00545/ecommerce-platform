package com.ecommerce.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "com.ecommerce.userservice",
        "com.ecommerce.common"    // scan common for filters, configs
})
@EnableDiscoveryClient
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableCaching
@EnableKafka
@EnableAsync
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
