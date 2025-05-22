package com.tally_backup.tally_backup.Dto.TallyExportConfigDtos;

import lombok.Data;

import java.util.ArrayList;

@Data
public class Master {
    public String name;
    public String collection;
    public String nature;
    public boolean isChunk;
    public ArrayList<Field> fields;
    public ArrayList<String> fetch;
    public ArrayList<String> filters;
}
