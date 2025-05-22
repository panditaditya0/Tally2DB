package com.tally_backup.tally_backup.Event;

import com.tally_backup.tally_backup.Dto.ProcessTableEventDto;
import org.springframework.context.ApplicationEvent;

public class DataFetchedEvent extends ApplicationEvent {
    private final ProcessTableEventDto processTableEventDto;
    public DataFetchedEvent(Object source, ProcessTableEventDto processTableEventDto) {
        super(source);
        this.processTableEventDto = processTableEventDto;
    }

    public ProcessTableEventDto processTableEventDto() {
        return processTableEventDto;
    }
}
