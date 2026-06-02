package com.example.platform.topbiz;

import com.example.platform.topbiz.config.TopbizRemoteProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.example.platform")
@EnableFeignClients(basePackages = "com.example.platform.topbiz.remote")
@EnableConfigurationProperties(TopbizRemoteProperties.class)
public class TopbizServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TopbizServiceApplication.class, args);
    }
}
