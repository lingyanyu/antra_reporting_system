package com.antra.evaluation.reporting_system.endpoint;

import com.antra.evaluation.reporting_system.pojo.api.CSVRequest;
import com.antra.evaluation.reporting_system.pojo.api.CSVResponse;
import com.antra.evaluation.reporting_system.pojo.api.CSVSNSRequest;
import com.antra.evaluation.reporting_system.pojo.report.CSVFile;
import com.antra.evaluation.reporting_system.service.CSVService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Component;

@Component
public class CSVRequestQueueListener {

    private static final Logger log = LoggerFactory.getLogger(CSVRequestQueueListener.class);

    private final QueueMessagingTemplate queueMessagingTemplate;

    private final CSVService csvService;

    public CSVRequestQueueListener(QueueMessagingTemplate queueMessagingTemplate, CSVService csvService) {
        this.queueMessagingTemplate = queueMessagingTemplate;
        this.csvService = csvService;
    }

    @SqsListener("CSV_Request_Queue")
    public void fanoutQueueListener(CSVSNSRequest request) {
        log.info("Get fanout request: {}", request);
        queueListener(request.getCsvRequest());
    }

    private void queueListener(CSVRequest request) {
        log.info("Get request: {}", request);
        CSVFile file = null;
        CSVResponse response = new CSVResponse();
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

        send(response);
        log.info("Replied back: {}", response);
    }

    private void send(Object message) {
        queueMessagingTemplate.convertAndSend("CSV_Response_Queue", message);
    }
}