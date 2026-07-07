package com.example.searchenginemcp.service;

import com.example.searchenginemcp.dto.DatasetDescription;
import com.example.searchenginemcp.dto.DatasetSummary;
import com.example.searchenginemcp.dto.FilterValue;
import java.util.List;

public interface DatasetCatalogService {

    List<DatasetSummary> listDatasets();

    DatasetDescription describeDataset(String datasetId);

    List<FilterValue> getFilterValues(String datasetId, String field, String search);
}
