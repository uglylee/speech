package com.baidu.speechmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@SpringBootApplication
public class SpeechMonitorApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(SpeechMonitorApplication.class, args);
    }

}
