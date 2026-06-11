package com.library.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TransactionConfig {

    @Bean(name = "chainedTransactionManager")
    public PlatformTransactionManager chainedTransactionManager(
            @Qualifier("primaryTransactionManager") PlatformTransactionManager primaryTxManager,
            @Qualifier("adminTransactionManager") PlatformTransactionManager adminTxManager,
            @Qualifier("userTransactionManager") PlatformTransactionManager userTxManager,
            @Qualifier("adminDataTransactionManager") PlatformTransactionManager adminDataTxManager,
            @Qualifier("userDataTransactionManager") PlatformTransactionManager userDataTxManager) {
        return new ChainedTransactionManager(
                primaryTxManager,
                adminTxManager,
                userTxManager,
                adminDataTxManager,
                userDataTxManager
        );
    }
}
