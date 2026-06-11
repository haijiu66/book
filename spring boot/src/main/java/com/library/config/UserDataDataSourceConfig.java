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
    entityManagerFactoryRef = "userDataEntityManagerFactory",
    transactionManagerRef = "userDataTransactionManager",
    basePackages = "com.library.repository.dynamic"
)
public class UserDataDataSourceConfig {

    @Bean(name = "userDataDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.user-data")
    public DataSourceProperties userDataDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "userDataDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.user-data")
    public DataSource userDataDataSource(@Qualifier("userDataDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "userDataEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean userDataEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("userDataDataSource") DataSource dataSource) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.put("hibernate.show_sql", "true");
        properties.put("hibernate.format_sql", "true");

        return builder
                .dataSource(dataSource)
                .packages("com.library.entity.dynamic")
                .persistenceUnit("userData")
                .properties(properties)
                .build();
    }

    @Bean(name = "userDataTransactionManager")
    public PlatformTransactionManager userDataTransactionManager(
            @Qualifier("userDataEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
