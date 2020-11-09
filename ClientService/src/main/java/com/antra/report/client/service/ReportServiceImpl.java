package com.antra.report.client.service;

import com.amazonaws.services.s3.AmazonS3;
import com.antra.report.client.entity.*;
import com.antra.report.client.exception.RequestNotFoundException;
import com.antra.report.client.pojo.FileType;
import com.antra.report.client.pojo.reponse.*;
import com.antra.report.client.pojo.request.ReportRequest;
import com.antra.report.client.repository.ReportRequestRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final ReportRequestRepo reportRequestRepo;
    private final SNSService snsService;
    private final AmazonS3 s3Client;
    private final ThreadPoolTaskExecutor es;
    private final RestTemplate rs;

    public ReportServiceImpl(ReportRequestRepo reportRequestRepo, SNSService snsService, AmazonS3 s3Client, ThreadPoolTaskExecutor es, RestTemplate rs) {
        this.reportRequestRepo = reportRequestRepo;
        this.snsService = snsService;
        this.s3Client = s3Client;
        this.es = es;
        this.rs = rs;
    }

    private ReportRequestEntity persistToLocal(ReportRequest request) {
        request.setReqId("Req-"+ UUID.randomUUID().toString());

        ReportRequestEntity entity = new ReportRequestEntity();
        entity.setReqId(request.getReqId());
        entity.setSubmitter(request.getSubmitter());
        entity.setDescription(request.getDescription());
        entity.setCreatedTime(LocalDateTime.now());

        PDFReportEntity pdfReport = new PDFReportEntity();
        pdfReport.setRequest(entity);
        pdfReport.setStatus(ReportStatus.PENDING);
        pdfReport.setCreatedTime(LocalDateTime.now());
        entity.setPdfReport(pdfReport);

        ExcelReportEntity excelReport = new ExcelReportEntity();
        BeanUtils.copyProperties(pdfReport, excelReport);
        entity.setExcelReport(excelReport);

        CSVReportEntity csvReport = new CSVReportEntity();
        BeanUtils.copyProperties(pdfReport, csvReport);
        entity.setCsvReport(csvReport);

        return reportRequestRepo.save(entity);
    }

    @Override
    public ReportVO generateReportsSync(ReportRequest request) {
        persistToLocal(request);
        sendDirectRequests(request);
        return new ReportVO(reportRequestRepo.findById(request.getReqId()).orElseThrow());
    }
    //TODO:Change to parallel process using Threadpool? CompletableFuture?
    private void sendDirectRequests(ReportRequest request) {
        ExcelResponse excelResponse = new ExcelResponse();
        PDFResponse pdfResponse = new PDFResponse();
        CSVResponse csvResponse = new CSVResponse();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ReportRequest> httpEntity = new HttpEntity<>(request, headers);
        CompletableFuture<ExcelResponse> excelResponseCompletableFuture = CompletableFuture.supplyAsync(
                ()->rs.postForEntity("http://ExcelService/excel", httpEntity, ExcelResponse.class).getBody(),es
        );
        CompletableFuture<PDFResponse> pdfResponseCompletableFuture = CompletableFuture.supplyAsync(
                ()->rs.postForEntity("http://PDFService/pdf", httpEntity, PDFResponse.class).getBody(),es
        );

        CompletableFuture<CSVResponse> csvResponseCompletableFuture = CompletableFuture.supplyAsync(
                ()->rs.postForEntity("http://CSVService/csv", httpEntity, CSVResponse.class).getBody(),es
        );

        try {
            excelResponse = excelResponseCompletableFuture.get();
//            excelResponse = rs.postForEntity("http://localhost:8888/excel", request, ExcelResponse.class).getBody();
        } catch(Exception e){
            log.error("Excel Generation Error (Sync) : e", e);
            excelResponse.setReqId(request.getReqId());
            excelResponse.setFailed(true);
        } finally {
            updateLocal(excelResponse);
        }
        try {
            pdfResponse = pdfResponseCompletableFuture.get();
//            pdfResponse = rs.postForEntity("http://localhost:9999/pdf", request, PDFResponse.class).getBody();
        } catch(Exception e){
            log.error("PDF Generation Error (Sync) : e", e);
            pdfResponse.setReqId(request.getReqId());
            pdfResponse.setFailed(true);
        } finally {
            updateLocal(pdfResponse);
        }

        try{
            csvResponse = csvResponseCompletableFuture.get();
        }
        catch (Exception e){
            log.error("CSV Generation Error (Sync) : e", e);
            csvResponse.setFailed(true);
            csvResponse.setReqId(request.getReqId());
        }
        finally {
            updateLocal(csvResponse);
        }
    }

    private void updateLocal(ExcelResponse excelResponse) {
        SqsResponse response = new SqsResponse();
        BeanUtils.copyProperties(excelResponse, response);
        updateAsyncExcelReport(response);
    }
    private void updateLocal(PDFResponse pdfResponse) {
        SqsResponse response = new SqsResponse();
        BeanUtils.copyProperties(pdfResponse, response);
        updateAsyncPDFReport(response);
    }

    private void updateLocal(CSVResponse csvResponse) {
        SqsResponse response = new SqsResponse();
        BeanUtils.copyProperties(csvResponse, response);
        updateAsyncPDFReport(response);
    }

    @Override
    @Transactional
    public ReportVO generateReportsAsync(ReportRequest request) {
        ReportRequestEntity entity = persistToLocal(request);
        snsService.sendReportNotification(request);
        log.info("Send SNS the message: {}",request);
        return new ReportVO(entity);
    }

    @Override
    @Transactional
    public void updateAsyncPDFReport(SqsResponse response) {
        ReportRequestEntity entity = reportRequestRepo.findById(response.getReqId()).orElseThrow(RequestNotFoundException::new);
        var pdfReport = entity.getPdfReport();
        pdfReport.setUpdatedTime(LocalDateTime.now());
        if (response.isFailed()) {
            pdfReport.setStatus(ReportStatus.FAILED);
        } else{
            pdfReport.setStatus(ReportStatus.COMPLETED);
            pdfReport.setFileId(response.getFileId());
            pdfReport.setFileLocation(response.getFileLocation());
            pdfReport.setFileSize(response.getFileSize());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        reportRequestRepo.save(entity);
    }

    @Override
    @Transactional
    public void updateAsyncExcelReport(SqsResponse response) {
        ReportRequestEntity entity = reportRequestRepo.findById(response.getReqId()).orElseThrow(RequestNotFoundException::new);
        var excelReport = entity.getExcelReport();
        excelReport.setUpdatedTime(LocalDateTime.now());
        if (response.isFailed()) {
            excelReport.setStatus(ReportStatus.FAILED);
        } else{
            excelReport.setStatus(ReportStatus.COMPLETED);
            excelReport.setFileId(response.getFileId());
            excelReport.setFileLocation(response.getFileLocation());
            excelReport.setFileSize(response.getFileSize());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        reportRequestRepo.save(entity);
    }

    @Override
    @Transactional
    public void updateAsyncCSVReport(SqsResponse response) {
        ReportRequestEntity entity = reportRequestRepo.findById(response.getReqId()).orElseThrow(RequestNotFoundException::new);
        var csvReport = entity.getCsvReport();
        csvReport.setUpdatedTime(LocalDateTime.now());
        if (response.isFailed()) {
            csvReport.setStatus(ReportStatus.FAILED);
        } else{
            csvReport.setStatus(ReportStatus.COMPLETED);
            csvReport.setFileId(response.getFileId());
            csvReport.setFileLocation(response.getFileLocation());
            csvReport.setFileSize(response.getFileSize());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        reportRequestRepo.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportVO> getReportList() {
        return reportRequestRepo.findAll().stream().map(ReportVO::new).collect(Collectors.toList());
    }

    @Override
    public InputStream getFileBodyByReqId(String reqId, FileType type) {
        ReportRequestEntity entity = reportRequestRepo.findById(reqId).orElseThrow(RequestNotFoundException::new);
        String fileLocation = "";
        if (type == FileType.PDF) {
            fileLocation = entity.getPdfReport().getFileLocation(); // this location is s3 "bucket/key"

        } else if (type == FileType.EXCEL) {
            fileLocation = entity.getExcelReport().getFileLocation();
//            String fileLocation = entity.getExcelReport().getFileLocation();
//            try {
//                return new FileInputStream(fileLocation);// this location is in local, definitely sucks
//            } catch (FileNotFoundException e) {
//                log.error("No file found", e);
//            }
//            RestTemplate restTemplate = new RestTemplate();
//            InputStream is = restTemplate.execute(, HttpMethod.GET, null, ClientHttpResponse::getBody, fileId);
//            ResponseEntity<Resource> exchange = restTemplate.exchange("http://localhost:8888/excel/{id}/content",
//                    HttpMethod.GET, null, Resource.class, fileId);
//            try {
//                return exchange.getBody().getInputStream();
//            } catch (IOException e) {
//                log.error("Cannot download excel",e);
//            }
        }
        else if(type == FileType.CSV){
            fileLocation = entity.getCsvReport().getFileLocation();
        }
        String bucket = fileLocation.split("/")[0];
        String key = fileLocation.split("/")[1];
        return s3Client.getObject(bucket, key).getObjectContent();
    }
}
