# Search Engine MCP

Spring Boot MCP server for exposing search service topology sources to AI clients.

The server calls the existing backend endpoint:

```http
GET /topology/sources
```

and exposes the result through one MCP tool:

- `listSearchServiceSources` - lists data sources available in the search service topology.

The MCP response intentionally omits backend `status` and `timestamp` fields and
returns only the source list that is useful for an AI answer.

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
  -> MCP tool listSearchServiceSources
    -> HttpSearchServiceTopologyClient
      -> GET /topology/sources
        -> search service backend
```
