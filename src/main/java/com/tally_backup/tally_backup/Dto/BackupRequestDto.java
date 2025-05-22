package com.tally_backup.tally_backup.Dto;

import lombok.Data;

@Data
public class BackupRequestDto {
    public String companyName;
    public String fromDate;
    public String toDate;
    public String processId;
}