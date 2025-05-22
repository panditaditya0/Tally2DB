package com.tally_backup.tally_backup.Dto;

import com.tally_backup.tally_backup.Dto.TallyExportConfigDtos.Master;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataToBeFetchedDto {
    private Master master;
    private String processId;
    private HashMap<String, String> reqDataConfig;
}