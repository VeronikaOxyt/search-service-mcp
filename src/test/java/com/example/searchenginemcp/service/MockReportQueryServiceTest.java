package com.example.searchenginemcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.searchenginemcp.dto.Aggregation;
import com.example.searchenginemcp.dto.Filter;
import com.example.searchenginemcp.dto.FilterOperator;
import com.example.searchenginemcp.dto.Metric;
import com.example.searchenginemcp.dto.QueryRequest;
import com.example.searchenginemcp.dto.QueryResult;
import com.example.searchenginemcp.dto.Sort;
import com.example.searchenginemcp.dto.SortDirection;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockReportQueryServiceTest {

    private final MockReportQueryService service = new MockReportQueryService();

    @Test
    void groupsPaidJuneOrdersByRegion() {
        QueryRequest request = new QueryRequest(
                "sales_orders",
                List.of(
                        new Filter("status", FilterOperator.EQ, "PAID"),
                        new Filter("orderDate", FilterOperator.BETWEEN, List.of("2026-06-01", "2026-06-30"))
                ),
                List.of("region"),
                List.of(new Metric("amount", Aggregation.SUM)),
                List.of(new Sort("amount_sum", SortDirection.DESC)),
                10
        );

        QueryResult result = service.runQuery(request);

        assertThat(result.columns()).containsExactly("region", "amount_sum");
        assertThat(result.rows()).hasSize(3);
        assertThat(result.rows().getFirst())
                .containsEntry("region", "EMEA")
                .containsEntry("amount_sum", new BigDecimal("3600.00"));
    }

    @Test
    void rejectsUnknownDataset() {
        QueryRequest request = new QueryRequest(
                "unknown",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                10
        );

        assertThatThrownBy(() -> service.runQuery(request))
                .isInstanceOf(UnknownDatasetException.class)
                .hasMessageContaining("unknown");
    }
}
