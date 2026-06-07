package com.example.platform.topbiz.config;

import com.example.platform.common.error.BusinessException;
import com.example.platform.topbiz.repository.InMemoryTopbizOrchestrationRepository;
import com.example.platform.topbiz.repository.JdbcTopbizOrchestrationRepository;
import com.example.platform.topbiz.repository.TopbizOrchestrationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TopbizOrchestrationPersistenceProperties.class)
public class TopbizOrchestrationRepositoryConfig {

    @Bean
    @ConditionalOnProperty(prefix = "topbiz.orchestration", name = "repository-type", havingValue = "jdbc")
    public DataSource topbizOrchestrationDataSource(TopbizOrchestrationPersistenceProperties properties) {
        TopbizOrchestrationPersistenceProperties.Jdbc jdbc = properties.getJdbc();
        if (!StringUtils.hasText(jdbc.getUrl())) {
            throw new BusinessException(
                    "ORCHESTRATION_JDBC_URL_MISSING",
                    "topbiz.orchestration.jdbc.url must be configured when repository-type=jdbc",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("topbiz-orchestration-pool");
        dataSource.setJdbcUrl(jdbc.getUrl().trim());
        dataSource.setUsername(jdbc.getUsername());
        dataSource.setPassword(jdbc.getPassword());
        dataSource.setDriverClassName(jdbc.getDriverClassName());
        dataSource.setMaximumPoolSize(jdbc.getMaximumPoolSize());
        dataSource.setConnectionTimeout(jdbc.getConnectionTimeout().toMillis());
        return dataSource;
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnProperty(prefix = "topbiz.orchestration", name = "repository-type", havingValue = "jdbc")
    public JdbcTemplate topbizOrchestrationJdbcTemplate(DataSource topbizOrchestrationDataSource) {
        return new JdbcTemplate(topbizOrchestrationDataSource);
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnProperty(prefix = "topbiz.orchestration", name = "repository-type", havingValue = "jdbc")
    public TopbizOrchestrationRepository jdbcTopbizOrchestrationRepository(JdbcTemplate topbizOrchestrationJdbcTemplate,
                                                                           ObjectMapper objectMapper,
                                                                           TopbizOrchestrationPersistenceProperties properties) {
        return new JdbcTopbizOrchestrationRepository(
                topbizOrchestrationJdbcTemplate,
                objectMapper,
                properties.getJdbc()
        );
    }

    @Bean
    @ConditionalOnMissingBean(TopbizOrchestrationRepository.class)
    public TopbizOrchestrationRepository inMemoryTopbizOrchestrationRepository() {
        return new InMemoryTopbizOrchestrationRepository();
    }
}
