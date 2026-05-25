package com.flossk.tts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TtsApplication {

	public static void main(String[] args) {
		SpringApplication.run(TtsApplication.class, args);
	}

}
