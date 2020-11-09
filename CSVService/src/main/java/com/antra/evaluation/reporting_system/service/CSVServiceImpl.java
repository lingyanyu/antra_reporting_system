package com.antra.evaluation.reporting_system.service;

import com.amazonaws.services.s3.AmazonS3;
import com.antra.evaluation.reporting_system.pojo.api.CSVRequest;
import com.antra.evaluation.reporting_system.pojo.report.CSVFile;
import com.antra.evaluation.reporting_system.repo.CSVRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CSVServiceImpl implements CSVService {

    private static final Logger log = LoggerFactory.getLogger(CSVServiceImpl.class);

    private final CSVRepository repository;

    private final CSVGenerator generator;

    private final AmazonS3 s3Client;

    @Value("${s3.bucket}")
    private String s3Bucket;

    public CSVServiceImpl(CSVRepository repository, CSVGenerator generator, AmazonS3 s3Client) {
        this.repository = repository;
        this.generator = generator;
        this.s3Client = s3Client;
    }

    @Override
    public CSVFile createCSV(final CSVRequest request) {
        CSVFile file = new CSVFile();
        file.setId("File-" + UUID.randomUUID().toString());
        file.setSubmitter(request.getSubmitter());
        file.setDescription(request.getDescription());
        file.setGeneratedTime(LocalDateTime.now());

        CSVFile generatedFile= generator.generate(request);

        File generatedCvsFile = new File(generatedFile.getFileLocation());
        log.debug("Upload temp file to s3 {}", generatedFile.getFileLocation());
        s3Client.putObject(s3Bucket,file.getId(),generatedCvsFile);
        log.debug("Uploaded");

        file.setFileLocation(String.join("/",s3Bucket,file.getId()));
        file.setFileSize(generatedFile.getFileSize());
        file.setFileName(generatedFile.getFileName());
        repository.save(file);

        log.debug("clear tem file {}", file.getFileLocation());
        if(generatedCvsFile.delete()){
            log.debug("cleared");
        }

        return file;
    }

}
