package com.antra.evaluation.reporting_system.service;

import com.antra.evaluation.reporting_system.pojo.api.CSVRequest;
import com.antra.evaluation.reporting_system.pojo.exception.CSVGenerationException;
import com.antra.evaluation.reporting_system.pojo.report.CSVFile;
import com.google.common.base.Joiner;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CSVGenerator {
    private static final Logger log = LoggerFactory.getLogger(CSVGenerator.class);

    public CSVFile generate(CSVRequest request) {

        try {
            File temp = File.createTempFile(request.getSubmitter(),"_tmp.csv");
            FileOutputStream fos = new FileOutputStream(temp);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(Joiner.on(",").join(request.getHeaders()));
            bw.newLine();
            for (List<String> row : request.getData()) {
                bw.write(Joiner.on(",").join(row));
                bw.newLine();
            }
            bw.close();
            CSVFile generatedFile = new CSVFile();
            generatedFile.setFileLocation(temp.getAbsolutePath());
            generatedFile.setFileName(temp.getName());
            generatedFile.setFileSize(temp.length());
            log.info("Generated CSV file: {}", generatedFile);
            return generatedFile;
        } catch (IOException e) {
            log.error("Error in generating CSV file",e);
            throw new CSVGenerationException();
        }
    }
}
