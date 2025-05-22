package com.tally_backup.tally_backup.Services;

import com.tally_backup.tally_backup.Dto.FullBackupConfig;
import com.tally_backup.tally_backup.Dto.TallyProcessConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class DbService {
    @Value("${backup_db_name}")
    private String BACKUP_DB_NAME_TEMPLATE;

    static Logger logger = LoggerFactory.getLogger(DbService.class);

    private final JdbcTemplate jdbcTemplate;
    private final DbConnectionFactory dbConnectionFactory;
    private final DataSource datasource;


    public DbService(JdbcTemplate jdbcTemplate
            , DbConnectionFactory dbConnectionFactory
            , DataSource datasource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dbConnectionFactory = dbConnectionFactory;
        this.datasource = datasource;
    }

    public List<FullBackupConfig> fetchAllCronConfig(){
        String sql = "SELECT * FROM tally_backup_cron_config";
        return jdbcTemplate.query(sql, new RowMapper<FullBackupConfig>() {
            @Override
            public FullBackupConfig mapRow(ResultSet rs, int rowNum) throws SQLException {
                FullBackupConfig config = new FullBackupConfig();
                if(rs.getString("to_date").equals("till_date")){
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    config.setToDate(formatter.format(LocalDate.now()));
                }else{
                    config.setToDate(String.valueOf(rs.getDate("to_date").toLocalDate()));
                }
                config.setCompanyName(rs.getString("company_name"));
                config.setFromDate(String.valueOf(rs.getDate("from_date")));
                config.setDbNamePrefix(rs.getString("db_name_prefix"));
                return config;
            }
        });
    }

    public String createBackupDatabase(String prefix) {
        String dbName = fetchFullDbName(prefix);
        jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS " + dbName);
        return dbName;
    }

    public CompletableFuture<String> UploadCsv(String tableName, String filePath, String targetDatabaseName) {
        try (
                Connection connection =  dbConnectionFactory.getDataSource(targetDatabaseName).getConnection();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(Files.readAllBytes(Paths.get(filePath)));
        ) {
            org.mariadb.jdbc.Connection mariaConnection = connection.unwrap(org.mariadb.jdbc.Connection.class);

            String sql = "LOAD DATA LOCAL INFILE 'dummy1.csv' " +
                    "INTO TABLE " + tableName + " " +
                    "FIELDS TERMINATED BY '\t' " +
                    "LINES TERMINATED BY '\r' " +
                    "IGNORE 1 LINES";

            org.mariadb.jdbc.ClientPreparedStatement stmt = (org.mariadb.jdbc.ClientPreparedStatement) mariaConnection.prepareStatement(sql);
            stmt.setLocalInfileInputStream(inputStream);
            stmt.setString(1, tableName);
            stmt.execute();
            logger.info("BackedUp " + tableName + " Success");

            File file = new File(filePath);
            file.delete();
            logger.info("File deleted " + file.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load CSV from memory", e);
        }
        return CompletableFuture.completedFuture("Success");
    }

    public void closeDataSource(String dbName) {
        dbConnectionFactory.closeDataSource(dbName);
    }

    private String fetchFullDbName(String prefix) {
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm");
        String formattedDate = myDateObj.format(myFormatObj);
        return prefix + "_" + formattedDate;
    }

    public void setupSchema(String fileName, String dbName) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(fileName));
        populator.execute(dbConnectionFactory.getDataSource(dbName));
        logger.info("Schema Created for Database Name -> " + "dbName");
    }

    public void uploadConfig(String targetDbName, TallyProcessConfig config) {
        HashMap<String, String> configMap = new HashMap<>();
        configMap.put("backup_db_name", targetDbName);
        configMap.put("Company Name", config.getCompanyName());
        configMap.put("From Date", config.getFromDate());
        configMap.put("To Date", config.getToDate());
        configMap.put("Request Data unique Key", config.getUniqueKey());
        String sql = "INSERT INTO config (name, value) VALUES (?, ?)";
        try(Connection conn = dbConnectionFactory.getDataSource(targetDbName).getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            Iterator<Map.Entry<String, String>> it = configMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                ps.setString(1, entry.getKey());
                ps.setString(2, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
            } catch (Exception exception){
            throw new RuntimeException("Failed to upload config", exception);
        }
    }
}