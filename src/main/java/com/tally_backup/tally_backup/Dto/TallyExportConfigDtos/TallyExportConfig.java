package com.tally_backup.tally_backup.Dto.TallyExportConfigDtos;

import lombok.Data;

import java.util.ArrayList;

@Data
public class TallyExportConfig {
    public ArrayList<Master> master;
    public ArrayList<Transaction> transaction;
}
