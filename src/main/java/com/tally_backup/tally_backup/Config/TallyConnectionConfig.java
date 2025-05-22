package com.tally_backup.tally_backup.Config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tally")
@Data
public class TallyConnectionConfig {
    public String[] ports;
    private String base_url;
    private String fromdate;
    private String todate;
    private String company;
}