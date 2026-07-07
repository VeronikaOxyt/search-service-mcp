package com.example.searchenginemcp.service;

import com.example.searchenginemcp.dto.Aggregation;
import com.example.searchenginemcp.dto.Filter;
import com.example.searchenginemcp.dto.FilterOperator;
import com.example.searchenginemcp.dto.Metric;
import com.example.searchenginemcp.dto.QueryRequest;
import com.example.searchenginemcp.dto.QueryResult;
import com.example.searchenginemcp.dto.Sort;
import com.example.searchenginemcp.dto.SortDirection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

@Service
public class MockReportQueryService implements ReportQueryService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private static final List<String> FILTERABLE_FIELDS = List.of("orderDate", "region", "status", "customer", "amount");
    private static final Set<String> GROUPABLE_FIELDS = Set.of("region", "status", "customer");
    private static final Set<String> SORTABLE_FIELDS = Set.of("orderDate", "region", "status", "customer", "amount",
            "amount_sum", "amount_avg", "amount_min", "amount_max", "id_count");

    private static final List<Map<String, Object>> ORDERS = List.of(
            order(1, "2026-06-01", "EMEA", "PAID", "Acme Corp", "1200.00"),
            order(2, "2026-06-03", "EMEA", "NEW", "Globex", "800.00"),
            order(3, "2026-06-04", "APAC", "PAID", "Initech", "2100.00"),
            order(4, "2026-06-08", "AMER", "CANCELLED", "Umbrella", "300.00"),
            order(5, "2026-06-10", "AMER", "PAID", "Acme Corp", "1700.00"),
            order(6, "2026-06-15", "APAC", "NEW", "Globex", "950.00"),
            order(7, "2026-06-18", "EMEA", "PAID", "Initech", "2400.00"),
            order(8, "2026-06-21", "AMER", "PAID", "Umbrella", "1250.00"),
            order(9, "2026-07-02", "APAC", "PAID", "Acme Corp", "3100.00"),
            order(10, "2026-07-05", "EMEA", "NEW", "Umbrella", "600.00")
    );

    @Override
    public QueryResult runQuery(QueryRequest request) {
        validate(request);

        int limit = request.limit() == null ? DEFAULT_LIMIT : request.limit();
        List<Map<String, Object>> filteredRows = ORDERS.stream()
                .filter(toPredicate(request.filters()))
                .toList();

        List<Map<String, Object>> resultRows = hasGrouping(request)
                ? aggregate(filteredRows, request.groupBy(), normalizedMetrics(request.metrics()))
                : projectRows(filteredRows);

        List<Map<String, Object>> sortedRows = sortRows(resultRows, request.sort());
        boolean truncated = sortedRows.size() > limit;
        List<Map<String, Object>> limitedRows = sortedRows.stream()
                .limit(limit)
                .toList();

        return new QueryResult(columnsFor(limitedRows, request), limitedRows, limitedRows.size(), truncated);
    }

    private void validate(QueryRequest request) {
        if (!MockDatasetCatalogService.SALES_ORDERS.equals(request.datasetId())) {
            throw new UnknownDatasetException(request.datasetId());
        }
        if (request.limit() != null && request.limit() > MAX_LIMIT) {
            throw new InvalidQueryException("limit must not exceed " + MAX_LIMIT);
        }
        if (request.filters() != null) {
            request.filters().forEach(filter -> {
                if (!FILTERABLE_FIELDS.contains(filter.field())) {
                    throw new InvalidQueryException("Unknown filter field: " + filter.field());
                }
            });
        }
        if (request.groupBy() != null) {
            request.groupBy().forEach(field -> {
                if (!GROUPABLE_FIELDS.contains(field)) {
                    throw new InvalidQueryException("Field is not groupable: " + field);
                }
            });
        }
        if (request.sort() != null) {
            request.sort().forEach(sort -> {
                if (!SORTABLE_FIELDS.contains(sort.field())) {
                    throw new InvalidQueryException("Field is not sortable: " + sort.field());
                }
            });
        }
    }

    private Predicate<Map<String, Object>> toPredicate(List<Filter> filters) {
        if (filters == null || filters.isEmpty()) {
            return row -> true;
        }
        return row -> filters.stream().allMatch(filter -> matches(row, filter));
    }

    private boolean matches(Map<String, Object> row, Filter filter) {
        Object fieldValue = row.get(filter.field());
        return switch (filter.operator()) {
            case EQ -> Objects.equals(normalize(fieldValue), normalize(filter.value()));
            case NE -> !Objects.equals(normalize(fieldValue), normalize(filter.value()));
            case GT -> compare(fieldValue, filter.value()) > 0;
            case GTE -> compare(fieldValue, filter.value()) >= 0;
            case LT -> compare(fieldValue, filter.value()) < 0;
            case LTE -> compare(fieldValue, filter.value()) <= 0;
            case BETWEEN -> {
                List<?> range = requireList(filter.value(), filter.field(), FilterOperator.BETWEEN);
                yield compare(fieldValue, range.get(0)) >= 0 && compare(fieldValue, range.get(1)) <= 0;
            }
            case CONTAINS -> String.valueOf(fieldValue).toLowerCase(Locale.ROOT)
                    .contains(String.valueOf(filter.value()).toLowerCase(Locale.ROOT));
            case IN -> {
                List<?> values = requireList(filter.value(), filter.field(), FilterOperator.IN);
                yield values.stream().map(this::normalize).anyMatch(value -> Objects.equals(value, normalize(fieldValue)));
            }
        };
    }

    private List<?> requireList(Object value, String field, FilterOperator operator) {
        if (value instanceof List<?> values && !values.isEmpty()) {
            return values;
        }
        throw new InvalidQueryException(operator + " filter for " + field + " expects a non-empty array value");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compare(Object left, Object right) {
        if (left instanceof BigDecimal leftDecimal) {
            return leftDecimal.compareTo(new BigDecimal(String.valueOf(right)));
        }
        if (left instanceof LocalDate leftDate) {
            return leftDate.compareTo(LocalDate.parse(String.valueOf(right)));
        }
        if (left instanceof Comparable comparable) {
            return comparable.compareTo(right);
        }
        throw new InvalidQueryException("Field value is not comparable: " + left);
    }

    private Object normalize(Object value) {
        if (value instanceof String stringValue) {
            return stringValue.toLowerCase(Locale.ROOT);
        }
        return value;
    }

    private boolean hasGrouping(QueryRequest request) {
        return request.groupBy() != null && !request.groupBy().isEmpty();
    }

    private List<Metric> normalizedMetrics(List<Metric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return List.of(new Metric("id", Aggregation.COUNT));
        }
        return metrics;
    }

    private List<Map<String, Object>> aggregate(List<Map<String, Object>> rows, List<String> groupBy, List<Metric> metrics) {
        Map<List<Object>, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            List<Object> key = groupBy.stream().map(row::get).toList();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }

        return groups.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    for (int index = 0; index < groupBy.size(); index++) {
                        result.put(groupBy.get(index), entry.getKey().get(index));
                    }
                    metrics.forEach(metric -> result.put(metricColumn(metric), calculate(entry.getValue(), metric)));
                    return result;
                })
                .toList();
    }

    private Object calculate(List<Map<String, Object>> rows, Metric metric) {
        if (metric.aggregation() == Aggregation.COUNT) {
            return rows.size();
        }

        List<BigDecimal> values = rows.stream()
                .map(row -> row.get(metric.field()))
                .map(value -> new BigDecimal(String.valueOf(value)))
                .toList();

        return switch (metric.aggregation()) {
            case SUM -> values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case AVG -> values.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
            case MIN -> values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            case MAX -> values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            case COUNT -> rows.size();
        };
    }

    private String metricColumn(Metric metric) {
        return metric.field() + "_" + metric.aggregation().name().toLowerCase(Locale.ROOT);
    }

    private List<Map<String, Object>> projectRows(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(LinkedHashMap::new)
                .map(row -> {
                    row.remove("id");
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> sortRows(List<Map<String, Object>> rows, List<Sort> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return rows;
        }

        Comparator<Map<String, Object>> comparator = null;
        for (Sort sort : sorts) {
            Comparator<Map<String, Object>> fieldComparator = Comparator.comparing(
                    row -> (Comparable<?>) row.get(sort.field()),
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            if (sort.direction() == SortDirection.DESC) {
                fieldComparator = fieldComparator.reversed();
            }
            comparator = comparator == null ? fieldComparator : comparator.thenComparing(fieldComparator);
        }

        return rows.stream().sorted(comparator).toList();
    }

    private List<String> columnsFor(List<Map<String, Object>> rows, QueryRequest request) {
        if (!rows.isEmpty()) {
            return List.copyOf(rows.getFirst().keySet());
        }
        if (hasGrouping(request)) {
            List<String> columns = new ArrayList<>(request.groupBy());
            normalizedMetrics(request.metrics()).stream().map(this::metricColumn).forEach(columns::add);
            return columns;
        }
        return List.of("orderDate", "region", "status", "customer", "amount");
    }

    private static Map<String, Object> order(int id, String orderDate, String region, String status, String customer,
                                             String amount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("orderDate", LocalDate.parse(orderDate));
        row.put("region", region);
        row.put("status", status);
        row.put("customer", customer);
        row.put("amount", new BigDecimal(amount));
        return row;
    }
}
