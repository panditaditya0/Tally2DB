package com.tally_backup.tally_backup.Config;

import com.tally_backup.tally_backup.Dto.TallyExportConfigDtos.TallyExportConfig;
import com.tally_backup.tally_backup.Dto.TallyProcessConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Configuration
public class TallyConfig {

    @Value("classpath:tally-export-config.json")
    private Resource myFile;

    @Value("${backup_db_name}")
    public String dbNamePrefix;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public TallyProcessConfig fetchTallyDefaultConfig(ObjectMapper objectMapper, TallyConnectionConfig tallyInfoFromBack){
        try{
            TallyExportConfig conf = objectMapper.readValue(StreamUtils.copyToString(myFile.getInputStream(), StandardCharsets.UTF_8), TallyExportConfig.class);
            conf.master.addAll(conf.transaction);
            conf.transaction = new ArrayList<>();
            TallyProcessConfig config = new TallyProcessConfig();
            config.setCompanyName(tallyInfoFromBack.getCompany());
            config.setFromDate(tallyInfoFromBack.getFromdate());
            config.setToDate(tallyInfoFromBack.getTodate());
            config.setMasters(conf.master);
            config.setDbPrefix(dbNamePrefix);
            return config;
        } catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }
}