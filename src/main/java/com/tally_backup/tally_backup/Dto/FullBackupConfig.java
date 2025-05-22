package com.tally_backup.tally_backup.Dto;

import lombok.Data;

@Data
public class FullBackupConfig {
    public String companyName;
    public String fromDate;
    public String toDate;
    public String dbNamePrefix;
}