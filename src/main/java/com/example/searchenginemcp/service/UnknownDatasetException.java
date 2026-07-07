package com.example.searchenginemcp.service;

public class UnknownDatasetException extends RuntimeException {

    public UnknownDatasetException(String datasetId) {
        super("Unknown dataset: " + datasetId);
    }
}
