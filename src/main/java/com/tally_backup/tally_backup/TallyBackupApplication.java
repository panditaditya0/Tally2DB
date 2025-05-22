package com.tally_backup.tally_backup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TallyBackupApplication {

	public static void main(String[] args) {
		SpringApplication.run(TallyBackupApplication.class, args);
	}

}
