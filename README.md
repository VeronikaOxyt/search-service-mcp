# Search Engine MCP

Demo Spring Boot MCP server for a web application where users build read-only
data queries through UI filters, sorting, grouping, metrics, and limits.

The server intentionally accepts a structured query model instead of raw SQL.
In a real project, the mock services should be replaced with calls to the
existing backend query builder or REST API.

## Tools

- `listDatasets` - lists datasets visible to the user.
- `describeDataset` - returns fields, allowed filters, grouping, sorting, and metrics.
- `getFilterValues` - returns dictionary values for filter controls.
- `runQuery` - executes a read-only structured query.

## Run

```bash
mvn spring-boot:run
```

The app listens on port `8081`.

## Example Query Payload

```json
{
  "datasetId": "sales_orders",
  "filters": [
    {
      "field": "status",
      "operator": "EQ",
      "value": "PAID"
    },
    {
      "field": "orderDate",
      "operator": "BETWEEN",
      "value": ["2026-06-01", "2026-06-30"]
    }
  ],
  "groupBy": ["region"],
  "metrics": [
    {
      "field": "amount",
      "aggregation": "SUM"
    }
  ],
  "sort": [
    {
      "field": "amount_sum",
      "direction": "DESC"
    }
  ],
  "limit": 10
}
```

## Production Shape

Replace `MockDatasetCatalogService` and `MockReportQueryService` with adapters to
your real Spring Boot backend:

- reuse existing authorization and dataset permissions;
- expose only approved datasets and fields;
- keep query execution read-only;
- enforce server-side limits;
- audit every tool call.
