package com.antra.evaluation.reporting_system.service;

import com.antra.evaluation.reporting_system.pojo.api.CSVRequest;
import com.antra.evaluation.reporting_system.pojo.report.CSVFile;

public interface CSVService {
    CSVFile createCSV(CSVRequest request);
}
