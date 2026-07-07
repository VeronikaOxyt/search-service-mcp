package com.example.searchenginemcp.service;

import com.example.searchenginemcp.dto.QueryRequest;
import com.example.searchenginemcp.dto.QueryResult;

public interface ReportQueryService {

    QueryResult runQuery(QueryRequest request);
}
