# antra_reporting_system
in each git commit I added the below functionalites sequentially
1)ClientService--use threadpool and completeableFuture to parallely call PDFService and ExcelService to generate pdf and xls file.
               --change downloading pdf file and xls file from s3 bucket
  ExcelService -- save excel to S3 bucket instead of local
2)Add DiscoverService to the project
3)Add ConfigService to the project
4)Add Dockerfile to each service
5)Add CSVService to the project
6)Integrate CSVService with ClientService
           
