package com.example.samplebatch.config;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.example.samplebatch.repository",
    entityManagerFactoryRef = "dataDbManagerFactory",
    transactionManagerRef = "dataDbTransactionManager"
)
public class DataDbConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.data")
    public DataSource dataDbSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "dataDbManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
        EntityManagerFactoryBuilder builder, @Qualifier("dataDbSource") DataSource dataSource) {
        return builder
            .dataSource(dataSource)
            .packages("com.example.samplebatch.entity") // 엔티티 패키지
            .persistenceUnit("datadb")
            .build();
    }

    @Bean(name = "dataDbTransactionManager")
    public PlatformTransactionManager transactionManager(
        @Qualifier("dataDbManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
