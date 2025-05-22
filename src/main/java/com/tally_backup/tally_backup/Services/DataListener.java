package com.tally_backup.tally_backup.Services;

import com.tally_backup.tally_backup.Dto.ProcessTableEventDto;
import com.tally_backup.tally_backup.Event.DataFetchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class DataListener {
    static Logger logger = LoggerFactory.getLogger(DataListener.class);

    @Autowired
    private DbService dbService;

    @Autowired
    private DataUtils dataUtils;

    @Autowired
    private RequestDataApiClient requestDataApiClient;

    @EventListener
    @Async("taskExecutorCsvUploader")
    public void onDataFetched(DataFetchedEvent event) {
        ProcessTableEventDto eventData = event.processTableEventDto();
        String cleanedUpData = dataUtils.processTdlOutputManipulation(eventData.getResponseBody());
        String[] dataRows = cleanedUpData.split("\n");
        String fullFilePath = dataUtils.writeCsv(eventData.getTableName(), dataRows);
        dbService.UploadCsv(eventData.getTableName()
                , fullFilePath, eventData.getDatabaseName());
        if (eventData.isLast()) {
//            dbService.closeDataSource(eventData.getDatabaseName());
            requestDataApiClient.notifyClient(eventData.getProcessId(), "Backed up");
        }
    }
}