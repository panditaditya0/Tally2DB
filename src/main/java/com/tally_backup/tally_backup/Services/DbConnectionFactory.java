package com.tally_backup.tally_backup.Services;

import com.tally_backup.tally_backup.Config.DbConfigProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class DbConnectionFactory {
    private static final Logger logger = LoggerFactory.getLogger(DbConnectionFactory.class);
    private ConcurrentMap<String, DataSource> customDataSourceList = new ConcurrentHashMap<>();
    private final DbConfigProperties dbConfig;

    public DbConnectionFactory(DbConfigProperties dbConfig) {
        this.dbConfig = dbConfig;
    }

    public synchronized DataSource getDataSource(String dbName) {
        return customDataSourceList.computeIfAbsent(dbName, this::createNewDataSource);
    }

    public void closeDataSource(String dbName) {
        if (this.customDataSourceList.containsKey(dbName)) {
            DataSource ds = this.customDataSourceList.remove(dbName);
            if (ds instanceof HikariDataSource) {
                ((HikariDataSource) ds).close();
            }
            logger.info("Custom DataSource Closed for Database Name -> " + dbName);
        }
    }

    private DataSource createNewDataSource(String dbName) {
        String fullUrl = dbConfig.getUrl() + dbName + "?allowLoadLocalInfile=true";

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(fullUrl);
        ds.setUsername(dbConfig.getUsername());
        ds.setPassword(dbConfig.getPassword());
        ds.setDriverClassName(dbConfig.getDriverClassName());

        ds.setMaximumPoolSize(5);
        ds.setMinimumIdle(3);
        ds.setIdleTimeout(30_000);
        ds.setConnectionTimeout(30_000);
        ds.setMaxLifetime(1_800_000);
        ds.setPoolName("DynamicPool-" + dbName);

        logger.info("Created new DataSource for DB: {}", dbName);
        return ds;
    }
}