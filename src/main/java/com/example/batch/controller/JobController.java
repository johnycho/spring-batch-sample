package com.example.batch.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobLauncher jobLauncher;
    private final Map<String, Job> jobs;

    @PostMapping("/jdbc-cursor")
    public String runJdbcCursorJob() throws Exception {
        Job job = jobs.get("jdbcCursorJob");
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(job, jobParameters);
        return "JdbcCursorJob 실행 완료";
    }

    @PostMapping("/jdbc-paging")
    public String runJdbcPagingJob() throws Exception {
        Job job = jobs.get("jdbcPagingJob");
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(job, jobParameters);
        return "JdbcPagingJob 실행 완료";
    }

    @PostMapping("/flat-file")
    public String runFlatFileJob() throws Exception {
        Job job = jobs.get("flatFileJob");
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(job, jobParameters);
        return "FlatFileJob 실행 완료";
    }

    @PostMapping("/json")
    public String runJsonJob() throws Exception {
        Job job = jobs.get("jsonJob");
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(job, jobParameters);
        return "JsonJob 실행 완료";
    }

    @PostMapping("/list-item")
    public String runListItemJob() throws Exception {
        Job job = jobs.get("listItemJob");
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(job, jobParameters);
        return "ListItemJob 실행 완료";
    }

    @PostMapping("/repository-item")
    public String runRepositoryItemJob() throws Exception {
        Job job = jobs.get("repositoryItemJob");
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(job, jobParameters);
        return "RepositoryItemJob 실행 완료";
    }
}

