package com.tally_backup.tally_backup.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbServiceTest {
    @InjectMocks
    private DbService dbService;

    @Mock
    private org.mariadb.jdbc.Connection mockMariaConnection;

    @Mock
    private org.mariadb.jdbc.ClientPreparedStatement mockStatement;

    @Mock
    private DataSource mockDataSource;

    @Mock
    private Connection mockConnection;

    @Mock
    private DbConnectionFactory dbConnectionFactory;

    @TempDir
    Path tempDir;

    @Test
    void uploadCsv_nonExistentFile() {
        assertThrows(RuntimeException.class, () ->
                dbService.UploadCsv("table", "/non/existent/path.csv", "db").join()
        );
    }

    @Test
    void uploadCsv_existentFile() throws Exception {
        String tableName = "test_table";
        Path tempFile = Files.createFile(tempDir.resolve("dummy1.csv"));
        Files.write(tempFile, "col1\tcol2\nval1\tval2".getBytes());

        when(dbConnectionFactory.getDataSource("test_db")).thenReturn(mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.unwrap(org.mariadb.jdbc.Connection.class)).thenReturn(mockMariaConnection);
        when(mockMariaConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        CompletableFuture<String> resultFuture = dbService.UploadCsv(tableName, tempFile.toString(), "test_db");
        assertEquals("Success", resultFuture.get());
        verify(mockStatement).setLocalInfileInputStream(any(InputStream.class));
        verify(mockStatement).setString(eq(1), eq(tableName));
        verify(mockStatement).execute();

        assertFalse(Files.exists(tempFile));
    }

    @Test
    void testSetupSchema() throws Exception {
        String fileName = "schema.sql";
        String dbName = "test_db";
        Path tempFile = Files.createFile(tempDir.resolve(fileName));
        Files.write(tempFile, "".getBytes());

        when(dbConnectionFactory.getDataSource(dbName)).thenReturn(mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        dbService.setupSchema(tempFile.toFile().getAbsolutePath(), dbName);
        verify(dbConnectionFactory).getDataSource(dbName);
    }
}