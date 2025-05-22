package com.tally_backup.tally_backup.Schedular;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tally_backup.tally_backup.Dto.*;
import com.tally_backup.tally_backup.Dto.TallyExportConfigDtos.Master;
import com.tally_backup.tally_backup.Dto.TallyExportConfigDtos.TallyExportConfig;
import com.tally_backup.tally_backup.Services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class Backup {
    static Logger logger = LoggerFactory.getLogger(Backup.class);

    private final DataIntegrationOrchestrator dataIntegrationOrchestrator;
    private final TallyProcessConfig tallyDefaultConfig;
    private final DbService dbService;
    private final DateTimeFormatter xmlFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private final DateTimeFormatter configFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private GChatApiClient gChatApiClient;


    @Autowired
    public Backup(DataIntegrationOrchestrator dataIntegrationOrchestrator
            , TallyProcessConfig tallyDefaultConfig
            , DbService processCsv) {
        this.tallyDefaultConfig = tallyDefaultConfig;
        this.dbService = processCsv;
        this.dataIntegrationOrchestrator = dataIntegrationOrchestrator;
    }

    @Value("${sql_schema_path}")
    private String SQL_SCHEMA_PATH;

    @Scheduled(cron = "0 0 4 * * *")
    public void fullBackup() {
        List<FullBackupConfig> listOfBackups = dbService.fetchAllCronConfig();
        for(FullBackupConfig backup : listOfBackups){
            long startTime = System.nanoTime();
            this.startBackup(new TallyProcessConfig(backup.getFromDate()
            , backup.getToDate()
            , backup.getCompanyName()
            , "NA"
            ,tallyDefaultConfig.masters
            , backup.getDbNamePrefix()));
            long stopTime = System.nanoTime();
            long durationNano = stopTime - startTime;
            double durationMinutes = durationNano / 1_000_000_000.0 / 60.0;
            DecimalFormat df = new DecimalFormat("#.##");
            String formattedDuration = df.format(durationMinutes);
            gChatApiClient.sendToGoogleChat("COMPLETED BACKUP FOR  " + backup.getCompanyName() + " IN " + formattedDuration + " mins");
            logger.info("COMPLETED BACKUP FOR  {} IN {} mins",backup.getCompanyName(), formattedDuration );
        }
    }

    public ResponseDto startBackup(TallyProcessConfig config) {
        List<Master> masters = config.getMasters();
        String targetDatabaseName = dbService.createBackupDatabase(config.getDbPrefix());
        try {
            logger.info("Starting backup...");
            logger.info("CompanyName name: " + config.getCompanyName());
            logger.info("From Date " + config.getFromDate());
            logger.info("To Date " + config.getToDate());
            logger.info("Target Database name: " + targetDatabaseName);
            dbService.setupSchema(SQL_SCHEMA_PATH, targetDatabaseName);
            dbService.uploadConfig(targetDatabaseName, config);
            List<DataToBeFetchedDto> listOfDataToBeFetched = prepareRequiredData(masters, config);
            AtomicInteger i = new AtomicInteger(0);
            AtomicInteger size = new AtomicInteger(listOfDataToBeFetched.size() -1);
            List<CompletableFuture<Void>> listOfaa = listOfDataToBeFetched
                    .stream()
                    .map(dto -> {
                        boolean isLast = i.get() == size.get();
                        CompletableFuture<Void> a = dataIntegrationOrchestrator.Process(dto, targetDatabaseName, isLast);
                        i.getAndIncrement();
                        return a;
                    }).toList();
            listOfaa.forEach(CompletableFuture::join);
        } catch (Exception ex) {
            return new ResponseDto(ex.getMessage(), false);
        }
        return new ResponseDto(null, true);
    }

    public TallyProcessConfig prepareBackupConfig(BackupRequestDto req, MultipartFile file) throws Exception {
        String companyName = req.getCompanyName().trim();
        List<Master> tables;
        if (null == file) {
            tables = tallyDefaultConfig.getMasters();
            logger.info("Using local config no config file provided");
        } else {
            TallyExportConfig conf = mapper.readValue(StreamUtils.copyToString(file.getInputStream(), StandardCharsets.UTF_8), TallyExportConfig.class);
            conf.master.addAll(conf.transaction);
            conf.transaction = new ArrayList<>();
            tables = conf.master;
        }
        return new TallyProcessConfig(req.getFromDate(), req.getToDate(), companyName, req.getProcessId(), tables, tallyDefaultConfig.dbPrefix);
    }

    private List<DataToBeFetchedDto> prepareRequiredData(List<Master> masters, TallyProcessConfig tallyProcessConfig) {
        List<DataToBeFetchedDto> dataToBeFetchedDtoList = new ArrayList<>();
        for (Master master : masters) {
            LocalDate startDate = LocalDate.parse(tallyProcessConfig.getFromDate(), configFormatter);
            LocalDate endDate = LocalDate.parse(tallyProcessConfig.getToDate(), configFormatter);
            if (master.isChunk) {
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(10)) {
                    HashMap<String, String> configTallyXML = new HashMap<>();
                    configTallyXML.put("targetCompany",tallyProcessConfig.getCompanyName());
                    configTallyXML.put("fromDate",xmlFormatter.format(date));
                    configTallyXML.put("toDate",xmlFormatter.format(date.plusDays(9)));
                    dataToBeFetchedDtoList.add(new DataToBeFetchedDto(master, tallyProcessConfig.getUniqueKey(), configTallyXML));
                }
                continue;
            }
            HashMap<String, String> configTallyXML = new HashMap<>();
            configTallyXML.put("targetCompany",tallyProcessConfig.getCompanyName());
            configTallyXML.put("fromDate", xmlFormatter.format(startDate));
            configTallyXML.put("toDate",xmlFormatter.format(endDate));
            dataToBeFetchedDtoList.add(new DataToBeFetchedDto(master,tallyProcessConfig.getUniqueKey(),  configTallyXML));
        }
        logger.info("Prepared {} data for backup ", dataToBeFetchedDtoList.size());
        return dataToBeFetchedDtoList;
    }

    public TallyProcessConfig getDefaultConfig() {
        return tallyDefaultConfig;
    }
}