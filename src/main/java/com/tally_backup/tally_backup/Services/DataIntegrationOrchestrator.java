package com.tally_backup.tally_backup.Services;

import com.tally_backup.tally_backup.Dto.DataToBeFetchedDto;
import com.tally_backup.tally_backup.Dto.ProcessTableEventDto;
import com.tally_backup.tally_backup.Event.DataFetchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
public class DataIntegrationOrchestrator {
    static Logger logger = LoggerFactory.getLogger(DataIntegrationOrchestrator.class);

    private final TallyApiClient tallyApiClient;
    private final DataUtils dataUtils;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public DataIntegrationOrchestrator(TallyApiClient tallyApiClient
            , DataUtils dataUtils
            , ApplicationEventPublisher eventPublisher) {
        this.tallyApiClient = tallyApiClient;
        this.dataUtils = dataUtils;
        this.eventPublisher = eventPublisher;
    }

    @Async("taskExecutorTallyApi")
    public CompletableFuture<Void> Process(DataToBeFetchedDto dataToBeFetchedDto, String targetDatabaseName, boolean isLastChunk) {
        short retriesCount = 0;
        do {
            String targetTable = dataToBeFetchedDto.getMaster().name;
            try {
                logger.info("Fetching {} from Tally", targetTable);
                StringBuilder xmlRequest = dataUtils.generateXMLfromYAML(dataToBeFetchedDto.getMaster(), dataToBeFetchedDto.getReqDataConfig());
                String responseBody = tallyApiClient.execute(xmlRequest.toString());
                eventPublisher.publishEvent(new DataFetchedEvent(this, new ProcessTableEventDto(
                        targetTable,
                        responseBody,
                        targetDatabaseName,
                        dataToBeFetchedDto.getProcessId(),
                        isLastChunk
                        )));
                return CompletableFuture.completedFuture(null);
            } catch (Exception ex) {
                logger.error("Error in table Backup  " + targetTable + " " + ex.getMessage().toString());
            }
        } while(retriesCount++ < 3);
        logger.error("Tried 3 times, but cannot process {}", dataToBeFetchedDto.getMaster().name);
        return CompletableFuture.completedFuture(null);
    }
}