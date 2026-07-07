package com.example.searchenginemcp.service;

import com.example.searchenginemcp.dto.Aggregation;
import com.example.searchenginemcp.dto.DatasetDescription;
import com.example.searchenginemcp.dto.DatasetSummary;
import com.example.searchenginemcp.dto.FieldDescription;
import com.example.searchenginemcp.dto.FieldType;
import com.example.searchenginemcp.dto.FilterOperator;
import com.example.searchenginemcp.dto.FilterValue;
import com.example.searchenginemcp.dto.MetricDescription;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class MockDatasetCatalogService implements DatasetCatalogService {

    public static final String SALES_ORDERS = "sales_orders";

    private static final DatasetDescription SALES_ORDERS_DESCRIPTION = new DatasetDescription(
            SALES_ORDERS,
            "Sales orders",
            "Demo dataset with sales orders, regions, statuses, customers, dates, and amounts.",
            List.of(
                    new FieldDescription("orderDate", FieldType.DATE, "Order date", true, false, true,
                            List.of(FilterOperator.BETWEEN, FilterOperator.GTE, FilterOperator.LTE)),
                    new FieldDescription("region", FieldType.STRING, "Customer region", true, true, true,
                            List.of(FilterOperator.EQ, FilterOperator.IN, FilterOperator.CONTAINS)),
                    new FieldDescription("status", FieldType.STRING, "Order status", true, true, true,
                            List.of(FilterOperator.EQ, FilterOperator.IN)),
                    new FieldDescription("customer", FieldType.STRING, "Customer name", true, true, true,
                            List.of(FilterOperator.EQ, FilterOperator.CONTAINS)),
                    new FieldDescription("amount", FieldType.NUMBER, "Order amount", true, false, true,
                            List.of(FilterOperator.GT, FilterOperator.GTE, FilterOperator.LT, FilterOperator.LTE))
            ),
            List.of("region", "status", "customer"),
            List.of("orderDate", "region", "status", "customer", "amount"),
            List.of(
                    new MetricDescription("amount", "Order amount", List.of(Aggregation.SUM, Aggregation.AVG,
                            Aggregation.MIN, Aggregation.MAX)),
                    new MetricDescription("id", "Order count", List.of(Aggregation.COUNT))
            )
    );

    @Override
    public List<DatasetSummary> listDatasets() {
        return List.of(new DatasetSummary(
                SALES_ORDERS_DESCRIPTION.id(),
                SALES_ORDERS_DESCRIPTION.name(),
                SALES_ORDERS_DESCRIPTION.description()
        ));
    }

    @Override
    public DatasetDescription describeDataset(String datasetId) {
        if (!SALES_ORDERS.equals(datasetId)) {
            throw new UnknownDatasetException(datasetId);
        }
        return SALES_ORDERS_DESCRIPTION;
    }

    @Override
    public List<FilterValue> getFilterValues(String datasetId, String field, String search) {
        describeDataset(datasetId);
        List<FilterValue> values = switch (field) {
            case "region" -> List.of(
                    new FilterValue("EMEA", "EMEA"),
                    new FilterValue("APAC", "APAC"),
                    new FilterValue("AMER", "AMER")
            );
            case "status" -> List.of(
                    new FilterValue("NEW", "New"),
                    new FilterValue("PAID", "Paid"),
                    new FilterValue("CANCELLED", "Cancelled")
            );
            case "customer" -> List.of(
                    new FilterValue("Acme Corp", "Acme Corp"),
                    new FilterValue("Globex", "Globex"),
                    new FilterValue("Initech", "Initech"),
                    new FilterValue("Umbrella", "Umbrella")
            );
            default -> throw new InvalidQueryException("Field is not backed by dictionary values: " + field);
        };

        if (search == null || search.isBlank()) {
            return values;
        }

        String normalizedSearch = search.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.label().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                        || value.value().toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .toList();
    }
}
