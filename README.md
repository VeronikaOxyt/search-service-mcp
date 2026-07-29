# Search Engine MCP

Spring Boot MCP server for exposing search service topology sources to AI clients.

The server calls the existing backend endpoint:

```http
GET /query/topology/sources
```

and exposes the result through MCP tools.

Topology tool:

- `listSearchServiceSources` - lists data sources available in the search service topology.

Saved-query template tools:

- `listSavedQueryTemplates` - lists personal or shared saved query templates;
- `getQueryTemplateParameters` - returns the typed execution schema for one template;
- `executeQueryTemplate` - fills empty unlocked filters and starts asynchronous execution;
- `getQueryResult` - checks execution readiness and returns a bounded page of table rows.

Templates are backend data, not dynamically registered MCP tools. A template created
in the web application is therefore available through `listSavedQueryTemplates` without
restarting or redeploying this MCP server.

The MCP response intentionally omits backend `status` and `timestamp` fields and
returns only the source list that is useful for an AI answer.

## MCP Transport

This project is not a regular REST API for AI clients. It uses the Spring AI
MCP WebMVC starter, which exposes MCP over HTTP/SSE:

```text
GET  /sse          opens the server-sent events stream
POST /mcp/message  receives MCP JSON-RPC messages
```

The AI client calls tools through JSON-RPC, for example:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "listSearchServiceSources",
    "arguments": {}
  }
}
```

The MCP tool then calls the backend REST endpoint internally:

```text
tools/call listSearchServiceSources
  -> GET /query/topology/sources
```

## Run

```bash
mvn spring-boot:run
```

The MCP server listens on port `8081`.

By default, it expects the search service backend at `http://localhost:8080`.
Override it with:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--search-service.base-url=http://localhost:8080
```

## Current Shape

```text
AI client
  -> ReportingTools / TemplateTools
    -> SearchServiceTopologyClient / TemplateExecutionService
      -> HttpSearchServiceTopologyClient / HttpTemplateBackendClient
        -> search service backend
```

## Template Backend Contract

The initial template implementation assumes these backend endpoints:

```text
POST /mid/template/list
GET  /mid/template?id={templateId}
GET  /mid/query/topology/structureTable
POST /mid/query/executeQuery
POST /mid/query/executeCrossQuery
POST /mid/query/result
POST /mid/query/crossResult
```

The MCP server builds the execution schema locally by recursively traversing the
template `where.filters` tree. It selects the execution and result endpoints
from `metaInfo.isCross`. Before execution it adds the display name, `templateId`,
and a new `rqUid` to the filled raw template. It converts `TimeRangeUI` to the
absolute `min/max` shape expected by execution endpoints. For a regular query it
also loads the selected table structure and adds every column marked
`isBaseColumn` to `baseColumns`. HTTP 425 and 426 mean that execution is still
pending; HTTP 200 contains the result page.
See [`docs/template-tools.md`](docs/template-tools.md) for request and response
examples and the complete MCP execution flow.
