package com.library.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "adminDataEntityManagerFactory",
    transactionManagerRef = "adminDataTransactionManager",
    basePackages = "com.library.repository.dynamic"
)
public class AdminDataDataSourceConfig {

    @Bean(name = "adminDataDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.admin-data")
    public DataSourceProperties adminDataDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "adminDataDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.admin-data")
    public DataSource adminDataDataSource(@Qualifier("adminDataDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "adminDataEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean adminDataEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("adminDataDataSource") DataSource dataSource) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.put("hibernate.show_sql", "true");
        properties.put("hibernate.format_sql", "true");

        return builder
                .dataSource(dataSource)
                .packages("com.library.entity.dynamic")
                .persistenceUnit("adminData")
                .properties(properties)
                .build();
    }

    @Bean(name = "adminDataTransactionManager")
    public PlatformTransactionManager adminDataTransactionManager(
            @Qualifier("adminDataEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
