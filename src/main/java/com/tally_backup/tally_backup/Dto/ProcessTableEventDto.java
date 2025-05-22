package com.tally_backup.tally_backup.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessTableEventDto {
    private String tableName;
    private String responseBody;
    private String databaseName;
    private String processId;
    private boolean isLast;
}