# Saved query templates through MCP

## Design

Templates remain backend entities. The MCP server exposes a fixed set of tools
that discover and execute them:

```text
listSavedQueryTemplates
getQueryTemplateParameters
executeQueryTemplate
getQueryResult
```

Creating a template does not change the MCP tool list and does not require an MCP
server restart.

## Backend contract assumed by the prototype

### List templates

```http
POST /mid/template/list
```

```json
{
  "sort": [
    {
      "column": "srcTable",
      "desc": false
    }
  ],
  "sortCount": false,
  "isPersonal": false,
  "limit": 20,
  "offset": 0
}
```

The MCP result intentionally drops backend `status` and `timestamp` metadata.

### Load a template and build its execution schema

```http
GET /mid/template?id={templateId}
```

The backend returns `template` and `metaInfo`. The MCP server keeps the raw
template as JSON so unknown backend fields survive the fetch-modify-submit
round-trip. It recursively traverses `template.where.filters` and exposes only
empty, unlocked leaf filters whose operators require a value.

The MCP tool result is generated locally:

```json
{
  "templateId": "92de4773-7a00-4000-8000-000000000000",
  "name": "User events",
  "description": null,
  "queryType": "QUERY",
  "parameters": [
    {
      "key": "where.filters[0]",
      "label": "Username",
      "description": "Enter a value for column Username and operator Equals",
      "dataType": "STRING",
      "format": null,
      "multiple": true,
      "required": true,
      "defaultValue": null
    }
  ]
}
```

Every exposed parameter is required. Filled filters, locked filters, and
value-less operators such as `is N/A` are not exposed. Values are always arrays
of strings because the backend model uses `List<String> value`.

### Start execution

The LLM calls `executeQueryTemplate` with MCP arguments:

```json
{
  "templateId": "92de4773-7a00-4000-8000-000000000000",
  "parameters": {
    "where.filters[0]": ["ivanov"]
  }
}
```

The MCP server fetches a fresh raw template, recursively finds the same filter
paths, validates that every required path has a non-blank string array, writes
the arrays into `value`, and adds:

```json
{
  "name": "Запрос по шаблону: User events",
  "templateId": "92de4773-7a00-4000-8000-000000000000",
  "rqUid": "faef348e-f8fd-47bf-9b99-8e35f35bfe5e"
}
```

For `queryType: QUERY`, it submits the filled template to:

```http
POST /mid/query/executeQuery
```

Before submission it reads `sourceName`, `table.schema`, and `table.tableName`
from the template and loads the table structure:

```http
GET /mid/query/topology/structureTable
  ?schemaName=log_armatm_src_distr
  &tableName=parsed
  &sourceName=datastore_clickhouse
```

It selects each structure column with `isBaseColumn: true`, takes its
`columnName`, and adds the resulting string array:

```json
{
  "baseColumns": [
    "SourceHostname",
    "SourceIP",
    "Username"
  ]
}
```

For `queryType: CROSS`, it submits it to:

```http
POST /mid/query/executeCrossQuery
```

Existing raw fields such as `sourceName`, `table`, `schemaName`, `select`,
`where`, `group`, `sort`, and `limit` are preserved.

The structure lookup and `baseColumns` field are used only for a regular query.
A cross query does not make this extra request.

Before either execution endpoint is called, `TimeRangeUI` is converted to the
backend `TimeRange` shape:

- `range` keeps the saved `min` and `max`;
- `mins` sets `max` to the current time and subtracts `value` minutes for `min`;
- `hours` subtracts `value` hours;
- `days` subtracts `value` days.

The outgoing format is `yyyy-MM-dd HH:mm:ss`; UI-only fields `type` and `value`
are removed. The timezone is configured by `search-service.time-zone` and
defaults to `Europe/Moscow`.

```json
{
  "resultId": "06a02258-736f-4ad6-a2e0-4259c66c8fe6",
  "templateId": "92de4773-7a00-4000-8000-000000000000",
  "queryType": "QUERY",
  "status": "QUEUED",
  "message": null
}
```

The MCP server checks required and unknown keys and never overwrites filled or
locked filters. This MVP intentionally has no template version, hash, or
concurrent-change check. The backend remains the authoritative validator for
permissions and query semantics. The MCP server derives `queryType` from
`metaInfo.isCross` rather than keeping it in process memory.

### Check readiness and fetch a bounded result page

For `queryType: QUERY`:

```http
POST /mid/query/result
```

For `queryType: CROSS`:

```http
POST /mid/query/crossResult
```

Both endpoints receive the same body:

```json
{
  "limit": 20,
  "offset": 0,
  "resultId": "06a02258-736f-4ad6-a2e0-4259c66c8fe6"
}
```

The MCP response for HTTP 425 or 426 has `state: PENDING` and contains available
progress metadata. The response for HTTP 200 has `state: READY`:

```json
{
  "resultId": "06a02258-736f-4ad6-a2e0-4259c66c8fe6",
  "queryType": "QUERY",
  "state": "READY",
  "backendStatus": 200,
  "metaInfo": {
    "status": "Success",
    "count": 1,
    "columns": [{"name": "Username", "type": "STRING"}]
  },
  "rows": [
    {
      "Username": "ivanov"
    }
  ],
  "offset": 0,
  "returnedRows": 1,
  "truncated": false
}
```

The same MCP tool performs polling and result retrieval. It limits a single
request to 100 rows so large tables are not inserted into the LLM context in one
response.
