package com.antra.evaluation.reporting_system.pojo.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

public class CSVSNSRequest {
    @JsonProperty("Message")
    @JsonDeserialize(using = SNSMessageDeserializer.class)
    CSVRequest CSVRequest;

    public CSVRequest getCsvRequest() {
        return CSVRequest;
    }

    public void setCsvRequest(CSVRequest CSVRequest) {
        this.CSVRequest = CSVRequest;
    }

    @Override
    public String toString() {
        return "CSVSNSRequest{" +
                "csvRequest=" + CSVRequest +
                '}';
    }

}
class SNSMessageDeserializer extends JsonDeserializer<CSVRequest> {
    @Override
    public CSVRequest deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        String text = p.getText();
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        return mapper.readValue(text, CSVRequest.class);
    }
}
