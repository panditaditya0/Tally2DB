package com.tally_backup.tally_backup.controller;

import com.tally_backup.tally_backup.Dto.BackupRequestDto;
import com.tally_backup.tally_backup.Dto.ResponseDto;
import com.tally_backup.tally_backup.Dto.TallyProcessConfig;
import com.tally_backup.tally_backup.Schedular.Backup;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class BackupController {
    private final Backup backup;

    public BackupController(Backup backup) {
        this.backup = backup;
    }

    @GetMapping("/backup/default")
    public ResponseEntity<ResponseDto> executeTallyBackup() {
        backup.fullBackup();
        return new ResponseEntity<>(new ResponseDto(), HttpStatus.ACCEPTED);
    }

    @PostMapping(value = "/backup/custom", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDto> uploadFile(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart("backupRequestDto") BackupRequestDto backupRequestDto) {
        try {
            this.validateRequest(backupRequestDto);
            TallyProcessConfig config = backup.prepareBackupConfig(backupRequestDto, file);
            config.setUniqueKey(backupRequestDto.getProcessId());
            backup.startBackup(config);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseDto(ex.getMessage(), false), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new ResponseDto("Success", true), HttpStatus.ACCEPTED);
    }

    private void validateRequest(BackupRequestDto backupRequestDto) {
        if (null == backupRequestDto.companyName || backupRequestDto.companyName.isEmpty()) {
            throw new NullPointerException("Company Name is required");
        }
        if (null == backupRequestDto.fromDate) {
            throw new NullPointerException("From Date is required");
        }
        if (null == backupRequestDto.toDate) {
            throw new NullPointerException("To Date is required");
        }
    }
}