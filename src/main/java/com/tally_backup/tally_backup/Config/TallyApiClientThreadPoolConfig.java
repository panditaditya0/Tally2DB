package com.tally_backup.tally_backup.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class TallyApiClientThreadPoolConfig {

    @Bean(name = "taskExecutorTallyApi")
    public ThreadPoolTaskExecutor tallyApi(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("TallyApiClient--");
        executor.initialize();
        return executor;
    }
}