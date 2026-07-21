# Saved query templates through MCP

## Design

Templates remain backend entities. The MCP server exposes a fixed set of tools
that discover and execute them:

```text
listQueryTemplates
getQueryTemplateParameters
executeQueryTemplate
getQueryStatus
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

### Get an execution schema

```http
GET /mid/template/{templateId}/execution-schema
```

```json
{
  "templateId": "92de4773-7a00-4000-8000-000000000000",
  "version": 3,
  "name": "User events",
  "description": "Find events for a user",
  "parameters": [
    {
      "key": "username",
      "label": "User name",
      "description": "User whose events should be found",
      "dataType": "STRING",
      "format": null,
      "multiple": true,
      "required": true,
      "defaultValue": null
    },
    {
      "key": "dateFrom",
      "label": "Start date",
      "description": "Start date for the search",
      "dataType": "DATE",
      "format": "yyyy-MM-dd",
      "multiple": false,
      "required": false,
      "defaultValue": "2026-07-01"
    }
  ]
}
```

The backend derives `required` from the template. An unlocked value-bearing
filter is required only when the saved template has no value. An unlocked filter
with a saved value is optional and that value acts as its default. Locked filters
are never exposed as overridable parameters.

### Start execution

```http
POST /mid/template/{templateId}/execute
```

```json
{
  "templateVersion": 3,
  "parameters": {
    "username": ["ivanov"]
  }
}
```

```json
{
  "executionId": "06a02258-736f-4ad6-a2e0-4259c66c8fe6",
  "templateId": "92de4773-7a00-4000-8000-000000000000",
  "status": "QUEUED",
  "message": null
}
```

The MCP server checks the template version, required keys, and unknown keys. The
backend remains the authoritative validator for value types, permissions, locked
filters, and query semantics.

### Poll status

```http
GET /mid/query/{executionId}/status
```

Expected status values are `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`, and
`CANCELLED`.

### Fetch a bounded result page

```http
GET /mid/query/{executionId}/result?offset=0&limit=20
```

```json
{
  "executionId": "06a02258-736f-4ad6-a2e0-4259c66c8fe6",
  "columns": [
    {
      "name": "Username",
      "type": "STRING"
    }
  ],
  "rows": [
    {
      "Username": "ivanov"
    }
  ],
  "totalRows": 1,
  "offset": 0,
  "returnedRows": 1,
  "truncated": false
}
```

The MCP tool limits a single result request to 100 rows so large tables are not
inserted into the LLM context in one response.
