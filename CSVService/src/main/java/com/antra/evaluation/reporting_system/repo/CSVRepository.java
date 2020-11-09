package com.antra.evaluation.reporting_system.repo;

import com.antra.evaluation.reporting_system.pojo.report.CSVFile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CSVRepository extends MongoRepository<CSVFile, String> {
}
