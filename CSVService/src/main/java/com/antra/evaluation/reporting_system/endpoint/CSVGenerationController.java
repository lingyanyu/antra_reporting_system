package com.antra.evaluation.reporting_system.endpoint;

import com.antra.evaluation.reporting_system.pojo.api.CSVRequest;
import com.antra.evaluation.reporting_system.pojo.api.CSVResponse;
import com.antra.evaluation.reporting_system.pojo.report.CSVFile;
import com.antra.evaluation.reporting_system.service.CSVService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CSVGenerationController {

    private static final Logger log = LoggerFactory.getLogger(CSVGenerationController.class);

    private CSVService csvService;

    @Autowired
    public CSVGenerationController(CSVService csvService) {
        this.csvService = csvService;
    }


    @PostMapping("/csv")
    public ResponseEntity<CSVResponse> createCSV(@RequestBody @Validated CSVRequest request) {
        log.info("Got request to generate CSV: {}", request);

        CSVResponse response = new CSVResponse();
        CSVFile file = null;
        response.setReqId(request.getReqId());

        try {
            file = csvService.createCSV(request);
            response.setFileId(file.getId());
            response.setFileLocation(file.getFileLocation());
            response.setFileSize(file.getFileSize());
            log.info("Generated: {}", file);
        } catch (Exception e) {
            response.setFailed(true);
            log.error("Error in generating pdf", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
