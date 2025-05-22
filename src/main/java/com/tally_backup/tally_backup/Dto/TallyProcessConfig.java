package com.tally_backup.tally_backup.Dto;

import com.tally_backup.tally_backup.Dto.TallyExportConfigDtos.Master;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TallyProcessConfig {
    public String fromDate;
    public String toDate;
    public String companyName;
    public String uniqueKey;
    public List<Master> masters;
    public String dbPrefix;
}