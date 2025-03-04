package com.example.samplebatch.controller;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FirstController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    @SneakyThrows
    @GetMapping("/first")
    public String firstApi(@RequestParam("value") String value) {
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("date", value)
            .toJobParameters();

        jobLauncher.run(jobRegistry.getJob("firstJob"), jobParameters);

        return "ok";
    }
}
