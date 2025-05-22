package com.tally_backup.tally_backup.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "custom.datasource")
@Data
public class DbConfigProperties {
    private String url;
    private String username;
    private String password;
    private String driverClassName;
}